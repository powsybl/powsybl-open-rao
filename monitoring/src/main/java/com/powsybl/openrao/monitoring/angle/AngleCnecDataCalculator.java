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
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.monitoring.SecurityStatus;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class AngleCnecDataCalculator {
    private AngleCnecDataCalculator() {
    }

    /**
     * @param angleCnec: the angle CNEC we seek to compute the angle of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit in which to compute the angle value
     * @return the angle of the CNEC in the network
     */
    public static Double computeAngle(AngleCnec angleCnec, Network network, Unit unit) {
        unit.checkPhysicalParameter(angleCnec.getPhysicalParameter());
        VoltageLevel exportingVoltageLevel = getVoltageLevelOfElement(angleCnec.getExportingNetworkElement().getId(), network);
        VoltageLevel importingVoltageLevel = getVoltageLevelOfElement(angleCnec.getImportingNetworkElement().getId(), network);
        return exportingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).max().getAsDouble()
            - importingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).min().getAsDouble();
    }

    /**
     * @param angleCnec: the angle CNEC we seek to compute the margin of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit in which to compute the margin
     * @return a double as the worst margin of a CNEC relatively to the @{@link Cnec} thresholds
     */
    public static double computeMargin(AngleCnec angleCnec, Network network, Unit unit) {
        Double angle = computeAngle(angleCnec, network, unit);
        double marginOnLowerBound = angle - angleCnec.getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = angleCnec.getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - angle;
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    /**
     * @param angleCnec: the angle CNEC we seek to compute the security status of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit in whcih to compute the angle values
     * Returns a {@link SecurityStatus} describing the {@link Cnec} result compared to the thresholds
     */
    public static SecurityStatus computeSecurityStatus(AngleCnec angleCnec, Network network, Unit unit) {
        double actualAngleValue = computeAngle(angleCnec, network, unit);

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
