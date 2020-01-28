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
 * Limits of a flow (in MEGAWATT or AMPERE) through a branch.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = AbsoluteFlowThreshold.class, name = "absoluteFlowThreshold"),
        @JsonSubTypes.Type(value = RelativeFlowThreshold.class, name = "relativeFlowThreshold")
    })
public abstract class AbstractFlowThreshold extends AbstractThreshold {

    /**
     * Side of the network element which is monitored
     */
    protected Side side;

    /**
     * Direction in which the flow is monitored
     */
    protected Direction direction;

    /**
     * maximum admissible flow value
     */
    protected double maxValue;

    /**
     * voltage level of the network element
     */
    protected double voltageLevel;

    public AbstractFlowThreshold(Unit unit, Side side, Direction direction) {
        super(unit);
        if (!unit.equals(Unit.AMPERE) && !unit.equals(Unit.MEGAWATT)) {
            throw new FaraoException(String.format("Unit of flow threshold can only be AMPERE or MEGAWATT, %s is not a valid value", unit.toString()));
        }
        this.side = side;
        this.direction = direction;
        this.maxValue = Double.NaN;
        this.voltageLevel = Double.NaN;
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

    private Terminal getTerminal(Network network, Cnec cnec) {
        return network.getBranch(cnec.getCriticalNetworkElement().getId()).getTerminal(getBranchSide());
    }

    private static boolean isCnecDisconnected(Network network, Cnec cnec) {
        Branch branch = network.getBranch(cnec.getCriticalNetworkElement().getId());
        return !branch.getTerminal1().isConnected() || !branch.getTerminal2().isConnected();
    }

    private double getI(Network network, Cnec cnec) {
        double i = isCnecDisconnected(network, cnec) ? 0 : getTerminal(network, cnec).getI();
        if (Double.isNaN(i)) {
            throw new FaraoException(String.format("No intensity (I) data available for CNEC %s", cnec.getName()));
        }
        return i;
    }

    private double getP(Network network, Cnec cnec) {
        double p = isCnecDisconnected(network, cnec) ? 0 : getTerminal(network, cnec).getP();
        if (Double.isNaN(p)) {
            throw new FaraoException(String.format("No transmitted power (P) data available for CNEC %s", cnec.getName()));
        }
        return p;
    }

    private double convertMwToAmps(double valueInMw) throws SynchronizationException {
        if (!Double.isNaN(voltageLevel)) {
            double ratio = voltageLevel * Math.sqrt(3) / 1000;
            return valueInMw / ratio;
        } else {
            throw new SynchronizationException("Voltage level has not been synchronised.");
        }
    }

    protected double convertAmpsToMw(double valueInA) throws SynchronizationException {
        if (!Double.isNaN(voltageLevel)) {
            double ratio = voltageLevel * Math.sqrt(3) / 1000;
            return valueInA * ratio;
        } else {
            throw new SynchronizationException("Voltage level has not been synchronised.");
        }
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit unit) throws SynchronizationException {
        if (unit == this.unit) {
            return getMaxThreshold();
        }
        if (!getMaxThreshold().isPresent()) {
            return Optional.empty();
        } else {
            if (unit.equals(Unit.AMPERE)) {
                // get AMPERE from a threshold in MEGAWATT
                return Optional.of(convertMwToAmps(getMaxThreshold().get()));
            } else if (unit.equals(Unit.MEGAWATT)) {
                // get MEGAWATT from a threshold in AMPERE
                return Optional.of(convertAmpsToMw(getMaxThreshold().get()));
            } else {
                throw new FaraoException(String.format("Unit of flow threshold can only be AMPERE or MEGAWATT, %s is not a valid value", unit.toString()));
            }
        }
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
    public boolean isMinThresholdOvercome(Network network, Cnec cnec) {
        return false;
    }

    @Override
    public boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        return computeMargin(network, cnec) < 0;
    }

    @Override
    public double computeMargin(Network network, Cnec cnec) throws SynchronizationException {
        switch (unit) {
            case AMPERE:
                return maxValue - getI(network, cnec);
            case MEGAWATT:
                return maxValue - getP(network, cnec);
            case DEGREE:
            case KILOVOLT:
            default:
                throw new FaraoException(String.format("Unit of flow threshold can only be AMPERE or MEGAWATT, %s is not a valid value", unit.toString()));
        }
    }

    @Override
    public void synchronize(Network network, Cnec cnec) {
        voltageLevel = network.getBranch(cnec.getCriticalNetworkElement().getId()).getTerminal(getBranchSide()).getVoltageLevel().getNominalV();
    }

    @Override
    public void desynchronize() {
        voltageLevel = Double.NaN;
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
