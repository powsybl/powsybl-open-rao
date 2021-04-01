/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ElementaryAction;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Group of simple elementary remedial actions (setpoint, open/close, ...).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("network-action-impl")
public class NetworkActionImpl extends AbstractRemedialAction<NetworkAction> implements NetworkAction {

    private Set<ElementaryAction> elementaryActions;

    public NetworkActionImpl(String id, String name, String operator, List<UsageRule> usageRules,
                             Set<ElementaryAction> elementaryNetworkActions) {
        super(id, name, operator, usageRules);
        this.elementaryActions = new HashSet<>(elementaryNetworkActions);
    }

    public NetworkActionImpl(String id, String name, String operator, Set<ElementaryAction> elementaryNetworkActions) {
        super(id, name, operator);
        this.elementaryActions = new HashSet<>(elementaryNetworkActions);
    }

    public NetworkActionImpl(String id, String name, String operator) {
        super(id, name, operator);
        this.elementaryActions = new HashSet<>();
    }

    public NetworkActionImpl(String id, String operator) {
        super(id, operator);
        this.elementaryActions = new HashSet<>();
    }

    public Set<ElementaryAction> getElementaryActions() {
        return elementaryActions;
    }

    @Override
    public void apply(Network network) {
        elementaryActions.forEach(action -> action.apply(network));
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        Set<NetworkElement> networkElements = new HashSet<>();
        elementaryActions.forEach(action -> networkElements.add(action.getNetworkElement()));
        return networkElements;
    }

    public void addNetworkAction(ElementaryAction elementaryAction) {
        this.elementaryActions.add(elementaryAction);
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
