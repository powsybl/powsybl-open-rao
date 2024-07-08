/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PerimeterResultWithCnecs implements OptimizationResult {

    private final PerimeterResultWithCnecs previousPerimeterResult;
    private final OptimizationResultImpl optimizationResult;

    public PerimeterResultWithCnecs(PerimeterResultWithCnecs previousPerimeterResult, OptimizationResultImpl optimizationResult) {
        this.optimizationResult = optimizationResult;
        this.previousPerimeterResult = previousPerimeterResult;
    }

    public static PerimeterResultWithCnecs buildFromPreviousResult(PerimeterResultWithCnecs previousResult) {
        return new PerimeterResultWithCnecs(previousResult,
            new OptimizationResultImpl(previousResult, previousResult, new NetworkActionResultImpl(Collections.emptySet()), RangeActionResultImpl.buildFromPreviousPerimeterResult(previousResult), previousResult)
        );
    }

    public static PerimeterResultWithCnecs buildFromSensiResultAndAppliedRas(PerimeterResultWithCnecs sensiResult, AppliedRemedialActions appliedArasAndCras, State state, PerimeterResultWithCnecs previousResult) {
        RangeActionResultImpl rangeActionResult = RangeActionResultImpl.buildFromPreviousPerimeterResult(previousResult);
        appliedArasAndCras.getAppliedRangeActions(state).forEach(rangeActionResult::activate);

        return new PerimeterResultWithCnecs(previousResult,
            new OptimizationResultImpl(sensiResult, sensiResult, new NetworkActionResultImpl(appliedArasAndCras.getAppliedNetworkActions(state)), rangeActionResult, sensiResult)
        );
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return optimizationResult.getFlow(flowCnec, side, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        return optimizationResult.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        return optimizationResult.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        return optimizationResult.getPtdfZonalSums();
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return optimizationResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return optimizationResult.getActivatedNetworkActions();
    }

    @Override
    public double getFunctionalCost() {
        return optimizationResult.getFunctionalCost();
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return optimizationResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return optimizationResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return optimizationResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return optimizationResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return optimizationResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        optimizationResult.excludeContingencies(contingenciesToExclude);
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        return optimizationResult.getObjectiveFunction();
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return optimizationResult.getRangeActions();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions() {
        return optimizationResult.getActivatedRangeActions();
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction) {
        return optimizationResult.getOptimizedSetpoint(rangeAction);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return optimizationResult.getOptimizedTap(pstRangeAction);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return optimizationResult.getOptimizedTaps();
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpoints() {
        return optimizationResult.getOptimizedSetpoints();
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return optimizationResult.getSensitivityStatus();
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return optimizationResult.getSensitivityStatus(state);
    }

    @Override
    public Set<String> getContingencies() {
        return optimizationResult.getContingencies();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return optimizationResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return optimizationResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }

    public PerimeterResultWithCnecs getPreviousResult() {
        return previousPerimeterResult;
    }

    public RangeActionResult getRangeActionResult() {
        return optimizationResult.getRangeActionResult();
    }
}
