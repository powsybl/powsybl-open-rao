package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RaoResultImpl implements RaoResult {

    private static final FlowCnecResult DEFAULT_FLOWCNEC_RESULT = new FlowCnecResult();
    private static final NetworkActionResult DEFAULT_NETWORKACTION_RESULT = new NetworkActionResult();

    private ComputationStatus sensitivityStatus;
    private Map<FlowCnec, FlowCnecResult> flowCnecResults = new HashMap<>();
    private Map<NetworkAction, NetworkActionResult> networkActionResults = new HashMap<>();

    @Override
    public ComputationStatus getComputationStatus() {
        return sensitivityStatus;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecResults.keySet();
    }

    @Override
    public double getFlow(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(optimizationState).getFlow(unit);
    }

    @Override
    public double getMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(optimizationState).getMargin(unit);
    }

    @Override
    public double getRelativeMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(optimizationState).getRelativeMargin(unit);
    }

    @Override
    public double getLoopFlow(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(optimizationState).getLoopFlow(unit);
    }

    @Override
    public double getCommercialFlow(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(optimizationState).getCommercialFlow(unit);
    }

    @Override
    public double getPtdfZonalSum(OptimizationState optimizationState, FlowCnec flowCnec) {
        return flowCnecResults.getOrDefault(flowCnec, DEFAULT_FLOWCNEC_RESULT).getResult(optimizationState).getPtdfZonalSum();

    }

    public FlowCnecResult getFlowCnecResult(FlowCnec flowCnec) {
        return flowCnecResults.get(flowCnec);
    }

    public FlowCnecResult addResultForCnec(FlowCnec flowCnec) {
        flowCnecResults.put(flowCnec, new FlowCnecResult());
        return flowCnecResults.get(flowCnec);
    }

    @Override
    public double getCost(OptimizationState optimizationState) {
        return 0;
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        return 0;
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(OptimizationState optimizationState, int number) {
        return null;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        return 0;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return null;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        return 0;
    }

    @Override
    public List<FlowCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        return null;
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        if (state.isPreventive() || state.getContingency().isEmpty()) {
            return false;
        }

        // if it is activated in the preventive state, return true
        if (networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT)
            .getIsBeingActivatedStates().stream()
            .anyMatch(State::isPreventive)) {
            return true;
        }

        return networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT)
            .getIsBeingActivatedStates().stream()
            .filter(st -> st.getContingency().isPresent())
            .filter(st -> st.getInstant().getOrder() < state.getInstant().getOrder())
            .anyMatch(st -> st.getContingency().get().getId().equals(state.getContingency().get().getId()));
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT).getIsBeingActivatedStates().contains(state);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return networkActionResults.entrySet().stream()
            .filter(e -> e.getValue().getIsBeingActivatedStates().contains(state))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction rangeAction) {
        return false;
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction rangeAction) {
        return 0;
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction rangeAction) {
        return 0;
    }

    @Override
    public Set<RangeAction> getActivatedRangeActionsDuringState(State state) {
        return null;
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return null;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState(State state) {
        return null;
    }

}
