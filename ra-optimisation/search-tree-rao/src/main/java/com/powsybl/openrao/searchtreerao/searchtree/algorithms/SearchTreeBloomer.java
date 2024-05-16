/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.commons.CountryGraph;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class SearchTreeBloomer {
    private final Network network;
    private final CountryGraph countryGraph;
    private final int maxRa;
    private final int maxTso;
    private final Map<String, Integer> maxTopoPerTso;
    private final Map<String, Integer> maxRaPerTso;
    private final Map<String, Integer> maxElementaryActionsPerTso;
    private final boolean filterFarElements;
    private final int maxNumberOfBoundariesForSkippingNetworkActions;
    private final List<NetworkActionCombination> preDefinedNaCombinations;
    private final State optimizedStateForNetworkActions;
    private final PrePerimeterResult prePerimeterResult;

    public SearchTreeBloomer(Network network,
                             int maxRa,
                             int maxTso,
                             Map<String, Integer> maxTopoPerTso,
                             Map<String, Integer> maxRaPerTso,
                             Map<String, Integer> maxElementaryActionsPerTso,
                             boolean filterFarElements,
                             int maxNumberOfBoundariesForSkippingNetworkActions,
                             List<NetworkActionCombination> preDefinedNaCombinations,
                             State optimizedStateForNetworkActions,
                             PrePerimeterResult prePerimeterResult) {
        this.network = network;
        countryGraph = new CountryGraph(network);
        this.maxRa = maxRa;
        this.maxTso = maxTso;
        this.maxTopoPerTso = maxTopoPerTso;
        this.maxRaPerTso = maxRaPerTso;
        this.maxElementaryActionsPerTso = maxElementaryActionsPerTso;
        this.filterFarElements = filterFarElements;
        this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
        this.preDefinedNaCombinations = preDefinedNaCombinations;
        this.optimizedStateForNetworkActions = optimizedStateForNetworkActions;
        this.prePerimeterResult = prePerimeterResult;
    }

    /**
     * This method generates a map of NetworkActionCombinations and associated boolean.
     * The networkActionCombinations generated would be available after this leaf inside the tree.
     * They are either individual NetworkAction as defined in the Crac, or predefined
     * combinations of NetworkActions, defined in the SearchTreeRaoParameters and considered as being efficient when
     * activated together.
     * If the associated boolean is false, the combination can be applied while keeping parentLeafRangeActions.
     * If it is true, parentLeafRangeActions must be removed before applying the combination.
     * <p>
     * Moreover, the bloom method ensure that the returned NetworkActionCombinations respect the following rules:
     * <ul>
     * <li>they do not exceed the maximum number of usable remedial actions</li>
     * <li>they do not exceed the maximum number of usable remedial actions (PST & topo) per operator</li>
     * <li>they do not exceed the maximum number of operators</li>
     * <li>they are not too far away from the most limiting CNEC</li>
     * </ul>
     */
    Map<NetworkActionCombination, Boolean> bloom(Leaf fromLeaf, Set<NetworkAction> networkActions) {

        // preDefined combinations
        Map<NetworkActionCombination, Boolean> networkActionCombinations = preDefinedNaCombinations.stream()
            .distinct()
            .filter(naCombination -> networkActions.containsAll(naCombination.getNetworkActionSet()))
            .collect(Collectors.toMap(naCombination -> naCombination, naCombination -> false));

        // + individual available Network Actions
        final List<NetworkActionCombination> finalNetworkActionCombinations = new ArrayList<>(networkActionCombinations.keySet());
        Map<NetworkActionCombination, Boolean> effectivelyFinalNACombinations = networkActionCombinations;
        networkActions.stream()
            .filter(na ->
                finalNetworkActionCombinations.stream().noneMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na))
            )
            .forEach(ra -> effectivelyFinalNACombinations.put(new NetworkActionCombination(Set.of(ra)), false));
        networkActionCombinations.putAll(effectivelyFinalNACombinations);

        // filters
        // (idea: create one class per filter which implement a common interface)
        networkActionCombinations = removeAlreadyActivatedNetworkActions(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeAlreadyTestedCombinations(networkActionCombinations, fromLeaf);

        networkActionCombinations = removeCombinationsWhichExceedMaxNumberOfRa(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeCombinationsWhichExceedMaxNumberOfRaPerTso(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeCombinationsWhichExceedMaxNumberOfTsos(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeCombinationsFarFromMostLimitingElement(networkActionCombinations, fromLeaf);
        networkActionCombinations = removeCombinationsWhichExceedMaxElementaryActionsPerTso(networkActionCombinations, fromLeaf);

        return networkActionCombinations;
    }

    Map<NetworkActionCombination, Boolean> removeAlreadyActivatedNetworkActions(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {
        return naCombinations.keySet().stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream().noneMatch(na -> fromLeaf.getActivatedNetworkActions().contains(na)))
            .collect(Collectors.toMap(naCombination -> naCombination, naCombinations::get));
    }

    /**
     * Remove combinations which have already been tested in the previous depths of the SearchTree.
     * <p>
     * For instance, if the preDefined combination ra1+ra2 exists, and if ra1 has already been selected, there is
     * no need to bloom on ra2. If the remedial action ra2 was relevant, the combination ra1+ra2 would have been
     * already selected in the previous depths.
     */
    Map<NetworkActionCombination, Boolean> removeAlreadyTestedCombinations(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {

        List<NetworkAction> alreadyTestedNetworkActions = new ArrayList<>();

        for (NetworkActionCombination preDefinedCombination : preDefinedNaCombinations) {
            if (preDefinedCombination.isDetectedDuringRao()) {
                continue;
            }

            // elements of the combination which have not been activated yet
            List<NetworkAction> notTestedNaInCombination = preDefinedCombination.getNetworkActionSet().stream()
                .filter(na -> !fromLeaf.getActivatedNetworkActions().contains(na))
                .toList();

            // if all the actions of the combinations have been selected but one, there is no need
            // to test that individual action anymore
            if (notTestedNaInCombination.size() == 1) {
                alreadyTestedNetworkActions.add(notTestedNaInCombination.get(0));
            }
        }

        return naCombinations.keySet().stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().size() != 1
                || !alreadyTestedNetworkActions.contains(naCombination.getNetworkActionSet().iterator().next()))
            .collect(Collectors.toMap(naCombination -> naCombination, naCombinations::get));
    }

    /**
     * For each network actions combination, two checks are carried out:
     * <ol>
     *     <li>We ensure that the cumulated number of network actions in the combination and already applied network actions in the root leaf does not exceed the limit number of remedial actions so the applied network actions can be kept</li>
     *     <li>If so, we also need to ensure that the cumulated number of network actions (combination + root leaf) and range actions (root leaf) does not exceed the limit number of remedial actions, so we know whether keeping the network actions requires unapplying the range actions or not.</li>
     * </ol>
     * If the first condition is not met, the combination is not kept. If the second condition is not met, the combination is kept but the range actions will be unapplied for the next optimization.
     */
    Map<NetworkActionCombination, Boolean> removeCombinationsWhichExceedMaxNumberOfRa(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {

        Map<NetworkActionCombination, Boolean> filteredNaCombinations = new HashMap<>();
        for (Map.Entry<NetworkActionCombination, Boolean> entry : naCombinations.entrySet()) {
            NetworkActionCombination naCombination = entry.getKey();
            int naCombinationSize = naCombination.getNetworkActionSet().size();
            int alreadyActivatedNetworkActionsSize = fromLeaf.getActivatedNetworkActions().size();
            if (naCombinationSize + alreadyActivatedNetworkActionsSize <= maxRa) {
                boolean removeRangeActions = alreadyActivatedNetworkActionsSize + fromLeaf.getNumberOfActivatedRangeActions() + naCombinationSize > maxRa;
                filteredNaCombinations.put(naCombination, removeRangeActions || naCombinations.get(naCombination));
            }
        }

        if (naCombinations.size() > filteredNaCombinations.size()) {
            TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the max number of usable RAs has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    /**
     * For each network actions combination, we iterate on the TSOs that operate the network actions, and for each one of them, two checks are carried out:
     * <ol>
     *     <li>We ensure that the cumulated number of network actions in the combination and already applied network actions in the root leaf does not exceed the limit number of remedial actions that the TSO can apply so the applied network actions can be kept</li>
     *     <li>If so, we also need to ensure that the cumulated number of network actions (combination + root leaf) and range actions (root leaf) does not exceed the limit number of remedial actions that the TSO can apply, so we know whether keeping the TSO's network actions requires unapplying the TSO's range actions or not.</li>
     * </ol>
     * If the first condition is not met for at least one TSO, the combination is not kept. If the second condition is not met for at least one TSO, the combination is kept but the range actions will be unapplied for the next optimization.
     */
    Map<NetworkActionCombination, Boolean> removeCombinationsWhichExceedMaxNumberOfRaPerTso(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {

        Map<NetworkActionCombination, Boolean> filteredNaCombinations = new HashMap<>();
        Map<String, Integer> maxNaPerTso = getMaxNetworkActionPerTso(fromLeaf);
        for (Map.Entry<NetworkActionCombination, Boolean> entry : naCombinations.entrySet()) {
            NetworkActionCombination naCombination = entry.getKey();
            Set<String> operators = naCombination.getOperators();
            boolean naShouldBeKept = true;
            boolean removeRangeActions = false;
            for (String tso : operators) {
                int naCombinationSize = (int) naCombination.getNetworkActionSet().stream().filter(networkAction -> tso.equals(networkAction.getOperator())).count();
                int numberOfAlreadyActivatedRangeActionsForTso = (int) fromLeaf.getActivatedRangeActions(optimizedStateForNetworkActions).stream().filter(ra -> tso.equals(ra.getOperator())).count();
                int numberOfAlreadyAppliedNetworkActionsForTso = (int) fromLeaf.getActivatedNetworkActions().stream().filter(na -> tso.equals(na.getOperator())).count();
                // The number of already applied network actions is taken in account in getMaxNetworkActionPerTso
                if (naCombinationSize > maxNaPerTso.getOrDefault(tso, Integer.MAX_VALUE)) {
                    naShouldBeKept = false;
                    break;
                } else if (numberOfAlreadyAppliedNetworkActionsForTso + numberOfAlreadyActivatedRangeActionsForTso + naCombinationSize > maxRaPerTso.getOrDefault(tso, Integer.MAX_VALUE)) {
                    removeRangeActions = true;
                }
            }
            if (naShouldBeKept) {
                filteredNaCombinations.put(naCombination, removeRangeActions || naCombinations.get(naCombination));
            }
        }

        if (naCombinations.size() > filteredNaCombinations.size()) {
            TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the maximum number of network actions for their TSO has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    Map<NetworkActionCombination, Boolean> removeCombinationsWhichExceedMaxNumberOfTsos(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {

        Set<String> alreadyActivatedTsos = getTsosWithActivatedNetworkActions(fromLeaf);
        Map<NetworkActionCombination, Boolean> filteredNaCombinations = new HashMap<>();
        for (Map.Entry<NetworkActionCombination, Boolean> entry : naCombinations.entrySet()) {
            NetworkActionCombination naCombination = entry.getKey();
            if (!exceedMaxNumberOfTsos(naCombination, alreadyActivatedTsos)) {
                Set<String> alreadyActivatedTsosWithRangeActions = new HashSet<>(alreadyActivatedTsos);
                fromLeaf.getActivatedRangeActions(optimizedStateForNetworkActions)
                    .stream().map(RemedialAction::getOperator)
                    .filter(Objects::nonNull)
                    .forEach(alreadyActivatedTsosWithRangeActions::add);
                boolean removeRangeActions = exceedMaxNumberOfTsos(naCombination, alreadyActivatedTsosWithRangeActions);
                filteredNaCombinations.put(naCombination, removeRangeActions || naCombinations.get(naCombination));
            }
        }

        if (naCombinations.size() > filteredNaCombinations.size()) {
            TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the max number of usable TSOs has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    /**
     * Removes network actions far from most limiting elements, using the user's parameters for activating/deactivating this
     * feature, and setting the number of boundaries allowed between the network action and the limiting element.
     * The most limiting elements are the most limiting functional cost element, and all elements with a non-zero virtual cost.
     */
    Map<NetworkActionCombination, Boolean> removeCombinationsFarFromMostLimitingElement(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {

        if (!filterFarElements) {
            return naCombinations;
        }

        Set<Optional<Country>> worstCnecLocation = getOptimizedMostLimitingElementsLocation(fromLeaf);

        Map<NetworkActionCombination, Boolean> filteredNaCombinations = naCombinations.keySet().stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream().anyMatch(na -> isNetworkActionCloseToLocations(na, worstCnecLocation, countryGraph)))
                .collect(Collectors.toMap(naCombination -> naCombination, naCombinations::get));

        if (naCombinations.size() > filteredNaCombinations.size()) {
            TECHNICAL_LOGS.info("{} network action combinations have been filtered out because they are too far from the most limiting element", naCombinations.size() - filteredNaCombinations.size());
        }
        return filteredNaCombinations;
    }

    Map<NetworkActionCombination, Boolean> removeCombinationsWhichExceedMaxElementaryActionsPerTso(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {
        // TODO : pst injection = 1 ea or abs(newtap - initialtap)?
        Map<NetworkActionCombination, Boolean> filteredNaCombinations = new HashMap<>();
        Map<String, Integer> movedPstTapsPerTso = getNumberOfPstTapsMovedByTso(fromLeaf);
        naCombinations.forEach((naCombination, mustBeFiltered) -> {
            int elementaryActions = 0;
            for (NetworkAction networkAction : naCombination.getNetworkActionSet()) {
                // TODO: what if some network actions share common elementary action?
                elementaryActions = elementaryActions + networkAction.getElementaryActions().size();
            }
            boolean combinationHasBeenRemoved = false;
            for (String operator : naCombination.getOperators()) {
                // The network action combination alone has more elementary action than the accepted limit, so it must be removed
                if (elementaryActions > maxElementaryActionsPerTso.getOrDefault(operator, Integer.MAX_VALUE)) {
                    TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the maximum number of elementary actions for TSO {} has been reached", naCombinations.size() - filteredNaCombinations.size(), operator);
                    combinationHasBeenRemoved = true;
                    break;
                }
            }
            if (!combinationHasBeenRemoved) {
                boolean shouldRemovePsts = false;
                for (String operator : naCombination.getOperators()) {
                    // The network action combination has less elementary actions than the limit but PST range actions must be removed first
                    shouldRemovePsts = shouldRemovePsts || elementaryActions + movedPstTapsPerTso.getOrDefault(operator, 0) > maxElementaryActionsPerTso.getOrDefault(operator, Integer.MAX_VALUE);
                }
                filteredNaCombinations.put(naCombination, mustBeFiltered || shouldRemovePsts);
            }
        });
        return filteredNaCombinations;
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
            int activatedTopoForTso = (int) fromLeaf.getActivatedNetworkActions().stream().filter(networkAction -> tso.equals(networkAction.getOperator())).count();

            int limitationDueToMaxRa = maxRaPerTso.getOrDefault(tso, Integer.MAX_VALUE) - activatedTopoForTso;
            int limitationDueToMaxTopo = maxTopoPerTso.getOrDefault(tso, Integer.MAX_VALUE) - activatedTopoForTso;

            updatedMaxTopoPerTso.put(tso, Math.min(limitationDueToMaxRa, limitationDueToMaxTopo));
        });
        return updatedMaxTopoPerTso;
    }

    private boolean exceedMaxNumberOfTsos(NetworkActionCombination naCombination, Set<String> alreadyActivatedTsos) {
        Set<String> involvedTsos = naCombination.getOperators();
        involvedTsos.addAll(alreadyActivatedTsos);
        return involvedTsos.size() > maxTso;
    }

    Set<String> getTsosWithActivatedNetworkActions(Leaf leaf) {
        return leaf.getActivatedNetworkActions().stream().map(RemedialAction::getOperator).filter(Objects::nonNull).collect(Collectors.toSet());
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

    boolean hasPreDefinedNetworkActionCombination(NetworkActionCombination naCombination) {
        return this.preDefinedNaCombinations.contains(naCombination);
    }

    Map<String, Integer> getNumberOfPstTapsMovedByTso(Leaf leaf) {
        Map<String, Integer> pstTapsMovedByTso = new HashMap<>();
        Set<PstRangeAction> activatedRangeActions = leaf.getActivatedRangeActions(optimizedStateForNetworkActions).stream().filter(PstRangeAction.class::isInstance).map(ra -> (PstRangeAction) ra).collect(Collectors.toSet());
        for (PstRangeAction pstRangeAction : activatedRangeActions) {
            String operator = pstRangeAction.getOperator();
            int tapsMoved = Math.abs(leaf.getOptimizedTap(pstRangeAction, optimizedStateForNetworkActions) - prePerimeterResult.getTap(pstRangeAction));
            pstTapsMovedByTso.put(operator, pstTapsMovedByTso.getOrDefault(operator, 0) + tapsMoved);
        }
        return pstTapsMovedByTso;
    }
}
