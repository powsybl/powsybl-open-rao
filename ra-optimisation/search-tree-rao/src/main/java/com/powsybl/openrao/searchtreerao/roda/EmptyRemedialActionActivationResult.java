/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Map;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class EmptyRemedialActionActivationResult implements RemedialActionActivationResult {

    private final Network network;

    EmptyRemedialActionActivationResult() {
        this.network = null;
    }

    EmptyRemedialActionActivationResult(Network network) {
        this.network = network;
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return Set.of();
    }

    @Override
    public Map<State, Set<NetworkAction>> getActivatedNetworkActionsPerState() {
        return Map.of();
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return Set.of();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return Set.of();
    }

    @Override
    public Map<State, Set<RangeAction<?>>> getActivatedRangeActionsPerState() {
        return Map.of();
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        if (this.network == null) {
            throw new OpenRaoException("Network is null");
        }
        return rangeAction.getCurrentSetpoint(network);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return Map.of();
    }

    @Override
    public double getSetPointVariation(RangeAction<?> rangeAction, State state) {
        return 0;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        if (this.network == null) {
            throw new OpenRaoException("Network is null");
        }
        return pstRangeAction.getCurrentTapPosition(network);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return Map.of();
    }

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        return 0;
    }
}
