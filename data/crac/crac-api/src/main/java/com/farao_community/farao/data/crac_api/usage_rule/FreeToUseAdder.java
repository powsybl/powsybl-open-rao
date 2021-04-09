package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;

public interface FreeToUseAdder<T extends RemedialActionAdder<T>> {

    FreeToUseAdder<T> withInstant(Instant instant);

    FreeToUseAdder<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
