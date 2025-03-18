/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.raomock;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class GlobalRaoResultMock implements GlobalRaoResult {
    @Override
    public List<OffsetDateTime> getTimestamps() {
        return null;
    }

    @Override
    public double getGlobalFunctionalCost() {
        return 0;
    }

    @Override
    public double getGlobalVirtualCost() {
        return 0;
    }

    @Override
    public double getGlobalVirtualCost(String virtualCostName) {
        return 0;
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return 0;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return 0;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName, OffsetDateTime timestamp) {
        return 0;
    }

    @Override
    public boolean isSecure(Instant optimizedInstant, OffsetDateTime timestamp, PhysicalParameter... u) {
        return false;
    }

    @Override
    public boolean isSecure(OffsetDateTime timestamp, PhysicalParameter... u) {
        return false;
    }

    @Override
    public RaoResult getIndividualRaoResult(OffsetDateTime timestamp) {
        return null;
    }

    @Override
    public void write(ZipOutputStream zipOutputStream, TemporalData<Crac> cracs) {

    }

    @Override
    public ComputationStatus getComputationStatus() {
        return null;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return null;
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return 0;
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return 0;
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return 0;
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        return 0;
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        return 0;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        return 0;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return null;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        return 0;
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
        return null;
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return false;
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return 0;
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return 0;
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return null;
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return null;
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return null;
    }

    @Override
    public String getExecutionDetails() {
        return null;
    }

    @Override
    public void setExecutionDetails(String executionDetails) {

    }

    @Override
    public boolean isSecure(Instant optimizedInstant, PhysicalParameter... u) {
        return false;
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return false;
    }
}
