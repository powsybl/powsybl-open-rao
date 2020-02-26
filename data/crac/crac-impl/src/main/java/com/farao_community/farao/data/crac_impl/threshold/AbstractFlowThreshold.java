/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * Limits of a flow (in MEGAWATT or AMPERE) through a branch.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AbsoluteFlowThreshold.class, name = "absolute-flow-threshold"),
        @JsonSubTypes.Type(value = RelativeFlowThreshold.class, name = "relative-flow-threshold")
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
        unit.checkPhysicalParameter(PhysicalParameter.FLOW);
        this.side = side;
        this.direction = direction;
        this.maxValue = Double.NaN;
        this.voltageLevel = Double.NaN;
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.FLOW;
    }

    @Override
    public Optional<Double> getMinThreshold(Unit requestedUnit) throws SynchronizationException {
        if (direction == Direction.DIRECT) {
            return Optional.empty();
        } else { // Direction.OPPOSITE and Direction.BOTH
            return Optional.of(-convert(getAbsoluteMax(), unit, requestedUnit));
        }
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit requestedUnit) throws SynchronizationException {
        if (direction == Direction.OPPOSITE) {
            return Optional.empty();
        } else { // Direction.DIRECT and Direction.BOTH
            return Optional.of(convert(getAbsoluteMax(), unit, requestedUnit));
        }
    }

    @Override
    public void synchronize(Network network, Cnec cnec) {
        voltageLevel = network.getBranch(cnec.getNetworkElement().getId()).getTerminal(getBranchSide()).getVoltageLevel().getNominalV();
    }

    @Override
    public void desynchronize() {
        voltageLevel = Double.NaN;
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

    /**
     * Convert the Farao Side of the Threshold, into a Powsybl Branch.Side
     */
    @JsonIgnore
    public Branch.Side getBranchSide() {
        // TODO: manage matching between LEFT/RIGHT and ONE/TWO
        if (side.equals(Side.LEFT)) {
            return Branch.Side.ONE;
        } else if (side.equals(Side.RIGHT)) {
            return Branch.Side.TWO;
        } else {
            throw new FaraoException("Side is not defined");
        }
    }

    /**
     * Convert a flow value from MW to A, or vice versa. To do so, the voltage
     * level of the Threshold is required. If the voltage level has not been
     * synchronised, this method returns an error.
     *
     * The conversion formula is the following one :
     * Flow(MW) = Flow(A) * sqrt(3) * Unom (kV) / 1000
     */
    private double convert(double value, Unit originUnit, Unit requestedUnit) throws SynchronizationException {
        requestedUnit.checkPhysicalParameter(PhysicalParameter.FLOW);
        if (originUnit.equals(requestedUnit)) {
            return value;
        }
        if (Double.isNaN(voltageLevel)) {
            throw new SynchronizationException("FlowThreshold unit conversion : voltage level must be synchronised with a Network");
        }
        double ratio = voltageLevel * Math.sqrt(3) / 1000;
        if (originUnit.equals(Unit.AMPERE) && requestedUnit.equals(Unit.MEGAWATT)) {
            return value * ratio;
        } else if (originUnit.equals(Unit.MEGAWATT) && requestedUnit.equals(Unit.AMPERE)) {
            return value / ratio;
        } else {
            throw new FaraoException(String.format("Conversion from %s to %s not handled", originUnit.toString(), requestedUnit.toString()));
        }
    }

    /**
     * Get the maximum admissible flow, regardless of the direction in which
     * the branch is monitored.
     */
    protected abstract double getAbsoluteMax() throws SynchronizationException;

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
