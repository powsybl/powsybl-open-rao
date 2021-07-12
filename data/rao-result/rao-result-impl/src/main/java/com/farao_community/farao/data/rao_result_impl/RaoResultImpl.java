/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultImpl implements RaoResult {

    private static final FlowCnecResult DEFAULT_FLOWCNEC_RESULT = new FlowCnecResult();
    private static final NetworkActionResult DEFAULT_NETWORKACTION_RESULT = new NetworkActionResult();
    private static final PstRangeActionResult DEFAULT_PSTRANGEACTION_RESULT = new PstRangeActionResult("");
    private static final CostResult DEFAULT_COST_RESULT = new CostResult();

    private ComputationStatus sensitivityStatus;
    private Map<FlowCnec, FlowCnecResult> flowCnecResults = new HashMap<>();
    private Map<NetworkAction, NetworkActionResult> networkActionResults = new HashMap<>();
    private Map<PstRangeAction, PstRangeActionResult> pstRangeActionResults = new HashMap<>();
    private Map<OptimizationState, CostResult> costResults = new EnumMap<>(OptimizationState.class);

    public void setComputationStatus(ComputationStatus computationStatus) {
        this.sensitivityStatus = computationStatus;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return sensitivityStatus;
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

    public FlowCnecResult getAndCreateIfAbsentFlowCnecResult(FlowCnec flowCnec) {
        flowCnecResults.putIfAbsent(flowCnec, new FlowCnecResult());
        return flowCnecResults.get(flowCnec);
    }

    public CostResult getAndCreateIfAbsentCostResult(OptimizationState optimizationState) {
        costResults.putIfAbsent(optimizationState, new CostResult());
        return costResults.get(optimizationState);
    }

    @Override
    public double getCost(OptimizationState optimizationState) {
        return costResults.getOrDefault(optimizationState, DEFAULT_COST_RESULT).getCost();
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        return costResults.getOrDefault(optimizationState, DEFAULT_COST_RESULT).getFunctionalCost();
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        return costResults.getOrDefault(optimizationState, DEFAULT_COST_RESULT).getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return costResults.values().stream().flatMap(c -> c.getVirtualCostNames().stream()).collect(Collectors.toSet());
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        return costResults.getOrDefault(optimizationState, DEFAULT_COST_RESULT).getVirtualCost(virtualCostName);
    }

    public NetworkActionResult getAndCreateIfAbsentNetworkActionResult(NetworkAction networkAction) {
        networkActionResults.putIfAbsent(networkAction, new NetworkActionResult());
        return networkActionResults.get(networkAction);
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        if (state.isPreventive() || state.getContingency().isEmpty()) {
            return false;
        }

        // if it is activated in the preventive state, return true
        if (networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT)
            .getStatesWithActivation().stream()
            .anyMatch(State::isPreventive)) {
            return true;
        }

        return networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT)
            .getStatesWithActivation().stream()
            .filter(st -> st.getContingency().isPresent())
            .filter(st -> st.getInstant().getOrder() < state.getInstant().getOrder())
            .anyMatch(st -> st.getContingency().get().getId().equals(state.getContingency().get().getId()));
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return networkActionResults.getOrDefault(networkAction, DEFAULT_NETWORKACTION_RESULT).getStatesWithActivation().contains(state);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return networkActionResults.entrySet().stream()
            .filter(e -> e.getValue().getStatesWithActivation().contains(state))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public PstRangeActionResult getAndCreateIfAbsentPstRangeActionResult(PstRangeAction pstRangeAction) {
        pstRangeActionResults.putIfAbsent(pstRangeAction, new PstRangeActionResult(pstRangeAction.getNetworkElement().getId()));
        return pstRangeActionResults.get(pstRangeAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction rangeAction) {

        if (rangeAction instanceof PstRangeAction) {
            PstRangeAction pstRangeAction = (PstRangeAction) rangeAction;
            return pstRangeActionResults.getOrDefault(pstRangeAction, DEFAULT_PSTRANGEACTION_RESULT).isActivatedDuringState(state);
        }
        // only handle PstRangeAction
        return false;
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return pstRangeActionResults.getOrDefault(pstRangeAction, DEFAULT_PSTRANGEACTION_RESULT).getPreOptimizedTapOnState(state);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return pstRangeActionResults.getOrDefault(pstRangeAction, DEFAULT_PSTRANGEACTION_RESULT).getOptimizedTapOnState(state);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction rangeAction) {

        if (rangeAction instanceof PstRangeAction) {
            PstRangeAction pstRangeAction = (PstRangeAction) rangeAction;
            return pstRangeActionResults.getOrDefault(pstRangeAction, DEFAULT_PSTRANGEACTION_RESULT).getPreOptimizedSetpointOnState(state);
        }
        // only handle PstRangeAction
        return Double.NaN;
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction rangeAction) {

        if (rangeAction instanceof PstRangeAction) {
            PstRangeAction pstRangeAction = (PstRangeAction) rangeAction;
            return pstRangeActionResults.getOrDefault(pstRangeAction, DEFAULT_PSTRANGEACTION_RESULT).getOptimizedSetpointOnState(state);
        }
        // only handle PstRangeAction
        return Double.NaN;
    }

    @Override
    public Set<RangeAction> getActivatedRangeActionsDuringState(State state) {
        return pstRangeActionResults.entrySet().stream()
            .filter(e -> e.getValue().isActivatedDuringState(state))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return pstRangeActionResults.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getOptimizedTapOnState(state)));
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState(State state) {
        return pstRangeActionResults.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getOptimizedSetpointOnState(state)));
    }
}
