/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.castor.algorithm.StateTree;

import java.util.*;

import static com.farao_community.farao.data.rao_result_api.ComputationStatus.FAILURE;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PreventiveAndCurativesRaoResultImpl implements SearchTreeRaoResult {
    private final State preventiveState;
    private final PrePerimeterResult initialResult;
    private final PerimeterResult firstPreventivePerimeterResult;
    private final PerimeterResult secondPreventivePerimeterResult;
    private final Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive;
    private final PrePerimeterResult resultsWithPrasForAllCnecs;
    private final Map<State, PerimeterResult> postContingencyResults;
    private final ObjectiveFunctionResult finalCostEvaluator;

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
                results.put(contingencyScenario.getCurativeState(), new PerimeterResultImpl(RangeActionSetpointResultImpl.buildFromActivationOfRangeActionAtState(automatonResult, automatonState.get()), postContingencyResults.get(contingencyScenario.getCurativeState())));
            } else {
                results.put(contingencyScenario.getCurativeState(), new PerimeterResultImpl(preContingencyResult, postContingencyResults.get(contingencyScenario.getCurativeState())));
            }
        });
        return results;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (initialResult.getSensitivityStatus() == FAILURE
            || secondPreventivePerimeterResult.getSensitivityStatus() == FAILURE
            || postContingencyResults.values().stream().anyMatch(perimeterResult -> perimeterResult.getSensitivityStatus() == FAILURE)) {
            return FAILURE;
        }
        // TODO: specify the behavior in case some perimeter are FALLBACK and other ones DEFAULT
        return ComputationStatus.DEFAULT;
    }

    public PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state) {
        if (state.getInstant().comesBefore(optimizationState.getFirstInstant())) {
            throw new FaraoException(String.format("Trying to access results for instant %s at optimization state %s is not allowed", state.getInstant(), optimizationState));
        }
        switch (optimizationState) {
            case INITIAL:
                throw new FaraoException("No PerimeterResult for INITIAL optimization state");
            case AFTER_PRA:
                return secondPreventivePerimeterResult;
            case AFTER_ARA:
                return postContingencyResults.keySet().stream()
                    .filter(optimizedState -> optimizedState.getInstant().equals(Instant.AUTO) && optimizedState.getContingency().equals(state.getContingency()))
                    .findAny().map(postContingencyResults::get).orElse(null);
            case AFTER_CRA:
                return postContingencyResults.get(state);
            default:
                throw new FaraoException(String.format("OptimizationState %s was not recognized", optimizationState));
        }
    }

    @Override
    public PerimeterResult getPostPreventivePerimeterResult() {
        return secondPreventivePerimeterResult;
    }

    @Override
    public PrePerimeterResult getInitialResult() {
        return initialResult;
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getFunctionalCost();
        } else if ((optimizationState == OptimizationState.AFTER_PRA || postContingencyResults.isEmpty()) ||
            (optimizationState == OptimizationState.AFTER_ARA && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().equals(Instant.AUTO)))) {
            // using postPreventiveResult would exclude curative CNECs
            return resultsWithPrasForAllCnecs.getFunctionalCost();
        } else if (optimizationState == OptimizationState.AFTER_CRA && finalCostEvaluator != null) {
            return finalCostEvaluator.getFunctionalCost();
        } else {
            return getHighestFunctionalForInstant(optimizationState.getFirstInstant());
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

    @Override
    public List<FlowCnec> getMostLimitingElements(OptimizationState optimizationState, int number) {
        //TODO : store values to be able to merge easily
        return null;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getVirtualCost();
        } else if ((optimizationState == OptimizationState.AFTER_PRA || postContingencyResults.isEmpty()) ||
            (optimizationState == OptimizationState.AFTER_ARA && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().equals(Instant.AUTO)))) {
            return resultsWithPrasForAllCnecs.getVirtualCost();
        } else if (optimizationState == OptimizationState.AFTER_CRA && finalCostEvaluator != null) {
            return finalCostEvaluator.getVirtualCost();
        } else {
            return postContingencyResults.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().equals(optimizationState.getFirstInstant()))
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
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getVirtualCost(virtualCostName);
        } else if ((optimizationState == OptimizationState.AFTER_PRA || postContingencyResults.isEmpty()) ||
            (optimizationState == OptimizationState.AFTER_ARA && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().equals(Instant.AUTO)))) {
            return resultsWithPrasForAllCnecs.getVirtualCost(virtualCostName);
        } else if (optimizationState == OptimizationState.AFTER_CRA && finalCostEvaluator != null) {
            return finalCostEvaluator.getVirtualCost(virtualCostName);
        } else {
            return postContingencyResults.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().equals(optimizationState.getFirstInstant()))
                .map(Map.Entry::getValue)
                .mapToDouble(perimeterResult -> perimeterResult.getVirtualCost(virtualCostName))
                .sum();
        }
    }

    @Override
    public List<FlowCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getCostlyElements(virtualCostName, number);
        } else if ((optimizationState == OptimizationState.AFTER_PRA || postContingencyResults.isEmpty()) ||
            (optimizationState == OptimizationState.AFTER_ARA && postContingencyResults.keySet().stream().noneMatch(state -> state.getInstant().equals(Instant.AUTO)))) {
            return resultsWithPrasForAllCnecs.getCostlyElements(virtualCostName, number);
        } else if (optimizationState == OptimizationState.AFTER_CRA && finalCostEvaluator != null) {
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
        if (state.getInstant() == Instant.PREVENTIVE) {
            return false;
        }
        State previousState = getStateOptimizedBefore(state);
        return isActivatedDuringState(previousState, networkAction) || wasActivatedBeforeState(previousState, networkAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return (remedialActionsExcludedFromSecondPreventive.contains(networkAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getActivatedNetworkActions().contains(networkAction);
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedNetworkActions().contains(networkAction);
        } else {
            return false;
        }
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE) {
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
        if (state.getInstant() == Instant.PREVENTIVE) {
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
        if (state.getInstant() == Instant.PREVENTIVE) {
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
        if (state.getInstant() == Instant.PREVENTIVE || !postContingencyResults.containsKey(state)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(pstRangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getOptimizedTap(pstRangeAction, state);
        } else {
            return postContingencyResults.get(state).getOptimizedTap(pstRangeAction, state);
        }
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
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
        if (state.getInstant() == Instant.PREVENTIVE || !postContingencyResults.containsKey(state)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(rangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult).getOptimizedSetpoint(rangeAction, state);
        } else {
            return postContingencyResults.get(state).getOptimizedSetpoint(rangeAction, state);
        }
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE) {
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
        if (state.getInstant() == Instant.PREVENTIVE || !postContingencyResults.containsKey(state)) {
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
        if (state.getInstant() == Instant.PREVENTIVE || !postContingencyResults.containsKey(state)) {
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
        if (state.getInstant() == Instant.PREVENTIVE) {
            throw new FaraoException("No state before preventive.");
        } else if (state.getInstant() == Instant.OUTAGE || state.getInstant() == Instant.AUTO) {
            return preventiveState;
        } else {
            // curative
            Contingency contingency = state.getContingency().orElseThrow();
            return postContingencyResults.keySet().stream()
                .filter(mapState -> mapState.getInstant().equals(Instant.AUTO) && mapState.getContingency().equals(Optional.of(contingency)))
                .findAny().orElse(preventiveState);
        }
    }
}
