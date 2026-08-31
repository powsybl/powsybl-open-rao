/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FailedRaoResultImpl extends AbstractExtendable<RaoResult> implements RaoResult {
    private String failureReason;
    private final String exceptionMessage;

    public FailedRaoResultImpl(String failureReason) {
        this.failureReason = failureReason;
        this.exceptionMessage = "This method should not be used, because the RAO failed: " + failureReason;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return ComputationStatus.FAILURE;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return ComputationStatus.FAILURE;
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public void setExecutionDetails(String executionDetails) {
        this.failureReason = executionDetails;
    }

    @Override
    public String getExecutionDetails() {
        return this.failureReason;
    }
}
