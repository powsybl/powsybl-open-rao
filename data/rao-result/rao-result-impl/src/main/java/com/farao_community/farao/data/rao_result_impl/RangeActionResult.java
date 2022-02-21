/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

import java.util.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeActionResult {
    protected double preOptimSetpoint;
    protected Map<State, Double> setpointPerState;
    protected State preventiveState = null;

    public RangeActionResult() {
        preOptimSetpoint = Double.NaN;
        setpointPerState = new HashMap<>();
    }

    public double getPreOptimSetpoint() {
        return preOptimSetpoint;
    }

    public double getPreOptimizedSetpointOnState(State state) {
        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant
        if (!state.getInstant().equals(Instant.PREVENTIVE) && Objects.nonNull(preventiveState)) {
            return setpointPerState.get(preventiveState);
        }
        return preOptimSetpoint;
    }

    public double getOptimizedSetpointOnState(State state) {
        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant
        if (setpointPerState.containsKey(state)) {
            return setpointPerState.get(state);
        } else if (!state.getInstant().equals(Instant.PREVENTIVE) && Objects.nonNull(preventiveState)) {
            return setpointPerState.get(preventiveState);
        }
        return preOptimSetpoint;
    }

    public boolean isActivatedDuringState(State state) {
        return setpointPerState.containsKey(state);
    }

    public Set<State> getStatesWithActivation() {
        return setpointPerState.keySet();
    }

    public void setPreOptimSetPoint(double setpoint) {
        this.preOptimSetpoint = setpoint;
    }

    public void addActivationForState(State state, double setpoint) {
        setpointPerState.put(state, setpoint);
        if (state.isPreventive()) {
            preventiveState = state;
        }
    }
}
