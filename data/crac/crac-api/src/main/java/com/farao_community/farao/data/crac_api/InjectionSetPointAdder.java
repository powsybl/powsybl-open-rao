package com.farao_community.farao.data.crac_api;

public interface InjectionSetPointAdder {

    InjectionSetPoint withNetworkElement(String networkElementId);

    InjectionSetPoint withSetPoint(double setPoint);

    NetworkActionAdder add();

}
