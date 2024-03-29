/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.Set;

/**
 * Topological remedial action: open or close a network element.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class TopologicalActionImpl implements TopologicalAction {

    private NetworkElement networkElement;
    private ActionType actionType;

    TopologicalActionImpl(NetworkElement networkElement, ActionType actionType) {
        this.networkElement = networkElement;
        this.actionType = actionType;
    }

    @Override
    public ActionType getActionType() {
        return actionType;
    }

    @Override
    public void apply(Network network) {
        Identifiable<?> element = network.getIdentifiable(networkElement.getId());
        if (element instanceof Branch<?> branch) {
            if (actionType == ActionType.OPEN) {
                branch.getTerminal1().disconnect();
                branch.getTerminal2().disconnect();
            } else {
                branch.getTerminal1().connect();
                branch.getTerminal2().connect();
            }
        } else if (element instanceof Switch sw) {
            sw.setOpen(actionType == ActionType.OPEN);
        } else {
            throw new NotImplementedException("Topological actions are only on branches or switches for now");
        }
    }

    @Override
    public boolean hasImpactOnNetwork(Network network) {
        Identifiable<?> element = network.getIdentifiable(networkElement.getId());
        if (element instanceof Branch<?> branch) {
            if (actionType == ActionType.OPEN) {
                // Line is considered closed if both terminal are connected
                return branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected();
            } else {
                // Line is already considered opened if one of the terminals is disconnected
                return !branch.getTerminal1().isConnected() || !branch.getTerminal2().isConnected();
            }
        } else if (element instanceof Switch sw) {
            return sw.isOpen() == (actionType == ActionType.CLOSE);
        } else {
            throw new NotImplementedException("Topological actions are only on branches or switches for now");
        }
    }

    @Override
    public boolean canBeApplied(Network network) {
        // TODO : always true ?
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TopologicalActionImpl oTopologicalAction = (TopologicalActionImpl) o;
        return oTopologicalAction.getNetworkElement().equals(this.networkElement) && oTopologicalAction.getActionType().equals(this.actionType);
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public int hashCode() {
        return networkElement.hashCode() + 37 * actionType.hashCode();
    }
}
