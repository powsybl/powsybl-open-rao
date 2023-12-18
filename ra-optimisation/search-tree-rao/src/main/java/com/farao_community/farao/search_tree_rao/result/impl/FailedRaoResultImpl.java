/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.result.impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.*;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.data.rao_result_api.OptimizationStepsExecuted;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FailedRaoResultImpl implements RaoResult {
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
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, Side side) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<String> getVirtualCostNames() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
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
}
