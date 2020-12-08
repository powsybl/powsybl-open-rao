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
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.CurrentLimits;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
    private static final String TIE_LINE_WARN = "For tie-line {}, the CNEC network element ID {} is not half1 nor half2 IDs. Most limiting threshold will be taken.";
    private static final int NB_CHARACTER_TWO_UCTE_NODES = 17;

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

    public RelativeFlowThreshold(NetworkElement networkElement, Side side, Direction direction, double percentageOfMax, double frmInMw) {
        super(Unit.AMPERE, networkElement, side, direction);
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
            branchLimit = getTieLineLimit(tieLine);
        } else {
            CurrentLimits currentLimits = branch.getCurrentLimits(Branch.Side.ONE);
            if (currentLimits == null) {
                currentLimits = branch.getCurrentLimits(Branch.Side.TWO);
            }
            if (currentLimits == null) {
                throw new FaraoException(String.format("No IMAP defined for %s", networkElement.getId()));
            }
            branchLimit = currentLimits.getPermanentLimit();
        }
    }

    private double getTieLineLimit(TieLine tieLine) {
        Optional<Branch.Side> side = getTieLineSide(tieLine);

        if (side.isPresent()) {
            return tieLine.getCurrentLimits(side.get()).getPermanentLimit();
        } else {
            LOGGER.warn(TIE_LINE_WARN, tieLine.getId(), networkElement.getId());
            return Math.min(
                tieLine.getCurrentLimits(Branch.Side.ONE).getPermanentLimit(),
                tieLine.getCurrentLimits(Branch.Side.TWO).getPermanentLimit());
        }
    }

    private Optional<Branch.Side> getTieLineSide(TieLine tieLine) {

        if (tieLine.getHalf1().getId().equals(networkElement.getId())) {
            return Optional.of(Branch.Side.ONE);
        }
        if (tieLine.getHalf2().getId().equals(networkElement.getId())) {
            return Optional.of(Branch.Side.TWO);
        }
        if (networkElement.getId().length() > NB_CHARACTER_TWO_UCTE_NODES * 2 && networkElement.getId().contains("+")) {
            // temporary UCTE patch : if the aggregated tie-line id is given (e.g. below) then an undefined side is returned
            // example : BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1
            return Optional.empty();
        }
        if (tieLine.getHalf1().getId().substring(1, NB_CHARACTER_TWO_UCTE_NODES).equals(networkElement.getId().substring(1, NB_CHARACTER_TWO_UCTE_NODES))) {
            // temporary UCTE patch : check id of the line without the order code as an element name could be given in the crac instead
            return Optional.of(Branch.Side.ONE);
        }
        if (tieLine.getHalf2().getId().substring(1, NB_CHARACTER_TWO_UCTE_NODES).equals(networkElement.getId().substring(1, NB_CHARACTER_TWO_UCTE_NODES))) {
            // temporary UCTE patch : check id of the line without the order code as an element name could be given in the crac instead
            return Optional.of(Branch.Side.TWO);
        }
        // no match
        return Optional.empty();
    }

    @Override
    public AbstractThreshold copy() {
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
