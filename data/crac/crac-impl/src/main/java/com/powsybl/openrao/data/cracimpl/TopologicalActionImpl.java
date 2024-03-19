/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.SwitchActionBuilder;
import com.powsybl.action.TerminalsConnectionActionBuilder;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import org.apache.commons.lang3.NotImplementedException;

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
        if (element instanceof Branch<?>) {
            new TerminalsConnectionActionBuilder()
                    .withId("id")
                    .withNetworkElementId(networkElement.getId())
                    .withOpen(actionType == ActionType.OPEN)
                .build()
                .toModification()
                .apply(network);
        } else if (element instanceof Switch) {
            new SwitchActionBuilder()
                    .withId("id")
                    .withNetworkElementId(networkElement.getId())
                    .withOpen(actionType == ActionType.OPEN)
                .build()
                .toModification()
                .apply(network);
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
        TopologicalActionImpl oTopologicalAction = (TopologicalActionImpl) o;
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
