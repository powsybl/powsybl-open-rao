/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RemedialActionActivationResultImpl implements RemedialActionActivationResult {
    private final PrePerimeterResult prePerimeterResult;
    private final NetworkActionsResult networkActionsResult;
    private final RangeActionActivationResult rangeActionActivationResult;

    public RemedialActionActivationResultImpl(PrePerimeterResult prePerimeterResult, RangeActionSetpointResult rangeActionSetpointResult, Set<NetworkAction> activatedNetworkActions) {
        this.prePerimeterResult = prePerimeterResult;
        this.networkActionsResult = new NetworkActionsResultImpl(activatedNetworkActions);
        this.rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
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
        return getOptimizedSetpoint(rangeAction, state) - prePerimeterResult.getSetpoint(rangeAction);
    }

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        return getOptimizedTap(pstRangeAction, state) - prePerimeterResult.getTap(pstRangeAction);
    }
}
