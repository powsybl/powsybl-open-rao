/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SkippedOptimizationResultImpl implements OptimizationResult {
    private static final String SHOULD_NOT_BE_USED = "Should not be used: the RAO failed.";

    @Override
    public ComputationStatus getSensitivityStatus() {
        return ComputationStatus.FAILURE;
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return ComputationStatus.FAILURE;
    }

    @Override
    public Set<String> getContingencies() {
        return new HashSet<>();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, RangeAction<?> rangeAction, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, SensitivityVariableSet linearGlsk, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFunctionalCost() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return new ArrayList<>();
    }

    @Override
    public double getVirtualCost() {
        return 0;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return new HashSet<>();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return 0;
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        //do not do anything
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        throw new FaraoException(SHOULD_NOT_BE_USED);
    }
}
