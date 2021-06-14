package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PstRangeActionResult {

    private final static PstRangeActionResultPerState DEFAULT_RESULT_PER_STATE = new PstRangeActionResultPerState();

    private String pstNetworkElementId;
    private Integer initialTap;
    private double initialSetpoint;
    private Map<State, PstRangeActionResultPerState> activationPerState;

    private static class PstRangeActionResultPerState {
        private Integer tap = null;
        private double setpoint = Double.NaN;
    }

    public PstRangeActionResult(String pstNetworkElementId) {
        this.pstNetworkElementId = pstNetworkElementId;
        initialTap = null;
        initialSetpoint = Double.NaN;
        activationPerState = new HashMap<>();
    }

    public void setPstNetworkElementId(String pstNetworkElementId) {
        this.pstNetworkElementId = pstNetworkElementId;
    }

    public void setInitialTap(int initialTap) {
        this.initialTap = initialTap;
    }

    public void setInitialSetPoint(double setpoint) {
        this.initialSetpoint = setpoint;
    }

    public void addActivationForState(State state, int tap, double setpoint) {
        PstRangeActionResultPerState pstRangeActionResultPerState = new PstRangeActionResultPerState();
        pstRangeActionResultPerState.tap = tap;
        pstRangeActionResultPerState.setpoint = setpoint;
        activationPerState.put(state, pstRangeActionResultPerState);
    }

    public String getPstNetworkElementId() {
        return pstNetworkElementId;
    }

    public int getInitialTap() {
        return initialTap;
    }

    public double getInitialSetpoint() {
        return initialSetpoint;
    }

    public boolean isActivatedDuringState(State state) {
        return activationPerState.containsKey(state);
    }

    public int getOptimizedTapOnState(State state) {

        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant

        if (activationPerState.containsKey(state)) {
            return activationPerState.get(state).tap;
        }

        else if (state.getInstant() == Instant.CURATIVE) {
            Optional<PstRangeActionResultPerState> resultForPreventiveState = findActivationForPreventiveState();

            if (resultForPreventiveState.isPresent()) {
                return resultForPreventiveState.get().tap;
            }
        }

        return initialTap;
    }

    public double getOptimizedSetpointOnState(State state) {

        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant

        if (activationPerState.containsKey(state)) {
            return activationPerState.get(state).setpoint;
        }

        else if (state.getInstant() == Instant.CURATIVE) {
            Optional<PstRangeActionResultPerState> resultForPreventiveState = findActivationForPreventiveState();

            if (resultForPreventiveState.isPresent()) {
                return resultForPreventiveState.get().setpoint;
            }
        }

        return initialSetpoint;
    }

    public int getPreOptimizedTapOnState(State state) {

        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant

        if (state.getInstant().equals(Instant.CURATIVE)) {
            Optional<PstRangeActionResultPerState> resultForPreventiveState = findActivationForPreventiveState();

            if (resultForPreventiveState.isPresent()) {
                return resultForPreventiveState.get().tap;
            }
        }

        return initialTap;
    }

    public double getPreOptimizedSetpointOnState(State state) {

        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant

        if (state.getInstant().equals(Instant.CURATIVE)) {
            Optional<PstRangeActionResultPerState> resultForPreventiveState = findActivationForPreventiveState();

            if (resultForPreventiveState.isPresent()) {
                return resultForPreventiveState.get().setpoint;
            }
        }

        return initialSetpoint;
    }

    public Set<State> getStatesWithActivation() {
        return activationPerState.keySet();
    }

    private Optional<PstRangeActionResultPerState> findActivationForPreventiveState() {
        return activationPerState.entrySet().stream()
            .filter(e -> e.getKey().isPreventive())
            .map(Map.Entry::getValue)
            .findAny();
    }
}
