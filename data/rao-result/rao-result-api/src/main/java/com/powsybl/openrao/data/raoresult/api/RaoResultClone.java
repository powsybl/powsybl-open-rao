/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;

import java.util.Map;
import java.util.Set;

/**
 * Clone that reproduces the results obtained by another RaoResult instance. It can be used to override some methods without rewriting all the implementations
 *
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResultClone extends AbstractExtendable<RaoResult> implements RaoResult {

    private final RaoResult raoResult;

    protected RaoResultClone(RaoResult raoResult) {
        if (raoResult == null) {
            throw new OpenRaoException("RaoResult must not be null");
        }
        this.raoResult = raoResult;
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return raoResult.getComputationStatus();
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return raoResult.getComputationStatus(state);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return raoResult.getFlow(optimizedInstant, flowCnec, side, unit);
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return raoResult.getMargin(optimizedInstant, flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return raoResult.getRelativeMargin(optimizedInstant, flowCnec, unit);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return raoResult.getLoopFlow(optimizedInstant, flowCnec, side, unit);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return raoResult.getCommercialFlow(optimizedInstant, flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        return raoResult.getPtdfZonalSum(optimizedInstant, flowCnec, side);
    }

    @Override
    public double getCost(Instant optimizedInstant) {
        return raoResult.getCost(optimizedInstant);
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        return raoResult.getFunctionalCost(optimizedInstant);
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        return raoResult.getVirtualCost(optimizedInstant);
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return raoResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        return raoResult.getVirtualCost(optimizedInstant, virtualCostName);
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        return raoResult.isActivatedDuringState(state, remedialAction);
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return raoResult.wasActivatedBeforeState(state, networkAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return raoResult.isActivatedDuringState(state, networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return raoResult.getActivatedNetworkActionsDuringState(state);
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return raoResult.isActivatedDuringState(state, rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return raoResult.getPreOptimizationTapOnState(state, pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return raoResult.getOptimizedTapOnState(state, pstRangeAction);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return raoResult.getPreOptimizationSetPointOnState(state, rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return raoResult.getOptimizedSetPointOnState(state, rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return raoResult.getActivatedRangeActionsDuringState(state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return raoResult.getOptimizedTapsOnState(state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return raoResult.getOptimizedSetPointsOnState(state);
    }

    @Override
    public void setExecutionDetails(String executionDetails) {
        raoResult.setExecutionDetails(executionDetails);
    }

    @Override
    public boolean isSecure(Instant optimizedInstant, PhysicalParameter... u) {
        return raoResult.isSecure(optimizedInstant, u);
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return raoResult.isSecure(u);
    }

    @Override
    public boolean isSecure() {
        return raoResult.isSecure();
    }

    @Override
    public String getExecutionDetails() {
        return raoResult.getExecutionDetails();
    }
}
