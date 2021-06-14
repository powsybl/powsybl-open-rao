/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FailedRaoOutput implements SearchTreeRaoResult {
    private static final String SHOULD_NOT_BE_USED = "Should not be used: the RAO failed.";
    //TODO: add optimization status (failed for this implem)

    @Override
    public ComputationStatus getComputationStatus() {
        return ComputationStatus.FAILURE;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public PerimeterResult getPostPreventivePerimeterResult() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public PrePerimeterResult getInitialResult() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(OptimizationState optimizationState, int number) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<String> getVirtualCostNames() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public List<FlowCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction rangeAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction rangeAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction rangeAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<RangeAction> getActivatedRangeActionsDuringState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

}
