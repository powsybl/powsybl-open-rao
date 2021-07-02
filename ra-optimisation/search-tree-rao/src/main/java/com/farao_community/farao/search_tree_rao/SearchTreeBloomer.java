/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.CountryGraph;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class SearchTreeBloomer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeBloomer.class);

    private final Network network;
    private final CountryGraph countryGraph;
    private final RangeActionResult prePerimeterRangeActionResult;
    private final int maxRa;
    private final int maxTso;
    private final Map<String, Integer> maxTopoPerTso;
    private final Map<String, Integer> maxRaPerTso;
    private final boolean filterFarElements;
    private final int maxNumberOfBoundariesForSkippingNetworkActions;

    public SearchTreeBloomer(Network network,
                             RangeActionResult prePerimeterRangeActionResult,
                             int maxRa,
                             int maxTso,
                             Map<String, Integer> maxTopoPerTso,
                             Map<String, Integer> maxRaPerTso,
                             boolean filterFarElements,
                             int maxNumberOfBoundariesForSkippingNetworkActions) {
        this.network = network;
        countryGraph = new CountryGraph(network);
        this.prePerimeterRangeActionResult = prePerimeterRangeActionResult;
        this.maxRa = maxRa;
        this.maxTso = maxTso;
        this.maxTopoPerTso = maxTopoPerTso;
        this.maxRaPerTso = maxRaPerTso;
        this.filterFarElements = filterFarElements;
        this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
    }

    /**
     * This method generates a set a of network actions that would be available after this leaf inside the tree. It
     * means all the available network actions in the CRAC except the ones already used in this leaf.
     *
     * @return A set of available network actions after this leaf.
     */
    public Set<NetworkAction> bloom(Leaf fromLeaf, Set<NetworkAction> networkActions) {
        int numberOfRangeActionsUsed = getNumberOfRangeActionsUsed(fromLeaf);
        int numberOfNetworkActionsUsed = fromLeaf.getActivatedNetworkActions().size();
        if (numberOfNetworkActionsUsed + numberOfRangeActionsUsed == maxRa) {
            return new HashSet<>();
        }
        Set<NetworkAction> availableNetworkActions = new HashSet<>(networkActions).stream()
                .filter(na -> !fromLeaf.getActivatedNetworkActions().contains(na))
                .collect(Collectors.toSet());
        Set<String> activatedTsos = getActivatedTsos(fromLeaf);
        if (activatedTsos.size() == maxTso) {
            availableNetworkActions = removeNetworkActionsTsoNotInSet(availableNetworkActions, activatedTsos);
        }
        if (filterFarElements) {
            availableNetworkActions = removeNetworkActionsFarFromMostLimitingElement(fromLeaf, availableNetworkActions);
        }
        availableNetworkActions = removeNetworkActionsIfMaxNumberReached(fromLeaf, availableNetworkActions);
        return availableNetworkActions;
    }

    private int getNumberOfRangeActionsUsed(Leaf fromLeaf) {
        return (int) fromLeaf.getRangeActions().stream()
            .filter(rangeAction -> hasRangeActionChangedComparedToPrePerimeter(fromLeaf, rangeAction))
            .count();
    }

    /**
     * Removes network actions for whom the maximum number of network actions has been reached
     *
     * @param networkActionsToFilter: the set of network actions to reduce
     * @return the reduced set of network actions
     */
    Set<NetworkAction> removeNetworkActionsIfMaxNumberReached(Leaf fromLeaf, Set<NetworkAction> networkActionsToFilter) {
        Set<NetworkAction> filteredNetworkActions = new HashSet<>(networkActionsToFilter);
        getMaxTopoPerTso(fromLeaf).forEach((String tso, Integer maxTopo) -> {
            long alreadyAppliedForTso = fromLeaf.getActivatedNetworkActions().stream().filter(networkAction -> networkAction.getOperator().equals(tso)).count();
            if (alreadyAppliedForTso >= maxTopo) {
                filteredNetworkActions.removeIf(networkAction -> networkAction.getOperator().equals(tso));
            }
        });
        if (networkActionsToFilter.size() > filteredNetworkActions.size()) {
            LOGGER.info("{} network actions have been filtered out because the maximum number of network actions for their TSO has been reached", networkActionsToFilter.size() - filteredNetworkActions.size());
        }
        return filteredNetworkActions;
    }

    /**
     * This function computes the allowed number of network actions for each TSO, as the minimum between the given
     * parameter and the maximum number of RA reduced by the number of PSTs already used
     */
    Map<String, Integer> getMaxTopoPerTso(Leaf fromLeaf) {
        Map<String, Integer> updatedMaxTopoPerTso = new HashMap<>(maxTopoPerTso);
        maxRaPerTso.forEach((tso, raLimit) -> {
            int activatedPstsForTso = (int) fromLeaf.getRangeActions().stream()
                    .filter(rangeAction -> rangeAction instanceof PstRangeAction && hasRangeActionChangedComparedToPrePerimeter(fromLeaf, rangeAction))
                    .count();
            int topoLimit =  raLimit - activatedPstsForTso;
            updatedMaxTopoPerTso.put(tso, Math.min(topoLimit, maxTopoPerTso.getOrDefault(tso, Integer.MAX_VALUE)));
        });
        return updatedMaxTopoPerTso;
    }

    private boolean hasRangeActionChangedComparedToPrePerimeter(Leaf fromLeaf, RangeAction rangeAction) {
        double optimizedSetPoint = fromLeaf.getOptimizedSetPoint(rangeAction);
        double prePerimeterSetPoint = prePerimeterRangeActionResult.getOptimizedSetPoint(rangeAction);
        if (Double.isNaN(optimizedSetPoint)) {
            return false;
        } else if (Double.isNaN(prePerimeterSetPoint)) {
            return true;
        } else {
            return Math.abs(optimizedSetPoint - prePerimeterSetPoint) > 1e-6;
        }
    }

    /**
     * Removes network actions far from most limiting elements, using the user's parameters for activating/deactivating this
     * feature, and setting the number of boundaries allowed between the network action and the limiting element.
     * The most limiting elements are the most limiting functional cost element, and all elements with a non-zero virtual cost.
     *
     * @param networkActionsToFilter: the set of network actions to reduce
     * @return the reduced set of network actions
     */
    Set<NetworkAction> removeNetworkActionsFarFromMostLimitingElement(Leaf leaf, Set<NetworkAction> networkActionsToFilter) {
        Set<Optional<Country>> worstCnecLocation = getOptimizedMostLimitingElementsLocation(leaf);
        Set<NetworkAction> filteredNetworkActions = networkActionsToFilter.stream()
                .filter(na -> isNetworkActionCloseToLocations(na, worstCnecLocation, countryGraph))
                .collect(Collectors.toSet());
        if (networkActionsToFilter.size() > filteredNetworkActions.size()) {
            LOGGER.info("{} network actions have been filtered out because they are far from the most limiting element", networkActionsToFilter.size() - filteredNetworkActions.size());
        }
        return filteredNetworkActions;
    }

    /**
     * Says if a network action is close to a given set of countries, respecting the maximum number of boundaries
     */
    boolean isNetworkActionCloseToLocations(NetworkAction networkAction, Set<Optional<Country>> locations, CountryGraph countryGraph) {
        if (locations.stream().anyMatch(Optional::isEmpty)) {
            return true;
        }
        Set<Optional<Country>> networkActionCountries = networkAction.getLocation(network);
        if (networkActionCountries.stream().anyMatch(Optional::isEmpty)) {
            return true;
        }
        for (Optional<Country> location : locations) {
            for (Optional<Country> networkActionCountry : networkActionCountries) {
                if (location.isPresent() && networkActionCountry.isPresent()
                        && countryGraph.areNeighbors(location.get(), networkActionCountry.get(), maxNumberOfBoundariesForSkippingNetworkActions)) {
                    return true;
                }
            }
        }
        return false;
    }

    Set<Optional<Country>> getOptimizedMostLimitingElementsLocation(Leaf leaf) {
        Set<Optional<Country>> locations = new HashSet<>();
        leaf.getMostLimitingElements(1).forEach(element -> locations.addAll(element.getLocation(network)));
        for (String virtualCost : leaf.getVirtualCostNames()) {
            leaf.getCostlyElements(virtualCost, Integer.MAX_VALUE).forEach(element -> locations.addAll(element.getLocation(network)));
        }
        return locations;
    }

    Set<NetworkAction> removeNetworkActionsTsoNotInSet(Set<NetworkAction> networkActionsToFilter, Set<String> tsosToKeep) {
        Set<NetworkAction> filteredNetworkActions = networkActionsToFilter.stream().filter(networkAction -> tsosToKeep.contains(networkAction.getOperator())).collect(Collectors.toSet());
        if (networkActionsToFilter.size() > filteredNetworkActions.size()) {
            LOGGER.info("{} network actions have been filtered out because the max number of usable TSOs has been reached", networkActionsToFilter.size() - filteredNetworkActions.size());
        }
        return filteredNetworkActions;
    }

    Set<String> getActivatedTsos(Leaf leaf) {
        Set<String> activatedTsos = leaf.getActivatedNetworkActions().stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
        activatedTsos.addAll(leaf.getRangeActions().stream().filter(rangeaction -> hasRangeActionChangedComparedToPrePerimeter(leaf, rangeaction))
                .map(RemedialAction::getOperator).collect(Collectors.toSet()));
        return activatedTsos;
    }
}
