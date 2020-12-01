/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.NotSynchronizedException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Limits of a flow through a branch. Given as a percentage of the branch limit
 * defined in a Network.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("relative-flow-threshold")
public class RelativeFlowThreshold extends AbstractFlowThreshold {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelativeFlowThreshold.class);
    private static final String TIE_LINE_WARN = "For tie-line {}, the network element ID {} is not half1 nor half2 IDs. Most limiting threshold will be taken.";
    private static final String SIDE_INVERSION_WARN = "Side of relative threshold on the network element {} will be inverted because no limit has been found on the defined side ({}).";

    private double branchLimit;

    /**
     * Percentage of the branch limit which shouldn't be overcome
     */
    private double percentageOfMax;

    @JsonCreator
    public RelativeFlowThreshold(@JsonProperty("side") Side side,
                                 @JsonProperty("direction") Direction direction,
                                 @JsonProperty("percentageOfMax") double percentageOfMax) {
        super(Unit.AMPERE, side, direction);
        initPercentageOfMax(percentageOfMax);
    }

    public RelativeFlowThreshold(Direction direction, double percentageOfMax) {
        super(Unit.AMPERE, Side.LEFT, direction);
        initPercentageOfMax(percentageOfMax);
    }

    public RelativeFlowThreshold(NetworkElement networkElement, Side side, Direction direction, double percentageOfMax, double frmInMw) {
        super(Unit.AMPERE, networkElement, side, direction);
        initPercentageOfMax(percentageOfMax);
        this.frmInMW = frmInMw;
    }

    public RelativeFlowThreshold(NetworkElement networkElement, Direction direction, double percentageOfMax, double frmInMw) {
        super(Unit.AMPERE, networkElement, Side.LEFT, direction);
        initPercentageOfMax(percentageOfMax);
        this.frmInMW = frmInMw;
    }

    private void initPercentageOfMax(double percentageOfMax) {
        if (percentageOfMax < 0) {
            throw new FaraoException("PercentageOfMax of RelativeFlowThresholds must be positive.");
        }
        this.percentageOfMax = percentageOfMax;
    }

    public double getPercentageOfMax() {
        return percentageOfMax;
    }

    @Override
    protected double getAbsoluteMax() {
        if (!isSynchronized) {
            throw new NotSynchronizedException(String.format("Relative threshold on branch %s has not been synchronized so its absolute max value cannot be accessed", networkElement.getId()));
        }
        return branchLimit * percentageOfMax / 100;
    }

    @Override
    public void synchronize(Network network) {
        super.synchronize(network);
        Branch branch = super.checkAndGetValidBranch(network, networkElement.getId());
        if (branch instanceof TieLine) {
            TieLine tieLine = (TieLine) branch;
            // Independently from what is defined in side field this method will return the limit of the side that
            // corresponds to the network element id (either half1 side or half2).
            // If no matching, it will return the limit of the most limiting side.
            branchLimit = getBranchLimit(tieLine);
        } else {
            CurrentLimits currentLimits = branch.getCurrentLimits(getBranchSide());
            if (currentLimits != null) {
                branchLimit = currentLimits.getPermanentLimit();
            } else {
                // If the branch is a simple line limit of the opposite will be taken as granted. If it a transformer,
                // a conversion will be done to bring back the limit according to side field.
                branchLimit = getBranchLimitFromOppositeSide(branch);
            }
        }
    }

    private double getBranchLimit(TieLine tieLine) {
        Branch.Side side;
        if (tieLine.getHalf1().getId().equals(networkElement.getId())) {
            side = Branch.Side.ONE;
        } else if (tieLine.getHalf2().getId().equals(networkElement.getId())) {
            side = Branch.Side.TWO;
        } else {
            LOGGER.warn(TIE_LINE_WARN, tieLine.getId(), networkElement.getId());
            return Math.min(
                tieLine.getCurrentLimits(Branch.Side.ONE).getPermanentLimit(),
                tieLine.getCurrentLimits(Branch.Side.TWO).getPermanentLimit());
        }
        return tieLine.getCurrentLimits(side).getPermanentLimit();
    }

    private double getBranchLimitFromOppositeSide(Branch branch) {
        CurrentLimits currentLimits = branch.getCurrentLimits(getOppositeBranchSide());
        if (currentLimits != null) {
            LOGGER.warn(SIDE_INVERSION_WARN, networkElement.getId(), side);
            // No conversion required if it is a simple line
            double conversionCoefficient = 1;
            if (branch instanceof TwoWindingsTransformer) {
                TwoWindingsTransformer twoWindingsTransformer = (TwoWindingsTransformer) branch;
                // Coefficient if side is ONE
                conversionCoefficient = twoWindingsTransformer.getRatedU2() / twoWindingsTransformer.getRatedU1();
                if (getBranchSide().equals(Branch.Side.TWO)) {
                    // Invert it if it is for side TWO
                    conversionCoefficient = 1 / conversionCoefficient;
                }
            }
            return currentLimits.getPermanentLimit() * conversionCoefficient;
        } else {
            throw new FaraoException(String.format("No IMAP defined for %s", networkElement.getId()));
        }
    }

    @Override
    public AbstractFlowThreshold copy() {
        RelativeFlowThreshold copiedRelativeFlowThreshold = new RelativeFlowThreshold(networkElement, side, direction, percentageOfMax, frmInMW);
        if (isSynchronized()) {
            copiedRelativeFlowThreshold.isSynchronized = isSynchronized;
            copiedRelativeFlowThreshold.voltageLevel = voltageLevel;
            copiedRelativeFlowThreshold.branchLimit = branchLimit;
        }
        return copiedRelativeFlowThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RelativeFlowThreshold threshold = (RelativeFlowThreshold) o;
        return super.equals(threshold) && percentageOfMax == threshold.percentageOfMax;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) percentageOfMax;
        return result;
    }
}
