package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.Action;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

public abstract class AbstractSingleNetworkElementActionAdderImpl<I> {
    protected NetworkActionAdderImpl ownerAdder;
    protected String networkElementId;
    private String networkElementName;

    AbstractSingleNetworkElementActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
    }

    public I withNetworkElement(String networkElementId) {
        this.networkElementId = networkElementId;
        return (I) this;
    }

    public I withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return (I) this;
    }

    public NetworkActionAdder add() {
        assertAttributeNotNull(networkElementId, getActionName(), "network element", "withNetworkElement()");
        assertSpecificAttributes();
        NetworkElement networkElement = this.ownerAdder.getCrac().addNetworkElement(networkElementId, networkElementName);
        ownerAdder.addElementaryAction(buildAction(), networkElement);
        return ownerAdder;
    }

    protected abstract Action buildAction();

    protected abstract void assertSpecificAttributes();

    protected abstract String getActionName();
}
