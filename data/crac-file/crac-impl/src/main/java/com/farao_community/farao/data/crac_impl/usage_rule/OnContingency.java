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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The remedial action is available only after a specific contingency, with a given method
 * and in a given state.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public final class OnContingency extends AbstractUsageRule {

    private Contingency contingency;

    @JsonCreator
    public OnContingency(@JsonProperty("usageMethod") UsageMethod usageMethod, @JsonProperty("state") State state,
                         @JsonProperty("contingency") Contingency contingency) {
        super(usageMethod, state);
        this.contingency = contingency;
    }

    public Contingency getContingency() {
        return contingency;
    }

    public void setContingency(Contingency contingency) {
        this.contingency = contingency;
    }
}
