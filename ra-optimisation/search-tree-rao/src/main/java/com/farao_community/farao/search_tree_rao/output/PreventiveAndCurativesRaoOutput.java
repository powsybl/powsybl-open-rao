/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.PerimeterOutput;
import com.farao_community.farao.search_tree_rao.state_tree.StateTree;

import java.util.*;

import static com.farao_community.farao.data.rao_result_api.ComputationStatus.FAILURE;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PreventiveAndCurativesRaoOutput implements SearchTreeRaoResult {
    private PrePerimeterResult initialResult;
    private PerimeterResult preventivePerimeterResult;
    private PrePerimeterResult resultsWithPrasForAllCnecs;
    private Map<State, PerimeterResult> postContingencyResults;

    public PreventiveAndCurativesRaoOutput(StateTree stateTree,
                                           PrePerimeterResult initialResult,
                                           PerimeterResult preventivePerimeterResult,
                                           PrePerimeterResult resultsWithPrasForAllCnecs,
                                           Map<State, OptimizationResult> postContingencyResults) {
        this.initialResult = initialResult;
        this.preventivePerimeterResult = preventivePerimeterResult;
        this.resultsWithPrasForAllCnecs = resultsWithPrasForAllCnecs;
        this.postContingencyResults = buildPostContingencyResults(stateTree, resultsWithPrasForAllCnecs, postContingencyResults);
    }

    private static Map<State, PerimeterResult> buildPostContingencyResults(StateTree stateTree, PrePerimeterResult preContingencyResult, Map<State, OptimizationResult> postContingencyResults) {
        Map<State, PerimeterResult> results = new HashMap<>();
        stateTree.getContingencyScenarios().forEach(contingencyScenario -> {
            Optional<State> automatonState = contingencyScenario.getAutomatonState();
            if (automatonState.isPresent()) {
                OptimizationResult automatonResult = postContingencyResults.get(automatonState.get());
                results.put(automatonState.get(), new PerimeterOutput(preContingencyResult, automatonResult));
                results.put(contingencyScenario.getCurativeState(), new PerimeterOutput(automatonResult, postContingencyResults.get(contingencyScenario.getCurativeState())));
            } else {
                results.put(contingencyScenario.getCurativeState(), new PerimeterOutput(preContingencyResult, postContingencyResults.get(contingencyScenario.getCurativeState())));
            }
        });
        return results;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (initialResult.getSensitivityStatus() == FAILURE
                || preventivePerimeterResult.getSensitivityStatus() == FAILURE
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
                return preventivePerimeterResult;
            case AFTER_ARA:
                Optional<State> autoState = postContingencyResults.keySet().stream()
                    .filter(optimizedState -> optimizedState.getInstant().equals(Instant.AUTO) && optimizedState.getContingency().equals(state.getContingency()))
                    .findAny();
                if (autoState.isPresent()) {
                    return postContingencyResults.get(autoState.get());
                } else {
                    return null;
                }
            case AFTER_CRA:
                return postContingencyResults.get(state);
            default:
                throw new FaraoException(String.format("OptimizationState %s was not recognized", optimizationState));
        }
    }

    @Override
    public PerimeterResult getPostPreventivePerimeterResult() {
        return preventivePerimeterResult;
    }

    @Override
    public PrePerimeterResult getInitialResult() {
        return initialResult;
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getFunctionalCost();
        } else if (optimizationState == OptimizationState.AFTER_PRA) {
            // using postPreventiveResult would exclude curative CNECs
            return resultsWithPrasForAllCnecs.getFunctionalCost();
        } else {
            return getHighestFunctionalForInstant(optimizationState.getFirstInstant());
        }
    }

    private double getHighestFunctionalForInstant(Instant instant) {
        double highestFunctionalCost = preventivePerimeterResult.getFunctionalCost();
        highestFunctionalCost = Math.max(
            highestFunctionalCost,
            postContingencyResults.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().equals(instant))
                .map(Map.Entry::getValue)
                .filter(PreventiveAndCurativesRaoOutput::hasActualFunctionalCost)
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
        }
        if (optimizationState == OptimizationState.AFTER_PRA) {
            return preventivePerimeterResult.getVirtualCost();
        }
        return postContingencyResults.entrySet().stream()
            .filter(entry -> entry.getKey().getInstant().equals(optimizationState.getFirstInstant()))
            .map(Map.Entry::getValue)
            .mapToDouble(ObjectiveFunctionResult::getVirtualCost)
            .sum();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        Set<String> virtualCostNames = new HashSet<>();
        if (initialResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(initialResult.getVirtualCostNames());
        }
        if (preventivePerimeterResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(preventivePerimeterResult.getVirtualCostNames());
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
        }
        if (optimizationState == OptimizationState.AFTER_PRA) {
            return preventivePerimeterResult.getVirtualCost(virtualCostName);
        }

        return postContingencyResults.entrySet().stream()
            .filter(entry -> entry.getKey().getInstant().equals(optimizationState.getFirstInstant()))
            .map(Map.Entry::getValue)
            .mapToDouble(perimeterResult -> perimeterResult.getVirtualCost(virtualCostName))
            .sum();
    }

    @Override
    public List<FlowCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        //TODO : store values to be able to merge easily
        return null;
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return false;
        } else {
            return preventivePerimeterResult.getActivatedNetworkActions().contains(networkAction);
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return preventivePerimeterResult.getActivatedNetworkActions().contains(networkAction);
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedNetworkActions().contains(networkAction);
        } else {
            return false;
        }
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return preventivePerimeterResult.getActivatedNetworkActions();
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedNetworkActions();
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction rangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return preventivePerimeterResult.getActivatedRangeActions().contains(rangeAction);
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedRangeActions().contains(rangeAction);
        } else {
            return false;
        }
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return initialResult.getOptimizedTap(pstRangeAction);
        } else if (postContingencyResults.containsKey(state)) {
            return preventivePerimeterResult.getOptimizedTap(pstRangeAction);
        } else {
            throw new FaraoException(String.format("State %s was not optimized and does not have pre-optim values", state.getId()));
        }
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE || !postContingencyResults.containsKey(state)) {
            return preventivePerimeterResult.getOptimizedTap(pstRangeAction);
        } else {
            return postContingencyResults.get(state).getOptimizedTap(pstRangeAction);
        }
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction rangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return initialResult.getOptimizedSetPoint(rangeAction);
        } else if (postContingencyResults.containsKey(state)) {
            return preventivePerimeterResult.getOptimizedSetPoint(rangeAction);
        } else {
            throw new FaraoException(String.format("State %s was not optimized and does not have pre-optim values", state.getId()));
        }
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction rangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE || !postContingencyResults.containsKey(state)) {
            return preventivePerimeterResult.getOptimizedSetPoint(rangeAction);
        } else {
            return postContingencyResults.get(state).getOptimizedSetPoint(rangeAction);
        }
    }

    @Override
    public Set<RangeAction> getActivatedRangeActionsDuringState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return preventivePerimeterResult.getActivatedRangeActions();
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getActivatedRangeActions();
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE || !postContingencyResults.containsKey(state)) {
            return preventivePerimeterResult.getOptimizedTaps();
        } else {
            return postContingencyResults.get(state).getOptimizedTaps();
        }
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE || !postContingencyResults.containsKey(state)) {
            return preventivePerimeterResult.getOptimizedSetPoints();
        } else {
            return postContingencyResults.get(state).getOptimizedSetPoints();
        }
    }
}
