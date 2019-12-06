/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.ApplicableNetworkAction;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * Group of simple elementary remedial actions (setpoint, open/close, ...).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class ComplexNetworkAction extends AbstractRemedialAction implements NetworkAction {

    @JsonProperty("applicableNetworkActions")
    private List<ApplicableNetworkAction> applicableNetworkActions;

    @JsonCreator
    public ComplexNetworkAction(@JsonProperty("id") String id, @JsonProperty("name") String name,
                                @JsonProperty("operator") String operator,
                                @JsonProperty("usageRules") List<UsageRule> usageRules,
                                @JsonProperty("applicableNetworkActions") List<ApplicableNetworkAction> applicableNetworkActions) {
        super(id, name, operator, usageRules);
        this.applicableNetworkActions = applicableNetworkActions;
    }

    public ComplexNetworkAction(String id, String operator, List<UsageRule> usageRules, List<ApplicableNetworkAction> applicableNetworkActions) {
        this (id, id, operator, usageRules, applicableNetworkActions);
    }

    public ComplexNetworkAction(String id, String operator) {
        this (id, id, operator, new ArrayList<>(), new ArrayList<>());
    }

    public void setApplicableNetworkActions(List<ApplicableNetworkAction> applicableNetworkActions) {
        this.applicableNetworkActions = applicableNetworkActions;
    }

    public List<ApplicableNetworkAction> getApplicableNetworkActions() {
        return applicableNetworkActions;
    }

    @Override
    public void apply(Network network) {
        applicableNetworkActions.forEach(applicableNetworkAction -> applicableNetworkAction.apply(network));
    }

    @JsonProperty("applicableNetworkActions")
    public void addApplicableNetworkAction(ApplicableNetworkAction networkAction) {
        this.applicableNetworkActions.add(networkAction);
    }
}
