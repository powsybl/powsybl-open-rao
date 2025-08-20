/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.SecurityStatus;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class VoltageCnecDataCalculator {
    private VoltageCnecDataCalculator() {
    }

    /**
     * @param voltageCnec: the voltage CNEC we seek to compute the min voltage of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit in which to compute the voltage value
     * @return the angle of the CNEC in the network
     */
    public static Double computeMinVoltage(VoltageCnec voltageCnec, Network network, Unit unit) {
        return getVoltages(voltageCnec, network, unit).stream().min(Double::compareTo).orElse(Double.NEGATIVE_INFINITY);
    }

    /**
     * @param voltageCnec: the voltage CNEC we seek to compute the max voltage of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit in which to compute the voltage value
     * @return the angle of the CNEC in the network
     */
    public static Double computeMaxVoltage(VoltageCnec voltageCnec, Network network, Unit unit) {
        return getVoltages(voltageCnec, network, unit).stream().max(Double::compareTo).orElse(Double.POSITIVE_INFINITY);
    }

    private static Set<Double> getVoltages(VoltageCnec voltageCnec, Network network, Unit unit) {
        unit.checkPhysicalParameter(voltageCnec.getPhysicalParameter());
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageCnec.getNetworkElement().getId());
        if (voltageLevel == null) {
            throw new OpenRaoException("Voltage level is missing on network element " + voltageCnec.getNetworkElement().getId());
        }
        Set<Double> voltages = new HashSet<>();
        BusbarSection busbarSection = network.getBusbarSection(voltageCnec.getNetworkElement().getId());
        if (busbarSection != null) {
            Double busBarVoltages = busbarSection.getV();
            voltages.add(busBarVoltages);
        } else {
            voltages.addAll(voltageLevel.getBusView().getBusStream().map(Bus::getV).collect(Collectors.toSet()));
        }
        return voltages;
    }

    /**
     * @param voltageCnec: the voltage CNEC we seek to compute the margin of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit in which to compute the margin
     * @return a double as the worst margin of a CNEC relatively to the @{@link Cnec} thresholds
     */
    public static double computeMargin(VoltageCnec voltageCnec, Network network, Unit unit) {
        Double minVoltage = computeMinVoltage(voltageCnec, network, unit);
        Double maxVoltage = computeMaxVoltage(voltageCnec, network, unit);
        double marginLowerBound = minVoltage - voltageCnec.getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginUpperBound = voltageCnec.getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - maxVoltage;
        return Math.min(marginLowerBound, marginUpperBound);
    }

    /**
     * @param voltageCnec: the voltage CNEC we seek to compute the security status of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit in whcih to compute the voltage values
     * Returns a {@link SecurityStatus} describing the {@link Cnec} result compared to the thresholds
     */
    public static SecurityStatus computeSecurityStatus(VoltageCnec voltageCnec, Network network, Unit unit) {
        Double minVoltage = computeMinVoltage(voltageCnec, network, unit);
        Double maxVoltage = computeMaxVoltage(voltageCnec, network, unit);

        if (computeMargin(voltageCnec, network, unit) < 0) {
            boolean highVoltageConstraints = false;
            boolean lowVoltageConstraints = false;

            double marginLowerBound = minVoltage - voltageCnec.getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
            double marginUpperBound = voltageCnec.getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - maxVoltage;

            if (marginUpperBound < 0) {
                highVoltageConstraints = true;
            }
            if (marginLowerBound < 0) {
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
}
