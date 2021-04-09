package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;

import java.util.HashSet;
import java.util.Set;

public class NetworkActionImplAdder extends AbstractRemedialActionAdder<NetworkActionAdder> implements NetworkActionAdder {

    private Set<ElementaryAction> elementaryActions;

    NetworkActionImplAdder(SimpleCrac owner) {
        super(owner);
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
        getCrac().addNetworkAction(networkAction);
        return networkAction;
    }

    void addElementaryAction(ElementaryAction elementaryAction) {
        this.elementaryActions.add(elementaryAction);
    }
}
