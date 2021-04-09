package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.data.crac_api.usage_rule.FreeToUseAdder;

public interface RemedialActionAdder<T extends RemedialActionAdder<T>> extends IdentifiableAdder<T> {

    T withOperator(String operator);

    FreeToUseAdder<T> newFreeToUseUsageRule();

}
