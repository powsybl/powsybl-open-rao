package com.farao_community.farao.data.crac_api;

public interface TopologicalActionAdder {

    TopologicalActionAdder withNetworkElement(String networkElementId);

    TopologicalActionAdder withNetworkElement(String networkElementId, String networkElementName);

    TopologicalActionAdder withActionType(ActionType actionType);

    NetworkActionAdder add();
}
