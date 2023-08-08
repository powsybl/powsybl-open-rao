/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Voltage monitoring result object
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoringResult {

    public enum Status {
        SECURE,
        HIGH_VOLTAGE_CONSTRAINT,
        LOW_VOLTAGE_CONSTRAINT,
        HIGH_AND_LOW_VOLTAGE_CONSTRAINTS,
        UNKNOWN;

        static Status fromConstraints(boolean highVoltageConstraints, boolean lowVoltageConstraints) {
            if (highVoltageConstraints && lowVoltageConstraints) {
                return HIGH_AND_LOW_VOLTAGE_CONSTRAINTS;
            } else if (highVoltageConstraints) {
                return HIGH_VOLTAGE_CONSTRAINT;
            } else if (lowVoltageConstraints) {
                return LOW_VOLTAGE_CONSTRAINT;
            } else {
                return SECURE;
            }
        }
    }

    private final Status status;
    private final Map<VoltageCnec, ExtremeVoltageValues> extremeVoltageValues;
    private final Set<VoltageCnec> constrainedElements;
    private final Map<State, Set<NetworkAction>> appliedCras;
    private List<String> constraints;

    public VoltageMonitoringResult(Map<VoltageCnec, ExtremeVoltageValues> extremeVoltageValues, Map<State, Set<NetworkAction>> appliedCras, Status status) {
        this.extremeVoltageValues = extremeVoltageValues;
        Set<VoltageCnec> tmpConstrainedElements = new HashSet<>();
        this.constrainedElements = Collections.unmodifiableSet(tmpConstrainedElements);
        for (Map.Entry<VoltageCnec, ExtremeVoltageValues> entry : extremeVoltageValues.entrySet()) {
            if (entry.getKey().getThresholds().stream()
                    .anyMatch(threshold -> threshold.limitsByMax() && entry.getValue().getMax() != null && entry.getValue().getMax() > threshold.max().orElseThrow())) {
                tmpConstrainedElements.add(entry.getKey());
            }
            if (entry.getKey().getThresholds().stream()
                    .anyMatch(threshold -> threshold.limitsByMin() && entry.getValue().getMin() != null && entry.getValue().getMin() < threshold.min().orElseThrow())) {
                tmpConstrainedElements.add(entry.getKey());
            }
        }
        this.appliedCras = appliedCras;
        this.status = status;
    }

    public static Status getUnsecureStatus(Map<VoltageCnec, ExtremeVoltageValues> extremeVoltageValues) {
        boolean highVoltageConstraints = false;
        boolean lowVoltageConstraints = false;
        for (Map.Entry<VoltageCnec, ExtremeVoltageValues> entry : extremeVoltageValues.entrySet()) {
            if (entry.getKey().getThresholds().stream()
                    .anyMatch(threshold -> threshold.limitsByMax() && entry.getValue().getMax() != null && entry.getValue().getMax() > threshold.max().orElseThrow())) {
                highVoltageConstraints = true;
            }
            if (entry.getKey().getThresholds().stream()
                    .anyMatch(threshold -> threshold.limitsByMin() && entry.getValue().getMin() != null && entry.getValue().getMin() < threshold.min().orElseThrow())) {
                lowVoltageConstraints = true;
            }
        }
        return Status.fromConstraints(highVoltageConstraints, lowVoltageConstraints);
    }

    public Set<VoltageCnec> getConstrainedElements() {
        return constrainedElements;
    }

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

    public Map<State, Set<NetworkAction>> getAppliedCras() {
        return appliedCras;
    }

    public Status getStatus() {
        return status;
    }

    public Double getMinVoltage(VoltageCnec voltageCnec) {
        return extremeVoltageValues.get(voltageCnec).getMin();
    }

    public Double getMinVoltage(String voltageCnecId) {
        return getMinVoltage(extremeVoltageValues.keySet().stream().filter(vc -> vc.getId().equals(voltageCnecId)).findAny().orElseThrow());
    }

    public Double getMaxVoltage(VoltageCnec voltageCnec) {
        return extremeVoltageValues.get(voltageCnec).getMax();
    }

    public Double getMaxVoltage(String voltageCnecId) {
        return getMinVoltage(extremeVoltageValues.keySet().stream().filter(vc -> vc.getId().equals(voltageCnecId)).findAny().orElseThrow());
    }

    public Map<VoltageCnec, ExtremeVoltageValues> getExtremeVoltageValues() {
        return extremeVoltageValues;
    }

    public List<String> printConstraints() {
        if (constraints == null) {
            if (constrainedElements.isEmpty()) {
                constraints = List.of("All voltage CNECs are secure.");
            } else {
                constraints = constrainedElements.stream()
                    .sorted(Comparator.comparing(VoltageCnec::getId)).map(vc ->
                        String.format("Network element %s at state %s has a voltage of %.0f - %.0f kV.",
                            vc.getNetworkElement().getId(),
                            vc.getState().getId(),
                            extremeVoltageValues.get(vc).getMin(),
                            extremeVoltageValues.get(vc).getMax()
                        )
                    ).collect(Collectors.toList());
            }
        }
        return constraints;
    }
}
