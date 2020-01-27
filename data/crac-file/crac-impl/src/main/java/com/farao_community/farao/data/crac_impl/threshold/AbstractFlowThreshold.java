/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

import java.util.Optional;

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
    protected Optional<Double> voltageLevel;

    protected Branch.Side getBranchSide() {
        // TODO: manage matching between LEFT/RIGHT and ONE/TWO
        if (side.equals(Side.LEFT)) {
            return Branch.Side.ONE;
        } else if (side.equals(Side.RIGHT)) {
            return Branch.Side.TWO;
        } else {
            throw new FaraoException("Side is not defined");
        }
    }

    protected double convertMwToAmps(double valueInMw) throws SynchronizationException {
        if (voltageLevel.isPresent()) {
            double ratio = voltageLevel.get() * Math.sqrt(3) / 1000;
            return valueInMw / ratio;
        } else {
            throw new SynchronizationException("Voltage level is not defined.");
        }
    }

    protected double convertAmpsToMw(double valueInA) throws SynchronizationException {
        if (voltageLevel.isPresent()) {
            double ratio = voltageLevel.get() * Math.sqrt(3) / 1000;
            return valueInA * ratio;
        } else {
            throw new SynchronizationException("Voltage level is not defined.");
        }
    }

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

    private Terminal getTerminal(Network network, Cnec cnec) {
        return network.getBranch(cnec.getCriticalNetworkElement().getId()).getTerminal(getBranchSide());
    }

    private static boolean isCnecDisconnected(Network network, Cnec cnec) {
        Branch branch = network.getBranch(cnec.getCriticalNetworkElement().getId());
        return !branch.getTerminal1().isConnected() || !branch.getTerminal2().isConnected();
    }

    protected double getI(Network network, Cnec cnec) {
        double i = isCnecDisconnected(network, cnec) ? 0 : getTerminal(network, cnec).getI();
        if (Double.isNaN(i)) {
            throw new FaraoException(String.format("No intensity (I) data available for CNEC %s", cnec.getName()));
        }
        return i;
    }

    protected double getP(Network network, Cnec cnec) {
        double p = isCnecDisconnected(network, cnec) ? 0 : getTerminal(network, cnec).getP();
        if (Double.isNaN(p)) {
            throw new FaraoException(String.format("No transmitted power (P) data available for CNEC %s", cnec.getName()));
        }
        return p;
    }

    @Override
    public Optional<Double> getMinThreshold() {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getMinThreshold(Unit unit) {
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractFlowThreshold threshold = (AbstractFlowThreshold) o;
        return unit.equals(threshold.unit) && side.equals(threshold.side) && direction.equals(threshold.direction);
    }

    @Override
    public int hashCode() {
        int result = unit.hashCode();
        result = 31 * result + side.hashCode();
        result = 31 * result + direction.hashCode();
        return result;
    }
}
