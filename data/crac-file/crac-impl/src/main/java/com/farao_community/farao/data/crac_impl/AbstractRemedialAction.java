/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.remedial_action.usage_rule.AbstractUsageRule;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public abstract class AbstractRemedialAction extends AbstractIdentifiable {
    protected List<AbstractUsageRule> usageRules;

    public AbstractRemedialAction(String id, String name, List<AbstractUsageRule> usageRules) {
        super(id, name);
        this.usageRules = usageRules;
    }

    public void setUsageRules(List<AbstractUsageRule> usageRules) {
        this.usageRules = usageRules;
    }

    public List<AbstractUsageRule> getUsageRules() {
        return usageRules;
    }

    public void addUsageRule(AbstractUsageRule abstractUsageRule) {
        usageRules.add(abstractUsageRule);
    }

    public UsageMethod getUsageMethod(Network network) {
        return null;
    }
}
