/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.AbstractRaoResultWithMonitoringResult;
import com.powsybl.openrao.monitoring.results.AngleCnecResult;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RaoResultWithAngleMonitoring extends AbstractRaoResultWithMonitoringResult<AngleCnec, AngleCnecResult> {
    public RaoResultWithAngleMonitoring(RaoResult raoResult, MonitoringResult<AngleCnec> monitoringResult) {
        super(raoResult, monitoringResult, PhysicalParameter.ANGLE);
    }

    @Override
    public double getAngle(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        return getCnecResult(angleCnec, optimizationInstant, unit).map(AngleCnecResult::getAngle).orElse(Double.NaN);
    }

    @Override
    public double getMargin(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        return getCnecResult(angleCnec, optimizationInstant, unit).map(CnecResult::getMargin).orElse(Double.NaN);
    }
}
