package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;

import static com.powsybl.openrao.monitoring.results.MonitoringResult.Status.*;

/**
 * Utility class to hold results for a single angleCnec
 */
public class AngleCnecResult implements CnecResult {
    private final AngleCnec angleCnec;
    private final Double angle;

    public AngleCnecResult(AngleCnec angleCnec, Double angle) {
        this.angleCnec = angleCnec;
        this.angle = angle;
    }

    public Double getAngle() {
        return angle;
    }

    public AngleCnec getAngleCnec() {
        return angleCnec;
    }

    public State getState() {
        return angleCnec.getState();
    }

    public String getId() {
        return angleCnec.getId();
    }

    @Override
    public Cnec getCnec() {
        return angleCnec;
    }

    @Override
    public boolean thresholdOvershoot() {
        return angleCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMax() && angle != null && angle > threshold.max().orElseThrow())
            || angleCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMin() && angle != null && angle < threshold.min().orElseThrow());
    }

    @Override
    public MonitoringResult.Status getStatus() {
        if (thresholdOvershoot()) {
            boolean highVoltageConstraints = false;
            boolean lowVoltageConstraints = false;
            if (angleCnec.getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMax() && angle > threshold.max().orElseThrow())) {
                highVoltageConstraints = true;
            }
            if (angleCnec.getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMin() && angle < threshold.min().orElseThrow())) {
                lowVoltageConstraints = true;
            }
            if (highVoltageConstraints && lowVoltageConstraints) {
                return HIGH_AND_LOW_CONSTRAINTS;
            } else if (highVoltageConstraints) {
                return HIGH_CONSTRAINT;
            } else {
                return LOW_CONSTRAINT;
            }
        } else {
            return MonitoringResult.Status.SECURE;
        }
    }

    @Override
    public String print() {
        return String.format("AngleCnec %s (with importing network element %s and exporting network element %s)" +
            " at state %s has an angle of %.0f°.", angleCnec.getId(), angleCnec.getImportingNetworkElement().getId(), angleCnec.getExportingNetworkElement().getId(), angleCnec.getState().getId(), angle);
    }
}
