/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RemedialActionActivationResultImpl implements RemedialActionActivationResult {
    private final NetworkActionsResult networkActionsResult;
    private final RangeActionActivationResult rangeActionActivationResult;

    public RemedialActionActivationResultImpl(RangeActionActivationResult rangeActionActivationResult, NetworkActionsResult networkActionsResult) {
        this.networkActionsResult = networkActionsResult;
        this.rangeActionActivationResult = rangeActionActivationResult;
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return networkActionsResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return networkActionsResult.getActivatedNetworkActions();
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return rangeActionActivationResult.getRangeActions();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return rangeActionActivationResult.getActivatedRangeActions(state);
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        return rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return rangeActionActivationResult.getOptimizedSetpointsOnState(state);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return rangeActionActivationResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return rangeActionActivationResult.getOptimizedTapsOnState(state);
    }

    @Override
    public double getSetPointVariation(RangeAction<?> rangeAction, State state) {
        return rangeActionActivationResult.getSetPointVariation(rangeAction, state);
    }

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        return rangeActionActivationResult.getTapVariation(pstRangeAction, state);
    }

    public static RemedialActionActivationResultImpl empty(RangeActionSetpointResult rangeActionSetpointResult) {
        return new RemedialActionActivationResultImpl(new RangeActionActivationResultImpl(rangeActionSetpointResult), new NetworkActionsResultImpl(Set.of()));
    }
}
