package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUseAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

public class FreeToUseImplAdder<T extends AbstractRemedialActionAdder<T>> implements FreeToUseAdder<T> {

    private T owner;
    private Instant instant;
    private UsageMethod usageMethod;

    FreeToUseImplAdder(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public FreeToUseAdder<T> withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public FreeToUseAdder<T> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instant, "FreeToUse", "instant", "withInstant()");
        assertAttributeNotNull(usageMethod, "FreeToUse", "usage method", "withUsageMethod()");

        // TODO: Big flaw here, if we add a contingency after the remedial action, it won't be available after it
        if (instant == Instant.PREVENTIVE) {
            owner.getCrac().addPreventiveState();
        } else {
            owner.getCrac().getContingencies().forEach(co -> owner.getCrac().addState(co, instant));
        }

        FreeToUse freeToUse = new FreeToUseImpl(usageMethod, instant);
        owner.addUsageRule(freeToUse);
        return owner;
    }
}
