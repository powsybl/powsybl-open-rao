/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.annotation.*;
import com.powsybl.iidm.network.Network;

/**
 * Critical network element and contingency.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonIdentityInfo(scope = SimpleCnec.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class SimpleCnec extends AbstractIdentifiable implements Cnec {
    private NetworkElement networkElement;
    private Threshold threshold;
    private State state;

    @JsonCreator
    public SimpleCnec(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("networkElement") NetworkElement networkElement,
                      @JsonProperty("threshold") Threshold threshold, @JsonProperty("state") State state) {
        super(id, name);
        this.networkElement = networkElement;
        this.threshold = threshold;
        this.state = state;
    }

    public SimpleCnec(String id, NetworkElement networkElement, AbstractThreshold threshold, State state) {
        this(id, id, networkElement, threshold, state);
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public double computeMargin(Network network) throws SynchronizationException {
        return threshold.computeMargin(network, this);
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    @Override
    public Threshold getThreshold() {
        return threshold;
    }

    public void setThreshold(AbstractThreshold threshold) {
        this.threshold = threshold;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @JsonIgnore
    public boolean isBasecase() {
        return !state.getContingency().isPresent();
    }

    @Override
    public boolean isThresholdViolated(Network network) throws SynchronizationException {
        return threshold.isMaxThresholdOvercome(network, this) || threshold.isMinThresholdOvercome(network, this);
    }

    @Override
    public void synchronize(Network network) {
        threshold.synchronize(network, this);
    }

    @Override
    public void desynchronize() {
        threshold.desynchronize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleCnec cnec = (SimpleCnec) o;
        return super.equals(cnec) && networkElement.equals(cnec.getNetworkElement())
            && state.equals(cnec.getState()) && threshold.equals(cnec.getThreshold());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + networkElement.hashCode();
        result = 31 * result + state.hashCode();
        result = 31 * result + threshold.hashCode();
        return result;
    }
}
