package com.farao_community.farao.data.crac_api;

public interface InjectionSetpointAdder {

    InjectionSetpointAdder withNetworkElement(String networkElementId);

    InjectionSetpointAdder withNetworkElement(String networkElementId, String networkElementName);

    InjectionSetpointAdder withSetpoint(double setPoint);

    NetworkActionAdder add();

}
