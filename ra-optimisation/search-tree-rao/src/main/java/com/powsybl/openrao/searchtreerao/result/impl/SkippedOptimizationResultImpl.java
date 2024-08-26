/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SkippedOptimizationResultImpl implements OptimizationResult {
    private static final String SHOULD_NOT_BE_USED = "Should not be used: optimization result has been skipped.";
    private static final String SENSITIVITY_FAILURE_COST = "sensitivity-failure-cost";
    private final State state;
    private final Set<NetworkAction> activatedNetworkActions;
    private final Set<RangeAction<?>> activatedRangeActions;
    private final ComputationStatus computationStatus;
    private final double sensitivityFailureOverCost;

    public SkippedOptimizationResultImpl(State state, Set<NetworkAction> activatedNetworkActions, Set<RangeAction<?>> activatedRangeActions, ComputationStatus computationStatus, double sensitivityFailureOverCost) {
        this.state = state;
        this.activatedNetworkActions = activatedNetworkActions;
        this.activatedRangeActions = activatedRangeActions;
        this.computationStatus = computationStatus;
        this.sensitivityFailureOverCost = sensitivityFailureOverCost;
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
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant instant) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        throw new OpenRaoException(SHOULD_NOT_BE_USED);
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
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
        return sensitivityFailureOverCost;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return Set.of(SENSITIVITY_FAILURE_COST);
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return virtualCostName.equals(SENSITIVITY_FAILURE_COST) ? sensitivityFailureOverCost : 0;
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
