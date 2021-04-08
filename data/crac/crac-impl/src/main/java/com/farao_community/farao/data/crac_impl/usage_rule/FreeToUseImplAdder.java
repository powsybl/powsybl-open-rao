package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUseAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;


import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

public class FreeToUseImplAdder implements FreeToUseAdder {

    private RemedialAction owner;
    private Instant instant;
    private UsageMethod usageMethod;

    FreeToUseImplAdder(RemedialAction owner) {
        this.owner = owner;
    }

    @Override
    public FreeToUseAdder withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public FreeToUseAdder withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public FreeToUse add() {
        assertAttributeNotNull(instant, "FreeToUse", "instant", "withInstant()");
        assertAttributeNotNull(usageMethod, "FreeToUse", "usage method", "withUsageMethod()");

        FreeToUse freeToUse = new FreeToUseImpl(usageMethod, instant);
        owner.addUsageRule(freeToUse);
        return freeToUse;
    }
}
