package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;

public interface OnStateAdder {

    OnStateAdder withContingency(String contingencyId);

    OnStateAdder withInstant(Instant instant);

    OnStateAdder withUsageMethod(UsageMethod usageMethod);

    OnState add();
}
