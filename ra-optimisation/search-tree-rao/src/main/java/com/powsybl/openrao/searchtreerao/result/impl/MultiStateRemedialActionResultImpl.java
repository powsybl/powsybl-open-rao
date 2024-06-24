/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class MultiStateRemedialActionResultImpl {
    private final Map<State, RangeActionResultImpl> rangeActionResultPerState = new HashMap<>();
    private final Map<State, NetworkActionResultImpl> networkActionResultPerState = new HashMap<>();

    public MultiStateRemedialActionResultImpl(PerimeterResultWithCnecs previousPerimeterResult, OptimizationPerimeter optimizationPerimeter) {
        optimizationPerimeter.getRangeActionsPerState().keySet().forEach(
            state -> rangeActionResultPerState.put(state, RangeActionResultImpl.buildFromPreviousPerimeterResult(previousPerimeterResult, optimizationPerimeter.getRangeActionsPerState().get(state)))
        );
        optimizationPerimeter.getRangeActionsPerState().keySet().forEach(
            state -> networkActionResultPerState.put(state, new NetworkActionResultImpl(new HashSet<>()))
        );
    }

    public MultiStateRemedialActionResultImpl(PerimeterResultWithCnecs previousPerimeterResult, AppliedRemedialActions appliedRemedialActions, OptimizationPerimeter optimizationPerimeter) {
        optimizationPerimeter.getRangeActionsPerState().keySet().forEach(
            state -> {
                RangeActionResultImpl rangeActionResult = RangeActionResultImpl.buildFromPreviousPerimeterResult(previousPerimeterResult, optimizationPerimeter.getRangeActionsPerState().get(state));
                appliedRemedialActions.getAppliedRangeActions(state).forEach(rangeActionResult::activate);
                rangeActionResultPerState.put(state, rangeActionResult);
            }
        );
        optimizationPerimeter.getRangeActionsPerState().keySet().forEach(
            state -> {
                NetworkActionResultImpl networkActionResult = new NetworkActionResultImpl(new HashSet<>());
                appliedRemedialActions.getAppliedNetworkActions(state).forEach(networkActionResult::activate);
                networkActionResultPerState.put(state, networkActionResult);
            }
        );
    }

    public AppliedRemedialActions toAppliedRemedialActions() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        rangeActionResultPerState.forEach((state, rangeActionResult) ->
            rangeActionResult.getActivatedRangeActions().forEach(rangeAction ->
                appliedRemedialActions.addAppliedRangeAction(state, rangeAction, rangeActionResult.getOptimizedSetpoint(rangeAction))
            )
        );
        networkActionResultPerState.forEach((state, networkActionResult) ->
            appliedRemedialActions.addAppliedNetworkActions(state, networkActionResult.getActivatedNetworkActions())
        );
        return appliedRemedialActions;
    }

    public void activate(RangeAction<?> rangeAction, State state, double setpoint) {
        rangeActionResultPerState.get(state).activate(rangeAction, setpoint);
        rangeActionResultPerState.keySet().stream()
            .filter(otherState -> otherState.getInstant().comesAfter(state.getInstant()) && otherState.getContingency().equals(state.getContingency()))
            .forEach(otherState -> rangeActionResultPerState.get(otherState).preActivate(rangeAction, setpoint));
    }

    public void activate(NetworkAction networkAction, State state) {
        networkActionResultPerState.get(state).activate(networkAction);
    }

    public Set<NetworkAction> getActivatedNetworkActionsOnState(State state) {
        return networkActionResultPerState.get(state).getActivatedNetworkActions();
    }

    public double getOptimizedSetpointOnState(RangeAction<?> rangeAction, State state) {
        return rangeActionResultPerState.get(state).getOptimizedSetpoint(rangeAction);
    }

    public int getOptimizedTapOnState(PstRangeAction pstRangeAction, State state) {
        return rangeActionResultPerState.get(state).getOptimizedTap(pstRangeAction);
    }

    public Set<RangeAction<?>> getActivatedRangeActionsOnState(State state) {
        return rangeActionResultPerState.get(state).getActivatedRangeActions();
    }

    public RangeActionResultImpl getRangeActionResultOnState(State state) {
        return rangeActionResultPerState.get(state);
    }
}
