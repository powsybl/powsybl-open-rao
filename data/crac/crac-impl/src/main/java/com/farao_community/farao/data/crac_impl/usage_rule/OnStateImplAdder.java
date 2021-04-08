package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.usage_rule.*;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

public class OnStateImplAdder implements OnStateAdder {

    private RemedialAction owner;
    private Instant instant;
    private String contingencyId;
    private UsageMethod usageMethod;

    OnStateImplAdder(RemedialAction owner) {
        this.owner = owner;
    }

    @Override
    public OnStateAdder withContingency(String contingencyId) {
        this.contingencyId = contingencyId;
        return this;
    }

    @Override
    public OnStateAdder withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public OnStateAdder withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public OnState add() {
        assertAttributeNotNull(contingencyId, "OnState", "contingency", "withContingency()");
        assertAttributeNotNull(instant, "OnState", "instant", "withInstant()");
        assertAttributeNotNull(usageMethod, "OnState", "usage method", "withUsageMethod()");

        FreeToUse freeToUse = new OnStateImpl(usageMethod, instant);
        owner.addUsageRule(freeToUse);
        return freeToUse;
    }
}
