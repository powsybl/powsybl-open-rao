/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A RaoResult implementation that contains the initial situation before optimization
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UnoptimizedRaoResultImpl extends AbstractExtendable<RaoResult> implements RaoResult {
    private final PrePerimeterResult initialResult;
    private String executionDetails = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;

    public UnoptimizedRaoResultImpl(PrePerimeterResult initialResult) {
        this.initialResult = initialResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return initialResult.getSensitivityStatus();
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return initialResult.getSensitivityStatus(state);
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return new HashSet<>();
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return false;
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return initialResult.getTap(pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return getPreOptimizationTapOnState(state, pstRangeAction);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return initialResult.getSetpoint(rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return getPreOptimizationSetPointOnState(state, rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return new HashSet<>();
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        Map<PstRangeAction, Integer> tapPerPst = new HashMap<>();
        initialResult.getRangeActions().forEach(ra -> {
            if (ra instanceof PstRangeAction pstRangeAction) {
                tapPerPst.put(pstRangeAction, initialResult.getTap(pstRangeAction));
            }
        });
        return tapPerPst;
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        Map<RangeAction<?>, Double> setpointPerRa = new HashMap<>();
        initialResult.getRangeActions().forEach(ra ->
            setpointPerRa.put(ra, initialResult.getSetpoint(ra))
        );
        return setpointPerRa;
    }

    @Override
    public void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
    }

    @Override
    public String getExecutionDetails() {
        return executionDetails;
    }
}
