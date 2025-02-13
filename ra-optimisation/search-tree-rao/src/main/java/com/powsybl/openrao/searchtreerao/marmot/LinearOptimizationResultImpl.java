/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class LinearOptimizationResultImpl implements LinearOptimizationResult {
    private final FlowResult flowResult;
    private final SensitivityResult sensitivityResult;
    private final RangeActionActivationResult rangeActionActivationResult;
    private final ObjectiveFunctionResult objectiveFunctionResult;
    private final LinearProblemStatus status;

    public LinearOptimizationResultImpl(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult, ObjectiveFunctionResult objectiveFunctionResult, LinearProblemStatus status) {
        this.flowResult = flowResult;
        this.sensitivityResult = sensitivityResult;
        this.rangeActionActivationResult = rangeActionActivationResult;
        this.objectiveFunctionResult = objectiveFunctionResult;
        this.status = status;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return flowResult.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
        return flowResult.getFlow(flowCnec, side, unit, optimizedInstant);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return flowResult.getMargin(flowCnec, unit);
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
    public LinearProblemStatus getStatus() {
        return status;
    }

    @Override
    public RangeActionActivationResult getRangeActionActivationResult() {
        return rangeActionActivationResult;
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
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        objectiveFunctionResult.excludeContingencies(contingenciesToExclude);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return rangeActionActivationResult.getRangeActions();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return rangeActionActivationResult.getActivatedRangeActions(state);
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        return rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return rangeActionActivationResult.getOptimizedSetpointsOnState(state);
    }

    @Override
    public double getSetPointVariation(RangeAction<?> rangeAction, State state) {
        return rangeActionActivationResult.getSetPointVariation(rangeAction, state);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return rangeActionActivationResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return rangeActionActivationResult.getOptimizedTapsOnState(state);
    }

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        return rangeActionActivationResult.getTapVariation(pstRangeAction, state);
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
}
