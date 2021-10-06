package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

import java.util.*;

public class RangeActionResult {
    protected String networkElementId;
    protected double preOptimSetpoint;
    protected Map<State, Double> setpointPerState;
    protected State preventiveState = null;

    public RangeActionResult(String networkElementId) {
        this.networkElementId = networkElementId;
        preOptimSetpoint = Double.NaN;
        setpointPerState = new HashMap<>();
    }

    public String getNetworkElementId() {
        return networkElementId;
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

    public void setNetworkElementId(String networkElementId) {
        this.networkElementId = networkElementId;
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
