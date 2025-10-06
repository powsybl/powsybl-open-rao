/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;

/**
 * The OnContingencyStateImpl UsageRule is only effective in a given State.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class OnContingencyStateImpl implements OnContingencyState {
    private State state;

    OnContingencyStateImpl(State state) {
        this.state = state;
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
        return rule.getState().equals(state);
    }

    @Override
    public int hashCode() {
        return state.hashCode() * 47;
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
