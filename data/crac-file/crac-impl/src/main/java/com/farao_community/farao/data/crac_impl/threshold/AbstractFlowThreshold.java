/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.Unit;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = AbsoluteFlowThreshold.class, name = "absoluteFlowThreshold"),
        @JsonSubTypes.Type(value = RelativeFlowThreshold.class, name = "relativeFlowThreshold")
    })
public abstract class AbstractFlowThreshold extends AbstractThreshold {

    protected Side side;
    protected Direction direction;
    protected double maxValue;

    public AbstractFlowThreshold(Unit unit, Side side, Direction direction) {
        super(unit);
        this.side = side;
        this.direction = direction;
        this.maxValue = Double.NaN;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    protected Terminal getTerminal(Network network, Cnec cnec) {
        // TODO: manage matching between LEFT/RIGHT and ONE/TWO
        switch (side) {
            case LEFT:
                return network.getBranch(cnec.getCriticalNetworkElement().getId()).getTerminal(Branch.Side.ONE);
            case RIGHT:
                return network.getBranch(cnec.getCriticalNetworkElement().getId()).getTerminal(Branch.Side.TWO);
            default:
                throw new FaraoException("Side is not defined");
        }
    }
}
