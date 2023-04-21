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
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.usage_rule.*;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * Adds an OnContingencyState usage rule to a RemedialActionAdder
 * Needs the CRAC to look up the contingency and add the new state if needed
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class OnContingencyStateAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnContingencyStateAdder<T> {

    private T owner;
    private Instant instant;
    private String contingencyId;
    private UsageMethod usageMethod;
    private static final String CLASS_NAME = "OnContingencyState";

    OnContingencyStateAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnContingencyStateAdder<T> withContingency(String contingencyId) {
        this.contingencyId = contingencyId;
        return this;
    }

    @Override
    public OnContingencyStateAdder<T> withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public OnContingencyStateAdder<T> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instant, CLASS_NAME, "instant", "withInstant()");
        assertAttributeNotNull(usageMethod, CLASS_NAME, "usage method", "withUsageMethod()");

        State state;
        if (instant.equals(Instant.PREVENTIVE)) {
            if (usageMethod != UsageMethod.FORCED) {
                throw new FaraoException("OnContingencyState usage rules are not allowed for PREVENTIVE instant, except when FORCED. Please use newOnInstantUsageRule() instead.");
            }
            state = owner.getCrac().addPreventiveState();
        } else if (instant.equals(Instant.OUTAGE)) {
            throw new FaraoException("OnContingencyState usage rules are not allowed for OUTAGE instant.");
        } else {
            assertAttributeNotNull(contingencyId, CLASS_NAME, "contingency", "withContingency()");
            Contingency contingency = owner.getCrac().getContingency(contingencyId);
            if (contingency == null) {
                throw new FaraoException(String.format("Contingency %s of OnContingencyState usage rule does not exist in the crac. Use crac.newContingency() first.", contingencyId));
            }
            state = owner.getCrac().addState(contingency, instant);
        }

        owner.addUsageRule(new OnContingencyStateImpl(usageMethod, state));
        return owner;
    }
}
