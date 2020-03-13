/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.ComplexNetworkActionSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Group of simple elementary remedial actions (setpoint, open/close, ...).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("complex-network-action")
@JsonSerialize(using = ComplexNetworkActionSerializer.class)
public class ComplexNetworkAction extends AbstractNetworkAction<ComplexNetworkAction> {
    private Set<AbstractElementaryNetworkAction> elementaryNetworkActions;

    @JsonCreator
    public ComplexNetworkAction(@JsonProperty("id") String id,
                                @JsonProperty("name") String name,
                                @JsonProperty("operator") String operator,
                                @JsonProperty("usageRules") List<UsageRule> usageRules,
                                @JsonProperty("elementaryNetworkActions") Set<AbstractElementaryNetworkAction> elementaryNetworkActions) {
        super(id, name, operator, usageRules);
        this.elementaryNetworkActions = new HashSet<>(elementaryNetworkActions);
    }

    public ComplexNetworkAction(String id, String name, String operator, Set<AbstractElementaryNetworkAction> elementaryNetworkActions) {
        super(id, name, operator);
        this.elementaryNetworkActions = new HashSet<>(elementaryNetworkActions);
    }

    public ComplexNetworkAction(String id, String name, String operator) {
        super(id, name, operator);
        this.elementaryNetworkActions = new HashSet<>();
    }

    public ComplexNetworkAction(String id, String operator) {
        super(id, operator);
        this.elementaryNetworkActions = new HashSet<>();
    }

    public Set<AbstractElementaryNetworkAction> getElementaryNetworkActions() {
        return elementaryNetworkActions;
    }

    @Override
    public void apply(Network network) {
        elementaryNetworkActions.forEach(networkAction -> networkAction.apply(network));
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        Set<NetworkElement> networkElements = new HashSet<>();
        elementaryNetworkActions.forEach(networkAction -> networkElements.addAll(networkAction.getNetworkElements()));
        return networkElements;
    }

    public void addNetworkAction(AbstractElementaryNetworkAction networkAction) {
        this.elementaryNetworkActions.add(networkAction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComplexNetworkAction otherComplexNetworkAction = (ComplexNetworkAction) o;
        return super.equals(otherComplexNetworkAction)
            && new HashSet<>(elementaryNetworkActions).equals(new HashSet<>(otherComplexNetworkAction.elementaryNetworkActions));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return result;
    }

}
