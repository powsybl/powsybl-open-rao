package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;

import static com.powsybl.openrao.monitoring.results.MonitoringResult.Status.*;

import java.util.Set;

/**
 * Utility class to hold results for a single voltageCnec
 */
public class VoltageCnecResult implements CnecResult {

    private final VoltageCnec voltageCnec;
    private final ExtremeVoltageValues extremeVoltageValues;

    public VoltageCnecResult(VoltageCnec voltageCnec, ExtremeVoltageValues extremeVoltageValues) {
        this.voltageCnec = voltageCnec;
        this.extremeVoltageValues = extremeVoltageValues;
    }

    public ExtremeVoltageValues getExtremeVoltageValues() {
        return extremeVoltageValues;
    }

    public VoltageCnec getVoltageCnec() {
        return voltageCnec;
    }

    public State getState() {
        return voltageCnec.getState();
    }

    public String getId() {
        return voltageCnec.getId();
    }

    public static class ExtremeVoltageValues {
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

    @Override
    public Cnec getCnec() {
        return voltageCnec;
    }

    @Override
    public boolean thresholdOvershoot() {
        return voltageCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMax() && extremeVoltageValues.getMax() != null && extremeVoltageValues.getMax() > threshold.max().orElseThrow())
            || voltageCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMin() && extremeVoltageValues.getMin() != null && extremeVoltageValues.getMin() < threshold.min().orElseThrow());
    }

    @Override
    public MonitoringResult.Status getStatus() {
        boolean highVoltageConstraints = false;
        boolean lowVoltageConstraints = false;
        if (voltageCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMax() && extremeVoltageValues.getMax() != null && extremeVoltageValues.getMax() > threshold.max().orElseThrow())) {
            highVoltageConstraints = true;
        }
        if (voltageCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMin() && extremeVoltageValues.getMin() != null && extremeVoltageValues.getMin() < threshold.min().orElseThrow())) {
            lowVoltageConstraints = true;
        }
        if (highVoltageConstraints && lowVoltageConstraints) {
            return HIGH_AND_LOW_CONSTRAINTS;
        } else if (highVoltageConstraints) {
            return HIGH_CONSTRAINT;
        } else if (lowVoltageConstraints) {
            return LOW_CONSTRAINT;
        } else {
            return SECURE;
        }
    }

    @Override
    public String print() {
        return String.format("Network element %s at state %s has a voltage of %.0f - %.0f kV.",
            voltageCnec.getNetworkElement().getId(),
            voltageCnec.getState().getId(),
            extremeVoltageValues.getMin(),
            extremeVoltageValues.getMax()
        );
    }
}
