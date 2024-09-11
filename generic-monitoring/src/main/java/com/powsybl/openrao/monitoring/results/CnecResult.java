package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.CnecValue;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracimpl.AngleCnecValue;
import com.powsybl.openrao.data.cracimpl.VoltageCnecValue;

public class CnecResult<T extends CnecValue> {

    private final Cnec cnec;
    private Unit unit;
    private final T value;
    private final double worstCnecMargin;

    private final Cnec.CnecSecurityStatus cnecSecurityStatus;

    public CnecResult(Cnec cnec, Unit unit, T value, double worstCnecMargin, Cnec.CnecSecurityStatus cnecSecurityStatus) {
        this.cnec = cnec;
        this.unit = unit;
        this.value = value;
        this.worstCnecMargin = worstCnecMargin;
        this.cnecSecurityStatus = cnecSecurityStatus;
    }

    public T getValue() {
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

    public double getWorstCnecMargin() {
        return worstCnecMargin;
    }

    public String print() {
        switch (value) {

            case VoltageCnecValue voltageValue -> {
                VoltageCnec voltageCnec = (VoltageCnec) cnec;
                return String.format("Network element %s at state %s has a min voltage of %.0f kV and a max voltage of %.0f kV.",
                    voltageCnec.getNetworkElement().getId(),
                    voltageCnec.getState().getId(),
                    voltageValue.minValue(),
                    voltageValue.maxValue());
            }
            case AngleCnecValue angleValue -> {
                AngleCnec angleCnec = (AngleCnec) cnec;
                return String.format("AngleCnec %s (with importing network element %s and exporting network element %s) at state %s has an angle of %.0f°.",
                    angleCnec.getId(),
                    angleCnec.getImportingNetworkElement().getId(),
                    angleCnec.getExportingNetworkElement().getId(),
                    cnec.getState().getId(),
                    angleValue.value());

            }
            default -> throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    public boolean thresholdOvershoot() {
        return this.getWorstCnecMargin() < 0;
    }
}


