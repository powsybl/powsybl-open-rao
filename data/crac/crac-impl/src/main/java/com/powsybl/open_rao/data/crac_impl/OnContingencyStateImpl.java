/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.data.crac_api.Contingency;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnContingencyState;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageMethod;

/**
 * The UsageMethod of the OnContingencyStateImpl UsageRule is only effective in a given State.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class OnContingencyStateImpl extends AbstractUsageRule implements OnContingencyState {

    private State state;

    OnContingencyStateImpl(UsageMethod usageMethod, State state) {
        super(usageMethod);
        this.state = state;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        return this.state.equals(state) ? usageMethod : UsageMethod.UNDEFINED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OnContingencyStateImpl rule = (OnContingencyStateImpl) o;
        return super.equals(o) && rule.getState().equals(state);
    }

    @Override
    public int hashCode() {
        return usageMethod.hashCode() * 19 + state.hashCode() * 47;
    }

    public State getState() {
        return state;
    }

    @Override
    public Contingency getContingency() {
        return state.getContingency().orElse(null);
    }

    @Override
    public Instant getInstant() {
        return state.getInstant();
    }
}
