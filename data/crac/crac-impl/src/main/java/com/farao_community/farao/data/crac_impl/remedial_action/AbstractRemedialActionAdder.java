package com.farao_community.farao.data.crac_impl.remedial_action;

import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractIdentifiableAdder;

import java.util.List;

public abstract class AbstractRemedialActionAdder<T extends RemedialActionAdder<T>>  extends AbstractIdentifiableAdder<T> implements RemedialActionAdder<T> {

    protected String operator;
    protected List<UsageRule> usageRules;

    @Override
    public T withOperator(String operator) {
        this.operator = operator;
        return (T) this;
    }

}
