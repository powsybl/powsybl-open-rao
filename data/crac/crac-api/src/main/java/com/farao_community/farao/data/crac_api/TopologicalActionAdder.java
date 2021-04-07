package com.farao_community.farao.data.crac_api;

public interface TopologicalActionAdder {

    TopologicalActionAdder withNetworkElement(String networkElementId);

    TopologicalActionAdder withActionType(ActionType actionType);

    TopologicalActionAdder withRangeDefinition(RangeDefinition rangeDefinition);

    NetworkActionAdder add();
}
