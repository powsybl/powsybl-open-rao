/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnStateSerializer;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The remedial action is available only after a specific contingency, with a given method
 * and in a given state.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("on-state")
@JsonSerialize(using = OnStateSerializer.class)
public final class OnState extends AbstractUsageRule {

    public OnState(UsageMethod usageMethod, State state) {
        super(usageMethod, state);
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        return this.state.equals(state) ? usageMethod : UsageMethod.UNDEFINED;
    }
}
