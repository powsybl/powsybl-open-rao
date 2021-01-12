/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.cnec;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.CurrentLimits;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_api.Side.LEFT;
import static com.farao_community.farao.data.crac_api.Side.RIGHT;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeName("flow-cnec")
public class FlowCnecImpl extends AbstractBranchCnec implements BranchCnec {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCnecImpl.class);
    private static final String TIE_LINE_WARN = "For tie-line {}, the network element ID {} is not half1 nor half2 IDs. Most limiting threshold will be taken.";
    private static final String IMAX_CONVERSION_WARN = "Imax on side {} for the network element {} will be converted.";
    private static final int NB_CHARACTER_UCTE_LINE_WITHOUT_ORDER_CODE = 19;
    private static final int NB_MAX_CHARACTER_ELEMENT_NAME = 12;

    private final Double[] iMax = new Double[2];
    private double frm;

    public FlowCnecImpl(String id, String name, NetworkElement networkElement, State state, boolean optimized, boolean monitored, Set<BranchThreshold> thresholds, double frm) {
        super(id, name, networkElement, state, optimized, monitored, thresholds);
        this.frm = frm;
    }

    public FlowCnecImpl(String id, NetworkElement networkElement, State state, boolean optimized, boolean monitored, Set<BranchThreshold> thresholds, double frm) {
        super(id, networkElement, state, optimized, monitored, thresholds);
        this.frm = frm;
    }

    private Double getIMax(Side side) {
        checkSynchronized(format("access iMax values of flow cnec %s", getId()));
        if (side.equals(LEFT)) {
            return iMax[0];
        } else {
            return iMax[1];
        }
    }

    private void setIMax(Side side, Double value) {
        if (side.equals(LEFT)) {
            iMax[0] = value;
        } else {
            iMax[1] = value;
        }
    }

    @Override
    public Optional<Double> getLowerBound(Side side, Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        if (!bounds.isLowerBoundComputed(side, requestedUnit)) {
            //LOGGER.debug("Lower bound computed for {} in {}", getId(), requestedUnit);
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
        requestedUnit.checkPhysicalParameter(getPhysicalParameter());
        if (!bounds.isUpperBoundComputed(side, requestedUnit)) {
            //LOGGER.debug("Upper bound computed for {} in {}", getId(), requestedUnit);
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
    public void synchronize(Network network) {
        super.synchronize(network);
        Branch<?> branch = super.checkAndGetValidBranch(network, networkElement.getId());
        if (branch instanceof TieLine) {
            TieLine tieLine = (TieLine) branch;
            // Independently from what is defined in side field this method will return the limit of the side that
            // corresponds to the network element id (either half1 side or half2).
            // If no matching, it will set the limit of the most limiting side.
            setIMaxForTieLine(tieLine);
        } else {
            CurrentLimits currentLimits1 = branch.getCurrentLimits(LEFT.iidmSide());
            CurrentLimits currentLimits2 = branch.getCurrentLimits(RIGHT.iidmSide());
            if (currentLimits1 == null && currentLimits2 == null) {
                throw new FaraoException(String.format("No IMAP defined for %s", networkElement.getId()));
            } else {
                if (currentLimits1 != null) {
                    setIMax(LEFT, currentLimits1.getPermanentLimit());
                } else {
                    LOGGER.warn(IMAX_CONVERSION_WARN, LEFT.iidmSide(), networkElement.getId());
                    setIMax(LEFT, changeValueSide(currentLimits2.getPermanentLimit(), RIGHT, LEFT));
                }
                if (currentLimits2 != null) {
                    setIMax(RIGHT, currentLimits2.getPermanentLimit());
                } else {
                    LOGGER.warn(IMAX_CONVERSION_WARN, RIGHT.iidmSide(), networkElement.getId());
                    setIMax(RIGHT, changeValueSide(currentLimits1.getPermanentLimit(), LEFT, RIGHT));
                }
            }
        }
    }

    private void setIMaxForTieLine(TieLine tieLine) {
        Optional<Side> side = getTieLineSide(tieLine);
        double tieLineLimit;
        if (side.isPresent()) {
            tieLineLimit = tieLine.getCurrentLimits(side.get().iidmSide()).getPermanentLimit();
        } else {
            LOGGER.warn(TIE_LINE_WARN, tieLine.getId(), networkElement.getId());
            tieLineLimit = Math.min(
                    tieLine.getCurrentLimits(LEFT.iidmSide()).getPermanentLimit(),
                    tieLine.getCurrentLimits(RIGHT.iidmSide()).getPermanentLimit());
        }
        setIMax(LEFT, tieLineLimit);
        setIMax(RIGHT, tieLineLimit);
    }

    private Optional<Side> getTieLineSide(TieLine tieLine) {

        if (tieLine.getHalf1().getId().equals(networkElement.getId())) {
            return Optional.of(LEFT);
        }
        if (tieLine.getHalf2().getId().equals(networkElement.getId())) {
            return Optional.of(RIGHT);
        }
        if (networkElement.getId().length() > NB_CHARACTER_UCTE_LINE_WITHOUT_ORDER_CODE + NB_MAX_CHARACTER_ELEMENT_NAME) {
            // temporary UCTE patch : check if network element length is compatible with an UCTE half-line id
            // if not, network element id is not of the type (e.g.) : BBE2AA1  X_BEFR1  ELEMENTNAME_
            // it is probably the aggregated tie-line id (e.g.) : BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1
            return Optional.empty();
        }
        if (tieLine.getHalf1().getId().substring(0, tieLine.getHalf1().getId().length() - 1).equals(networkElement.getId().substring(0, NB_CHARACTER_UCTE_LINE_WITHOUT_ORDER_CODE - 1))) {
            // temporary UCTE patch : check id of the line without the order code as an element name could be given in the crac instead
            // assuming HalfLine id (e.g.) : DDE2AA1  X_NLDE1  1
            // assuming NetworkElement id (e.g.) : DDE2AA1  X_NLDE1  E_NAME_H1
            return Optional.of(LEFT);
        }
        if (tieLine.getHalf2().getId().substring(0, tieLine.getHalf2().getId().length() - 1).equals(networkElement.getId().substring(0, NB_CHARACTER_UCTE_LINE_WITHOUT_ORDER_CODE - 1))) {
            // temporary UCTE patch : check id of the line without the order code as an element name could be given in the crac instead
            return Optional.of(RIGHT);
        }
        // no match
        return Optional.empty();
    }

    @Override
    public double getReliabilityMargin() {
        return frm;
    }

    @Override
    public void setReliabilityMargin(double reliabilityMargin) {
        if (reliabilityMargin < 0) {
            throw new FaraoException();
        }
        this.frm = reliabilityMargin;
    }

    @Override
    public BranchCnec copy() {
        return new FlowCnecImpl(getId(), getName(), networkElement, state, optimized, monitored, thresholds, frm);
    }

    @Override
    public BranchCnec copy(NetworkElement networkElement, State state) {
        return new FlowCnecImpl(getId(), getName(), networkElement, state, optimized, monitored, thresholds, frm);
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
