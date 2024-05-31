/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PerimeterResultImpl implements PerimeterResult {

    private final OptimizationResult optimizationResult;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpoint;

    public PerimeterResultImpl(RangeActionSetpointResult prePerimeterRangeActionSetpoint, OptimizationResult optimizationResult) {
        this.optimizationResult = optimizationResult;
        this.prePerimeterRangeActionSetpoint = prePerimeterRangeActionSetpoint;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit, Instant instant) {
        return optimizationResult.getFlow(flowCnec, side, unit, instant);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        return optimizationResult.getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        return optimizationResult.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
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
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return optimizationResult.getActivatedRangeActions(state);
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {

        // todo: check behaviour of this method when end of POC
        // todo: move this logics in RangeActionActivationResultImpl (?)

        if (optimizationResult.getRangeActions().contains(rangeAction)) {
            return optimizationResult.getOptimizedSetpoint(rangeAction, state);
        }

        // if rangeAction is not in perimeter, check if there is not another rangeAction
        // on the same network element.
        RangeAction<?> rangeActionOnSameElement = null;
        if (rangeAction.getNetworkElements().size() == 1) {
            NetworkElement networkElement = rangeAction.getNetworkElements().iterator().next();
            for (RangeAction<?> ra : optimizationResult.getRangeActions()) {
                if (ra.getNetworkElements().contains(networkElement)) {
                    rangeActionOnSameElement = ra;
                    break;
                }
            }
        }

        if (rangeActionOnSameElement != null) {
            return optimizationResult.getOptimizedSetpoint(rangeActionOnSameElement, state);
        } else {
            return prePerimeterRangeActionSetpoint.getSetpoint(rangeAction);
        }
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {

        if (optimizationResult.getRangeActions().contains(pstRangeAction)) {
            return optimizationResult.getOptimizedTap(pstRangeAction, state);
        }

        // if pstRangeAction is not in perimeter, check if there is not another rangeAction
        // on the same network element.
        PstRangeAction pstRangeActionOnSameElement = null;
        NetworkElement networkElement = pstRangeAction.getNetworkElement();

        for (RangeAction<?> rangeAction : optimizationResult.getRangeActions()) {
            if (rangeAction instanceof PstRangeAction otherPstRA && otherPstRA.getNetworkElement() != null
                && otherPstRA.getNetworkElement().equals(networkElement)) {
                pstRangeActionOnSameElement = otherPstRA;
                break;
            }
        }

        if (pstRangeActionOnSameElement != null) {
            return optimizationResult.getOptimizedTap(pstRangeActionOnSameElement, state);
        } else {
            return prePerimeterRangeActionSetpoint.getTap(pstRangeAction);
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return optimizationResult.getOptimizedTapsOnState(state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return optimizationResult.getOptimizedSetpointsOnState(state);
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
    public double getSensitivityValue(FlowCnec flowCnec, Side side, RangeAction<?> rangeAction, Unit unit) {
        throw new NotImplementedException("This method is not implemented");
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, SensitivityVariableSet linearGlsk, Unit unit) {
        throw new NotImplementedException("This method is not implemented");
    }
}
