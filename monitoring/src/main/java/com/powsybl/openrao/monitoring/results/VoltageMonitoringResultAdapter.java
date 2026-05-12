/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.impl.VoltageCnecValue;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class VoltageMonitoringResultAdapter {
    private VoltageMonitoringResultAdapter() {
    }

    public static VoltageResult convertToVoltageExtension(MonitoringResult voltageMonitoringResult) {
        VoltageResult voltageExtension = new VoltageResult();
        voltageMonitoringResult.getCnecResults().forEach(voltageResult -> voltageExtension.addMeasurement(((VoltageCnecValue) voltageResult.getValue()).minValue(), ((VoltageCnecValue) voltageResult.getValue()).maxValue(), voltageResult.getCnec().getState().getInstant(), (VoltageCnec) voltageResult.getCnec(), Unit.DEGREE));
        return voltageExtension;
    }
}
