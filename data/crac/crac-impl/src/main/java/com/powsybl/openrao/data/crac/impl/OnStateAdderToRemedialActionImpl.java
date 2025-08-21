/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyStateAdderToRemedialAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * Adds an OnContingencyState usage rule to a RemedialActionAdder
 * Needs the Contingency object and does not need the CRAC
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnStateAdderToRemedialActionImpl<T extends AbstractRemedialAction<T>> implements OnContingencyStateAdderToRemedialAction<T> {

    private T owner;
    private UsageMethod usageMethod;
    private State state;
    private static final String CLASS_NAME = "OnState";

    OnStateAdderToRemedialActionImpl(RemedialAction<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnContingencyStateAdderToRemedialAction<T> withState(State state) {
        this.state = state;
        return this;
    }

    @Override
    public OnStateAdderToRemedialActionImpl<T> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(state, CLASS_NAME, "state", "withState()");
        assertAttributeNotNull(usageMethod, CLASS_NAME, "usage method", "withUsageMethod()");
        if (state.isPreventive() && usageMethod != UsageMethod.FORCED) {
            throw new OpenRaoException("OnContingencyState usage rules are not allowed for PREVENTIVE instant except when FORCED. Please use newOnInstantUsageRule() instead.");
        } else if (state.getInstant().isOutage()) {
            throw new OpenRaoException("OnContingencyState usage rules are not allowed for OUTAGE instant.");
        }
        owner.addUsageRule(new OnContingencyStateImpl(usageMethod, state));
        return owner;
    }
}
