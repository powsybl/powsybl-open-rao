/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Angle monitoring result object
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringResult {

    public enum Status {
        SECURE,
        UNSECURE,
        UNKNOWN
    }

    /**
     * Utility class to hold results for a single angleCnec - state duo
     */
    public static class AngleResult {
        private final AngleCnec angleCnec;
        private final State state;
        private final Double angle;

        public AngleResult(AngleCnec angleCnec, State state, Double angle) {
            this.angleCnec = angleCnec;
            this.state = state;
            this.angle = angle;
        }

        public Double getAngle() {
            return angle;
        }

        public State getState() {
            return state;
        }

        public AngleCnec getAngleCnec() {
            return angleCnec;
        }

        public String getId()  {
            return angleCnec.getId();
        }

    }

    private final Set<AngleResult> angleCnecsWithAngle;
    private final Map<State, Set<NetworkAction>> appliedCras;
    private final Status status;

    public AngleMonitoringResult(Set<AngleResult> angleCnecsWithAngle, Map<State, Set<NetworkAction>> appliedCras, Status status) {
        this.angleCnecsWithAngle = angleCnecsWithAngle;
        this.appliedCras = appliedCras;
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSecure() {
        return getStatus() == Status.SECURE; }

    public boolean isUnsecure() {
        return getStatus() == Status.UNSECURE; }

    public boolean isUnknown() {
        return getStatus() == Status.UNKNOWN; }

    public Set<NetworkAction> getAppliedCras(State state) {
        return appliedCras.getOrDefault(state, Collections.emptySet());
    }

    public Set<String> getAppliedCras(String stateId) {
        Set<State> states = appliedCras.keySet().stream().filter(s -> s.getId().equals(stateId)).collect(Collectors.toSet());
        if (states.isEmpty()) {
            return Collections.emptySet();
        } else if (states.size() > 1) {
            throw new FaraoException(String.format("%s states share the same id : %s.", states.size(), stateId));
        } else {
            return appliedCras.get(states.iterator().next()).stream().map(NetworkAction::getId).collect(Collectors.toSet());
        }
    }

    public Set<AngleResult> getAngleCnecsWithAngle() {
        return angleCnecsWithAngle;
    }

    public Map<State, Set<NetworkAction>> getAppliedCras() {
        return appliedCras;
    }

    public double getAngle(AngleCnec angleCnec, State state, Unit unit) {
        if (!unit.equals(Unit.DEGREE)) {
            throw new FaraoException(String.format("Unhandled unit %s for AngleCnec %s", unit.toString(), angleCnec.toString()));
        }
        Set<Double> angles = angleCnecsWithAngle.stream().filter(angleResult -> angleResult.getAngleCnec().equals(angleCnec) && angleResult.getState().equals(state))
                .map(AngleResult::getAngle).collect(Collectors.toSet());
        if (angles.isEmpty()) {
            throw new FaraoException(String.format("AngleMonitoringResult was not defined with AngleCnec %s and state %s", angleCnec.toString(), state.getId()));
        } else if (angles.size() > 1) {
            throw new FaraoException(String.format("AngleMonitoringResult was defined %s times with AngleCnec %s and state %s", angles.size(), angleCnec.toString(), state.getId()));
        } else {
            return angles.iterator().next();
        }
    }

    public List<String> printConstraints() {
        if (isUnknown()) {
            return List.of("Unknown status on AngleCnecs.");
        }
        List<String> constraints = new ArrayList<>();
        angleCnecsWithAngle.stream().forEach(angleResult -> {
            if (AngleMonitoring.thresholdOvershoot(angleResult.getAngleCnec(), angleResult.getAngle())) {
                constraints.add(String.format("AngleCnec %s (with importing network element %s and exporting network element %s)" +
                                " at state %s has an angle of %.0f°.",
                        angleResult.getAngleCnec().getId(),
                        angleResult.getAngleCnec().getImportingNetworkElement().getId(),
                        angleResult.getAngleCnec().getExportingNetworkElement().getId(),
                        angleResult.getState().getId(),
                        angleResult.getAngle()));
            }
        });
        if (constraints.isEmpty()) {
            return List.of("All AngleCnecs are secure.");
        } else {
            return constraints;
        }
    }
}
