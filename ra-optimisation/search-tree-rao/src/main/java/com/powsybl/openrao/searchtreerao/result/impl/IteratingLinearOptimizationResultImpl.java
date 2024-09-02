/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizationResultImpl implements LinearOptimizationResult {

    private LinearProblemStatus status;
    private int nbOfIteration;
    private final RangeActionActivationResult rangeActionActivationResult;
    private final FlowResult flowResult;
    private final SensitivityResult sensitivityResult;
    private final ObjectiveFunctionResult objectiveFunctionResult;

    public IteratingLinearOptimizationResultImpl(LinearProblemStatus status,
                                                 int nbOfIteration,
                                                 RangeActionActivationResult rangeActionActivationResult,
                                                 FlowResult flowResult,
                                                 ObjectiveFunctionResult objectiveFunctionResult,
                                                 SensitivityResult sensitivityResult) {
        this.status = status;
        this.nbOfIteration = nbOfIteration;
        this.rangeActionActivationResult = rangeActionActivationResult;
        this.flowResult = flowResult;
        this.objectiveFunctionResult = objectiveFunctionResult;
        this.sensitivityResult = sensitivityResult;
    }

    public void setStatus(LinearProblemStatus status) {
        this.status = status;
    }

    public int getNbOfIteration() {
        return nbOfIteration;
    }

    public void setNbOfIteration(int nbOfIteration) {
        this.nbOfIteration = nbOfIteration;
    }

    public SensitivityResult getSensitivityResult() {
        return sensitivityResult;
    }

    public FlowResult getBranchResult() {
        return flowResult;
    }

    public ObjectiveFunctionResult getObjectiveFunctionResult() {
        return objectiveFunctionResult;
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

    @Override
    public LinearProblemStatus getStatus() {
        return status;
    }

    @Override
    public double getFlow(FlowCnec branchCnec, TwoSides side, Unit unit) {
        return flowResult.getFlow(branchCnec, side, unit);
    }

    @Override
    public double getFlow(FlowCnec branchCnec, TwoSides side, Unit unit, Instant instant) {
        return flowResult.getFlow(branchCnec, side, unit, instant);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return flowResult.getMargin(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec branchCnec, TwoSides side, Unit unit) {
        return flowResult.getCommercialFlow(branchCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec branchCnec, TwoSides side) {
        return flowResult.getPtdfZonalSum(branchCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return flowResult.getPtdfZonalSums();
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
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return rangeActionActivationResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return rangeActionActivationResult.getOptimizedTapsOnState(state);
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
    public double getSensitivityValue(FlowCnec branchCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return sensitivityResult.getSensitivityValue(branchCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec branchCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return sensitivityResult.getSensitivityValue(branchCnec, side, linearGlsk, unit);
    }

    @Override
    public RangeActionActivationResult getRangeActionActivationResult() {
        return rangeActionActivationResult;
    }

}
