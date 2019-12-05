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
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * Limits of a flow through an equipment.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class AbsoluteFlowThreshold extends AbstractFlowThreshold {

    @JsonCreator
    public AbsoluteFlowThreshold(@JsonProperty("unit") Unit unit,
                                 @JsonProperty("side") Side side,
                                 @JsonProperty("direction") Direction direction,
                                 @JsonProperty("maxValue") double maxValue) {
        super(unit, side, direction);
        this.maxValue = maxValue;
    }

    @Override
    public Optional<Double> getMaxThreshold() {
        return Optional.of(maxValue);
    }

    @Override
    public boolean isMinThresholdOvercome(Network network, Cnec cnec) {
        return false;
    }

    @Override
    public boolean isMaxThresholdOvercome(Network network, Cnec cnec) {
        return computeMargin(network, cnec) < 0;
    }

    @Override
    public double computeMargin(Network network, Cnec cnec) {
        switch (unit) {
            case AMPERE:
                return maxValue - getTerminal(network, cnec).getI();
            case MEGAWATT:
                return maxValue - getTerminal(network, cnec).getP();
            case DEGREE:
            case KILOVOLT:
            default:
                throw new FaraoException("Incompatible type of unit between FlowThreshold and degree or kV");
        }
    }

    @Override
    public void synchronize(Network network, Cnec cnec) {

    }

    @Override
    public void desynchronize() {

    }
}
