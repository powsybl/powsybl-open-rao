/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.monitoring.api.CnecHelper;
import com.powsybl.openrao.monitoring.api.SecurityStatus;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleCnecHelper implements CnecHelper<AngleCnec> {
    @Override
    public AngleCnecValue computeValue(AngleCnec angleCnec, Network network, Unit unit) {
        unit.checkPhysicalParameter(angleCnec.getPhysicalParameter());
        VoltageLevel exportingVoltageLevel = getVoltageLevelOfElement(angleCnec.getExportingNetworkElement().getId(), network);
        VoltageLevel importingVoltageLevel = getVoltageLevelOfElement(angleCnec.getImportingNetworkElement().getId(), network);
        return new AngleCnecValue(exportingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).max().getAsDouble()
            - importingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).min().getAsDouble());
    }

    @Override
    public double computeMargin(AngleCnec angleCnec, Network network, Unit unit) {
        unit.checkPhysicalParameter(angleCnec.getPhysicalParameter()); // TODO: redundant
        AngleCnecValue actualAngleValue = computeValue(angleCnec, network, unit);
        double marginOnLowerBound = actualAngleValue.value() - angleCnec.getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = angleCnec.getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - actualAngleValue.value();
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    @Override
    public SecurityStatus computeSecurityStatus(AngleCnec angleCnec, Network network, Unit unit) {
        double actualAngleValue = computeValue(angleCnec, network, unit).value();

        if (computeMargin(angleCnec, actualAngleValue, unit) < 0) {
            boolean highVoltageConstraints = false;
            boolean lowVoltageConstraints = false;
            if (angleCnec.getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMax() && actualAngleValue > threshold.max().orElseThrow())) {
                highVoltageConstraints = true;
            }
            if (angleCnec.getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMin() && actualAngleValue < threshold.min().orElseThrow())) {
                lowVoltageConstraints = true;
            }
            if (highVoltageConstraints && lowVoltageConstraints) {
                return SecurityStatus.HIGH_AND_LOW_CONSTRAINTS;
            } else if (highVoltageConstraints) {
                return SecurityStatus.HIGH_CONSTRAINT;
            } else {
                return SecurityStatus.LOW_CONSTRAINT;
            }
        } else {
            return SecurityStatus.SECURE;
        }
    }

    private static VoltageLevel getVoltageLevelOfElement(String elementId, Network network) {
        if (network.getBusBreakerView().getBus(elementId) != null) {
            return network.getBusBreakerView().getBus(elementId).getVoltageLevel();
        }
        return network.getVoltageLevel(elementId);
    }

    private static double computeMargin(AngleCnec angleCnec, double actualAngleValue, Unit unit) {
        double marginOnLowerBound = actualAngleValue - angleCnec.getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = angleCnec.getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - actualAngleValue;
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }
}
