package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AdderUtils;

import java.util.Objects;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

public class TopologicalActionImplAdder implements TopologicalActionAdder {

    private NetworkActionImplAdder ownerAdder;
    private String networkElementId;
    private String networkElementName;
    private ActionType actionType;

    TopologicalActionImplAdder(NetworkActionImplAdder ownerAdder) {
        this.ownerAdder = ownerAdder;
    }

    @Override
    public TopologicalActionAdder withNetworkElement(String networkElementId) {
        this.networkElementId = networkElementId;
        return this;
    }

    @Override
    public TopologicalActionAdder withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return this;
    }

    @Override
    public TopologicalActionAdder withActionType(ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    @Override
    public NetworkActionAdder add() {

        assertAttributeNotNull(networkElementId, "TopologicalAction", "network element", "withNetworkElement()");
        assertAttributeNotNull(actionType, "TopologicalAction", "action type", "withActionType()");

        NetworkElement networkElement;
        if (Objects.isNull(networkElementName)) {
            networkElement = this.ownerAdder.getOwner().addNetworkElement(networkElementId);
        } else {
            networkElement = this.ownerAdder.getOwner().addNetworkElement(networkElementId, networkElementName);
        }

        TopologicalAction topologicalAction = new TopologicalActionImpl(networkElement, actionType);
        ownerAdder.addElementaryAction(topologicalAction);
        return ownerAdder;
    }
}
