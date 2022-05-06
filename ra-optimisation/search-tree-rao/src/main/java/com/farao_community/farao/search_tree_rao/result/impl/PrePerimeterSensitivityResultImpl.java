/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.*;
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
    public ComputationStatus getSensitivityStatus() {
        return sensitivityResult.getSensitivityStatus();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, RangeAction<?> rangeAction, Unit unit) {
        return sensitivityResult.getSensitivityValue(flowCnec, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, SensitivityVariableSet linearGlsk, Unit unit) {
        return sensitivityResult.getSensitivityValue(flowCnec, linearGlsk, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Unit unit) {
        return flowResult.getFlow(flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(FlowCnec flowCnec, Unit unit) {
        return flowResult.getRelativeMargin(flowCnec, unit);
    }

    @Override
    public double getLoopFlow(FlowCnec flowCnec, Unit unit) {
        return flowResult.getLoopFlow(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Unit unit) {
        return flowResult.getCommercialFlow(flowCnec, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec) {
        return flowResult.getPtdfZonalSum(flowCnec);
    }

    @Override
    public Map<FlowCnec, Double> getPtdfZonalSums() {
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

    public FlowResult getBranchResult() {
        return flowResult;
    }

    public SensitivityResult getSensitivityResult() {
        return sensitivityResult;
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
}
