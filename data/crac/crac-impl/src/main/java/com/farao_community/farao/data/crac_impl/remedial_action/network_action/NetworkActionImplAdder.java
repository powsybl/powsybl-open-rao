package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialActionAdder;

import java.util.HashSet;
import java.util.Set;

public class NetworkActionImplAdder extends AbstractRemedialActionAdder<NetworkActionAdder> implements NetworkActionAdder {

    private SimpleCrac owner;
    private Set<ElementaryAction> elementaryActions;

    //todo move files and make it private package
    public NetworkActionImplAdder(SimpleCrac owner) {
        this.owner = owner;
        this.elementaryActions = new HashSet<>();
    }

    @Override
    protected String getTypeDescription() {
        return "NetworkAction";
    }

    @Override
    public TopologicalActionAdder newTopologicalAction() {
        return new TopologicalActionImplAdder(this);
    }

    @Override
    public PstSetpointAdder newPstSetPoint() {
        return new PstSetpointImplAdder(this);
    }

    @Override
    public InjectionSetpointAdder newInjectionSetPoint() {
        return new InjectionSetpointImplAdder(this);
    }

    @Override
    public NetworkAction add() {
        checkId();
        NetworkAction networkAction = new NetworkActionImpl(id, name, operator, usageRules, elementaryActions);
        owner.addNetworkAction(networkAction);
        return networkAction;
    }

    SimpleCrac getOwner() {
        return owner;
    }

    void addElementaryAction(ElementaryAction elementaryAction) {
        this.elementaryActions.add(elementaryAction);
    }
}
