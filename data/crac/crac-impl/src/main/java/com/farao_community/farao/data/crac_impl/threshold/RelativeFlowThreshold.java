/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.NotSynchronizedException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.CurrentLimits;
import com.powsybl.iidm.network.Network;

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

    public RelativeFlowThreshold(NetworkElement networkElement, Side side, Direction direction, double percentageOfMax) {
        super(Unit.AMPERE, networkElement, side, direction);
        initPercentageOfMax(percentageOfMax);
    }

    private void initPercentageOfMax(double percentageOfMax) {
        if (percentageOfMax < 0 || percentageOfMax > 100) {
            throw new FaraoException("PercentageOfMax of RelativeFlowThresholds must be in [0, 100]");
        }
        this.percentageOfMax = percentageOfMax;
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
        CurrentLimits currentLimits = branch.getCurrentLimits(Branch.Side.ONE);
        if (currentLimits == null) {
            currentLimits = branch.getCurrentLimits(Branch.Side.TWO);
        }
        if (currentLimits == null) {
            throw new FaraoException(String.format("No IMAP defined for %s", networkElement.getId()));
        }
        // compute maxValue, in Unit.AMPERE
        branchLimit = currentLimits.getPermanentLimit();
    }

    @Override
    public AbstractThreshold copy() {
        RelativeFlowThreshold copiedRelativeFlowThreshold = new RelativeFlowThreshold(networkElement, side, direction, percentageOfMax);
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
