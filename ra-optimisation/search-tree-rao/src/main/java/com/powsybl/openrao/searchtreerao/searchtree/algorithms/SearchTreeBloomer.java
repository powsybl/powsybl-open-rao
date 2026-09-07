/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates the network actions combinations tested at every depth of the search tree.
 * <p>
 * In a time-coupled search tree, a single combination is applied simultaneously on every timestamp's network.
 * <p>
 * Note :
 * <li> The RaUsageLimits are assumed the same between the timestamps and are not checked for now.
 * <li> For now, the first timestamp's perimeter and pre-perimeter results are the reference for all the usage-limits
 * checks, like the single perimeter does in the single-timestamp search tree.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class SearchTreeBloomer {
    private final List<NetworkActionCombination> preDefinedNaCombinations;
    private final List<NetworkActionCombinationFilter> networkActionCombinationFilters;
    private final SearchTreeInput input;
    private final SearchTreeParameters parameters;

    public SearchTreeBloomer(SearchTreeInput input, SearchTreeParameters parameters) {
        this.input = input;
        this.parameters = parameters;
        RaUsageLimits raUsageLimits = parameters.getRaLimitationParameters().getOrDefault(getMainOptimizationPerimeter().getMainOptimizationState().getInstant(), new RaUsageLimits());
        this.preDefinedNaCombinations = parameters.getNetworkActionParameters().getNetworkActionCombinations();
        this.networkActionCombinationFilters = new ArrayList<>(List.of(
            new AlreadyAppliedNetworkActionsFilter(),
            new AlreadyTestedCombinationsFilter(preDefinedNaCombinations),
            new MaximumNumberOfRemedialActionsFilter(raUsageLimits.getMaxRa()),
            new MaximumNumberOfRemedialActionPerTsoFilter(raUsageLimits.getMaxTopoPerTso(), raUsageLimits.getMaxRaPerTso()),
            new ElementaryActionsCompatibilityFilter(),
            new MaximumNumberOfElementaryActionsFilter(raUsageLimits.getMaxElementaryActionsPerTso()),
            // an action is considered ineffective if it has no impact on any of the timestamps' networks
            new IneffectiveActionsFilter(input.getAllNetworks())
        ));
        if (parameters.getNetworkActionParameters().skipNetworkActionFarFromMostLimitingElements()) {
            this.networkActionCombinationFilters.add(
                new FarFromMostLimitingElementFilter(input.getAllNetworks(), parameters.getNetworkActionParameters().getMaxNumberOfBoundariesForSkippingNetworkActions())
            );
        }
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
    Set<NetworkActionCombination> bloom(final Leaf fromLeaf, final Set<NetworkAction> networkActions, final ReportNode reportNode) {

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
     */
    boolean shouldRangeActionsBeRemovedToApplyNa(NetworkActionCombination naCombination, OptimizationResult optimizationResult) {
        State optimizationState = getMainOptimizationPerimeter().getMainOptimizationState();
        RaUsageLimits raUsageLimits = parameters.getRaLimitationParameters().get(optimizationState.getInstant());
        if (Objects.isNull(raUsageLimits)) {
            return false;
        }

        // maxRa
        int naCombinationSize = naCombination.getNetworkActionSet().size();
        Set<NetworkAction> alreadyActivatedNetworkActions = optimizationResult.getActivatedNetworkActions();
        Set<RangeAction<?>> alreadyActivatedRangeActions = optimizationResult.getActivatedRangeActions(optimizationState);
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

        // maxElementaryActionPerTso
        Map<String, Integer> movedPstTapsPerTso = getNumberOfPstTapsMovedByTso(optimizationResult);
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

    Map<String, Integer> getNumberOfPstTapsMovedByTso(OptimizationResult optimizationResult) {
        Map<String, Integer> pstTapsMovedByTso = new HashMap<>();
        State mainOptimizationState = getMainOptimizationPerimeter().getMainOptimizationState();
        PrePerimeterResult prePerimeterResult = input.getAllPrePerimeterResults().getData(input.getAllPrePerimeterResults().getTimestamps().getFirst()).orElseThrow();
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

    private OptimizationPerimeter getMainOptimizationPerimeter() {
        // the reference perimeter for the ra usage limits checks the first timestamp's perimeter which makes it work for a single timestamp search tree
        return input.getAllOptimizationPerimeters().getData(input.getAllOptimizationPerimeters().getTimestamps().getFirst()).orElseThrow();
    }
}
