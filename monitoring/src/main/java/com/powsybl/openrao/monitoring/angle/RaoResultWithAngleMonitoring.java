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
import com.powsybl.openrao.monitoring.SecurityStatus;
import com.powsybl.openrao.monitoring.results.AbstractRaoResultWithMonitoringResult;
import com.powsybl.openrao.monitoring.results.AngleCnecResult;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RaoResultWithAngleMonitoring extends AbstractRaoResultWithMonitoringResult<AngleCnec> {
    public RaoResultWithAngleMonitoring(RaoResult raoResult, MonitoringResult<AngleCnec> monitoringResult) {
        super(raoResult, monitoringResult);
    }

    @Override
    public double getAngle(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.ANGLE);
        checkInstant(optimizationInstant, PhysicalParameter.ANGLE);
        Optional<AngleCnecResult> angleCnecResultOpt = monitoringResult.getCnecResults().stream()
            .filter(angleCnecRes -> angleCnecRes.getId().equals(angleCnec.getId()))
            .filter(AngleCnecResult.class::isInstance)
            .map(AngleCnecResult.class::cast)
            .findFirst();

        if (angleCnecResultOpt.isPresent()) {
            return angleCnecResultOpt.get().getAngle();
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getMargin(Instant optimizationInstant, AngleCnec angleCnec, Unit unit) {
        unit.checkPhysicalParameter(PhysicalParameter.ANGLE);
        Optional<CnecResult<AngleCnec>> angleCnecResultOpt = monitoringResult.getCnecResults().stream().filter(angleCnecRes -> angleCnecRes.getId().equals(angleCnec.getId())).findFirst();
        return angleCnecResultOpt.map(CnecResult::getMargin).orElse(Double.NaN);
    }

    @Override
    public boolean isSecure(Instant instant, PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.ANGLE)) {
            return raoResult.isSecure(instant, physicalParameters.toArray(new PhysicalParameter[0])) && monitoringResult.getStatus().equals(SecurityStatus.SECURE);
        } else {
            return raoResult.isSecure(instant, u);
        }
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        List<PhysicalParameter> physicalParameters = new ArrayList<>(Stream.of(u).sorted().toList());
        if (physicalParameters.remove(PhysicalParameter.ANGLE)) {
            return raoResult.isSecure(physicalParameters.toArray(new PhysicalParameter[0])) && monitoringResult.getStatus().equals(SecurityStatus.SECURE);
        } else {
            return raoResult.isSecure(u);
        }
    }
}
