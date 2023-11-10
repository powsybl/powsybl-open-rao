/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.search_tree_rao.castor.algorithm.StateTree;
import com.farao_community.farao.search_tree_rao.result.api.*;

import java.util.*;

import static com.farao_community.farao.data.rao_result_api.ComputationStatus.FAILURE;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PreventiveAndCurativesRaoResultImpl implements RaoResult {
    private final State preventiveState;
    private final PrePerimeterResult initialResult;
    private final PerimeterResult firstPreventivePerimeterResult;
    private final PerimeterResult secondPreventivePerimeterResult;
    private final Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive;
    private final PrePerimeterResult resultsWithPrasForAllCnecs;
    private final Map<State, PerimeterResult> postContingencyResults;
    private final ObjectiveFunctionResult finalCostEvaluator;
    private OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;

    /**
     * Constructor used when no post-contingency RAO has been run. Then the post-contingency results will be the
     * same as the post-preventive RAO results.
     */
    public PreventiveAndCurativesRaoResultImpl(State preventiveState,
                                               PrePerimeterResult initialResult,
                                               PerimeterResult preventivePerimeterResult,
                                               PrePerimeterResult resultsWithPrasForAllCnecs) {
        this(preventiveState, initialResult, preventivePerimeterResult, preventivePerimeterResult, new HashSet<>(), resultsWithPrasForAllCnecs, new HashMap<>(), null);
    }

    /**
     * Constructor used when preventive and post-contingency RAOs have been run
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               PerimeterResult preventivePerimeterResult,
                                               PrePerimeterResult resultsWithPrasForAllCnecs,
                                               Map<State, OptimizationResult> postContingencyResults) {
        this(stateTree.getBasecaseScenario().getBasecaseState(), initialResult, preventivePerimeterResult, preventivePerimeterResult, new HashSet<>(), resultsWithPrasForAllCnecs, buildPostContingencyResults(stateTree, resultsWithPrasForAllCnecs, postContingencyResults), null);
        excludeContingencies(getContingenciesToExclude(stateTree));
    }

    /**
     * Constructor used when preventive and post-contingency RAOs have been run, if 2 preventive RAOs were run
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               PerimeterResult firstPreventivePerimeterResult,
                                               PerimeterResult secondPreventivePerimeterResult,
                                               Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive,
                                               PrePerimeterResult resultsWithPrasForAllCnecs,
                                               Map<State, OptimizationResult> postContingencyResults) {
        this(stateTree.getBasecaseScenario().getBasecaseState(), initialResult, firstPreventivePerimeterResult, secondPreventivePerimeterResult, remedialActionsExcludedFromSecondPreventive, resultsWithPrasForAllCnecs, buildPostContingencyResults(stateTree, resultsWithPrasForAllCnecs, postContingencyResults), secondPreventivePerimeterResult);
        excludeContingencies(getContingenciesToExclude(stateTree));
    }

    /**
     * Constructor used when preventive and post-contingency RAOs have been run, if 2 preventive RAOs were run, and 2 AUTO RAOs were run
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               PerimeterResult firstPreventivePerimeterResult,
                                               PerimeterResult secondPreventivePerimeterResult,
                                               Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive,
                                               PrePerimeterResult resultsWithPrasForAllCnecs,
                                               Map<State, OptimizationResult> postContingencyResults,
                                               ObjectiveFunctionResult postSecondAraoResults) {
        this(stateTree.getBasecaseScenario().getBasecaseState(), initialResult, firstPreventivePerimeterResult, secondPreventivePerimeterResult, remedialActionsExcludedFromSecondPreventive, resultsWithPrasForAllCnecs, buildPostContingencyResults(stateTree, resultsWithPrasForAllCnecs, postContingencyResults), postSecondAraoResults);
        excludeContingencies(getContingenciesToExclude(stateTree));
    }

    private PreventiveAndCurativesRaoResultImpl(State preventiveState,
                                                PrePerimeterResult initialResult,
                                                PerimeterResult firstPreventivePerimeterResult,
                                                PerimeterResult preventivePerimeterResult,
                                                Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive,
                                                PrePerimeterResult resultsWithPrasForAllCnecs,
                                                Map<State, PerimeterResult> postContingencyPerimeterResults,
                                                ObjectiveFunctionResult finalCostEvaluator) {
        this.preventiveState = preventiveState;
        this.initialResult = initialResult;
        this.firstPreventivePerimeterResult = firstPreventivePerimeterResult;
        this.secondPreventivePerimeterResult = preventivePerimeterResult;
        this.remedialActionsExcludedFromSecondPreventive = remedialActionsExcludedFromSecondPreventive;
        this.resultsWithPrasForAllCnecs = resultsWithPrasForAllCnecs;
        this.postContingencyResults = postContingencyPerimeterResults;
        this.finalCostEvaluator = finalCostEvaluator;
    }

    private static Map<State, PerimeterResult> buildPostContingencyResults(StateTree stateTree, PrePerimeterResult preContingencyResult, Map<State, OptimizationResult> postContingencyResults) {
        Map<State, PerimeterResult> results = new HashMap<>();
        stateTree.getContingencyScenarios().forEach(contingencyScenario -> {
            Optional<State> automatonState = contingencyScenario.getAutomatonState();
            if (automatonState.isPresent()) {
                OptimizationResult automatonResult = postContingencyResults.get(automatonState.get());
                results.put(automatonState.get(), new PerimeterResultImpl(preContingencyResult, automatonResult));
                OptimizationResult curativeResult = postContingencyResults.get(contingencyScenario.getCurativeState());
                if (automatonResult.getSensitivityStatus() == FAILURE || curativeResult.getSensitivityStatus() == FAILURE) {
                    results.put(contingencyScenario.getCurativeState(), new PerimeterResultImpl(preContingencyResult, curativeResult));
                } else {
                    results.put(contingencyScenario.getCurativeState(), new PerimeterResultImpl(RangeActionSetpointResultImpl.buildFromActivationOfRangeActionAtState(automatonResult, automatonState.get()), curativeResult));
                }
            } else {
                OptimizationResult curativeResult = postContingencyResults.get(contingencyScenario.getCurativeState());
                results.put(contingencyScenario.getCurativeState(), new PerimeterResultImpl(preContingencyResult, curativeResult));
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
                OptimizationResult curativeResult = postContingencyResults.get(contingencyScenario.getCurativeState());
                if (!curativeResult.getContingencies().contains(contingencyScenario.getContingency().getId())) {
                    contingenciesToExclude.add(contingencyScenario.getContingency().getId());
                }
            } else {
                OptimizationResult curativeResult = postContingencyResults.get(contingencyScenario.getCurativeState());
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
            || secondPreventivePerimeterResult.getSensitivityStatus() == FAILURE
            || postContingencyResults.entrySet().stream().anyMatch(entry -> Objects.isNull(entry.getValue()) || entry.getValue().getSensitivityStatus(entry.getKey()) == FAILURE)) {
            return FAILURE;
        }
        return ComputationStatus.DEFAULT;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        Instant instant = state.getInstant();
        while (instant != null) {
            PerimeterResult perimeterResult = getPerimeterResult(instant, state);
            if (Objects.nonNull(perimeterResult)) {
                return perimeterResult.getSensitivityStatus(state);
            }
            instant = instant.getPreviousInstant();
        }
        return FAILURE;
    }

    public PerimeterResult getPerimeterResult(Instant optimizedInstant, State state) {
        if (optimizedInstant == null) {
            throw new FaraoException("No PerimeterResult for INITIAL optimization state");
        }
        if (state.getInstant().comesBefore(optimizedInstant)) {
            throw new FaraoException(String.format("Trying to access results for instant %s at optimization state %s is not allowed", state.getInstant(), optimizedInstant));
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
        throw new FaraoException(String.format("Optimized instant %s was not recognized", optimizedInstant));
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getFunctionalCost();
        } else if ((optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty()) ||
            (optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto()))) {
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
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getFlow(flowCnec, side, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getCommercialFlow(flowCnec, side, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getLoopFlow(flowCnec, side, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, Side side) {
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
            throw new FaraoException(String.format("Trying to access results for instant %s at optimization state %s is not allowed", flowCnec.getState().getInstant(), optimizedInstant));
        } else if ((optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty()) ||
            (optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto()))) {
            // using postPreventiveResult would exclude curative CNECs
            return resultsWithPrasForAllCnecs;
        } else if (postContingencyResults.containsKey(flowCnec.getState()) && optimizedInstant.equals(flowCnec.getState().getInstant())) {
            // if cnec has been optimized during a post contingency instant
            return postContingencyResults.get(flowCnec.getState());
        } else if (!postContingencyResults.containsKey(flowCnec.getState())) {
            // if post contingency cnec has been optimized in preventive perimeter (no remedial actions)
            return secondPreventivePerimeterResult;
        } else {
            // e.g Auto instant for curative cnecs optimized in 2P
            return null;
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
                .map(PerimeterResult::getFunctionalCost)
                .max(Double::compareTo)
                .orElse(-Double.MAX_VALUE)
        );
        return highestFunctionalCost;
    }

    /**
     * Returns true if the perimeter has an actual functional cost, ie has CNECs
     * (as opposed to a perimeter with pure MNECs only)
     */
    private static boolean hasActualFunctionalCost(PerimeterResult perimeterResult) {
        return !perimeterResult.getMostLimitingElements(1).isEmpty();
    }

    public List<FlowCnec> getMostLimitingElements() {
        //TODO : store values to be able to merge easily
        return Collections.emptyList();
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost();
        } else if ((optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty()) ||
            (optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto()))) {
            return resultsWithPrasForAllCnecs.getVirtualCost();
        } else if (optimizedInstant.isCurative() && finalCostEvaluator != null) {
            return finalCostEvaluator.getVirtualCost();
        } else {
            return postContingencyResults.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().equals(optimizedInstant))
                .map(Map.Entry::getValue)
                .mapToDouble(ObjectiveFunctionResult::getVirtualCost)
                .sum();
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
            .filter(perimeterResult -> perimeterResult.getVirtualCostNames() != null)
            .forEach(perimeterResult -> virtualCostNames.addAll(perimeterResult.getVirtualCostNames()));

        return virtualCostNames;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost(virtualCostName);
        } else if ((optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty()) ||
            (optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto()))) {
            return resultsWithPrasForAllCnecs.getVirtualCost(virtualCostName);
        } else if (optimizedInstant.isCurative() && finalCostEvaluator != null) {
            return finalCostEvaluator.getVirtualCost(virtualCostName);
        } else {
            return postContingencyResults.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().equals(optimizedInstant))
                .map(Map.Entry::getValue)
                .mapToDouble(perimeterResult -> perimeterResult.getVirtualCost(virtualCostName))
                .sum();
        }
    }

    public List<FlowCnec> getCostlyElements(Instant optimizedInstant, String virtualCostName, int number) {
        if (optimizedInstant == null) {
            return initialResult.getCostlyElements(virtualCostName, number);
        } else if ((optimizedInstant.isPreventive() || optimizedInstant.isOutage() || postContingencyResults.isEmpty()) ||
            (optimizedInstant.isAuto() && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().isAuto()))) {
            return resultsWithPrasForAllCnecs.getCostlyElements(virtualCostName, number);
        } else if (optimizedInstant.isCurative() && finalCostEvaluator != null) {
            return finalCostEvaluator.getCostlyElements(virtualCostName, number);
        } else {
            // TODO : for other cases, store values to be able to merge easily
            return null;
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        if (remedialAction instanceof NetworkAction) {
            return isActivatedDuringState(state, (NetworkAction) remedialAction);
        } else if (remedialAction instanceof RangeAction<?>) {
            return isActivatedDuringState(state, (RangeAction<?>) remedialAction);
        } else {
            throw new FaraoException("Unrecognized remedial action type");
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
            throw new FaraoException(String.format("State %s was not optimized and does not have pre-optim values", state.getId()));
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
            throw new FaraoException("No state before preventive.");
        } else if (state.getInstant().isOutage() || state.getInstant().isAuto()) {
            return preventiveState;
        } else {
            // curative
            Contingency contingency = state.getContingency().orElseThrow();
            return postContingencyResults.keySet().stream()
                .filter(mapState -> mapState.getInstant().isAuto() && mapState.getContingency().equals(Optional.of(contingency)))
                .findAny().orElse(preventiveState);
        }
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        if (this.optimizationStepsExecuted.isOverwritePossible(optimizationStepsExecuted)) {
            this.optimizationStepsExecuted = optimizationStepsExecuted;
        } else {
            throw new FaraoException("The RaoResult object should not be modified outside of its usual routine");
        }
    }

    @Override
    public OptimizationStepsExecuted getOptimizationStepsExecuted() {
        return optimizationStepsExecuted;
    }
}
