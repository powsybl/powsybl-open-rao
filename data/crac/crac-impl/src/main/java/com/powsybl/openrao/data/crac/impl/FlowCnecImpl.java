/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.UnitConverter.getFlowUnitMultiplier;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecImpl extends AbstractCnec<FlowCnec> implements FlowCnec {

    private Set<BranchThreshold> thresholds;
    private final Double[] nominalVoltages = new Double[2];
    private BranchBoundsCache bounds = new BranchBoundsCache();
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
        super(id, name, Collections.singleton(networkElement), operator, border, state, optimized, monitored, frm);
        this.thresholds = thresholds;
        this.nominalVoltages[0] = nominalVLeft;
        this.nominalVoltages[1] = nominalVRight;
        this.iMax[0] = iMaxLeft;
        this.iMax[1] = iMaxRight;
    }

    @Override
    public NetworkElement getNetworkElement() {
        return getNetworkElements().iterator().next();
    }

    @Override
    public double computeMargin(double actualValue, TwoSides side, Unit unit) {
        double marginOnLowerBound = actualValue - getLowerBound(side, unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = getUpperBound(side, unit).orElse(Double.POSITIVE_INFINITY) - actualValue;
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    @Override
    public final Set<BranchThreshold> getThresholds() {
        return thresholds;
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
        if (!bounds.isLowerBoundComputed(side, requestedUnit)) {
            Set<BranchThreshold> limitingThresholds = thresholds.stream()
                .filter(Threshold::limitsByMin)
                .filter(branchThreshold -> branchThreshold.getSide().equals(side))
                .collect(Collectors.toSet());

            if (!limitingThresholds.isEmpty()) {
                double lowerBound = Double.NEGATIVE_INFINITY;
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
                    Unit initialUnit = threshold.getUnit();
                    if (initialUnit.equals(Unit.PERCENT_IMAX)) {
                        initialUnit = Unit.AMPERE;
                    }
                    currentBound = currentBound * getFlowUnitMultiplier(getNominalVoltage(threshold.getSide()), initialUnit, requestedUnit);
                    currentBound -= reliabilityMargin * getFlowUnitMultiplier(getNominalVoltage(side), Unit.MEGAWATT, requestedUnit);
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

    public boolean isConnected(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(getNetworkElement().getId());
        if (identifiable instanceof Connectable) {
            Connectable<?> connectable = (Connectable<?>) identifiable;
            return connectable.getTerminals().stream().allMatch(Terminal::isConnected);
        }
        return true; // by default
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

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.FLOW;
    }
}
