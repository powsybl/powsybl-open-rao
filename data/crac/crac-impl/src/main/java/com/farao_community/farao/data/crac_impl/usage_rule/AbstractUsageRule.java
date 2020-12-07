/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Business object of a usage rule in the CRAC file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FreeToUseImpl.class, name = "free-to-use"),
        @JsonSubTypes.Type(value = OnStateImpl.class, name = "on-state")
    })
public abstract class AbstractUsageRule implements UsageRule {

    protected UsageMethod usageMethod;

    protected AbstractUsageRule(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
    }

    @Override
    public UsageMethod getUsageMethod() {
        return usageMethod;
    }

    public void setUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractUsageRule rule = (AbstractUsageRule) o;
        return usageMethod.equals(rule.getUsageMethod());
    }

    @Override
    public int hashCode()  {
        return usageMethod.hashCode() * 23;
    }
}
