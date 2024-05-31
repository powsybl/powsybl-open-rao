/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class LinearOptimizationResultWithNetworkActions implements OptimizationResult {

    private final LinearOptimizationResult linearOptimizationResult;
    private final NetworkActionsResult networkActionsResult;

    public LinearOptimizationResultWithNetworkActions(LinearOptimizationResult linearOptimizationResult, Set<NetworkAction> activatedNetworkActions) {
        this.linearOptimizationResult = linearOptimizationResult;
        this.networkActionsResult = new NetworkActionResultImpl(new HashSet<>(activatedNetworkActions));
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
        return linearOptimizationResult.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        return linearOptimizationResult.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        return linearOptimizationResult.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        return linearOptimizationResult.getPtdfZonalSums();
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return networkActionsResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return networkActionsResult.getActivatedNetworkActions();
    }

    @Override
    public double getFunctionalCost() {
        return linearOptimizationResult.getFunctionalCost();
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return linearOptimizationResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return linearOptimizationResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return linearOptimizationResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return linearOptimizationResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return linearOptimizationResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        linearOptimizationResult.excludeContingencies(contingenciesToExclude);
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        return linearOptimizationResult.getObjectiveFunction();
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return linearOptimizationResult.getRangeActions();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return linearOptimizationResult.getActivatedRangeActions(state);
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        return linearOptimizationResult.getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return linearOptimizationResult.getOptimizedSetpointsOnState(state);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return linearOptimizationResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return linearOptimizationResult.getOptimizedTapsOnState(state);
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return linearOptimizationResult.getSensitivityStatus();
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return linearOptimizationResult.getSensitivityStatus(state);
    }

    @Override
    public Set<String> getContingencies() {
        return linearOptimizationResult.getContingencies();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, RangeAction<?> rangeAction, Unit unit) {
        return linearOptimizationResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, SensitivityVariableSet linearGlsk, Unit unit) {
        return linearOptimizationResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }
}
