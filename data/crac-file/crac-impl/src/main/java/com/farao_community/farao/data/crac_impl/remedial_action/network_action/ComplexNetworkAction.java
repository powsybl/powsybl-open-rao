/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ApplicableNetworkAction;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Group of simple elementary remedial actions (setpoint, open/close, ...).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class ComplexNetworkAction extends AbstractRemedialAction implements NetworkAction {

    @JsonProperty("applicableNetworkActions")
    private Set<ApplicableNetworkAction> applicableNetworkActions;

    @JsonCreator
    public ComplexNetworkAction(@JsonProperty("id") String id,
                                @JsonProperty("name") String name,
                                @JsonProperty("operator") String operator,
                                @JsonProperty("usageRules") List<UsageRule> usageRules,
                                @JsonProperty("applicableNetworkActions") Set<ApplicableNetworkAction> applicableNetworkActions) {
        super(id, name, operator, usageRules);
        this.applicableNetworkActions = new HashSet<>(applicableNetworkActions);
    }

    public ComplexNetworkAction(String id, String name, String operator) {
        this (id, name, operator, new ArrayList<>(), new HashSet<>());
    }

    public ComplexNetworkAction(String id, String operator) {
        this (id, id, operator, new ArrayList<>(), new HashSet<>());
    }

    @Override
    public Set<ApplicableNetworkAction> getApplicableNetworkActions() {
        return applicableNetworkActions;
    }

    @Override
    public void apply(Network network) {
        applicableNetworkActions.forEach(applicableNetworkAction -> applicableNetworkAction.apply(network));
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        Set<NetworkElement> networkElements = new HashSet<>();
        applicableNetworkActions.forEach(applicableNetworkAction -> networkElements.addAll(applicableNetworkAction.getNetworkElements()));
        return networkElements;
    }

    @JsonProperty("applicableNetworkActions")
    public void addApplicableNetworkAction(ApplicableNetworkAction networkAction) {
        this.applicableNetworkActions.add(networkAction);
    }
}
