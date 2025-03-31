/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyStateAdder;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * Adds an OnContingencyState usage rule to a RemedialActionAdder
 * Needs the CRAC to look up the contingency and put the new state if needed
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class OnContingencyStateAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnContingencyStateAdder<T> {

    private T owner;
    private String instantId;
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
    public OnContingencyStateAdder<T> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public OnContingencyStateAdder<T> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, CLASS_NAME, "instant", "withInstant()");
        assertAttributeNotNull(usageMethod, CLASS_NAME, "usage method", "withUsageMethod()");

        State state;
        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isPreventive()) {
            throw new OpenRaoException("OnContingencyState usage rules are not allowed for PREVENTIVE instant. Please use newOnInstantUsageRule() instead.");
        } else if (instant.isOutage()) {
            throw new OpenRaoException("OnContingencyState usage rules are not allowed for OUTAGE instant.");
        } else {
            assertAttributeNotNull(contingencyId, CLASS_NAME, "contingency", "withContingency()");
            Contingency contingency = owner.getCrac().getContingency(contingencyId);
            if (contingency == null) {
                throw new OpenRaoException(String.format("Contingency %s of OnContingencyState usage rule does not exist in the crac. Use crac.newContingency() first.", contingencyId));
            }
            state = owner.getCrac().addState(contingency, instant);
        }

        owner.addUsageRule(new OnContingencyStateImpl(usageMethod, state));
        return owner;
    }
}
