/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * Generic threshold (flow, voltage, etc.) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AbsoluteFlowThreshold.class, name = "absolute-flow-threshold"),
        @JsonSubTypes.Type(value = RelativeFlowThreshold.class, name = "relative-flow-threshold"),
        @JsonSubTypes.Type(value = VoltageThreshold.class, name = "voltage-threshold")
    })
public abstract class AbstractThreshold implements Synchronizable {
    protected Unit unit;
    protected NetworkElement networkElement;

    public AbstractThreshold(Unit unit, NetworkElement networkElement) {
        this.unit = unit;
        this.networkElement = networkElement;
    }

    public AbstractThreshold(Unit unit) {
        this(unit, null);
    }

    @JsonIgnore
    public NetworkElement getNetworkElement() {
        if (networkElement == null) {
            throw new FaraoException("Network element on threshold has not been defined");
        }
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public Unit getUnit() {
        return unit;
    }

    /**
     * A Threshold consists in monitoring a given physical value (FLOW, VOLTAGE
     * or ANGLE). This physical value can be retrieved by the getPhysicalParameter()
     * method.
     */
    @JsonIgnore
    public abstract PhysicalParameter getPhysicalParameter();

    /**
     * If it is defined, this function returns the maximum limit of the Threshold,
     * below which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded above.
     * The returned value is given with the Unit given in argument of the function.
     */
    @JsonIgnore
    public abstract Optional<Double> getMinThreshold(Unit unit);

    /**
     * If it is defined, this function returns the maximum limit of the Threshold,
     * below which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded above.
     * The returned value is given with the unit given in argument of the function.
     */
    @JsonIgnore
    public abstract Optional<Double> getMaxThreshold(Unit unit);

    @Override
    public void synchronize(Network network) {
    }

    @Override
    public void desynchronize() {
    }

    @Override
    public boolean isSynchronized() {
        return true;
    }

    public abstract AbstractThreshold copy();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract void setMargin(double margin, Unit unit);
}
