/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageCnecImpl extends AbstractCnec<VoltageCnec> implements VoltageCnec {

    // min, max
    private double[] boundValues = {Double.NaN, Double.NaN};

    VoltageCnecImpl(String id,
                    String name,
                    String networkElementId,
                    String operator,
                    String border,
                    State state,
                    boolean optimized,
                    boolean monitored,
                    Set<Threshold> thresholds,
                    double reliabilityMargin) {
        super(id, name, Set.of(networkElementId), operator, border, state, optimized, monitored, reliabilityMargin);
        computeUpperBound(thresholds);
        computeLowerBound(thresholds);
    }

    @Override
    public String getNetworkElementId() {
        return networkElementsIds.iterator().next();
    }

    @Override
    public Optional<Double> getLowerBound(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        return Double.isNaN(boundValues[0]) ? Optional.empty() : Optional.of(boundValues[0]);
    }

    @Override
    public Optional<Double> getUpperBound(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        return Double.isNaN(boundValues[1]) ? Optional.empty() : Optional.of(boundValues[1]);
    }

    private void computeLowerBound(Set<Threshold> thresholds) {
        Set<Threshold> limitingThresholds = thresholds.stream()
            .filter(Threshold::limitsByMin)
            .collect(Collectors.toSet());

        if (!limitingThresholds.isEmpty()) {
            double lowerBound = Double.NEGATIVE_INFINITY;
            for (Threshold threshold : limitingThresholds) {
                double currentBound = threshold.min().orElseThrow() + reliabilityMargin;
                if (currentBound > lowerBound) {
                    lowerBound = currentBound;
                }
            }
            boundValues[0] = lowerBound;
        }
    }

    public void computeUpperBound(Set<Threshold> thresholds) {
        Set<Threshold> limitingThresholds = thresholds.stream()
            .filter(Threshold::limitsByMax)
            .collect(Collectors.toSet());
        if (!limitingThresholds.isEmpty()) {
            double upperBound = Double.POSITIVE_INFINITY;
            for (Threshold threshold : limitingThresholds) {
                double currentBound = threshold.max().orElseThrow() - reliabilityMargin;
                if (currentBound < upperBound) {
                    upperBound = currentBound;
                }
            }
            boundValues[1] = upperBound;
        }
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.VOLTAGE;
    }

    @Override
    public VoltageCnecValue computeValue(Network network, Unit unit) {
        unit.checkPhysicalParameter(getPhysicalParameter());
        VoltageLevel voltageLevel = network.getVoltageLevel(getNetworkElementId());
        if (voltageLevel == null) {
            throw new OpenRaoException("Voltage level is missing on network element " + getNetworkElementId());
        }
        Set<Double> voltages = new HashSet<>();
        BusbarSection busbarSection = network.getBusbarSection(getNetworkElementId());
        if (busbarSection != null) {
            Double busBarVoltages = busbarSection.getV();
            voltages.add(busBarVoltages);
        } else {
            voltages.addAll(voltageLevel.getBusView().getBusStream().map(Bus::getV).collect(Collectors.toSet()));
        }
        Double minVoltage = voltages.stream().min(Double::compareTo).orElse(Double.NEGATIVE_INFINITY);
        Double maxVoltage = voltages.stream().max(Double::compareTo).orElse(Double.POSITIVE_INFINITY);

        return new VoltageCnecValue(minVoltage, maxVoltage);
    }

    @Override
    public double computeMargin(Network network, Unit unit) {
        unit.checkPhysicalParameter(getPhysicalParameter());
        VoltageCnecValue voltageValue = computeValue(network, unit);
        double marginLowerBound = voltageValue.minValue() - getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginUpperBound = getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - voltageValue.maxValue();
        return Math.min(marginLowerBound, marginUpperBound);
    }

    private double computeMargin(VoltageCnecValue voltageValue, Unit unit) {
        double marginLowerBound = voltageValue.minValue() - getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginUpperBound = getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - voltageValue.maxValue();
        return Math.min(marginLowerBound, marginUpperBound);
    }

    public SecurityStatus computeSecurityStatus(Network network, Unit unit) {
        VoltageCnecValue voltageValue = computeValue(network, unit);

        if (computeMargin(voltageValue, unit) < 0) {
            boolean highVoltageConstraints = false;
            boolean lowVoltageConstraints = false;

            double marginLowerBound = voltageValue.minValue() - getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
            double marginUpperBound = getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - voltageValue.maxValue();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VoltageCnecImpl cnec = (VoltageCnecImpl) o;
        return super.equals(cnec)
            && getLowerBound(Unit.KILOVOLT) == cnec.getLowerBound(Unit.KILOVOLT)
            && getUpperBound(Unit.KILOVOLT) == cnec.getUpperBound(Unit.KILOVOLT);
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + boundValues.hashCode();
        return hashCode;
    }

}
