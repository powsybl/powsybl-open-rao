package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;

public interface OnStateAdder<T extends RemedialActionAdder<T>> {

    OnStateAdder<T> withContingency(String contingencyId);

    OnStateAdder<T> withInstant(Instant instant);

    OnStateAdder<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
