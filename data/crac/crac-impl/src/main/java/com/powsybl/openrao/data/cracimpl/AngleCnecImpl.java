/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.threshold.Threshold;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleCnecImpl extends AbstractCnec<AngleCnec> implements AngleCnec {
    private final Set<Threshold> thresholds;
    private final NetworkElement exportingNetworkElement;
    private final NetworkElement importingNetworkElement;

    AngleCnecImpl(String id,
                 String name,
                 NetworkElement exportingNetworkElement,
                 NetworkElement importingNetworkElement,
                 String operator,
                 String border,
                 State state,
                 boolean optimized,
                 boolean monitored,
                  Set<Threshold> thresholds,
                 double reliabilityMargin) {
        super(id, name, Set.of(exportingNetworkElement, importingNetworkElement), operator, border, state, optimized, monitored, reliabilityMargin);
        this.thresholds = thresholds;
        this.exportingNetworkElement = exportingNetworkElement;
        this.importingNetworkElement = importingNetworkElement;
    }

    @Override
    public NetworkElement getExportingNetworkElement() {
        return exportingNetworkElement;
    }

    @Override
    public NetworkElement getImportingNetworkElement() {
        return importingNetworkElement;
    }

    @Override
    public Set<Threshold> getThresholds() {
        return thresholds;
    }

    @Override
    public Optional<Double> getLowerBound(Unit requestedUnit) {
        if (!requestedUnit.equals(Unit.DEGREE)) {
            throw new OpenRaoException("AngleCnec lowerBound can only be requested in DEGREE");
        }

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
            return Optional.of(lowerBound);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Double> getUpperBound(Unit requestedUnit) {
        if (!requestedUnit.equals(Unit.DEGREE)) {
            throw new OpenRaoException("AngleCnec upperBound can only be requested in DEGREE");
        }

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
            return Optional.of(upperBound);
        }
        return Optional.empty();
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.ANGLE;
    }

    @Override
    public AngleCnecValue computeValue(Network network, Unit unit) {
        if (!unit.equals(Unit.DEGREE)) {
            throw new OpenRaoException("AngleCnec margin can only be requested in DEGREE");
        }
        VoltageLevel exportingVoltageLevel = getVoltageLevelOfElement(exportingNetworkElement.getId(), network);
        VoltageLevel importingVoltageLevel = getVoltageLevelOfElement(importingNetworkElement.getId(), network);
        return new AngleCnecValue(exportingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).max().getAsDouble()
            - importingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).min().getAsDouble());
    }

    @Override
    public double computeWorstMargin(Network network, Unit unit) {
        if (!unit.equals(Unit.DEGREE)) {
            throw new OpenRaoException("AngleCnec margin can only be requested in DEGREE");
        }

        AngleCnecValue actualAngleValue = computeValue(network, unit);
        double marginOnLowerBound = actualAngleValue.value() - getLowerBound(unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = getUpperBound(unit).orElse(Double.POSITIVE_INFINITY) - actualAngleValue.value();
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    public SecurityStatus computeSecurityStatus(Network network, Unit unit) {
        if (computeWorstMargin(network, unit) < 0) {
            double actualAngleValue = computeValue(network, unit).value();
            boolean highVoltageConstraints = false;
            boolean lowVoltageConstraints = false;
            if (getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMax() && actualAngleValue > threshold.max().orElseThrow())) {
                highVoltageConstraints = true;
            }
            if (getThresholds().stream()
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
            && thresholds.equals(cnec.getThresholds())
            && exportingNetworkElement.equals(cnec.getExportingNetworkElement())
            && importingNetworkElement.equals(cnec.getImportingNetworkElement());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + thresholds.hashCode();
        hashCode = 31 * hashCode + exportingNetworkElement.hashCode();
        hashCode = 31 * hashCode + importingNetworkElement.hashCode();
        return hashCode;
    }
}
