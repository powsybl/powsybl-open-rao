package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUseAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRemedialActionAdder<T extends RemedialActionAdder<T>>  extends AbstractIdentifiableAdder<T> implements RemedialActionAdder<T> {

    protected String operator;
    protected List<UsageRule> usageRules = new ArrayList<>();
    private SimpleCrac crac;

    AbstractRemedialActionAdder(SimpleCrac crac) {
        this.crac = crac;
    }

    @Override
    public T withOperator(String operator) {
        this.operator = operator;
        return (T) this;
    }

    @Override
    public FreeToUseAdder<T> newFreeToUseUsageRule() {
        return new FreeToUseImplAdder(this);
    }

    void addUsageRule(UsageRule usageRule) {
        this.usageRules.add(usageRule);
    }

    SimpleCrac getCrac() {
        return this.crac;
    }

}
