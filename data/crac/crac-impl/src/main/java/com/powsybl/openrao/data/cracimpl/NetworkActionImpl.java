/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.Set;

/**
 * Group of simple elementary remedial actions (setpoint, open/close, ...).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionImpl extends AbstractRemedialAction<NetworkAction> implements NetworkAction {

    private final Set<ElementaryAction> elementaryActions;

    NetworkActionImpl(String id, String name, String operator, Set<TriggerCondition> triggerConditions,
                      Set<ElementaryAction> elementaryNetworkActions, Integer speed) {
        super(id, name, triggerConditions, operator, speed);
        this.elementaryActions = new HashSet<>(elementaryNetworkActions);
    }

    public Set<ElementaryAction> getElementaryActions() {
        return elementaryActions;
    }

    @Override
    public boolean hasImpactOnNetwork(Network network) {
        return elementaryActions.stream().anyMatch(elementaryAction -> elementaryAction.hasImpactOnNetwork(network));
    }

    @Override
    public boolean apply(Network network) {
        if (elementaryActions.stream().anyMatch(elementaryAction -> !elementaryAction.canBeApplied(network))) {
            return false;
        } else {
            elementaryActions.forEach(action -> action.apply(network));
            return true;
        }
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        Set<NetworkElement> networkElements = new HashSet<>();
        elementaryActions.forEach(action -> networkElements.addAll(action.getNetworkElements()));
        return networkElements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkActionImpl otherNetworkActionImpl = (NetworkActionImpl) o;
        return super.equals(otherNetworkActionImpl)
            && new HashSet<>(elementaryActions).equals(new HashSet<>(otherNetworkActionImpl.elementaryActions));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
