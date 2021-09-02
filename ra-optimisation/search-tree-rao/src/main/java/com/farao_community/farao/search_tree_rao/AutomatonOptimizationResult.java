/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutomatonOptimizationResult implements OptimizationResult {
    @Override
    public double getFlow(FlowCnec flowCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getRelativeMargin(FlowCnec flowCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getLoopFlow(FlowCnec flowCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec) {
        return 0;
    }

    @Override
    public Map<FlowCnec, Double> getPtdfZonalSums() {
        return null;
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return null;
    }

    @Override
    public double getCost() {
        return 0;
    }

    @Override
    public double getFunctionalCost() {
        return 0;
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return null;
    }

    @Override
    public double getVirtualCost() {
        return 0;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return null;
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return 0;
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return null;
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        return null;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return 0;
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return null;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return null;
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
