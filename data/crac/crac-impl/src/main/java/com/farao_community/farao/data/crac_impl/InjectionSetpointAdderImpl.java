/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpointAdder;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;

import java.util.Objects;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionSetpointAdderImpl implements InjectionSetpointAdder {

    private NetworkActionAdderImpl ownerAdder;
    private String networkElementId;
    private String networkElementName;
    private Double setpoint;

    InjectionSetpointAdderImpl(NetworkActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
    }

    @Override
    public InjectionSetpointAdder withNetworkElement(String networkElementId) {
        this.networkElementId = networkElementId;
        return this;
    }

    @Override
    public InjectionSetpointAdder withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return this;
    }

    @Override
    public InjectionSetpointAdder withSetpoint(double setpoint) {
        this.setpoint = setpoint;
        return this;
    }

    @Override
    public NetworkActionAdder add() {
        assertAttributeNotNull(networkElementId, "InjectionSetPoint", "network element", "withNetworkElement()");
        assertAttributeNotNull(setpoint, "InjectionSetPoint", "setpoint", "withSetPoint()");

        NetworkElement networkElement = this.ownerAdder.getCrac().addNetworkElement(networkElementId, networkElementName);
        InjectionSetpoint injectionSetpoint = new InjectionSetpointImpl(networkElement, setpoint);
        ownerAdder.addElementaryAction(injectionSetpoint);
        return ownerAdder;
    }
}
