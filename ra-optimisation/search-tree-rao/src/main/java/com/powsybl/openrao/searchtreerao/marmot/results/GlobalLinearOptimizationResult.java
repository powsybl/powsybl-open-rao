/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** This class stores an inter-temporal linear problem's outputs and implements getters for easy access.
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class GlobalLinearOptimizationResult implements LinearOptimizationResult {
    private final FlowResult globalFlowResult;
    private final SensitivityResult globalSensitivityResult;
    private final RangeActionActivationResult globalRangeActionActivationResult;
    private final ObjectiveFunctionResult globalObjectiveFunctionResult;
    private LinearProblemStatus status;

    public GlobalLinearOptimizationResult(TemporalData<? extends FlowResult> flowResults, TemporalData<SensitivityResult> sensitivityResults, TemporalData<RangeActionActivationResult> rangeActionActivationResults, TemporalData<NetworkActionsResult> preventiveTopologicalActions, ObjectiveFunction objectiveFunction, LinearProblemStatus status) {
        this.globalFlowResult = new GlobalFlowResult(flowResults);
        this.globalSensitivityResult = new GlobalSensitivityResult(sensitivityResults);
        this.globalRangeActionActivationResult = new GlobalRangeActionActivationResult(rangeActionActivationResults);
        this.globalObjectiveFunctionResult = objectiveFunction.evaluate(globalFlowResult, new GlobalRemedialActionActivationResult(rangeActionActivationResults, preventiveTopologicalActions));
        this.status = status;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return globalFlowResult.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
        return globalFlowResult.getFlow(flowCnec, side, unit, optimizedInstant);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return globalFlowResult.getMargin(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return globalFlowResult.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return globalFlowResult.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return globalFlowResult.getPtdfZonalSums();
    }

    public void setStatus(LinearProblemStatus status) {
        this.status = status;
    }

    @Override
    public LinearProblemStatus getStatus() {
        return status;
    }

    @Override
    public RangeActionActivationResult getRangeActionActivationResult() {
        return globalRangeActionActivationResult;
    }

    @Override
    public double getFunctionalCost() {
        return globalObjectiveFunctionResult.getFunctionalCost();
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return globalObjectiveFunctionResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return globalObjectiveFunctionResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return globalObjectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return globalObjectiveFunctionResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return globalObjectiveFunctionResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        globalObjectiveFunctionResult.excludeContingencies(contingenciesToExclude);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return globalRangeActionActivationResult.getRangeActions();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return globalRangeActionActivationResult.getActivatedRangeActions(state);
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        return globalRangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return globalRangeActionActivationResult.getOptimizedSetpointsOnState(state);
    }

    @Override
    public double getSetPointVariation(RangeAction<?> rangeAction, State state) {
        return globalRangeActionActivationResult.getSetPointVariation(rangeAction, state);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return globalRangeActionActivationResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return globalRangeActionActivationResult.getOptimizedTapsOnState(state);
    }

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        return globalRangeActionActivationResult.getTapVariation(pstRangeAction, state);
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return globalSensitivityResult.getSensitivityStatus();
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return globalSensitivityResult.getSensitivityStatus(state);
    }

    @Override
    public Set<String> getContingencies() {
        return globalSensitivityResult.getContingencies();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return globalSensitivityResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return globalSensitivityResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }
}
