/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

import java.util.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeActionResult {
    protected double initialSetpoint = Double.NaN;
    protected double postPraSetpoint = Double.NaN;
    protected final Map<State, Double> postAraSetpoints;
    protected final Map<State, Double> setpointPerState;
    protected State preventiveState = null;

    public RangeActionResult() {
        postAraSetpoints = new HashMap<>();
        setpointPerState = new HashMap<>();
    }

    public double getPreOptimizedSetpointOnState(State state) {
        if (state.isPreventive()) {
            return initialSetpoint;
        } else {
            return getOptimizedSetpointOnState(stateBefore(state));
        }
    }

    public double getOptimizedSetpointOnState(State state) {
        if (setpointPerState.containsKey(state)) {
            return setpointPerState.get(state);
        }
        if (postAraSetpoints.containsKey(state)) {
            return postAraSetpoints.get(state);
        }
        if (Objects.isNull(state) || state.isPreventive()) { // preventiveState can be null
            if (!Double.isNaN(postPraSetpoint)) {
                return postPraSetpoint;
            } else {
                return initialSetpoint;
            }
        }
        return getOptimizedSetpointOnState(stateBefore(state));
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

    public void addPostAraSetpoint(State state, double setpoint) {
        postAraSetpoints.put(state, setpoint);
    }

    public void setInitialSetpoint(double initialSetpoint) {
        this.initialSetpoint = initialSetpoint;
    }

    public void setPostPraSetpoint(Double postPraSetpoint) {
        this.postPraSetpoint = postPraSetpoint;
    }

    protected State stateBefore(State state) {
        if (state.getContingency().isPresent()) {
            return stateBefore(state.getContingency().orElseThrow().getId(), state.getInstant());
        } else {
            return preventiveState;
        }
    }

    private State stateBefore(String contingencyId, Instant instant) {
        if (instant.comesBefore(Instant.AUTO)) {
            return preventiveState;
        }
        State stateBefore = lookupState(contingencyId, instantBefore(instant));
        if (Objects.nonNull(stateBefore)) {
            return stateBefore;
        } else {
            return stateBefore(contingencyId, instantBefore(instant));
        }
    }

    private Instant instantBefore(Instant instant) {
        switch (instant) {
            case PREVENTIVE:
            case OUTAGE:
                return Instant.PREVENTIVE;
            case AUTO:
                return Instant.OUTAGE;
            case CURATIVE:
                return Instant.AUTO;
            default:
                throw new FaraoException(String.format("Unknown instant: %s", instant));
        }
    }

    private State lookupState(String contingencyId, Instant instant) {
        Set<State> knownStates = new HashSet<>(setpointPerState.keySet());
        knownStates.addAll(postAraSetpoints.keySet());
        return knownStates.stream().filter(state -> state.getInstant().equals(instant)
                        && state.getContingency().isPresent() && state.getContingency().get().getId().equals(contingencyId))
                .findAny()
                .orElse(null);
    }
}
