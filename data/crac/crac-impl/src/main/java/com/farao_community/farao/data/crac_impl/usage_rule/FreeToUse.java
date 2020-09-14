/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.FreeToUseSerializer;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The remedial action is free to use with a given method and in a given state.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("free-to-use")
@JsonSerialize(using = FreeToUseSerializer.class)
public final class FreeToUse extends AbstractUsageRule {

    public FreeToUse(UsageMethod usageMethod, State state) {
        super(usageMethod, state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return usageMethod.hashCode() * 19 + state.hashCode() * 47;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        return this.state.equals(state) ? usageMethod : UsageMethod.UNDEFINED;
    }
}
