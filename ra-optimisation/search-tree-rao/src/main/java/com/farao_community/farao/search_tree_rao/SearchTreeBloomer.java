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
final class SearchTreeBloomer {
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
    private final List<NetworkActionCombination> preDefinedNaCombinations;

    SearchTreeBloomer(Network network,
                             RangeActionResult prePerimeterRangeActionResult,
                             int maxRa,
                             int maxTso,
                             Map<String, Integer> maxTopoPerTso,
                             Map<String, Integer> maxRaPerTso,
                             boolean filterFarElements,
                             int maxNumberOfBoundariesForSkippingNetworkActions,
                             List<NetworkActionCombination> preDefinedNaCombinations) {

        this.network = network;
        countryGraph = new CountryGraph(network);
        this.prePerimeterRangeActionResult = prePerimeterRangeActionResult;
        this.maxRa = maxRa;
        this.maxTso = maxTso;
        this.maxTopoPerTso = maxTopoPerTso;
        this.maxRaPerTso = maxRaPerTso;
        this.filterFarElements = filterFarElements;
        this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
        this.preDefinedNaCombinations = preDefinedNaCombinations;
    }

    /**
     * This method generates a list a of NetworkActionCombinations that would be available after this leaf inside the tree.
     * The returned NetworkActionCombination are either individual NetworkAction as defined in the Crac, or predefined
     * combinations of NetworkActions, defined in the SearchTreeRaoParameters and considered as being efficient when
     * activated together.
     *
     * Moreover, the bloom method ensure that the returned NetworkActionCombinations respect the following rules:
     * - they do not exceed the maximum number of usable remedial actions
     * - they do not exceed the maximum number of usable remedial actions (PST & topo) per operator
     * - they do not exceed the maximum number of operators
     * - they are not too far away from the most limiting CNEC
     */
    List<NetworkActionCombination> bloom(Leaf fromLeaf, Set<NetworkAction> networkActions) {

        // preDefined combinations
        List<NetworkActionCombination> networkActionCombinations = preDefinedNaCombinations.stream()
            .filter(naCombination -> networkActions.containsAll(naCombination.getNetworkActionSet()))
            .collect(Collectors.toList());

        // + individual available Network Actions
        networkActionCombinations.addAll(networkActions.stream()
                .map(NetworkActionCombination::new)
                .collect(Collectors.toList()));

        // filters
        // (idea: create one class per filter which implement a common interface)
        networkActionCombinations = removeAlreadyActivatedNetworkActions(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeAlreadyTestedCombinations(networkActionCombinations, fromLeaf);

        networkActionCombinations = removeCombinationsWhichExceedMaxNumberOfRa(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeCombinationsWhichExceedMaxNumberOfRaPerTso(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeCombinationsWhichExceedMaxNumberOfTsos(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeCombinationsFarFromMostLimitingElement(networkActionCombinations, fromLeaf);

        return networkActionCombinations;
    }

    List<NetworkActionCombination> removeAlreadyActivatedNetworkActions(List<NetworkActionCombination> naCombinations, Leaf fromLeaf) {
        return naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream().noneMatch(na -> fromLeaf.getActivatedNetworkActions().contains(na)))
            .collect(Collectors.toList());
    }

    /**
     * Remove combinations which have already been tested in the previous depths of the SearchTree.
     *
     * For instance, if the preDefined combination ra1+ra2 exists, and if ra1 has already been selected, there is
     * no need to bloom on ra2. If the remedial action ra2 was relevant, the combination ra1+ra2 would have been
     * already selected in the previous depths.
     */
    List<NetworkActionCombination> removeAlreadyTestedCombinations(List<NetworkActionCombination> naCombinations, Leaf fromLeaf) {

        List<NetworkAction> alreadyTestedNetworkActions = new ArrayList<>();

        for (NetworkActionCombination preDefinedCombination : preDefinedNaCombinations) {

            // elements of the combination which have not been activated yet
            List<NetworkAction> notTestedNaInCombination = preDefinedCombination.getNetworkActionSet().stream()
                .filter(na -> !fromLeaf.getActivatedNetworkActions().contains(na))
                .collect(Collectors.toList());

            // if all the actions of the combinations have been selected but one, there is no need
            // to test that individual action anymore
            if (notTestedNaInCombination.size() == 1) {
                alreadyTestedNetworkActions.add(notTestedNaInCombination.get(0));
            }
        }

        return naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().size() != 1
                || !alreadyTestedNetworkActions.contains(naCombination.getNetworkActionSet().iterator().next()))
            .collect(Collectors.toList());
    }

    List<NetworkActionCombination> removeCombinationsWhichExceedMaxNumberOfRa(List<NetworkActionCombination> naCombinations, Leaf fromLeaf) {

        int numberOfRangeActionsUsed = getNumberOfRangeActionsUsed(fromLeaf);
        int numberOfNetworkActionsUsed = fromLeaf.getActivatedNetworkActions().size();

        List<NetworkActionCombination> filteredNaCombinations = naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().size() + numberOfRangeActionsUsed + numberOfNetworkActionsUsed <= maxRa)
            .collect(Collectors.toList());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            LOGGER.info("{} network action combinations have been filtered out because the max number of usable RAs has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    List<NetworkActionCombination> removeCombinationsWhichExceedMaxNumberOfRaPerTso(List<NetworkActionCombination> naCombinations, Leaf fromLeaf) {

        Map<String, Integer> maxNaPerTso = getMaxNetworkActionPerTso(fromLeaf);

        List<NetworkActionCombination> filteredNaCombinations = naCombinations.stream()
            .filter(naCombination -> !exceedMaxNumberOfRaPerTso(naCombination, maxNaPerTso))
            .collect(Collectors.toList());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            LOGGER.info("{} network action combinations have been filtered out because the maximum number of network actions for their TSO has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    List<NetworkActionCombination> removeCombinationsWhichExceedMaxNumberOfTsos(List<NetworkActionCombination> naCombinations, Leaf fromLeaf) {

        Set<String> alreadyActivatedTsos = getActivatedTsos(fromLeaf);

        List<NetworkActionCombination> filteredNaCombinations = naCombinations.stream()
            .filter(naCombination -> !exceedMaxNumberOfTsos(naCombination, alreadyActivatedTsos))
            .collect(Collectors.toList());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            LOGGER.info("{} network action combinations have been filtered out because the max number of usable TSOs has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    /**
     * Removes network actions far from most limiting elements, using the user's parameters for activating/deactivating this
     * feature, and setting the number of boundaries allowed between the network action and the limiting element.
     * The most limiting elements are the most limiting functional cost element, and all elements with a non-zero virtual cost.
     */
    List<NetworkActionCombination> removeCombinationsFarFromMostLimitingElement(List<NetworkActionCombination> naCombinations, Leaf fromLeaf) {

        if (!filterFarElements) {
            return naCombinations;
        }

        Set<Optional<Country>> worstCnecLocation = getOptimizedMostLimitingElementsLocation(fromLeaf);

        List<NetworkActionCombination> filteredNaCombinations = naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream().anyMatch(na -> isNetworkActionCloseToLocations(na, worstCnecLocation, countryGraph)))
            .collect(Collectors.toList());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            LOGGER.info("{} network action combinations have been filtered out because they are too far from the most limiting element", naCombinations.size() - filteredNaCombinations.size());
        }
        return filteredNaCombinations;
    }

    private int getNumberOfRangeActionsUsed(Leaf fromLeaf) {
        return (int) fromLeaf.getRangeActions().stream()
            .filter(rangeAction -> hasRangeActionChangedComparedToPrePerimeter(fromLeaf, rangeAction))
            .count();
    }

    private boolean exceedMaxNumberOfRaPerTso(NetworkActionCombination naCombination, Map<String, Integer> maxNaPerTso) {
        return naCombination.getOperators().stream().anyMatch(operator -> {
            int numberOfActionForTso = (int) naCombination.getNetworkActionSet().stream().filter(na -> na.getOperator().equals(operator)).count();
            return numberOfActionForTso > maxNaPerTso.getOrDefault(operator, Integer.MAX_VALUE);
        });
    }

    /**
     * This function computes the allowed number of network actions for each TSO, as the minimum between the given
     * parameter and the maximum number of RA reduced by the number of remedial actions already used
     */
    private Map<String, Integer> getMaxNetworkActionPerTso(Leaf fromLeaf) {
        Map<String, Integer> updatedMaxTopoPerTso = new HashMap<>();

        // get set of all TSOs considered in the max number of RA limitation
        Set<String> tsos = new HashSet<>(maxRaPerTso.keySet());
        tsos.addAll(maxTopoPerTso.keySet());

        // get max number of network action which can still be activated, per Tso
        tsos.forEach(tso -> {
            int activatedPstsForTso = (int) fromLeaf.getRangeActions().stream().filter(rangeAction -> rangeAction instanceof PstRangeAction && hasRangeActionChangedComparedToPrePerimeter(fromLeaf, rangeAction)).count();
            int activatedTopoForTso = (int) fromLeaf.getActivatedNetworkActions().stream().filter(networkAction -> networkAction.getOperator().equals(tso)).count();

            int limitationDueToMaxRa =  maxRaPerTso.getOrDefault(tso, Integer.MAX_VALUE) - activatedPstsForTso - activatedTopoForTso;
            int limitationDueToMaxTopo =  maxTopoPerTso.getOrDefault(tso, Integer.MAX_VALUE) - activatedTopoForTso;

            updatedMaxTopoPerTso.put(tso, Math.min(limitationDueToMaxRa, limitationDueToMaxTopo));
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

    private boolean exceedMaxNumberOfTsos(NetworkActionCombination naCombination, Set<String> alreadyActivatedTsos) {
        Set<String> involvedTsos = naCombination.getOperators();
        involvedTsos.addAll(alreadyActivatedTsos);
        return involvedTsos.size() > maxTso;
    }

    Set<String> getActivatedTsos(Leaf leaf) {
        Set<String> activatedTsos = leaf.getActivatedNetworkActions().stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
        activatedTsos.addAll(leaf.getRangeActions().stream().filter(rangeaction -> hasRangeActionChangedComparedToPrePerimeter(leaf, rangeaction))
            .map(RemedialAction::getOperator).collect(Collectors.toSet()));
        return activatedTsos;
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
}
