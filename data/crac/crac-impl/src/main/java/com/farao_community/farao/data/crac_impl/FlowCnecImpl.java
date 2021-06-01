/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.Threshold;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecImpl extends AbstractBranchCnec<FlowCnec> implements FlowCnec {

    private final Double[] iMax = new Double[2];

    FlowCnecImpl(String id,
                 String name,
                 NetworkElement networkElement,
                 String operator,
                 State state,
                 boolean optimized,
                 boolean monitored,
                 Set<BranchThreshold> thresholds,
                 double frm,
                 Double nominalVLeft,
                 Double nominalVRight,
                 Double iMaxLeft,
                 Double iMaxRight) {
        super(id, name, networkElement, operator, state, optimized, monitored, thresholds, frm, nominalVLeft, nominalVRight);
        this.iMax[0] = iMaxLeft;
        this.iMax[1] = iMaxRight;
    }

    @Override
    public Double getIMax(Side side) {
        if (side.equals(LEFT)) {
            return iMax[0];
        } else {
            return iMax[1];
        }
    }

    @Override
    public Optional<Double> getLowerBound(Side side, Unit requestedUnit) {

        if (!requestedUnit.equals(Unit.AMPERE) && !requestedUnit.equals(Unit.MEGAWATT)) {
            throw new FaraoException("FlowCnec lowerBound can only be requested in AMPERE or MEGAWATT");
        }
        if (!bounds.isLowerBoundComputed(side, requestedUnit)) {
            Set<BranchThreshold> limitingThresholds = thresholds.stream()
                    .filter(Threshold::limitsByMin)
                    .collect(Collectors.toSet());

            if (!limitingThresholds.isEmpty()) {
                double lowerBound = Double.NEGATIVE_INFINITY;
                for (BranchThreshold threshold : limitingThresholds) {
                    double currentBound = getRawBound(threshold, threshold.min().orElseThrow());
                    currentBound = changeValueUnit(currentBound, threshold.getUnit(), requestedUnit, threshold.getSide());
                    currentBound = changeValueSide(currentBound, threshold, side, requestedUnit);
                    currentBound += changeValueUnit(frm, Unit.MEGAWATT, requestedUnit, side);
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
    public Optional<Double> getUpperBound(Side side, Unit requestedUnit) {

        if (!requestedUnit.equals(Unit.AMPERE) && !requestedUnit.equals(Unit.MEGAWATT)) {
            throw new FaraoException("FlowCnec upperBound can only be requested in AMPERE or MEGAWATT");
        }

        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        if (!bounds.isUpperBoundComputed(side, requestedUnit)) {
            Set<BranchThreshold> limitingThresholds = thresholds.stream()
                    .filter(Threshold::limitsByMax)
                    .collect(Collectors.toSet());
            if (!limitingThresholds.isEmpty()) {
                double upperBound = Double.POSITIVE_INFINITY;
                for (BranchThreshold threshold : limitingThresholds) {
                    double currentBound = getRawBound(threshold, threshold.max().orElseThrow());
                    currentBound = changeValueUnit(currentBound, threshold.getUnit(), requestedUnit, threshold.getSide());
                    currentBound = changeValueSide(currentBound, threshold, side, requestedUnit);
                    currentBound -= changeValueUnit(frm, Unit.MEGAWATT, requestedUnit, side);
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

    private double changeValueUnit(double value, Unit oldUnit, Unit newUnit, Side side) {
        if (oldUnit.equals(newUnit) || (oldUnit.equals(Unit.PERCENT_IMAX) && newUnit.equals(Unit.AMPERE))) {
            return value;
        } else {
            double conversionFactor = Math.sqrt(3) * getNominalVoltage(side) / 1000; // Conversion from A to MW
            if (oldUnit.equals(Unit.MEGAWATT) && newUnit.equals(Unit.AMPERE)) {
                conversionFactor = 1 / conversionFactor;
            }
            return value * conversionFactor;
        }
    }

    private double changeValueSide(double thresholdValue, BranchThreshold threshold, Side requestedSide, Unit requestedUnit) {
        if (!threshold.getSide().equals(requestedSide)
                && requestedUnit.equals(Unit.AMPERE)
                && !getNominalVoltage(LEFT).equals(getNominalVoltage(RIGHT))) {
            return changeValueSide(thresholdValue, threshold.getSide(), requestedSide);
        }
        return thresholdValue;
    }

    private double changeValueSide(double value, Side oldSide, Side newSide) {
        return value * getNominalVoltage(oldSide) / getNominalVoltage(newSide);
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.FLOW;
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
        return super.equals(cnec) && frm == cnec.frm;
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + (int) frm;
        return hashCode;
    }
}
