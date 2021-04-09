/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnStateSerializer;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The UsageMethod of the OnStateImpl UsageRule is only effective in a given State.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("on-state")
@JsonSerialize(using = OnStateSerializer.class)
public final class OnStateImpl extends AbstractUsageRule implements OnState {

    private State state;

    @Deprecated
    // TODO : convert to private package
    public OnStateImpl(UsageMethod usageMethod, State state) {
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
        OnStateImpl rule = (OnStateImpl) o;
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
