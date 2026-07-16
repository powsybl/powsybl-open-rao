/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.timecoupledsearchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.inputs.TimeCoupledSearchTreeInput;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.parameters.SearchTreeParameters;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates the network actions combinations tested at every depth of the search tree.
 * In time coupled, a single combination is applied simultaneously on every timestamp's network.
 *  <li> RaUsageLimits are considered common between all the timestamps.
 *  <li> {@link TimeCoupledFarFromMostLimitingElementFilter} because with the global objective function, the most limiting
 *  elements can belong to any timestamp, so each CNEC's location is resolved on its own timestamp's network
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class TimeCoupledSearchTreeBloomer {

    private final List<NetworkActionCombination> preDefinedNaCombinations;
    private final List<NetworkActionCombinationFilter> networkActionCombinationFilters;
    private final TimeCoupledSearchTreeInput input;
    private final RaUsageLimits raUsageLimits;

    public TimeCoupledSearchTreeBloomer(TimeCoupledSearchTreeInput input,
                                        SearchTreeParameters parameters,
                                        RaUsageLimits raUsageLimits) {
        // raUsageLimits are considered shared amongst all the timestamps
        this.raUsageLimits = raUsageLimits;
        this.preDefinedNaCombinations = parameters.getNetworkActionParameters().getNetworkActionCombinations();
        this.networkActionCombinationFilters = new ArrayList<>(List.of(
            new AlreadyAppliedNetworkActionsFilter(),
            new AlreadyTestedCombinationsFilter(preDefinedNaCombinations),
            new MaximumNumberOfRemedialActionsFilter(raUsageLimits.getMaxRa()),
            new MaximumNumberOfRemedialActionPerTsoFilter(raUsageLimits.getMaxTopoPerTso(), raUsageLimits.getMaxRaPerTso()),
            new ElementaryActionsCompatibilityFilter(),
            new MaximumNumberOfElementaryActionsFilter(raUsageLimits.getMaxElementaryActionsPerTso()))
        );
        if (parameters.getNetworkActionParameters().skipNetworkActionFarFromMostLimitingElements()) {
            // with the global objective function, the most limiting elements can belong to any timestamp. Each CNEC's location must be resolved on its own timestamp's network
            this.networkActionCombinationFilters.add(
                    new TimeCoupledFarFromMostLimitingElementFilter(input.getNetworks(), parameters.getNetworkActionParameters().getMaxNumberOfBoundariesForSkippingNetworkActions())
            );
        }
        this.input = input;
    }

    /**
     * This method generates a Set of NetworkActionCombinations.
     * The networkActionCombinations generated would be available after this leaf inside the tree.
     * They are either individual NetworkAction as defined in the Crac, or predefined
     * combinations of NetworkActions, defined in the SearchTreeRaoParameters and considered as being efficient when
     * activated together.
     * The bloom method ensures that the returned NetworkActionCombinations respect the following rules:
     * <ul>
     * <li>they do not exceed the maximum number of usable remedial actions</li>
     * <li>they do not exceed the maximum number of usable remedial actions (PST & topo) per operator</li>
     * <li>they do not exceed the maximum number of operators</li>
     * <li>they are not too far away from the most limiting CNEC</li>
     * </ul>
     */
    Set<NetworkActionCombination> bloom(final TimeCoupledLeaf fromLeaf, final Set<NetworkAction> networkActions, final ReportNode reportNode) {

        // preDefined combinations
        Set<NetworkActionCombination> networkActionCombinations = preDefinedNaCombinations.stream()
            .distinct()
            .filter(naCombination -> networkActions.containsAll(naCombination.getNetworkActionSet()))
            .collect(Collectors.toSet());

        // + individual available Network Actions
        final List<NetworkActionCombination> finalNetworkActionCombinations = new ArrayList<>(networkActionCombinations);
        Set<NetworkActionCombination> effectivelyFinalNACombinations = networkActionCombinations;
        networkActions.stream()
            .filter(na ->
                finalNetworkActionCombinations.stream().noneMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na))
            )
            .forEach(ra -> effectivelyFinalNACombinations.add(new NetworkActionCombination(Set.of(ra))));
        networkActionCombinations.addAll(effectivelyFinalNACombinations);

        // filters
        for (NetworkActionCombinationFilter networkActionCombinationFilter : networkActionCombinationFilters) {
            networkActionCombinations = networkActionCombinationFilter.filter(networkActionCombinations, fromLeaf, reportNode);
        }

        return networkActionCombinations;
    }

    /**
     * This method checks if range action must be removed before applying a network action combination.
     * If so, parentLeafRangeActions must be removed before applying the combination.
     * Otherwise, it can be applied while keeping them.
     * Such a check is performed by analyzing RaUsageLimits for the given state.
     *
     * In time coupled :
     * because the same network action combination is applied on every timestamp, the ra usage limits are checked
     * on every timestamp's perimeter, one violation in one timestamp -> removing the range action.
     */
    boolean shouldRangeActionsBeRemovedToApplyNa(NetworkActionCombination naCombination, OptimizationResult optimizationResult) {
        return input.getOptimizationPerimeters().getDataPerTimestamp().entrySet().stream().anyMatch(optimizationPerimeter ->
            naCombinationExceedsRaUsageLimitsOnPerimeter(
                optimizationPerimeter.getValue(),
                naCombination,
                optimizationResult,
                getNumberOfPstTapsMovedByTso(optimizationResult, optimizationPerimeter.getKey(), optimizationPerimeter.getValue())));
    }

    private boolean naCombinationExceedsRaUsageLimitsOnPerimeter(OptimizationPerimeter optimizationPerimeter,
                                                                 NetworkActionCombination naCombination,
                                                                 OptimizationResult optimizationResult,
                                                                 Map<String, Integer> movedPstTapsPerTso) {
        State optimizationState = optimizationPerimeter.getMainOptimizationState();

        // maxRa
        int naCombinationSize = naCombination.getNetworkActionSet().size();
        Set<NetworkAction> alreadyActivatedNetworkActions = optimizationResult.getActivatedNetworkActions(); // shared across all the timestamps
        Set<RangeAction<?>> alreadyActivatedRangeActions = optimizationResult.getActivatedRangeActions(optimizationState);  // defined per timestamp
        if (alreadyActivatedNetworkActions.size() + alreadyActivatedRangeActions.size() + naCombinationSize > raUsageLimits.getMaxRa()) {
            return true;
        }

        // maxRaPerTso
        Set<String> operators = naCombination.getOperators();
        for (String tso : operators) {
            int numberOfAlreadyActivatedRangeActionsForTso = (int) alreadyActivatedRangeActions.stream().filter(ra -> tso.equals(ra.getOperator())).count();
            int numberOfAlreadyAppliedNetworkActionsForTso = (int) alreadyActivatedNetworkActions.stream().filter(na -> tso.equals(na.getOperator())).count();
            if (numberOfAlreadyAppliedNetworkActionsForTso + numberOfAlreadyActivatedRangeActionsForTso + naCombinationSize > raUsageLimits.getMaxRaPerTso().getOrDefault(tso, Integer.MAX_VALUE)) {
                return true;
            }
        }

        // maxElementaryActionsPerTso
        for (String tso : raUsageLimits.getMaxElementaryActionsPerTso().keySet()) {
            int elementaryActions = 0;
            Set<NetworkAction> tsosNetworkActions = naCombination.getNetworkActionSet().stream().filter(networkAction -> tso.equals(networkAction.getOperator())).collect(Collectors.toSet());
            for (NetworkAction networkAction : tsosNetworkActions) {
                // TODO: what if some network actions share common elementary action?
                elementaryActions = elementaryActions + networkAction.getElementaryActions().size();
            }
            if (elementaryActions + movedPstTapsPerTso.getOrDefault(tso, 0) > raUsageLimits.getMaxElementaryActionsPerTso().getOrDefault(tso, Integer.MAX_VALUE)) {
                return true;
            }
        }

        return false;
    }

    boolean hasPreDefinedNetworkActionCombination(NetworkActionCombination naCombination) {
        return this.preDefinedNaCombinations.contains(naCombination);
    }

    /**
     * In time-coupled :
     * groups per operator the number of PST taps moved on one's timestamp's perimeter, compared to this timestamp's own
     * pre-perimeter position.
     */
    Map<String, Integer> getNumberOfPstTapsMovedByTso(OptimizationResult optimizationResult, OffsetDateTime timestamp, OptimizationPerimeter perimeter) {
        Map<String, Integer> pstTapsMovedByTso = new HashMap<>();
        State mainOptimizationState = perimeter.getMainOptimizationState();
        PrePerimeterResult prePerimeterResult = input.getPrePerimeterResults().getData(timestamp).orElseThrow();
        Set<PstRangeAction> activatedRangeActions = optimizationResult
            .getActivatedRangeActions(mainOptimizationState)
            .stream()
            .filter(PstRangeAction.class::isInstance)
            .map(ra -> (PstRangeAction) ra)
            .collect(Collectors.toSet());
        for (PstRangeAction pstRangeAction : activatedRangeActions) {
            String operator = pstRangeAction.getOperator();
            int tapsMoved = Math.abs(
                optimizationResult.getOptimizedTap(pstRangeAction, mainOptimizationState)
                    - prePerimeterResult.getTap(pstRangeAction)
            );
            pstTapsMovedByTso.put(operator, pstTapsMovedByTso.getOrDefault(operator, 0) + tapsMoved);
        }
        return pstTapsMovedByTso;
    }
}
