/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.data.crac.api.State;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeActionResult {
    protected double initialSetpoint = Double.NaN;
    protected final Map<State, Double> setpointPerState;
    protected State preventiveState = null;

    public RangeActionResult() {
        setpointPerState = new HashMap<>();
    }

    public double getInitialSetpoint() {
        return initialSetpoint;
    }

    public Double getOptimizedSetpointOnState(State state) {
        State lastActivatedStateBefore = getLastActivatedBefore(state);
        if (Objects.nonNull(lastActivatedStateBefore)) {
            return setpointPerState.get(lastActivatedStateBefore);
        }
        return initialSetpoint;
    }

    private State getLastActivatedBefore(State state) {
        return setpointPerState.keySet().stream().filter(otherState -> otherState.isPreventive() || otherState.getContingency().equals(state.getContingency()))
                .filter(otherState -> otherState.getInstant().equals(state.getInstant()) || otherState.getInstant().comesBefore(state.getInstant()))
                .max(Comparator.comparingInt(s -> s.getInstant().getOrder())).orElse(null);
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
}
