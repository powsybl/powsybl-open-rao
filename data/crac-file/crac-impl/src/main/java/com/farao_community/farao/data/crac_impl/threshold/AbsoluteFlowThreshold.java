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
    public Optional<Double> getMaxThreshold(Unit unit) throws SynchronizationException {
        if (unit == this.unit) {
            return getMaxThreshold();
        } else {
            if (getMaxThreshold().isPresent()) {
                if (unit.equals(Unit.AMPERE)) {
                    return Optional.of(convertMwToAmps(getMaxThreshold().get()));
                } else if (unit.equals(Unit.MEGAWATT)) {
                    return Optional.of(convertAmpsToMw(getMaxThreshold().get()));
                } else {
                    throw new FaraoException("Unit of voltage threshold should be A or MW.");
                }
            } else {
                return Optional.empty();
            }
        }
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
        voltageLevel = Optional.of(network.getBranch(cnec.getCriticalNetworkElement().getId()).getTerminal(getBranchSide()).getVoltageLevel().getNominalV());
    }

    @Override
    public void desynchronize() {
        voltageLevel = Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbsoluteFlowThreshold threshold = (AbsoluteFlowThreshold) o;
        return super.equals(threshold) && maxValue == threshold.maxValue;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) maxValue;
        return result;
    }
}
