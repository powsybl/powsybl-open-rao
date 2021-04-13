/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractRemedialAction<I extends RemedialAction<I>> extends AbstractIdentifiable<I> implements RemedialAction<I> {
    protected String operator;
    protected List<UsageRule> usageRules;

    public AbstractRemedialAction(String id, String name, String operator, List<UsageRule> usageRules) {
        super(id, name);
        this.operator = operator;
        this.usageRules = usageRules;
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

    @Deprecated
    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public final List<UsageRule> getUsageRules() {
        return usageRules;
    }

    @Override
    @Deprecated
    // TODO : convert to private package
    public void addUsageRule(UsageRule usageRule) {
        usageRules.add(usageRule);
    }

    @Override
    // TODO : remove network
    public UsageMethod getUsageMethod(State state) {
        List<UsageMethod> usageMethods = usageRules.stream()
            .map(usageRule -> usageRule.getUsageMethod(state))
            .collect(Collectors.toList());

        if (usageMethods.contains(UsageMethod.UNAVAILABLE)) {
            return UsageMethod.UNAVAILABLE;
        } else if (usageMethods.contains(UsageMethod.FORCED)) {
            return UsageMethod.FORCED;
        } else if (usageMethods.contains(UsageMethod.AVAILABLE)) {
            return UsageMethod.AVAILABLE;
        } else {
            return UsageMethod.UNAVAILABLE;
        }
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
