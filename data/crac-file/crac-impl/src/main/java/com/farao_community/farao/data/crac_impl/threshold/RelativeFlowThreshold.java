/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
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

    /**
     * Percentage of the branch limit which shouldn't be overcome
     */
    private double percentageOfMax;

    @JsonCreator
    public RelativeFlowThreshold(@JsonProperty("side") Side side,
                                 @JsonProperty("direction") Direction direction,
                                 @JsonProperty("percentageOfMax") double percentageOfMax) {
        super(Unit.AMPERE, side, direction);
        if (percentageOfMax < 0 || percentageOfMax > 100) {
            throw new FaraoException("PercentageOfMax of RelativeFlowThresholds must be in [0, 100]");
        }
        this.percentageOfMax = percentageOfMax;
    }

    public RelativeFlowThreshold(Side side, Direction direction, NetworkElement networkElement, double percentageOfMax) {
        super(Unit.AMPERE, side, direction, networkElement);
        this.percentageOfMax = percentageOfMax;
    }

    @Override
    protected double getAbsoluteMax() {
        if (!isSynchronized) {
            throw new NotSynchronizedException(String.format("Relative threshold on branch %s has not been synchronized with network so its absolute max value cannot be accessed", networkElement.getId()));
        }
        return maxValue;
    }

    @Override
    public void synchronize(Network network) {
        super.synchronize(network);
        // compute maxValue, in Unit.AMPERE
        maxValue = super.checkAndGetValidBranch(network, networkElement.getId()).getCurrentLimits(getBranchSide()).getPermanentLimit() * percentageOfMax / 100;
    }

    @Override
    public void desynchronize() {
        super.desynchronize();
        maxValue = Double.NaN;
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
