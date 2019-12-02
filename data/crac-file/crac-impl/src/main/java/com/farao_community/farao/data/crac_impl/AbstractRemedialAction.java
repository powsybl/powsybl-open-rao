/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.AbstractIdentifiable;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public abstract class AbstractRemedialAction extends AbstractIdentifiable implements RemedialAction {
    protected List<UsageRule> usageRules;

    @JsonCreator
    public AbstractRemedialAction(@JsonProperty("id") String id,  @JsonProperty("name") String name,
                                  @JsonProperty("usageRules") List<UsageRule> usageRules) {
        super(id, name);
        this.usageRules = usageRules;
    }

    public void setUsageRules(List<UsageRule> usageRules) {
        this.usageRules = usageRules;
    }

    @Override
    public List<UsageRule> getUsageRules() {
        return usageRules;
    }

    @JsonProperty("usageRules")
    public void addUsageRule(UsageRule usageRule) {
        usageRules.add(usageRule);
    }

    @Override
    public UsageMethod getUsageMethod(Network network) {
        return null;
    }
}
