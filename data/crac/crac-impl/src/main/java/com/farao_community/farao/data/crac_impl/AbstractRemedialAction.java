/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PstSetpoint.class, name = "pst-setpoint"),
        @JsonSubTypes.Type(value = HvdcSetpoint.class, name = "hvdc-setpoint"),
        @JsonSubTypes.Type(value = InjectionSetpoint.class, name = "injection-setpoint"),
        @JsonSubTypes.Type(value = Topology.class, name = "topology"),
        @JsonSubTypes.Type(value = ComplexNetworkAction.class, name = "complex-network-action"),
        @JsonSubTypes.Type(value = PstWithRange.class, name = "pst-with-range"),
        @JsonSubTypes.Type(value = HvdcRange.class, name = "hvdc-range"),
        @JsonSubTypes.Type(value = InjectionRange.class, name = "injection-range"),
        @JsonSubTypes.Type(value = Redispatching.class, name = "redispatching"),
        @JsonSubTypes.Type(value = Countertrading.class, name = "countertrading"),
        @JsonSubTypes.Type(value = AlignedRangeAction.class, name = "aligned-range-action")
    })
public abstract class AbstractRemedialAction extends AbstractIdentifiable implements RemedialAction {
    protected String operator;
    protected List<UsageRule> usageRules;

    @JsonCreator
    public AbstractRemedialAction(@JsonProperty("id") String id,
                                  @JsonProperty("name") String name,
                                  @JsonProperty("operator") String operator,
                                  @JsonProperty("usageRules") List<UsageRule> usageRules) {
        super(id, name);
        this.operator = operator;
        this.usageRules = new ArrayList<>(usageRules);
    }

    public AbstractRemedialAction(String id, String name, String operator) {
        super(id, name);
        this.operator = operator;
        this.usageRules = new ArrayList<>();
    }

    public AbstractRemedialAction(String id, String operator) {
        super(id);
        this.operator = operator;
        this.usageRules = new ArrayList<>();
    }

    public AbstractRemedialAction(String id) {
        super(id);
        this.operator = "";
        usageRules = new ArrayList<>();
    }

    @Override
    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public final List<UsageRule> getUsageRules() {
        return usageRules;
    }

    @Override
    public void addUsageRule(UsageRule usageRule) {
        usageRules.add(usageRule);
    }

    @Override
    public UsageMethod getUsageMethod(Network network, State state) {
        // TODO: implement method
        return UsageMethod.AVAILABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRemedialAction remedialAction = (AbstractRemedialAction) o;
        return super.equals(remedialAction) && new HashSet<>(usageRules).equals(new HashSet<>(remedialAction.getUsageRules())) && operator.equals(remedialAction.operator);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
