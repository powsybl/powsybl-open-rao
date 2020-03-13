/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnContingencySerializer;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The remedial action is available only after a specific contingency, with a given method
 * and in a given state.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("on-contingency")
@JsonSerialize(using = OnContingencySerializer.class)
public final class OnContingency extends AbstractUsageRule {

    private Contingency contingency;

    public OnContingency(UsageMethod usageMethod, State state, Contingency contingency) {
        super(usageMethod, state);
        this.contingency = contingency;
    }

    public Contingency getContingency() {
        return contingency;
    }

    public void setContingency(Contingency contingency) {
        this.contingency = contingency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OnContingency rule = (OnContingency) o;
        return super.equals(o) && rule.contingency.equals(contingency);
    }

    @Override
    public int hashCode() {
        return usageMethod.hashCode() * 29 + state.hashCode() * 59 + contingency.hashCode();
    }
}
