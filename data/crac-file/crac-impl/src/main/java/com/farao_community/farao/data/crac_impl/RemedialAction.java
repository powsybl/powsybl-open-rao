/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import java.util.List;

/**
 * Business object of a remedial action the CRAC file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class RemedialAction extends AbstractIdentifiable {
    private RemedialActionLever remedialActionLever;
    private List<UsageRule> usageRules;

    public RemedialAction(String id, String name, RemedialActionLever remedialActionLever, List<UsageRule> usageRules) {
        super(id, name);
        this.remedialActionLever = remedialActionLever;
        this.usageRules = usageRules;
    }

    public void addUsageRule(UsageRule usageRule) {
        usageRules.add(usageRule);
    }

    @Override
    protected String getTypeDescription() {
        return "Remedial Action";
    }

    public RemedialActionLever getRemedialActionLever() {
        return remedialActionLever;
    }

    public void setRemedialActionLever(RemedialActionLever remedialActionLever) {
        this.remedialActionLever = remedialActionLever;
    }

    public List<UsageRule> getUsageRules() {
        return usageRules;
    }

    public void setUsageRules(List<UsageRule> usageRules) {
        this.usageRules = usageRules;
    }
}
