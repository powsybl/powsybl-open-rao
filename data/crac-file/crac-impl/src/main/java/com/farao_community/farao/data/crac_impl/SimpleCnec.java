/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

/**
 * Critical network element and contingency.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class SimpleCnec extends AbstractIdentifiable implements Cnec {
    private NetworkElement criticalNetworkElement;
    private Threshold threshold;
    private State state;

    @JsonCreator
    public SimpleCnec(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("criticalNetworkElement") NetworkElement criticalNetworkElement,
                      @JsonProperty("threshold") Threshold threshold, @JsonProperty("state") State state) {
        super(id, name);
        this.criticalNetworkElement = criticalNetworkElement;
        this.threshold = threshold;
        this.state = state;
    }

    public SimpleCnec(String id, NetworkElement criticalNetworkElement, AbstractThreshold threshold, State state) {
        this(id, id, criticalNetworkElement, threshold, state);
    }

    @Override
    public NetworkElement getCriticalNetworkElement() {
        return criticalNetworkElement;
    }

    public void setCriticalNetworkElement(NetworkElement criticalNetworkElement) {
        this.criticalNetworkElement = criticalNetworkElement;
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
    public boolean isMinThresholdViolated(Network network) throws SynchronizationException {
        return threshold.isMinThresholdOvercome(network, this);
    }

    @Override
    public boolean isMaxThresholdViolated(Network network) throws SynchronizationException {
        return threshold.isMaxThresholdOvercome(network, this);
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
        return super.equals(cnec) && criticalNetworkElement.equals(cnec.getCriticalNetworkElement())
            && state.equals(cnec.getState()) && threshold.equals(cnec.getThreshold());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + criticalNetworkElement.hashCode();
        result = 31 * result + state.hashCode();
        result = 31 * result + threshold.hashCode();
        return result;
    }
}
