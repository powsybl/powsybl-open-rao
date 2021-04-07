package com.farao_community.farao.data.crac_api;

public interface PstSetpointAdder {

    PstSetpointAdder withNetworkElement(String networkElementId);

    PstSetpointAdder withSetpoint(double setPoint);

    PstSetpointAdder withRangeDefinition(RangeDefinition rangeDefinition);

    NetworkActionAdder add();
}
