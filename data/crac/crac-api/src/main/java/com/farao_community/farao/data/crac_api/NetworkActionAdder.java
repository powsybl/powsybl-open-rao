package com.farao_community.farao.data.crac_api;

public interface NetworkActionAdder extends RemedialActionAdder<NetworkActionAdder> {

    TopologicalActionAdder newTopologicalAction();

    PstSetpointAdder newPstSetPoint();

    InjectionSetpointAdder newInjectionSetPoint();

    NetworkAction add();
}
