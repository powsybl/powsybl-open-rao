/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
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
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public Set<String> getVirtualCostNames() {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        return false;
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
        return Set.of();
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return false;
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

    @Override
    public boolean isSecure(Instant optimizedInstant, PhysicalParameter... u) {
        throw new OpenRaoException(exceptionMessage);
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        throw new OpenRaoException(exceptionMessage);
    }
}
