package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;

import java.util.Objects;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

public class InjectionSetpointImplAdder implements InjectionSetpointAdder {

    private NetworkActionImplAdder ownerAdder;
    private String networkElementId;
    private String networkElementName;
    private Double setpoint;

    InjectionSetpointImplAdder(NetworkActionImplAdder ownerAdder) {
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

        NetworkElement networkElement;
        if (Objects.isNull(networkElementName)) {
            networkElement = this.ownerAdder.getOwner().addNetworkElement(networkElementId);
        } else {
            networkElement = this.ownerAdder.getOwner().addNetworkElement(networkElementId, networkElementName);
        }

        InjectionSetpoint injectionSetpoint = new InjectionSetpointImpl(networkElement, setpoint);
        ownerAdder.addElementaryAction(injectionSetpoint);
        return ownerAdder;
    }
}
