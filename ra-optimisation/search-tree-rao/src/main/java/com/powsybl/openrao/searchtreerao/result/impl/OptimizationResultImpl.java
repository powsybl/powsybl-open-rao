/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class OptimizationResultImpl implements OptimizationResult {

    private final FlowResult flowResult;
    private final SensitivityResult sensitivityResult;
    private final NetworkActionsResult networkActionsResult;
    private final RangeActionResult rangeActionResult;
    private final ObjectiveFunctionResult objectiveFunctionResult;

    public OptimizationResultImpl(FlowResult flowResult, SensitivityResult sensitivityResult, NetworkActionsResult networkActionsResult, RangeActionResult rangeActionResult, ObjectiveFunctionResult objectiveFunctionResult) {
        this.flowResult = flowResult;
        this.sensitivityResult = sensitivityResult;
        this.networkActionsResult = networkActionsResult;
        this.rangeActionResult = rangeActionResult;
        this.objectiveFunctionResult = objectiveFunctionResult;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return flowResult.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return flowResult.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return flowResult.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return flowResult.getPtdfZonalSums();
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
    public void activate(NetworkAction networkAction) {
        networkActionsResult.activate(networkAction);
    }

    @Override
    public double getFunctionalCost() {
        return objectiveFunctionResult.getFunctionalCost();
    }

    @Override
    public double getInstantFunctionalCost() {
        return objectiveFunctionResult.getInstantFunctionalCost();
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return objectiveFunctionResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return objectiveFunctionResult.getVirtualCost();
    }

    @Override
    public double getInstantVirtualCost() {
        return objectiveFunctionResult.getInstantVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return objectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return objectiveFunctionResult.getVirtualCost(virtualCostName);
    }

    @Override
    public double getInstantVirtualCost(String virtualCostName) {
        return objectiveFunctionResult.getInstantVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return objectiveFunctionResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        //TODO: something?
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunctionResult.getObjectiveFunction();
    }

    @Override
    public void activate(RangeAction<?> rangeAction, double setpoint) {
        rangeActionResult.activate(rangeAction, setpoint);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return rangeActionResult.getRangeActions();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions() {
        return rangeActionResult.getActivatedRangeActions();
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction) {
        return rangeActionResult.getOptimizedSetpoint(rangeAction);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpoints() {
        return rangeActionResult.getOptimizedSetpoints();
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return rangeActionResult.getOptimizedTap(pstRangeAction);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return rangeActionResult.getOptimizedTaps();
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return sensitivityResult.getSensitivityStatus();
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return sensitivityResult.getSensitivityStatus(state);
    }

    @Override
    public Set<String> getContingencies() {
        return sensitivityResult.getContingencies();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return sensitivityResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return sensitivityResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }

    public RangeActionResult getRangeActionResult() {
        return rangeActionResult;
    }
}
