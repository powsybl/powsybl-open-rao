package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;

public class CnecResult {

    private Unit unit;
    private final Cnec cnec;
    private final Double value;

    private final Cnec.CnecSecurityStatus cnecSecurityStatus;

    public CnecResult(Cnec cnec, Double value, Unit unit, Cnec.CnecSecurityStatus cnecSecurityStatus) {
        this.cnec = cnec;
        this.value = value;
        this.unit = unit;
        this.cnecSecurityStatus = cnecSecurityStatus;
    }

    public Double getValue() {
        return value;
    }

    public Cnec getCnec() {
        return cnec;
    }

    public State getState() {
        return cnec.getState();
    }

    public String getId() {
        return cnec.getId();
    }

    public Unit getUnit() {
        return unit;
    }

    public Cnec.CnecSecurityStatus getCnecSecurityStatus() {
        return cnecSecurityStatus;
    }

    public boolean thresholdOvershoot() {
        return cnec.computeMargin(value, unit) < 0;
    }

    public String print() {
        if (unit.equals(Unit.DEGREE)) {
            AngleCnec angleCnec = (AngleCnec) cnec;
            return String.format("AngleCnec %s (with importing network element %s and exporting network element %s)" +
                " at state %s has an angle of %.0f°.", angleCnec.getId(), angleCnec.getImportingNetworkElement().getId(), angleCnec.getExportingNetworkElement().getId(), cnec.getState().getId(), value);
        } else {
            VoltageCnec voltageCnec = (VoltageCnec) cnec;
            return String.format("Network element %s at state %s has a voltage of %.0f kV.",
                voltageCnec.getNetworkElement().getId(),
                voltageCnec.getState().getId(),
                value
            );
        }

    }

}


