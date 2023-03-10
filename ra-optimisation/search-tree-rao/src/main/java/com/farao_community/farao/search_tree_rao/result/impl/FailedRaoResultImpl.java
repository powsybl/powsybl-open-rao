/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.PerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.SearchTreeRaoResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FailedRaoResultImpl implements SearchTreeRaoResult {
    private static final String SHOULD_NOT_BE_USED = "Should not be used: the RAO failed.";

    @Override
    public ComputationStatus getComputationStatus() {
        return ComputationStatus.FAILURE;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return ComputationStatus.FAILURE;
    }

    @Override
    public double getMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getRelativeMargin(OptimizationState optimizationState, FlowCnec flowCnec, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFlow(OptimizationState optimizationState, FlowCnec flowCnec, Side side, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getCommercialFlow(OptimizationState optimizationState, FlowCnec flowCnec, Side side, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getLoopFlow(OptimizationState optimizationState, FlowCnec flowCnec, Side side, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getPtdfZonalSum(OptimizationState optimizationState, FlowCnec flowCnec, Side side) {
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
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
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
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
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
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public OptimizationStepsExecuted getOptimizationStepsExecuted() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getAngle(OptimizationState optimizationState, AngleCnec angleCnec, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getVoltage(OptimizationState optimizationState, VoltageCnec voltageCnec, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getMargin(OptimizationState optimizationState, AngleCnec angleCnec, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getMargin(OptimizationState optimizationState, VoltageCnec voltageCnec, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }
}
