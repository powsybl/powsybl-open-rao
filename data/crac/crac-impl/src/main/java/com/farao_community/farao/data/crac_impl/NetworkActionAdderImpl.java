/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.network_action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkActionAdderImpl extends AbstractRemedialActionAdder<NetworkActionAdder> implements NetworkActionAdder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkActionAdderImpl.class);
    private Set<ElementaryAction> elementaryActions;

    NetworkActionAdderImpl(CracImpl owner) {
        super(owner);
        this.elementaryActions = new HashSet<>();
    }

    @Override
    protected String getTypeDescription() {
        return "NetworkAction";
    }

    @Override
    public TopologicalActionAdder newTopologicalAction() {
        return new TopologicalActionAdderImpl(this);
    }

    @Override
    public PstSetpointAdder newPstSetPoint() {
        return new PstSetpointAdderImpl(this);
    }

    @Override
    public InjectionSetpointAdder newInjectionSetPoint() {
        return new InjectionSetpointAdderImpl(this);
    }

    @Override
    public NetworkAction add() {
        checkId();

        if (usageRules.isEmpty()) {
            LOGGER.warn("NetworkAction {} does not contain any usage rule, by default it will never be available", id);
        }

        NetworkAction networkAction = new NetworkActionImpl(id, name, operator, usageRules, elementaryActions);
        getCrac().addNetworkAction(networkAction);
        return networkAction;
    }

    void addElementaryAction(ElementaryAction elementaryAction) {
        this.elementaryActions.add(elementaryAction);
    }
}
