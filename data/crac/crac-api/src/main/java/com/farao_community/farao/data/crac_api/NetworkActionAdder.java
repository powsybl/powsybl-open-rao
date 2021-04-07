package com.farao_community.farao.data.crac_api;

public interface NetworkActionAdder extends IdentifiableAdder<NetworkActionAdder> {

    TopologicalActionAdder newTopologicalAction();

    PstSetPointAdder newPstSetPoint();

    InjectionSetPointAdder newInjectionSetPoint();
}
