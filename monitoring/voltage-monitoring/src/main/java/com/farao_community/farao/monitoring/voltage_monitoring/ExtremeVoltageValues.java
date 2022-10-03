package com.farao_community.farao.monitoring.voltage_monitoring;

import java.util.Set;

/**
 * Min and max voltage values on a network element
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ExtremeVoltageValues {
    private final Double min;
    private final Double max;

    public ExtremeVoltageValues(Set<Double> values) {
        this.min = values.stream().min(Double::compareTo).orElse(null);
        this.max = values.stream().max(Double::compareTo).orElse(null);
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }
}
