/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.ActionType;

/**
 * Topological remedial actionType
 *
 * @author Xxx Xxx {@literal <xxx.xxx at rte-france.com>}
 */

public class TopologyModification implements RemedialActionLever {

    private NetworkElement networkElement;
    private ActionType actionType;

    public TopologyModification(NetworkElement networkElement, ActionType actionType) {
        this.networkElement = networkElement;
        this.actionType = actionType;
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
}
