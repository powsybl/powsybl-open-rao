package com.farao_community.farao.data.crac_api;

public interface PstSetPointAdder {

    PstSetPointAdder withNetworkElement(String networkElementId);

    PstSetPointAdder withSetPoint(double setPoint);

    PstSetPointAdder withRangeDefinition(RangeDefinition rangeDefinition);

    NetworkActionAdder add();
}
