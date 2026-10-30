/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.UnitConverter.getFlowUnitMultiplier;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecImpl extends AbstractCnec<FlowCnec> implements FlowCnec {

    // mw_lower_1, mw_lower_2, mw_upper_1, mw_upper_2, ampere_lower_1, ampere_lower_2, ampere_upper_1, ampere_upper_2
    private double[] boundValues = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    private final Double[] nominalVoltages = new Double[2];
    private final Double[] iMax = new Double[2];

    FlowCnecImpl(String id,
                 String name,
                 String networkElementId,
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
        super(id, name, Collections.singleton(networkElementId), operator, border, state, optimized, monitored, frm);
        this.nominalVoltages[0] = nominalVLeft;
        this.nominalVoltages[1] = nominalVRight;
        this.iMax[0] = iMaxLeft;
        this.iMax[1] = iMaxRight;
        computeBoundsFromThresholds(thresholds);
    }

    @Override
    public String getNetworkElementId() {
        return networkElementsIds.iterator().next();
    }

    @Override
    public double computeMargin(double actualValue, TwoSides side, Unit unit) {
        double marginOnLowerBound = actualValue - safeGetLowerBound(side, unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = safeGetUpperBound(side, unit).orElse(Double.POSITIVE_INFINITY) - actualValue;
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    @Override
    public Double getNominalVoltage(TwoSides side) {
        return nominalVoltages[side.equals(TwoSides.ONE) ? 0 : 1];
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
        return Double.isNaN(boundValues[getIndex(side, requestedUnit, Bound.LOWER)])
            ? Optional.empty()
            : Optional.of(boundValues[getIndex(side, requestedUnit, Bound.LOWER)]);

    }

    @Override
    public Optional<Double> safeGetLowerBound(TwoSides side, Unit requestedUnit) {
        Optional<Double> lowerBound = getLowerBound(side, requestedUnit);
        Unit otherUnit = requestedUnit.equals(Unit.MEGAWATT) ? Unit.AMPERE : Unit.MEGAWATT;
        if (lowerBound.isPresent()) {
            return lowerBound;
        }
        if (!lowerBound.isPresent() && getLowerBound(side, otherUnit).isPresent()) {
            return Optional.of(getLowerBound(side, otherUnit).get() * getFlowUnitMultiplier(getNominalVoltage(side), otherUnit, requestedUnit));
        }
        return Optional.empty();
    }

    @Override
    public Set<TwoSides> getMonitoredSides() {
        // A side is considered monitored if at least one bound was defined on this side
        Set<TwoSides> sides = new TreeSet<>();
        if (safeGetLowerBound(TwoSides.ONE, Unit.MEGAWATT).isPresent() ||
            safeGetUpperBound(TwoSides.ONE, Unit.MEGAWATT).isPresent()) {
            sides.add(TwoSides.ONE);
        }

        if (safeGetLowerBound(TwoSides.TWO, Unit.MEGAWATT).isPresent() ||
            safeGetUpperBound(TwoSides.TWO, Unit.MEGAWATT).isPresent()) {
            sides.add(TwoSides.TWO);
        }
        return sides;
    }

    private void computeBoundsFromThresholds(Set<BranchThreshold> thresholds) {
        for (TwoSides side : Set.of(TwoSides.ONE, TwoSides.TWO)) {
            for (Unit unit : Set.of(Unit.AMPERE, Unit.MEGAWATT)) {
                computeLowerBound(side, unit, thresholds);
                computeUpperBound(side, unit, thresholds);
            }
        }
    }

    private void computeLowerBound(TwoSides side, Unit requestedUnit, Set<BranchThreshold> thresholds) {
        Set<BranchThreshold> limitingThresholds = thresholds.stream()
            .filter(Threshold::limitsByMin)
            .filter(branchThreshold -> branchThreshold.getSide().equals(side))
            .collect(Collectors.toSet());
        if (!limitingThresholds.isEmpty()) {
            double lowerBound = Double.NEGATIVE_INFINITY;
            Unit lowerBoundInitialUnit = null;
            for (BranchThreshold threshold : limitingThresholds) {
                double currentBound = getRawBound(threshold, threshold.min().orElseThrow());
                Unit initialUnit = threshold.getUnit();
                if (initialUnit.equals(Unit.PERCENT_IMAX)) {
                    initialUnit = Unit.AMPERE;
                }
                currentBound = currentBound * getFlowUnitMultiplier(getNominalVoltage(threshold.getSide()), initialUnit, requestedUnit);
                currentBound += reliabilityMargin * getFlowUnitMultiplier(getNominalVoltage(side), Unit.MEGAWATT, requestedUnit);
                if (currentBound > lowerBound) {
                    lowerBound = currentBound;
                    lowerBoundInitialUnit = initialUnit;
                }
            }
            if (lowerBoundInitialUnit == requestedUnit) {
                boundValues[getIndex(side, requestedUnit, Bound.LOWER)] = lowerBound;
            }
        }
    }

    @Override
    public Optional<Double> getUpperBound(TwoSides side, Unit requestedUnit) {

        if (!requestedUnit.equals(Unit.AMPERE) && !requestedUnit.equals(Unit.MEGAWATT)) {
            throw new OpenRaoException("FlowCnec upperBound can only be requested in AMPERE or MEGAWATT");
        }

        return Double.isNaN(boundValues[getIndex(side, requestedUnit, Bound.UPPER)])
            ? Optional.empty()
            : Optional.of(boundValues[getIndex(side, requestedUnit, Bound.UPPER)]);
    }

    @Override
    public Optional<Double> safeGetUpperBound(TwoSides side, Unit requestedUnit) {
        Optional<Double> upperBound = getUpperBound(side, requestedUnit);
        Unit otherUnit = requestedUnit.equals(Unit.MEGAWATT) ? Unit.AMPERE : Unit.MEGAWATT;
        if (upperBound.isPresent()) {
            return upperBound;
        }
        if (!upperBound.isPresent() && getUpperBound(side, otherUnit).isPresent()) {
            return Optional.of(getUpperBound(side, otherUnit).get() * getFlowUnitMultiplier(getNominalVoltage(side), otherUnit, requestedUnit));
        }
        return Optional.empty();
    }

    private void computeUpperBound(TwoSides side, Unit requestedUnit, Set<BranchThreshold> thresholds) {
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        Set<BranchThreshold> limitingThresholds = thresholds.stream()
            .filter(Threshold::limitsByMax)
            .filter(branchThreshold -> branchThreshold.getSide().equals(side))
            .collect(Collectors.toSet());
        if (!limitingThresholds.isEmpty()) {
            double upperBound = Double.POSITIVE_INFINITY;
            Unit upperBoundInitialUnit = null;
            for (BranchThreshold threshold : limitingThresholds) {
                double currentBound = getRawBound(threshold, threshold.max().orElseThrow());
                Unit initialUnit = threshold.getUnit();
                if (initialUnit.equals(Unit.PERCENT_IMAX)) {
                    initialUnit = Unit.AMPERE;
                }
                currentBound = currentBound * getFlowUnitMultiplier(getNominalVoltage(threshold.getSide()), initialUnit, requestedUnit);
                currentBound -= reliabilityMargin * getFlowUnitMultiplier(getNominalVoltage(side), Unit.MEGAWATT, requestedUnit);
                if (currentBound < upperBound) {
                    upperBound = currentBound;
                    upperBoundInitialUnit = initialUnit;
                }
            }

            if (upperBoundInitialUnit == requestedUnit) {
                boundValues[getIndex(side, requestedUnit, Bound.UPPER)] = upperBound;
            }
        }
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

    public boolean isConnected(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(getNetworkElementId());
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
        Branch<?> branch = network.getBranch(getNetworkElementId());
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

    private double getFlow(Branch<?> branch, TwoSides side, Unit unit) {
        double activeFlow = branch.getTerminal(side).getP();
        double intensity = branch.getTerminal(side).getI();
        if (unit.equals(Unit.AMPERE)) {
            // In case flows are negative, we shall replace this value by its opposite
            return Double.isNaN(intensity) ? activeFlow * getFlowUnitMultiplier(getNominalVoltage(side), Unit.MEGAWATT, Unit.AMPERE) : Math.signum(activeFlow) * intensity;
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
        return getMinimumMarginBetweenTwoSides(unit, flowCnecValue);
    }

    private double computeMargin(FlowCnecValue flowCnecValue, Unit unit) {
        return getMinimumMarginBetweenTwoSides(unit, flowCnecValue);
    }

    private double getMinimumMarginBetweenTwoSides(Unit unit, FlowCnecValue flowCnecValue) {
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
                double marginLowerBoundSideOne = flowCnecValue.side1Value() - safeGetLowerBound(TwoSides.ONE, unit).orElse(Double.NEGATIVE_INFINITY);
                double marginUpperBoundSideOne = safeGetUpperBound(TwoSides.ONE, unit).orElse(Double.POSITIVE_INFINITY) - flowCnecValue.side1Value();

                if (marginUpperBoundSideOne < 0) {
                    highVoltageConstraints = true;
                }
                if (marginLowerBoundSideOne < 0) {
                    lowVoltageConstraints = true;
                }
            }
            if (getMonitoredSides().contains(TwoSides.TWO)) {
                double marginLowerBoundSideTwo = flowCnecValue.side2Value() - safeGetLowerBound(TwoSides.TWO, unit).orElse(Double.NEGATIVE_INFINITY);
                double marginUpperBoundSideTwo = safeGetUpperBound(TwoSides.TWO, unit).orElse(Double.POSITIVE_INFINITY) - flowCnecValue.side2Value();
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

    private static int getIndex(TwoSides side, Unit unit, Bound bound) {
        if (unit.equals(Unit.AMPERE)) {
            return getAmpereIndex(side, bound);
        } else if (unit.equals(Unit.MEGAWATT)) {
            return getMegawattIndex(side, bound);
        } else {
            throw new UnsupportedOperationException(format("Unit %s not supported", unit));
        }
    }

    private static int getMegawattIndex(TwoSides side, Bound bound) {
        if (bound.equals(Bound.LOWER)) {
            return side.equals(TwoSides.ONE) ? 0 : 1;
        } else {
            return side.equals(TwoSides.ONE) ? 2 : 3;
        }
    }

    private static int getAmpereIndex(TwoSides side, Bound bound) {
        if (bound.equals(Bound.LOWER)) {
            return side.equals(TwoSides.ONE) ? 4 : 5;
        } else {
            return side.equals(TwoSides.ONE) ? 6 : 7;
        }
    }

    private enum Bound {
        LOWER,
        UPPER
    }
}
