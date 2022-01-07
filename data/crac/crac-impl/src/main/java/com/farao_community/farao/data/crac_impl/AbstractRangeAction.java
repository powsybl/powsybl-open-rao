/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;

import java.util.List;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractRangeAction extends AbstractRemedialAction<RangeAction> implements RangeAction {

    protected String groupId = null;

    AbstractRangeAction(String id, String name, String operator, List<UsageRule> usageRules, String groupId) {
        super(id, name, operator, usageRules);
        this.groupId = groupId;
    }

    AbstractRangeAction(String id, String name, String operator, List<UsageRule> usageRules) {
        super(id, name, operator, usageRules);
    }

    @Override
    public Optional<String> getGroupId() {
        return Optional.ofNullable(groupId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRangeAction otherRa = (AbstractRangeAction) o;

        return super.equals(o)
                && (groupId == null && otherRa.getGroupId().isEmpty()) || (groupId != null && groupId.equals(otherRa.getGroupId().orElse(null)));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        String nonNullGroupId = groupId == null ? "" : groupId;
        result = 31 * result + nonNullGroupId.hashCode();
        return result;
    }
}
