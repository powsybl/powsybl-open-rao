/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.AbstractIdentifiable;
import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.ElementaryAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

/**
 * Topological remedial action: open or close a network element.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("topology")
public final class Topology extends AbstractIdentifiable implements ElementaryAction {

    private NetworkElement networkElement;
    private ActionType actionType;

    public Topology(String id, String name, NetworkElement networkElement, ActionType actionType) {
        super(id, name);
        this.networkElement = networkElement;
        this.actionType = actionType;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
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
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Topology topology = (Topology) o;
        return super.equals(o) && actionType == topology.getActionType();
    }

    @Override
    public int hashCode() {
        return String.format("%s%s", getId(), getActionType().toString()).hashCode();
    }
}
