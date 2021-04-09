/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.usage_rule.*;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class OnStateAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnStateAdder<T> {

    private T owner;
    private Instant instant;
    private String contingencyId;
    private UsageMethod usageMethod;

    OnStateAdderImpl(AbstractRemedialActionAdder<T> owner) {
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
            throw new FaraoException(String.format("Contingency %s of OnState usage rule does not exist in the crac. Use crac.newContingency() first.", contingencyId));
        }

        OnState onState = new OnStateImpl(usageMethod, owner.getCrac().addState(contingency, instant));
        owner.addUsageRule(onState);
        return owner;
    }
}
