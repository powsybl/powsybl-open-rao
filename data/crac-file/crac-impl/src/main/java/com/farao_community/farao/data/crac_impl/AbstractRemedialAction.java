/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.ComplexRangeAction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.List;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ComplexNetworkAction.class, name = "complexNetworkAction"),
        @JsonSubTypes.Type(value = ComplexRangeAction.class, name = "complexRangeAction")
    })
public abstract class AbstractRemedialAction extends AbstractIdentifiable implements RemedialAction {
    protected String operator;
    protected List<UsageRule> usageRules;

    @JsonCreator
    public AbstractRemedialAction(@JsonProperty("id") String id,  @JsonProperty("name") String name,
                                  @JsonProperty("operator") String operator,
                                  @JsonProperty("usageRules") List<UsageRule> usageRules) {
        super(id, name);
        this.operator = operator;
        this.usageRules = usageRules;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    public void setUsageRules(List<UsageRule> usageRules) {
        this.usageRules = usageRules;
    }

    @Override
    public List<UsageRule> getUsageRules() {
        return usageRules;
    }

    @Override
    @JsonProperty("usageRules")
    public void addUsageRule(UsageRule usageRule) {
        usageRules.add(usageRule);
    }

    @Override
    public UsageMethod getUsageMethod(Network network, State state) {
        // TODO: implement method
        throw new NotImplementedException("Get usage method is not implemented yet.");
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
        return super.equals(remedialAction) && new HashSet<>(usageRules).equals(new HashSet<>(remedialAction.getUsageRules()));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (UsageRule rule : usageRules) {
            result = 31 * result + rule.hashCode();
        }
        return result;
    }
}
