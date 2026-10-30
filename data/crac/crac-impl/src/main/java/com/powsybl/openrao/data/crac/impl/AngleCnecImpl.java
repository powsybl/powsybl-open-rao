/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleCnecImpl extends AbstractCnec<AngleCnec> implements AngleCnec {
    private final String exportingNetworkElementId;
    private final String importingNetworkElementId;
    // min, max
    private double[] boundValues = {Double.NaN, Double.NaN};

    AngleCnecImpl(String id,
                  String name,
                  String exportingNetworkElementId,
                  String importingNetworkElementId,
                  String operator,
                  String border,
                  State state,
                  boolean optimized,
                  boolean monitored,
                  Set<Threshold> thresholds,
                  double reliabilityMargin) {
        super(id, name, Set.of(exportingNetworkElementId, importingNetworkElementId), operator, border, state, optimized, monitored, reliabilityMargin);
        this.exportingNetworkElementId = exportingNetworkElementId;
        this.importingNetworkElementId = importingNetworkElementId;
        computeLowerBound(thresholds);
        computeUpperBound(thresholds);
    }

    @Override
    public String getExportingNetworkElementId() {
        return exportingNetworkElementId;
    }

    @Override
    public String getImportingNetworkElementId() {
        return importingNetworkElementId;
    }

    @Override
    public Optional<Double> getLowerBound(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(PhysicalParameter.ANGLE);
        return Double.isNaN(boundValues[0]) ? Optional.empty() : Optional.of(boundValues[0]);
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

    @Override
    public Optional<Double> getUpperBound(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(PhysicalParameter.ANGLE);
        return Double.isNaN(boundValues[1]) ? Optional.empty() : Optional.of(boundValues[1]);
    }

    private void computeUpperBound(Set<Threshold> thresholds) {
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
        return PhysicalParameter.ANGLE;
    }

    @Override
    public AngleCnecValue computeValue(Network network, Unit unit) {
        unit.checkPhysicalParameter(getPhysicalParameter());
        VoltageLevel exportingVoltageLevel = getVoltageLevelOfElement(exportingNetworkElementId, network);
        VoltageLevel importingVoltageLevel = getVoltageLevelOfElement(importingNetworkElementId, network);
        return new AngleCnecValue(exportingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).max().getAsDouble()
            - importingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).min().getAsDouble());
    }

    @Override
    public double computeMargin(Network network, Unit unit) {
        unit.checkPhysicalParameter(getPhysicalParameter());
        AngleCnecValue actualAngleValue = computeValue(network, unit);
        double marginOnLowerBound = actualAngleValue.value() - getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - actualAngleValue.value();
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    private double computeMargin(double actualAngleValue, Unit unit) {
        double marginOnLowerBound = actualAngleValue - getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - actualAngleValue;
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    public SecurityStatus computeSecurityStatus(Network network, Unit unit) {
        double actualAngleValue = computeValue(network, unit).value();

        if (computeMargin(actualAngleValue, unit) < 0) {
            return getUpperBound(Unit.DEGREE).orElse(Double.POSITIVE_INFINITY) < actualAngleValue
                ? SecurityStatus.HIGH_CONSTRAINT
                : SecurityStatus.LOW_CONSTRAINT;
        } else {
            return SecurityStatus.SECURE;
        }
    }

    private VoltageLevel getVoltageLevelOfElement(String elementId, Network network) {
        if (network.getBusBreakerView().getBus(elementId) != null) {
            return network.getBusBreakerView().getBus(elementId).getVoltageLevel();
        }
        return network.getVoltageLevel(elementId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AngleCnecImpl cnec = (AngleCnecImpl) o;
        return super.equals(cnec)
            && getLowerBound(Unit.DEGREE) == cnec.getLowerBound(Unit.DEGREE)
            && getUpperBound(Unit.DEGREE) == cnec.getUpperBound(Unit.DEGREE)
            && getExportingNetworkElementId().equals(cnec.getExportingNetworkElementId())
            && getImportingNetworkElementId().equals(cnec.getImportingNetworkElementId());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + Arrays.hashCode(boundValues);
        hashCode = 31 * hashCode + getExportingNetworkElementId().hashCode();
        hashCode = 31 * hashCode + getImportingNetworkElementId().hashCode();
        return hashCode;
    }
}
