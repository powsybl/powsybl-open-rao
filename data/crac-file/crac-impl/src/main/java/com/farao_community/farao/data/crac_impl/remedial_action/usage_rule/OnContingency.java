/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.usage_rule;

import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.Contingency;
import com.farao_community.farao.data.crac_impl.State;

/**
 * The remedial action is available only after a specific contingency, with a given method
 * and in a given state.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
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
}
