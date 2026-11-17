/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.CnecValue;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.impl.AngleCnecValue;
import com.powsybl.openrao.data.crac.impl.VoltageCnecValue;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */

/**
 * @deprecated Will be replaced by CnecMonitoringResult
 */
@Deprecated(since = "7.5.0")
public class CnecResult<T extends CnecValue> {

    private final Cnec<?> cnec;
    private Unit unit;
    private final T value;
    private final double margin;

    private final Cnec.SecurityStatus securityStatus;

    @Deprecated(since = "7.5.0")
    public CnecResult(Cnec<?> cnec, Unit unit, T value, double margin, Cnec.SecurityStatus securityStatus) {
        this.cnec = cnec;
        this.unit = unit;
        this.value = value;
        this.margin = margin;
        this.securityStatus = securityStatus;
    }

    @Deprecated(since = "7.5.0")
    public T getValue() {
        return value;
    }

    @Deprecated(since = "7.5.0")
    public Cnec<?> getCnec() {
        return cnec;
    }

    @Deprecated(since = "7.5.0")
    public State getState() {
        return cnec.getState();
    }

    @Deprecated(since = "7.5.0")
    public String getId() {
        return cnec.getId();
    }

    @Deprecated(since = "7.5.0")
    public Unit getUnit() {
        return unit;
    }

    @Deprecated(since = "7.5.0")
    public Cnec.SecurityStatus getCnecSecurityStatus() {
        return securityStatus;
    }

    @Deprecated(since = "7.5.0")
    public double getMargin() {
        return margin;
    }

    @Deprecated(since = "7.5.0")
    public String print() {
        if (value instanceof VoltageCnecValue) {
            VoltageCnecValue voltageValue = (VoltageCnecValue) value;
            VoltageCnec voltageCnec = (VoltageCnec) cnec;
            return String.format("Network element %s at state %s has a min voltage of %s kV and a max voltage of %s kV.",
                voltageCnec.getNetworkElementId(),
                voltageCnec.getState().getId(),
                MeasurementRounding.roundValueBasedOnMargin(voltageValue.minValue(), margin, 2).doubleValue(),
                MeasurementRounding.roundValueBasedOnMargin(voltageValue.maxValue(), margin, 2).doubleValue());
        } else if (value instanceof AngleCnecValue) {
            AngleCnecValue angleValue = (AngleCnecValue) value;
            AngleCnec angleCnec = (AngleCnec) cnec;
            return String.format("AngleCnec %s (with importing network element %s and exporting network element %s) at state %s has an angle of %s°.",
                angleCnec.getId(),
                angleCnec.getImportingNetworkElementId(),
                angleCnec.getExportingNetworkElementId(),
                cnec.getState().getId(),
                MeasurementRounding.roundValueBasedOnMargin(angleValue.value(), margin, 2).doubleValue());
        } else {
            throw new IllegalStateException("Unexpected value: " + value);
        }
    }

}
