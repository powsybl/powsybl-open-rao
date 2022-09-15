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
    protected double initialSetpoint = Double.NaN;
    protected double postPraSetpoint = Double.NaN;
    protected Map<State, Double> setpointPerState;
    protected State preventiveState = null;

    public RangeActionResult() {
        setpointPerState = new HashMap<>();
    }

    public double getPreOptimizedSetpointOnState(State state) {
        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant
        if (!state.getInstant().equals(Instant.PREVENTIVE) && Objects.nonNull(preventiveState) && setpointPerState.containsKey(preventiveState)) {
            return setpointPerState.get(preventiveState);
        } else if (!state.getInstant().equals(Instant.PREVENTIVE) && !Double.isNaN(postPraSetpoint)) {
            return postPraSetpoint;
        }
        return initialSetpoint;
    }

    public double getOptimizedSetpointOnState(State state) {
        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant
        if (setpointPerState.containsKey(state)) {
            return setpointPerState.get(state);
        } else if (!state.getInstant().equals(Instant.PREVENTIVE) && Objects.nonNull(preventiveState) && setpointPerState.containsKey(preventiveState)) {
            return setpointPerState.get(preventiveState);
        } else if (!Double.isNaN(postPraSetpoint)) {
            return postPraSetpoint;
        }
        return initialSetpoint;
    }

    public boolean isActivatedDuringState(State state) {
        return setpointPerState.containsKey(state);
    }

    public Set<State> getStatesWithActivation() {
        return setpointPerState.keySet();
    }

    public void addActivationForState(State state, double setpoint) {
        setpointPerState.put(state, setpoint);
        if (state.isPreventive()) {
            preventiveState = state;
        }
    }

    public void setInitialSetpoint(double initialSetpoint) {
        this.initialSetpoint = initialSetpoint;
    }

    public void setPostPraSetpoint(Double postPraSetpoint) {
        this.postPraSetpoint = postPraSetpoint;
    }
}
