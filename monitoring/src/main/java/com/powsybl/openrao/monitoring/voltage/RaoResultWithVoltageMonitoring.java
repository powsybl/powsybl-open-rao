/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.AbstractRaoResultWithMonitoringResult;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.VoltageCnecResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RaoResultWithVoltageMonitoring extends AbstractRaoResultWithMonitoringResult<VoltageCnec, VoltageCnecResult> {
    public RaoResultWithVoltageMonitoring(RaoResult raoResult, MonitoringResult<VoltageCnec> monitoringResult) {
        super(raoResult, monitoringResult, PhysicalParameter.VOLTAGE);
    }

    @Override
    public double getMinVoltage(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        return getCnecResult(voltageCnec, optimizationInstant, unit).map(VoltageCnecResult::getMinVoltage).orElse(Double.NaN);
    }

    @Override
    public double getMaxVoltage(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        return getCnecResult(voltageCnec, optimizationInstant, unit).map(VoltageCnecResult::getMaxVoltage).orElse(Double.NaN);
    }

    @Override
    public double getMargin(Instant optimizationInstant, VoltageCnec voltageCnec, Unit unit) {
        return getCnecResult(voltageCnec, optimizationInstant, unit).map(CnecResult::getMargin).orElse(Double.NaN);
    }
}
