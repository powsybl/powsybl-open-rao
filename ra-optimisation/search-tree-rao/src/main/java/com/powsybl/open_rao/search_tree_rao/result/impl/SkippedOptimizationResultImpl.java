/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.result.impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.*;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.powsybl.open_rao.search_tree_rao.result.api.OptimizationResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SkippedOptimizationResultImpl implements OptimizationResult {
    private static final String SHOULD_NOT_BE_USED = "Should not be used: optimization result has been skipped.";
    private final State state;
    private final Set<NetworkAction> activatedNetworkActions;
    private final Set<RangeAction<?>> activatedRangeActions;
    private final ComputationStatus computationStatus;

    public SkippedOptimizationResultImpl(State state, Set<NetworkAction> activatedNetworkActions, Set<RangeAction<?>> activatedRangeActions, ComputationStatus computationStatus) {
        this.state = state;
        this.activatedNetworkActions = activatedNetworkActions;
        this.activatedRangeActions = activatedRangeActions;
        this.computationStatus = computationStatus;
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return computationStatus;
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return computationStatus;
    }

    @Override
    // The following method is used to determine which contingencies must be excluded from cost computation
    public Set<String> getContingencies() {
        if (computationStatus != ComputationStatus.FAILURE) {
            Optional<Contingency> contingency = state.getContingency();
            if (contingency.isPresent()) {
                return Set.of(contingency.get().getId());
            }
        }
        return new HashSet<>();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, RangeAction<?> rangeAction, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, SensitivityVariableSet linearGlsk, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return activatedNetworkActions.contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return new HashSet<>(activatedNetworkActions);
    }

    @Override
    public double getFunctionalCost() {
        return -1.0;
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
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        //do not do anything
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return activatedRangeActions;
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return activatedRangeActions;
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }
}
