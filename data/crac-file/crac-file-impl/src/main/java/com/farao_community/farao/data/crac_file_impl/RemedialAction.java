/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;

import java.util.List;

/**
 * Business object of a remedial action the CRAC file
 *
 * @author Xxx Xxx {@literal <xxx.xxx at rte-france.com>}
 */

public class RemedialAction extends AbstractIdentifiable {
    private List<RemedialActionLever> remedialActionLevers;
    private List<UsageRule> usageRules;

    public RemedialAction(String id, String name, List<RemedialActionLever> remedialActionLevers, List<UsageRule> usageRules) {
        super(id, name);
        this.remedialActionLevers = remedialActionLevers;
        this.usageRules = usageRules;
    }

    public void addRemedialActionLever(RemedialActionLever remedialActionLever) {
        remedialActionLevers.add(remedialActionLever);
    }

    public void addUsageRule(UsageRule usageRule) {
        usageRules.add(usageRule);
    }

    @Override
    protected String getTypeDescription() {
        return "Remedial Action";
    }
}
