/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

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
    public double getFlow(FlowCnec flowCnec, Unit unit) {
        return optimizationResult.getFlow(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Unit unit) {
        return optimizationResult.getCommercialFlow(flowCnec, unit);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec) {
        return optimizationResult.getPtdfZonalSum(flowCnec);
    }

    @Override
    public Map<FlowCnec, Double> getPtdfZonalSums() {
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

        // todo: check behaviour of this method when end of POC
        // todo: move this logics in RangeActionActivationResultImpl (?)
        if (optimizationResult.getRangeActions().contains(pstRangeAction)) {
            return optimizationResult.getOptimizedTap(pstRangeAction, state);
        }

        // if pstRangeAction is not in perimeter, check if there is not another rangeAction
        // on the same network element.
        PstRangeAction pstRangeActionOnSameElement = null;
        NetworkElement networkElement = pstRangeAction.getNetworkElement();

        for (RangeAction<?> rangeAction : optimizationResult.getRangeActions()) {
            if (rangeAction instanceof PstRangeAction && ((PstRangeAction) rangeAction).getNetworkElement() != null
                &&  ((PstRangeAction) rangeAction).getNetworkElement().equals(networkElement)) {
                pstRangeActionOnSameElement = (PstRangeAction) rangeAction;
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
    public double getSensitivityValue(FlowCnec flowCnec, RangeAction<?> rangeAction, Unit unit) {
        return 0;
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, LinearGlsk linearGlsk, Unit unit) {
        return 0;
    }
}
