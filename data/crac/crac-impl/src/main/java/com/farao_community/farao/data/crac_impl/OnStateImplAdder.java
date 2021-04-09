package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.usage_rule.*;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

public class OnStateImplAdder<T extends AbstractRemedialActionAdder<T>> implements OnStateAdder<T> {

    private T owner;
    private Instant instant;
    private String contingencyId;
    private UsageMethod usageMethod;

    OnStateImplAdder(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnStateAdder<T> withContingency(String contingencyId) {
        this.contingencyId = contingencyId;
        return this;
    }

    @Override
    public OnStateAdder<T> withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public OnStateAdder<T> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(contingencyId, "OnState", "contingency", "withContingency()");
        assertAttributeNotNull(instant, "OnState", "instant", "withInstant()");
        assertAttributeNotNull(usageMethod, "OnState", "usage method", "withUsageMethod()");
        if (instant.equals(Instant.PREVENTIVE)) {
            throw new FaraoException("OnState usage rules are not allowed for PREVENTIVE instant. Please use newFreeToUseUsageRule() instead.");
        } else if (instant.equals(Instant.OUTAGE)) {
            throw new FaraoException("OnState usage rules are not allowed for OUTAGE instant.");
        }
        Contingency contingency = owner.getCrac().getContingency(contingencyId);
        if (contingency == null) {
            throw new FaraoException(String.format("Contingency %s of OnState usage rule does not exist in the crac. Use newContingency() first.", contingencyId));
        }

        OnState onState = new OnStateImpl(usageMethod, owner.getCrac().addState(contingency, instant));
        owner.addUsageRule(onState);
        return owner;
    }
}
