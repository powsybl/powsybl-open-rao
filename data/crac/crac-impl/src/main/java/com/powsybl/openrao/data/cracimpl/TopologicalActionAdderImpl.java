/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalActionAdder;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class TopologicalActionAdderImpl implements TopologicalActionAdder {

    private NetworkActionAdderImpl ownerAdder;
    private String networkElementId;
    private String networkElementName;
    private ActionType actionType;

    TopologicalActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
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
        networkElement = this.ownerAdder.getCrac().addNetworkElement(networkElementId, networkElementName);

        TopologicalAction topologicalAction = new TopologicalActionImpl(networkElement, actionType);
        ownerAdder.addElementaryAction(topologicalAction, networkElement);
        return ownerAdder;
    }
}
