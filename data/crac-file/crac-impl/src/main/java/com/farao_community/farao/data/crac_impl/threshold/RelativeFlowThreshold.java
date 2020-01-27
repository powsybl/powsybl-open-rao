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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class RelativeFlowThreshold extends AbstractFlowThreshold {

    private double percentageOfMax;

    @JsonCreator
    public RelativeFlowThreshold(@JsonProperty("unit") Unit unit,
                                 @JsonProperty("side") Side side,
                                 @JsonProperty("direction") Direction direction,
                                 @JsonProperty("percentageOfMax") double percentageOfMax) {
        super(unit, side, direction);
        this.percentageOfMax = percentageOfMax;
    }

    @Override
    public Optional<Double> getMaxThreshold() throws SynchronizationException {
        if (Double.isNaN(maxValue)) {
            throw new SynchronizationException("Relative flow threshold have not been synchronized with network");
        }
        return Optional.of(maxValue);
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit unit) throws SynchronizationException {
        return Optional.empty();
    }

    @Override
    public boolean isMinThresholdOvercome(Network network, Cnec cnec) {
        return false;
    }

    @Override
    public boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        if (Double.isNaN(maxValue)) {
            throw new SynchronizationException("Relative flow threshold have not been synchronized with network");
        }
        return computeMargin(network, cnec) < 0;
    }

    @Override
    public double computeMargin(Network network, Cnec cnec) throws SynchronizationException {
        if (Double.isNaN(maxValue)) {
            throw new SynchronizationException("Relative flow threshold have not been synchronized with network");
        }
        switch (unit) {
            case AMPERE:
                return maxValue - getI(network, cnec);
            case MEGAWATT:
                return maxValue - getP(network, cnec);
            case DEGREE:
            case KILOVOLT:
            default:
                throw new FaraoException("Incompatible type of unit between FlowThreshold and degree or kV");
        }
    }

    @Override
    public void synchronize(Network network, Cnec cnec) {
        // TODO: manage matching between LEFT/RIGHT and ONE/TWO
        switch (side) {
            case LEFT:
                maxValue = network.getBranch(cnec.getCriticalNetworkElement().getId()).getCurrentLimits(Branch.Side.ONE).getPermanentLimit() * percentageOfMax / 100;
                break;
            case RIGHT:
                maxValue = network.getBranch(cnec.getCriticalNetworkElement().getId()).getCurrentLimits(Branch.Side.TWO).getPermanentLimit() * percentageOfMax / 100;
                break;
            default:
                throw new FaraoException("Side is not defined");
        }

    }

    @Override
    public void desynchronize() {
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
