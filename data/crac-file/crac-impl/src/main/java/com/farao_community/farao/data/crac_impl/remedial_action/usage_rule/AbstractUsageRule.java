/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.usage_rule;

import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.State;

/**
 * Business object of a usage rule in the CRAC file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractUsageRule {

    protected UsageMethod usageMethod;
    protected State state;

    public AbstractUsageRule(UsageMethod usageMethod, State state) {
        this.usageMethod = usageMethod;
        this.state = state;
    }

    public UsageMethod getUsageMethod() {
        return usageMethod;
    }

    public void setUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
