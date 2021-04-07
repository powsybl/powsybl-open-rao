package com.farao_community.farao.data.crac_api;

public interface InjectionSetpointAdder {

    InjectionSetpoint withNetworkElement(String networkElementId);

    InjectionSetpoint withSetpoint(double setPoint);

    NetworkActionAdder add();

}
