package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;

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
            return MonitoringResult.Status.FAILURE;
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
