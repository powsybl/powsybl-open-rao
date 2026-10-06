/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.monitoring.results.CnecResult;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleCnecResult extends CnecResult<AngleCnec> {

    private static AngleCnec angleCnec;
    private double angle;

    public AngleCnecResult(AngleCnec angleCnec, Unit unit, Network network) {
        super(angleCnec, unit, network);
    }

    public AngleCnecResult(AngleCnec angleCnec, Unit unit, double margin, double angle) {
        super(angleCnec, unit, margin);
        this.angle = angle;
    }

    @Override
    public AngleCnec getCnec() {
        return angleCnec;
    }

    @Override
    public void computeMargin(Network network) {
        double marginOnLowerBound = angle - angleCnec.getLowerBound(Unit.DEGREE).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = angleCnec.getUpperBound(Unit.DEGREE).orElse(Double.POSITIVE_INFINITY) - angle;
        this.margin = Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    @Override
    public Cnec.SecurityStatus getCnecSecurityStatus() {
        if (margin < 0) {
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
                return Cnec.SecurityStatus.HIGH_AND_LOW_CONSTRAINTS;
            } else if (highVoltageConstraints) {
                return Cnec.SecurityStatus.HIGH_CONSTRAINT;
            } else {
                return Cnec.SecurityStatus.LOW_CONSTRAINT;
            }
        } else {
            return Cnec.SecurityStatus.SECURE;
        }
    }

    @Override
    public void computeValue(Network network) {
        VoltageLevel exportingVoltageLevel = getVoltageLevelOfElement(angleCnec.getExportingNetworkElement().getId(), network);
        VoltageLevel importingVoltageLevel = getVoltageLevelOfElement(angleCnec.getImportingNetworkElement().getId(), network);
        this.angle = exportingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).max().getAsDouble()
            - importingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).min().getAsDouble();
    }

    public double getAngle() {
        return angle;
    }

    @Override
    public String print() {
        return String.format("AngleCnec %s (with importing network element %s and exporting network element %s) at state %s has an angle of %s°.",
            angleCnec.getId(),
            angleCnec.getImportingNetworkElement().getId(),
            angleCnec.getExportingNetworkElement().getId(),
            angleCnec.getState().getId(),
            MeasurementRounding.roundValueBasedOnMargin(angle, margin, 2).doubleValue());
    }

    private static VoltageLevel getVoltageLevelOfElement(String elementId, Network network) {
        if (network.getBusBreakerView().getBus(elementId) != null) {
            return network.getBusBreakerView().getBus(elementId).getVoltageLevel();
        }
        return network.getVoltageLevel(elementId);
    }
}