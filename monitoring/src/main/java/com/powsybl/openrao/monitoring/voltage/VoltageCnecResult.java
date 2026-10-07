/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.results.AbstractCnecResult;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class VoltageCnecResult extends AbstractCnecResult<VoltageCnec> {
    private Double minVoltage;
    private Double maxVoltage;

    public VoltageCnecResult(VoltageCnec voltageCnec, Unit unit, Network network) {
        super(voltageCnec, unit, network);
    }

    public VoltageCnecResult(VoltageCnec voltageCnec, Unit unit, Double minVoltage, Double maxVoltage) {
        super(voltageCnec, unit);
        this.minVoltage = minVoltage;
        this.maxVoltage = maxVoltage;
        computeMargin();
    }

    @Override
    public String print() {
        return String.format("Network element %s at state %s has a min voltage of %s kV and a max voltage of %s kV.",
            cnec.getNetworkElement().getId(),
            cnec.getState().getId(),
            MeasurementRounding.roundValueBasedOnMargin(minVoltage, margin, 2).doubleValue(),
            MeasurementRounding.roundValueBasedOnMargin(maxVoltage, margin, 2).doubleValue());
    }

    @Override
    protected void computeValue(Network network) {
        VoltageLevel voltageLevel = network.getVoltageLevel(cnec.getNetworkElement().getId());
        if (voltageLevel == null) {
            throw new OpenRaoException("Voltage level is missing on network element " + cnec.getNetworkElement().getId());
        }
        Set<Double> voltages = new HashSet<>();
        BusbarSection busbarSection = network.getBusbarSection(cnec.getNetworkElement().getId());
        if (busbarSection != null) {
            Double busBarVoltages = busbarSection.getV();
            voltages.add(busBarVoltages);
        } else {
            voltages.addAll(voltageLevel.getBusView().getBusStream().map(Bus::getV).collect(Collectors.toSet()));
        }
        this.minVoltage = voltages.stream().min(Double::compareTo).orElse(Double.NEGATIVE_INFINITY);
        this.maxVoltage = voltages.stream().max(Double::compareTo).orElse(Double.POSITIVE_INFINITY);
    }

    @Override
    protected void computeMargin() {
        double marginLowerBound = minVoltage - cnec.getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginUpperBound = cnec.getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - maxVoltage;
        this.margin = Math.min(marginLowerBound, marginUpperBound);
    }

    @Override
    public Cnec.SecurityStatus getCnecSecurityStatus() {
        if (minVoltage.isNaN() || maxVoltage.isNaN()) {
            return Cnec.SecurityStatus.FAILURE;
        }
        if (margin < 0) {
            boolean highVoltageConstraints = false;
            boolean lowVoltageConstraints = false;

            double marginLowerBound = minVoltage - cnec.getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
            double marginUpperBound = cnec.getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - maxVoltage;

            if (marginUpperBound < 0) {
                highVoltageConstraints = true;
            }
            if (marginLowerBound < 0) {
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

    public double getMinVoltage() {
        return minVoltage;
    }

    public double getMaxVoltage() {
        return maxVoltage;
    }

    protected void checkUnit() {
        if (unit != Unit.KILOVOLT) {
            throw new OpenRaoException("A voltageCnecMonitoringResult must be in KILOVOLT");
        }
    }

}
