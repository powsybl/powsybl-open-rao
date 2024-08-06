/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracapi.threshold.Threshold;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecImpl extends AbstractBranchCnec<FlowCnec> implements FlowCnec {

    private final Double[] iMax = new Double[2];

    FlowCnecImpl(String id,
                 String name,
                 NetworkElement networkElement,
                 String operator,
                 String border,
                 State state,
                 boolean optimized,
                 boolean monitored,
                 Set<BranchThreshold> thresholds,
                 double frm,
                 Double nominalVLeft,
                 Double nominalVRight,
                 Double iMaxLeft,
                 Double iMaxRight) {
        super(id, name, networkElement, operator, border, state, optimized, monitored, thresholds, frm, nominalVLeft, nominalVRight);
        this.iMax[0] = iMaxLeft;
        this.iMax[1] = iMaxRight;
    }

    @Override
    public Double getIMax(TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
            return iMax[0];
        } else {
            return iMax[1];
        }
    }

    @Override
    public Optional<Double> getLowerBound(TwoSides side, Unit requestedUnit) {

        if (!requestedUnit.equals(Unit.AMPERE) && !requestedUnit.equals(Unit.MEGAWATT)) {
            throw new OpenRaoException("FlowCnec lowerBound can only be requested in AMPERE or MEGAWATT");
        }
        if (!bounds.isLowerBoundComputed(side, requestedUnit)) {
            Set<BranchThreshold> limitingThresholds = thresholds.stream()
                .filter(Threshold::limitsByMin)
                .filter(branchThreshold -> branchThreshold.getSide().equals(side))
                .collect(Collectors.toSet());

            if (!limitingThresholds.isEmpty()) {
                double lowerBound = Double.NEGATIVE_INFINITY;
                for (BranchThreshold threshold : limitingThresholds) {
                    double currentBound = getRawBound(threshold, threshold.min().orElseThrow());
                    currentBound = changeValueUnit(currentBound, threshold.getUnit(), requestedUnit, threshold.getSide());
                    currentBound += changeValueUnit(reliabilityMargin, Unit.MEGAWATT, requestedUnit, side);
                    if (currentBound > lowerBound) {
                        lowerBound = currentBound;
                    }
                }
                bounds.setLowerBound(lowerBound, side, requestedUnit);
            } else {
                bounds.setLowerBound(null, side, requestedUnit);
            }
        }
        return Optional.ofNullable(bounds.getLowerBound(side, requestedUnit));
    }

    @Override
    public Optional<Double> getUpperBound(TwoSides side, Unit requestedUnit) {

        if (!requestedUnit.equals(Unit.AMPERE) && !requestedUnit.equals(Unit.MEGAWATT)) {
            throw new OpenRaoException("FlowCnec upperBound can only be requested in AMPERE or MEGAWATT");
        }

        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        if (!bounds.isUpperBoundComputed(side, requestedUnit)) {
            Set<BranchThreshold> limitingThresholds = thresholds.stream()
                .filter(Threshold::limitsByMax)
                .filter(branchThreshold -> branchThreshold.getSide().equals(side))
                .collect(Collectors.toSet());
            if (!limitingThresholds.isEmpty()) {
                double upperBound = Double.POSITIVE_INFINITY;
                for (BranchThreshold threshold : limitingThresholds) {
                    double currentBound = getRawBound(threshold, threshold.max().orElseThrow());
                    currentBound = changeValueUnit(currentBound, threshold.getUnit(), requestedUnit, threshold.getSide());
                    currentBound -= changeValueUnit(reliabilityMargin, Unit.MEGAWATT, requestedUnit, side);
                    if (currentBound < upperBound) {
                        upperBound = currentBound;
                    }
                }
                bounds.setUpperBound(upperBound, side, requestedUnit);
            } else {
                bounds.setUpperBound(null, side, requestedUnit);
            }
        }
        return Optional.ofNullable(bounds.getUpperBound(side, requestedUnit));
    }

    private double getRawBound(BranchThreshold threshold, double thresholdValue) {
        if (threshold.getUnit().equals(Unit.PERCENT_IMAX)) {
            return getIMax(threshold.getSide()) * thresholdValue;
        } else {
            return thresholdValue;
        }
    }

    private double changeValueUnit(double value, Unit oldUnit, Unit newUnit, TwoSides side) {
        if (oldUnit.equals(newUnit) ||
            oldUnit.equals(Unit.PERCENT_IMAX) && newUnit.equals(Unit.AMPERE)) {
            return value;
        } else {
            double conversionFactor = Math.sqrt(3) * getNominalVoltage(side) / 1000; // Conversion from A to MW
            if (oldUnit.equals(Unit.MEGAWATT) && newUnit.equals(Unit.AMPERE)) {
                conversionFactor = 1 / conversionFactor;
            }
            return value * conversionFactor;
        }
    }

    public boolean isConnected(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(getNetworkElement().getId());
        if (identifiable instanceof Connectable) {
            Connectable<?> connectable = (Connectable<?>) identifiable;
            return connectable.getTerminals().stream().allMatch(Terminal::isConnected);
        }
        return true; // by default
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.FLOW;
    }

    @Override
    public double computeValue(Network network, Unit unit) {
        if (!unit.equals(Unit.AMPERE) && !unit.equals(Unit.MEGAWATT)) {
            throw new OpenRaoException("FlowCnec can only be requested in AMPERE or MEGAWATT");
        }
        Branch branch = network.getBranch(getNetworkElement().getId());
        if (getMonitoredSides().size() == 2) {
            double power1 = getPower(branch, TwoSides.ONE, unit);
            double power2 = getPower(branch, TwoSides.TWO, unit);
            return computeMargin(power1, TwoSides.ONE, unit) < computeMargin(power2, TwoSides.TWO, unit) ? power1 : power2;
        } else {
            return getPower(branch, TwoSides.ONE, unit);
        }
    }

    private double getPower(Branch branch, TwoSides side, Unit unit) {
        double power = unit == Unit.MEGAWATT ? branch.getTerminal(side).getP() : branch.getTerminal(side).getI();
        return Double.isNaN(power) ? branch.getTerminal(side).getP() * getFlowUnitMultiplier(side) : power;
    }

    @Override
    public double computeMargin(double value, Unit unit) {
        if (!unit.equals(Unit.AMPERE) && !unit.equals(Unit.MEGAWATT)) {
            throw new OpenRaoException("FlowCnec can only be requested in AMPERE or MEGAWATT");
        }
        if (getMonitoredSides().size() == 2) {
            throw new OpenRaoException("cnec " + getId() + " is monitored on both sides. Please use computeMargin using side as parameter");
        }
        return computeMargin(value, getMonitoredSides().iterator().next(), unit);
    }

    public CnecSecurityStatus getCnecSecurityStatus(double actualValue, Unit unit) {
        if (computeMargin(actualValue, unit) < 0) {
            boolean highVoltageConstraints = false;
            boolean lowVoltageConstraints = false;
            if (getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMax() && actualValue > threshold.max().orElseThrow())) {
                highVoltageConstraints = true;
            }
            if (getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMin() && actualValue < threshold.min().orElseThrow())) {
                lowVoltageConstraints = true;
            }
            if (highVoltageConstraints && lowVoltageConstraints) {
                return CnecSecurityStatus.HIGH_AND_LOW_CONSTRAINTS;
            } else if (highVoltageConstraints) {
                return CnecSecurityStatus.HIGH_CONSTRAINT;
            } else {
                return CnecSecurityStatus.LOW_CONSTRAINT;
            }
        } else {
            return CnecSecurityStatus.SECURE;
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
        FlowCnecImpl cnec = (FlowCnecImpl) o;
        return super.equals(cnec);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private double getFlowUnitMultiplier(TwoSides voltageSide) {
        double nominalVoltage = getNominalVoltage(voltageSide);
        return 1000 / (nominalVoltage * Math.sqrt(3));
    }

}
