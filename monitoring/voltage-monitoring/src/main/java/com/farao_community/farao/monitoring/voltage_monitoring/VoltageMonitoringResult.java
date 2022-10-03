/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring;

import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;

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
        HIGH_AND_LOW_VOLTAGE_CONSTRAINTS;

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
    private List<String> constraints;

    public VoltageMonitoringResult(Map<VoltageCnec, ExtremeVoltageValues> extremeVoltageValues) {
        this.extremeVoltageValues = extremeVoltageValues;
        Set<VoltageCnec> tmpConstrainedElements = new HashSet<>();
        boolean highVoltageConstraints = false;
        boolean lowVoltageConstraints = false;
        for (Map.Entry<VoltageCnec, ExtremeVoltageValues> entry : extremeVoltageValues.entrySet()) {
            if (entry.getKey().getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMax() && entry.getValue().getMax() != null && entry.getValue().getMax() > threshold.max().orElseThrow())) {
                tmpConstrainedElements.add(entry.getKey());
                highVoltageConstraints = true;
            }
            if (entry.getKey().getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMin() && entry.getValue().getMin() != null && entry.getValue().getMin() < threshold.min().orElseThrow())) {
                tmpConstrainedElements.add(entry.getKey());
                lowVoltageConstraints = true;
            }
        }
        this.constrainedElements = Collections.unmodifiableSet(tmpConstrainedElements);
        this.status = Status.fromConstraints(highVoltageConstraints, lowVoltageConstraints);
    }

    public Set<VoltageCnec> getConstrainedElements() {
        return constrainedElements;
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
