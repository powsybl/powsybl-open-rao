/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.TopologicalAction;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Topological remedial action: open or close a network element.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("topological-action")
public final class TopologicalActionImpl implements TopologicalAction {

    private NetworkElement networkElement;
    private ActionType actionType;

    public TopologicalActionImpl(NetworkElement networkElement, ActionType actionType) {
        this.networkElement = networkElement;
        this.actionType = actionType;
    }

    public ActionType getActionType() {
        return actionType;
    }

    @Override
    public void apply(Network network) {
        Identifiable element = network.getIdentifiable(networkElement.getId());
        if (element instanceof Branch) {
            Branch branch = (Branch) element;
            if (actionType == ActionType.OPEN) {
                branch.getTerminal1().disconnect();
                branch.getTerminal2().disconnect();
            } else {
                branch.getTerminal1().connect();
                branch.getTerminal2().connect();
            }
        } else if (element instanceof Switch) {
            Switch aSwitch = (Switch) element;
            aSwitch.setOpen(actionType == ActionType.OPEN);
        } else {
            throw new NotImplementedException("Topological actions are only on branches or switches for now");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TopologicalActionImpl oTopologicalAction =  (TopologicalActionImpl) o;
        return oTopologicalAction.getNetworkElement().equals(this.networkElement) && oTopologicalAction.getActionType().equals(this.actionType);
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public int hashCode() {
        return networkElement.hashCode() + 37 * actionType.hashCode();
    }
}
