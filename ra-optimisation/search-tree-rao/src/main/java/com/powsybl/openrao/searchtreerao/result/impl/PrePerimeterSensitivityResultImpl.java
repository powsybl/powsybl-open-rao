/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

public class PrePerimeterSensitivityResultImpl implements PrePerimeterResult {

    private final FlowResult flowResult;
    private final SensitivityResult sensitivityResult;
    private final RangeActionSetpointResult prePerimeterSetpoints;
    private final ObjectiveFunctionResult objectiveFunctionResult;

    public PrePerimeterSensitivityResultImpl(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionSetpointResult prePerimeterSetpoints, ObjectiveFunctionResult objectiveFunctionResult) {
        this.flowResult = flowResult;
        this.sensitivityResult = sensitivityResult;
        this.prePerimeterSetpoints = prePerimeterSetpoints;
        this.objectiveFunctionResult = objectiveFunctionResult;
    }

    @Override
    public FlowResult getFlowResult() {
        return flowResult;
    }

    @Override
    public SensitivityResult getSensitivityResult() {
        return sensitivityResult;
    }

    public ObjectiveFunctionResult getObjectiveFunctionResult() {
        return objectiveFunctionResult;
    }

    @Override
    public RangeActionSetpointResult getRangeActionSetpointResult() {
        return prePerimeterSetpoints;
    }

    public Set<String> getContingencies() {
        return sensitivityResult.getContingencies();
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
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return sensitivityResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return sensitivityResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return flowResult.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant instant) {
        return flowResult.getFlow(flowCnec, side, unit, instant);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return flowResult.getMargin(flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(FlowCnec flowCnec, Unit unit) {
        return flowResult.getRelativeMargin(flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return flowResult.getRelativeMargin(flowCnec, side, unit);
    }

    @Override
    public double getLoopFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return flowResult.getLoopFlow(flowCnec, side, unit);
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
    public Set<RangeAction<?>> getRangeActions() {
        return prePerimeterSetpoints.getRangeActions();
    }

    @Override
    public double getSetpoint(RangeAction<?> rangeAction) {
        return prePerimeterSetpoints.getSetpoint(rangeAction);
    }

    @Override
    public int getTap(PstRangeAction pstRangeAction) {
        return prePerimeterSetpoints.getTap(pstRangeAction);
    }

    @Override
    public double getFunctionalCost() {
        return objectiveFunctionResult.getFunctionalCost();
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
    public Set<String> getVirtualCostNames() {
        return objectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return objectiveFunctionResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return objectiveFunctionResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunctionResult.getObjectiveFunction();
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        objectiveFunctionResult.excludeContingencies(contingenciesToExclude);
    }
}
