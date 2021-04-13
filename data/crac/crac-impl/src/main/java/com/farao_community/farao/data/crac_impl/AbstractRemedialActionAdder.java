/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUseAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnStateAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractRemedialActionAdder<T extends RemedialActionAdder<T>>  extends AbstractIdentifiableAdder<T> implements RemedialActionAdder<T> {

    protected String operator;
    protected List<UsageRule> usageRules = new ArrayList<>();
    private CracImpl crac;

    AbstractRemedialActionAdder(CracImpl crac) {
        Objects.requireNonNull(crac);
        this.crac = crac;
    }

    @Override
    public T withOperator(String operator) {
        this.operator = operator;
        return (T) this;
    }

    @Override
    public FreeToUseAdder<T> newFreeToUseUsageRule() {
        return new FreeToUseAdderImpl(this);
    }

    @Override
    public OnStateAdder<T> newOnStateUsageRule() {
        return new OnStateAdderImpl(this);
    }

    void addUsageRule(UsageRule usageRule) {
        this.usageRules.add(usageRule);
    }

    CracImpl getCrac() {
        return this.crac;
    }

}
