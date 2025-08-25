/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;

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
    public Optional<Double> getIMax(TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
            return Optional.ofNullable(iMax[0]);
        } else {
            return Optional.ofNullable(iMax[1]);
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
            TwoSides side = threshold.getSide();
            double iMax = getIMax(side).orElseThrow(() -> new OpenRaoException("Failed to retrieve iMax on side %s for FlowCNEC %s.".formatted(side, getId())));
            return iMax * thresholdValue;
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
    public FlowCnecValue computeValue(Network network, Unit unit) {
        if (!unit.equals(Unit.AMPERE) && !unit.equals(Unit.MEGAWATT)) {
            throw new OpenRaoException("FlowCnec can only be requested in AMPERE or MEGAWATT");
        }
        Branch branch = network.getBranch(getNetworkElement().getId());
        if (getMonitoredSides().size() == 2) {
            return new FlowCnecValue(getFlow(branch, TwoSides.ONE, unit), getFlow(branch, TwoSides.TWO, unit));
        } else {
            TwoSides monitoredSide = getMonitoredSides().iterator().next();
            double power = getFlow(branch, monitoredSide, unit);
            if (monitoredSide.equals(TwoSides.ONE)) {
                return new FlowCnecValue(power, Double.NaN);
            } else {
                return new FlowCnecValue(Double.NaN, power);
            }
        }
    }

    private double getFlow(Branch branch, TwoSides side, Unit unit) {
        double activeFlow = branch.getTerminal(side).getP();
        double intensity = branch.getTerminal(side).getI();
        if (unit.equals(Unit.AMPERE)) {
            // In case flows are negative, we shall replace this value by its opposite
            return Double.isNaN(intensity) ? activeFlow * getFlowUnitMultiplierMegawattToAmpere(side) : Math.signum(activeFlow) * intensity;
        } else if (!unit.equals(Unit.MEGAWATT)) {
            throw new OpenRaoException("FlowCnec can only be requested in AMPERE or MEGAWATT");
        }
        return activeFlow;
    }

    @Override
    public double computeMargin(Network network, Unit unit) {
        if (!unit.equals(Unit.AMPERE) && !unit.equals(Unit.MEGAWATT)) {
            throw new OpenRaoException("FlowCnec can only be requested in AMPERE or MEGAWATT");
        }
        FlowCnecValue flowCnecValue = computeValue(network, unit);
        return getMinimimMarginBetweenTwoSides(unit, flowCnecValue);
    }

    private double computeMargin(FlowCnecValue flowCnecValue, Unit unit) {
        return getMinimimMarginBetweenTwoSides(unit, flowCnecValue);
    }

    private double getMinimimMarginBetweenTwoSides(Unit unit, FlowCnecValue flowCnecValue) {
        if (getMonitoredSides().size() == 2) {
            double marginSide1 = computeMargin(flowCnecValue.side1Value(), TwoSides.ONE, unit);
            double marginSide2 = computeMargin(flowCnecValue.side2Value(), TwoSides.TWO, unit);
            return Math.min(marginSide1, marginSide2);
        } else {
            TwoSides monitoredSide = getMonitoredSides().iterator().next();
            if (monitoredSide.equals(TwoSides.ONE)) {
                return computeMargin(flowCnecValue.side1Value(), TwoSides.ONE, unit);
            } else {
                return computeMargin(flowCnecValue.side2Value(), TwoSides.TWO, unit);
            }
        }
    }

    public SecurityStatus computeSecurityStatus(Network network, Unit unit) {
        FlowCnecValue flowCnecValue = computeValue(network, unit);

        if (computeMargin(flowCnecValue, unit) < 0) {
            boolean highVoltageConstraints = false;
            boolean lowVoltageConstraints = false;

            if (getMonitoredSides().contains(TwoSides.ONE)) {
                double marginLowerBoundSideOne = flowCnecValue.side1Value() - getLowerBound(TwoSides.ONE, unit).orElse(Double.NEGATIVE_INFINITY);
                double marginUpperBoundSideOne = getUpperBound(TwoSides.ONE, unit).orElse(Double.POSITIVE_INFINITY) - flowCnecValue.side1Value();

                if (marginUpperBoundSideOne < 0) {
                    highVoltageConstraints = true;
                }
                if (marginLowerBoundSideOne < 0) {
                    lowVoltageConstraints = true;
                }
            }
            if (getMonitoredSides().contains(TwoSides.TWO)) {
                double marginLowerBoundSideTwo = flowCnecValue.side2Value() - getLowerBound(TwoSides.TWO, unit).orElse(Double.NEGATIVE_INFINITY);
                double marginUpperBoundSideTwo = getUpperBound(TwoSides.TWO, unit).orElse(Double.POSITIVE_INFINITY) - flowCnecValue.side2Value();
                if (marginUpperBoundSideTwo < 0) {
                    highVoltageConstraints = true;
                }
                if (marginLowerBoundSideTwo < 0) {
                    lowVoltageConstraints = true;
                }
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
        FlowCnecImpl cnec = (FlowCnecImpl) o;
        return super.equals(cnec);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private double getFlowUnitMultiplierMegawattToAmpere(TwoSides voltageSide) {
        double nominalVoltage = getNominalVoltage(voltageSide);
        return 1000 / (nominalVoltage * Math.sqrt(3));
    }

}
