package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;

public interface FreeToUseAdder {

    FreeToUseAdder withInstant(Instant instant);

    FreeToUseAdder withUsageMethod(UsageMethod usageMethod);

    FreeToUse add();
}
