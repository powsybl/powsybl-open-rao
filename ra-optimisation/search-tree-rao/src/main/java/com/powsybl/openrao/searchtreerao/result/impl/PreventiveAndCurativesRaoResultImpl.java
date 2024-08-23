/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;

import java.util.*;

import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PreventiveAndCurativesRaoResultImpl extends AbstractFlowRaoResult {
    private final State preventiveState;
    private final PrePerimeterResult initialResult;
    private final OptimizationResult firstPreventivePerimeterResult;
    private final OptimizationResult secondPreventivePerimeterResult;
    private final Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive;
    private final PrePerimeterResult resultsWithPrasForAllCnecs;
    private final Map<State, OptimizationResult> postContingencyResults;
    private final ObjectiveFunctionResult finalCostEvaluator;
    private final Crac crac;
    private OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;

    private final Map<Instant, Map<State, State>> optimizedStateForInstantAndState = new HashMap<>();

    /**
     * Constructor used when no post-contingency RAO has been run. Then the post-contingency results will be the
     * same as the post-preventive RAO results.
     */
    public PreventiveAndCurativesRaoResultImpl(State preventiveState,
                                               PrePerimeterResult initialResult,
                                               OptimizationResult preventivePerimeterResult,
                                               PrePerimeterResult resultsWithPrasForAllCnecs,
                                               Crac crac) {
        this(preventiveState, initialResult, preventivePerimeterResult, preventivePerimeterResult, new HashSet<>(), resultsWithPrasForAllCnecs, new HashMap<>(), null, crac);
    }

    /**
     * Constructor used when preventive and post-contingency RAOs have been run
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               OptimizationResult preventivePerimeterResult,
                                               PrePerimeterResult resultsWithPrasForAllCnecs,
                                               Map<State, OptimizationResult> postContingencyResults,
                                               Crac crac) {
        this(stateTree.getBasecaseScenario().getRaOptimisationState(), initialResult, preventivePerimeterResult, preventivePerimeterResult, new HashSet<>(), resultsWithPrasForAllCnecs, buildPostContingencyResults(stateTree, postContingencyResults), null, crac);
        excludeContingencies(getContingenciesToExclude(stateTree));
    }

    /**
     * Constructor used when preventive and post-contingency RAOs have been run, if 2 preventive RAOs were run
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               OptimizationResult firstPreventivePerimeterResult,
                                               OptimizationResult secondPreventivePerimeterResult,
                                               Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive,
                                               PrePerimeterResult resultsWithPrasForAllCnecs,
                                               Map<State, OptimizationResult> postContingencyResults,
                                               Crac crac) {
        this(stateTree.getBasecaseScenario().getRaOptimisationState(), initialResult, firstPreventivePerimeterResult, secondPreventivePerimeterResult, remedialActionsExcludedFromSecondPreventive, resultsWithPrasForAllCnecs, buildPostContingencyResults(stateTree, postContingencyResults), secondPreventivePerimeterResult, crac);
        excludeContingencies(getContingenciesToExclude(stateTree));
    }

    /**
     * Constructor used when preventive and post-contingency RAOs have been run, if 2 preventive RAOs were run, and 2 AUTO RAOs were run
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               OptimizationResult firstPreventivePerimeterResult,
                                               OptimizationResult secondPreventivePerimeterResult,
                                               Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive,
                                               PrePerimeterResult resultsWithPrasForAllCnecs,
                                               Map<State, OptimizationResult> postContingencyResults,
                                               ObjectiveFunctionResult postSecondAraoResults,
                                               Crac crac) {
        this(stateTree.getBasecaseScenario().getRaOptimisationState(), initialResult, firstPreventivePerimeterResult, secondPreventivePerimeterResult, remedialActionsExcludedFromSecondPreventive, resultsWithPrasForAllCnecs, buildPostContingencyResults(stateTree, postContingencyResults), postSecondAraoResults, crac);
        excludeContingencies(getContingenciesToExclude(stateTree));
    }

    private PreventiveAndCurativesRaoResultImpl(State preventiveState,
                                                PrePerimeterResult initialResult,
                                                OptimizationResult firstPreventivePerimeterResult,
                                                OptimizationResult preventivePerimeterResult,
                                                Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive,
                                                PrePerimeterResult resultsWithPrasForAllCnecs,
                                                Map<State, OptimizationResult> postContingencyPerimeterResults,
                                                ObjectiveFunctionResult finalCostEvaluator,
                                                Crac crac) {
        this.preventiveState = preventiveState;
        this.initialResult = initialResult;
        this.firstPreventivePerimeterResult = firstPreventivePerimeterResult;
        this.secondPreventivePerimeterResult = preventivePerimeterResult;
        this.remedialActionsExcludedFromSecondPreventive = remedialActionsExcludedFromSecondPreventive;
        this.resultsWithPrasForAllCnecs = resultsWithPrasForAllCnecs;
        this.postContingencyResults = postContingencyPerimeterResults;
        this.finalCostEvaluator = finalCostEvaluator;
        this.crac = crac;
    }

    private static Map<State, OptimizationResult> buildPostContingencyResults(StateTree stateTree, Map<State, OptimizationResult> postContingencyResults) {
        Map<State, OptimizationResult> results = new HashMap<>();
        stateTree.getContingencyScenarios().forEach(contingencyScenario -> {
            Optional<State> automatonState = contingencyScenario.getAutomatonState();
            if (automatonState.isPresent()) {
                OptimizationResult automatonResult = postContingencyResults.get(automatonState.get());
                results.put(automatonState.get(), automatonResult);

                boolean allPreviousPerimetersSucceded = automatonResult.getSensitivityStatus() == DEFAULT;

                for (Perimeter curativePerimeter : contingencyScenario.getCurativePerimeters()) {
                    OptimizationResult curativeResult = postContingencyResults.get(curativePerimeter.getRaOptimisationState());
                    allPreviousPerimetersSucceded = allPreviousPerimetersSucceded && curativeResult.getSensitivityStatus() == DEFAULT;
                    results.put(curativePerimeter.getRaOptimisationState(), curativeResult);
                }
            } else {
                boolean allPreviousPerimetersSucceded = true;

                for (Perimeter curativePerimeter : contingencyScenario.getCurativePerimeters()) {
                    OptimizationResult curativeResult = postContingencyResults.get(curativePerimeter.getRaOptimisationState());
                    allPreviousPerimetersSucceded = allPreviousPerimetersSucceded && curativeResult.getSensitivityStatus() == DEFAULT;
                    results.put(curativePerimeter.getRaOptimisationState(), curativeResult);
                }
            }
        });
        return results;
    }

    private Set<String> getContingenciesToExclude(StateTree stateTree) {
        Set<String> contingenciesToExclude = new HashSet<>();
        stateTree.getContingencyScenarios().forEach(contingencyScenario -> {
            Optional<State> automatonState = contingencyScenario.getAutomatonState();
            if (automatonState.isPresent()) {
                OptimizationResult automatonResult = postContingencyResults.get(automatonState.get());
                if (!automatonResult.getContingencies().contains(contingencyScenario.getContingency().getId())) {
                    contingenciesToExclude.add(contingencyScenario.getContingency().getId());
                    return;
                }
            }
            for (Perimeter curativePerimeter : contingencyScenario.getCurativePerimeters()) {
                OptimizationResult curativeResult = postContingencyResults.get(curativePerimeter.getRaOptimisationState());
                if (!curativeResult.getContingencies().contains(contingencyScenario.getContingency().getId())) {
                    contingenciesToExclude.add(contingencyScenario.getContingency().getId());
                }
            }
        });
        return contingenciesToExclude;
    }

    private void excludeContingencies(Set<String> contingenciesToExclude) {
        initialResult.excludeContingencies(contingenciesToExclude);
        firstPreventivePerimeterResult.excludeContingencies(contingenciesToExclude);
        secondPreventivePerimeterResult.excludeContingencies(contingenciesToExclude);
        resultsWithPrasForAllCnecs.excludeContingencies(contingenciesToExclude);
        postContingencyResults.values().forEach(result -> result.excludeContingencies(contingenciesToExclude));
        if (finalCostEvaluator != null) {
            finalCostEvaluator.excludeContingencies(contingenciesToExclude);
        }
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (initialResult.getSensitivityStatus() == FAILURE
            || secondPreventivePerimeterResult.getSensitivityStatus() == FAILURE) {
            return FAILURE;
        }
        if (initialResult.getSensitivityStatus() == PARTIAL_FAILURE ||
            secondPreventivePerimeterResult.getSensitivityStatus() == PARTIAL_FAILURE ||
            postContingencyResults.entrySet().stream().anyMatch(entry ->
                entry.getValue() == null || entry.getValue().getSensitivityStatus(entry.getKey()) != DEFAULT)) {
            return PARTIAL_FAILURE;
        }
        return DEFAULT;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        Instant instant = state.getInstant();
        while (instant != null) {
            OptimizationResult perimeterResult = getOptimizationResult(instant, state);
            if (Objects.nonNull(perimeterResult)) {
                return perimeterResult.getSensitivityStatus(state);
            }
            instant = crac.getInstantBefore(instant);
        }
        return FAILURE;
    }

    public OptimizationResult getOptimizationResult(Instant optimizedInstant, State state) {
        if (optimizedInstant == null) {
            throw new OpenRaoException("No OptimizationResult for INITIAL optimization state");
        }
        if (state.getInstant().comesBefore(optimizedInstant)) {
            throw new OpenRaoException(String.format("Trying to access results for instant %s at optimization state %s is not allowed", state.getInstant(), optimizedInstant));
        }
        if (optimizedInstant.isPreventive() || optimizedInstant.isOutage()) {
            return secondPreventivePerimeterResult;
        }
        if (optimizedInstant.isAuto()) {
            return postContingencyResults.keySet().stream()
                .filter(optimizedState -> optimizedState.getInstant().isAuto() && optimizedState.getContingency().equals(state.getContingency()))
                .findAny().map(postContingencyResults::get).orElse(null);
        }
        if (optimizedInstant.isCurative()) {
            return postContingencyResults.get(state);
        }
        throw new OpenRaoException(String.format("Optimized instant %s was not recognized", optimizedInstant));
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getFunctionalCost();
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty() ||
            optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto())) {
            // using postPreventiveResult would exclude curative CNECs
            return resultsWithPrasForAllCnecs.getFunctionalCost();
        } else if (optimizedInstant.isCurative() && finalCostEvaluator != null) {
            // When a second preventive optimization has been run, use its updated cost evaluation
            return finalCostEvaluator.getFunctionalCost();
        } else {
            // No second preventive was run => use CRAO1 results
            // OR ARA
            return getHighestFunctionalForInstant(optimizedInstant);
        }
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getMargin(flowCnec, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getRelativeMargin(flowCnec, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getFlow(flowCnec, side, unit, optimizedInstant);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getCommercialFlow(flowCnec, side, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getLoopFlow(flowCnec, side, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getPtdfZonalSum(flowCnec, side);
        } else {
            return Double.NaN;
        }
    }

    private FlowResult getFlowResult(Instant optimizedInstant, FlowCnec flowCnec) {
        if (optimizedInstant == null) {
            return initialResult;
        } else if (flowCnec.getState().getInstant().comesBefore(optimizedInstant)) {
            throw new OpenRaoException(String.format("Trying to access results for instant %s at optimization state %s is not allowed", flowCnec.getState().getInstant(), optimizedInstant));
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty() ||
            optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto())) {
            // using postPreventiveResult would exclude curative CNECs
            return resultsWithPrasForAllCnecs;
        } else if (Objects.nonNull(findStateOptimizedFor(optimizedInstant, flowCnec))) {
            // if cnec has been optimized during a post contingency instant
            return postContingencyResults.get(findStateOptimizedFor(optimizedInstant, flowCnec));
        } else {
            return secondPreventivePerimeterResult;
        }
    }

    private State findStateOptimizedFor(Instant optimizedInstant, FlowCnec flowCnec) {
        if (optimizedInstant.isPreventive()) {
            return null;
        }
        optimizedStateForInstantAndState.putIfAbsent(optimizedInstant, new HashMap<>());
        Map<State, State> optimizedStateForState = optimizedStateForInstantAndState.get(optimizedInstant);
        State cnecState = flowCnec.getState();
        if (optimizedStateForState.containsKey(cnecState)) {
            return optimizedStateForState.get(cnecState);
        } else {
            State optimizedState = postContingencyResults.keySet().stream().filter(state ->
                !state.getInstant().comesAfter(cnecState.getInstant())
                    && state.getInstant().equals(optimizedInstant)
                    && state.getContingency().equals(cnecState.getContingency())
            ).findAny().orElseGet(() -> findStateOptimizedFor(crac.getInstantBefore(optimizedInstant), flowCnec));
            optimizedStateForState.put(cnecState, optimizedState);
            return optimizedState;
        }
    }

    private double getHighestFunctionalForInstant(Instant instant) {
        double highestFunctionalCost = secondPreventivePerimeterResult.getFunctionalCost();
        highestFunctionalCost = Math.max(
            highestFunctionalCost,
            postContingencyResults.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().equals(instant) || entry.getKey().getInstant().comesBefore(instant))
                .map(Map.Entry::getValue)
                .filter(PreventiveAndCurativesRaoResultImpl::hasActualFunctionalCost)
                .map(OptimizationResult::getFunctionalCost)
                .max(Double::compareTo)
                .orElse(-Double.MAX_VALUE)
        );
        return highestFunctionalCost;
    }

    /**
     * Returns true if the perimeter has an actual functional cost, ie has CNECs
     * (as opposed to a perimeter with pure MNECs only)
     */
    private static boolean hasActualFunctionalCost(OptimizationResult perimeterResult) {
        return !perimeterResult.getMostLimitingElements(1).isEmpty();
    }

    public List<FlowCnec> getMostLimitingElements() {
        //TODO : store values to be able to merge easily
        return null;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost();
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty() ||
            optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto())) {
            return resultsWithPrasForAllCnecs.getVirtualCost();
        } else if (optimizedInstant.isCurative() && finalCostEvaluator != null) {
            return finalCostEvaluator.getVirtualCost();
        } else {
            return postContingencyResults.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().equals(optimizedInstant))
                .map(Map.Entry::getValue)
                .mapToDouble(ObjectiveFunctionResult::getVirtualCost)
                .sum() + secondPreventivePerimeterResult.getVirtualCost();
        }
    }

    @Override
    public Set<String> getVirtualCostNames() {
        Set<String> virtualCostNames = new HashSet<>();
        if (initialResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(initialResult.getVirtualCostNames());
        }
        if (firstPreventivePerimeterResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(firstPreventivePerimeterResult.getVirtualCostNames());
        }
        if (secondPreventivePerimeterResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(secondPreventivePerimeterResult.getVirtualCostNames());
        }
        postContingencyResults.values().stream()
            .filter(optimizationResult -> optimizationResult.getVirtualCostNames() != null)
            .forEach(optimizationResult -> virtualCostNames.addAll(optimizationResult.getVirtualCostNames()));

        return virtualCostNames;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost(virtualCostName);
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty() ||
            optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto())) {
            return resultsWithPrasForAllCnecs.getVirtualCost(virtualCostName);
        } else if (optimizedInstant.isCurative() && finalCostEvaluator != null) {
            return finalCostEvaluator.getVirtualCost(virtualCostName);
        } else {
            return postContingencyResults.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().equals(optimizedInstant))
                .map(Map.Entry::getValue)
                .mapToDouble(optimizationResult -> optimizationResult.getVirtualCost(virtualCostName))
                .sum() + secondPreventivePerimeterResult.getVirtualCost(virtualCostName);
        }
    }

    public List<FlowCnec> getCostlyElements(Instant optimizedInstant, String virtualCostName, int number) {
        if (optimizedInstant == null) {
            return initialResult.getCostlyElements(virtualCostName, number);
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty() ||
            optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto())) {
            return resultsWithPrasForAllCnecs.getCostlyElements(virtualCostName, number);
        } else if (optimizedInstant.isCurative() && finalCostEvaluator != null) {
            return finalCostEvaluator.getCostlyElements(virtualCostName, number);
        } else {
            // TODO : for other cases, store values to be able to merge easily
            return null;
        }
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        if (state.getInstant().isPreventive()) {
            return false;
        }
        State previousState = getStateOptimizedBefore(state);
        return isActivatedDuringState(previousState, networkAction) || wasActivatedBeforeState(previousState, networkAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        if (state.getInstant().isPreventive()) {
            return (remedialActionsExcludedFromSecondPreventive.contains(networkAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getActivatedNetworkActions().contains(networkAction);
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedNetworkActions().contains(networkAction);
        } else {
            return false;
        }
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        if (state.getInstant().isPreventive()) {
            Set<NetworkAction> set = secondPreventivePerimeterResult.getActivatedNetworkActions();
            firstPreventivePerimeterResult.getActivatedNetworkActions().stream()
                .filter(remedialActionsExcludedFromSecondPreventive::contains)
                .forEach(set::add);
            return set;
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedNetworkActions();
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        if (state.getInstant().isPreventive()) {
            return (remedialActionsExcludedFromSecondPreventive.contains(rangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getActivatedRangeActions(state).contains(rangeAction);
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedRangeActions(state).contains(rangeAction);
        } else {
            return false;
        }
    }

    private void throwIfNotOptimized(State state) {
        if (!postContingencyResults.containsKey(state)) {
            throw new OpenRaoException(String.format("State %s was not optimized and does not have pre-optim values", state.getId()));
        }
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        if (state.getInstant().isPreventive()) {
            return initialResult.getTap(pstRangeAction);
        }
        throwIfNotOptimized(state);
        State previousState = getStateOptimizedBefore(state);
        if (preventiveState.equals(previousState)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(pstRangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getOptimizedTap(pstRangeAction, preventiveState);
        } else {
            return postContingencyResults.get(previousState).getOptimizedTap(pstRangeAction, previousState);
        }
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        if (state.getInstant().isPreventive() || !postContingencyResults.containsKey(state)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(pstRangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getOptimizedTap(pstRangeAction, state);
        } else {
            return postContingencyResults.get(state).getOptimizedTap(pstRangeAction, state);
        }
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (state.getInstant().isPreventive()) {
            return initialResult.getSetpoint(rangeAction);
        }
        throwIfNotOptimized(state);
        State previousState = getStateOptimizedBefore(state);
        if (preventiveState.equals(previousState)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(rangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getOptimizedSetpoint(rangeAction, preventiveState);
        } else {
            return postContingencyResults.get(previousState).getOptimizedSetpoint(rangeAction, previousState);
        }
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (state.getInstant().isPreventive() || !postContingencyResults.containsKey(state)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(rangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getOptimizedSetpoint(rangeAction, state);
        } else {
            return postContingencyResults.get(state).getOptimizedSetpoint(rangeAction, state);
        }
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        if (state.getInstant().isPreventive()) {
            Set<RangeAction<?>> set = secondPreventivePerimeterResult.getActivatedRangeActions(state);
            firstPreventivePerimeterResult.getActivatedRangeActions(state).stream()
                .filter(remedialActionsExcludedFromSecondPreventive::contains)
                .forEach(set::add);
            return set;
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedRangeActions(state);
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (state.getInstant().isPreventive() || !postContingencyResults.containsKey(state)) {
            Map<PstRangeAction, Integer> map = new HashMap<>(secondPreventivePerimeterResult.getOptimizedTapsOnState(state));
            firstPreventivePerimeterResult.getOptimizedTapsOnState(state).entrySet().stream()
                .filter(entry -> remedialActionsExcludedFromSecondPreventive.contains(entry.getKey()))
                .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
            return map;
        } else {
            return postContingencyResults.get(state).getOptimizedTapsOnState(state);
        }
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        if (state.getInstant().isPreventive() || !postContingencyResults.containsKey(state)) {
            Map<RangeAction<?>, Double> map = new HashMap<>(secondPreventivePerimeterResult.getOptimizedSetpointsOnState(state));
            firstPreventivePerimeterResult.getOptimizedSetpointsOnState(state).entrySet().stream()
                .filter(entry -> remedialActionsExcludedFromSecondPreventive.contains(entry.getKey()))
                .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
            return map;
        } else {
            return postContingencyResults.get(state).getOptimizedSetpointsOnState(state);
        }
    }

    private State getStateOptimizedBefore(State state) {
        if (state.getInstant().isPreventive()) {
            throw new OpenRaoException("No state before preventive.");
        } else if (state.getInstant().isOutage() || state.getInstant().isAuto()) {
            return preventiveState;
        } else {
            // curative
            Contingency contingency = state.getContingency().orElseThrow();
            return postContingencyResults.keySet().stream()
                .filter(mapState -> mapState.getContingency().equals(Optional.of(contingency)))
                .filter(mapState -> mapState.getInstant().isAuto() || mapState.getInstant().isCurative())
                .filter(mapState -> mapState.getInstant().comesBefore(state.getInstant()))
                .max(Comparator.comparingInt(mapState -> mapState.getInstant().getOrder()))
                .orElse(preventiveState);
        }
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        if (this.optimizationStepsExecuted.isOverwritePossible(optimizationStepsExecuted)) {
            this.optimizationStepsExecuted = optimizationStepsExecuted;
        } else {
            throw new OpenRaoException("The RaoResult object should not be modified outside of its usual routine");
        }
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return isSecure(crac.getLastInstant(), u);
    }

    @Override
    public OptimizationStepsExecuted getOptimizationStepsExecuted() {
        return optimizationStepsExecuted;
    }
}
