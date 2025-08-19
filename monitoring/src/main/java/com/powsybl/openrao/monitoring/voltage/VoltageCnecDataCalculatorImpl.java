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
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.SecurityStatus;
import com.powsybl.openrao.monitoring.VoltageCnecDataCalculator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class VoltageCnecDataCalculatorImpl implements VoltageCnecDataCalculator {
    @Override
    public Double computeMinVoltage(VoltageCnec voltageCnec, Network network, Unit unit) {
        return getVoltages(voltageCnec, network, unit).stream().min(Double::compareTo).orElse(Double.NEGATIVE_INFINITY);
    }

    @Override
    public Double computeMaxVoltage(VoltageCnec voltageCnec, Network network, Unit unit) {
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

    @Override
    public double computeMargin(VoltageCnec voltageCnec, Network network, Unit unit) {
        Double minVoltage = computeMinVoltage(voltageCnec, network, unit);
        Double maxVoltage = computeMaxVoltage(voltageCnec, network, unit);
        double marginLowerBound = minVoltage - voltageCnec.getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginUpperBound = voltageCnec.getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - maxVoltage;
        return Math.min(marginLowerBound, marginUpperBound);
    }

    @Override
    public SecurityStatus computeSecurityStatus(VoltageCnec voltageCnec, Network network, Unit unit) {
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
