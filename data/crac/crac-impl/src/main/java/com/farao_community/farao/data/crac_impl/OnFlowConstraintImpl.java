/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintImpl extends AbstractUsageRule implements OnFlowConstraint {
    private final Instant instant;
    private final FlowCnec flowCnec;

    OnFlowConstraintImpl(UsageMethod usageMethod, Instant instant, FlowCnec flowCnec) {
        super(usageMethod);
        this.instant = instant;
        this.flowCnec = flowCnec;
    }

    @Override
    public FlowCnec getFlowCnec() {
        return flowCnec;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        if (state.isPreventive()) {
            return state.getInstant().equals(instant) ? usageMethod : UsageMethod.UNDEFINED;
        } else {
            return state.getInstant().equals(instant) && state.equals(this.flowCnec.getState()) ? usageMethod : UsageMethod.UNDEFINED;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OnFlowConstraintImpl rule = (OnFlowConstraintImpl) o;
        return super.equals(o) && rule.getInstant().equals(instant) && rule.getFlowCnec().equals(flowCnec);
    }

    @Override
    public int hashCode() {
        return flowCnec.hashCode() * 19 + instant.hashCode() * 47;
    }
}
