/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.output.PerimeterResult;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;
import java.util.stream.Collectors;

public class PerimeterOutput implements PerimeterResult {

    OptimizationResult optimizationResult;
    PrePerimeterResult prePerimeterResult;

    public PerimeterOutput(PrePerimeterResult prePerimeterResult, OptimizationResult optimizationResult) {
        this.prePerimeterResult = prePerimeterResult;
        this.optimizationResult = optimizationResult;
    }

    @Override
    public Set<RangeAction> getActivatedRangeActions() {
        return optimizationResult.getRangeActions().stream()
                .filter(rangeAction -> prePerimeterResult.getOptimizedSetPoint(rangeAction) != optimizationResult.getOptimizedSetPoint(rangeAction))
                .collect(Collectors.toSet());
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
        return getCostlyElements(virtualCostName, number);
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        return prePerimeterResult.getRangeActions();
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        // TODO: better handling in case of accessing of a tap that is not present in the results
        try {
            return optimizationResult.getOptimizedTap(pstRangeAction);
        } catch (FaraoException e) {
            return prePerimeterResult.getOptimizedTap(pstRangeAction);
        }
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return Optional.of(optimizationResult.getOptimizedSetPoint(rangeAction)).orElse(prePerimeterResult.getOptimizedSetPoint(rangeAction));
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return optimizationResult.getOptimizedTaps();
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return optimizationResult.getOptimizedSetPoints();
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return null;
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, RangeAction rangeAction, Unit unit) {
        return 0;
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, LinearGlsk linearGlsk, Unit unit) {
        return 0;
    }
}
