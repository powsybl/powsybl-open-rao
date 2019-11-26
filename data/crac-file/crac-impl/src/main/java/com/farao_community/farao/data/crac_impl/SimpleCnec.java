/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.AbstractIdentifiable;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
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
    private AbstractThreshold threshold;
    private State state;

    public SimpleCnec(String id, String name, NetworkElement criticalNetworkElement, AbstractThreshold threshold, State state) {
        super(id, name);
        this.criticalNetworkElement = criticalNetworkElement;
        this.threshold = threshold;
        this.state = state;
    }

    @Override
    public NetworkElement getCriticalNetworkElement() {
        return criticalNetworkElement;
    }

    public void setCriticalNetworkElement(NetworkElement criticalNetworkElement) {
        this.criticalNetworkElement = criticalNetworkElement;
    }

    public AbstractThreshold getThreshold() {
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

    public boolean isBasecase() {
        return !state.getContingency().isPresent();
    }

    @Override
    public boolean isMinThresholdViolated(Network network) {
        return true;
    }

    @Override
    public boolean isMaxThresholdViolated(Network network) {
        return true;
    }
}
