/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnAngleConstraintImpl extends AbstractUsageRule implements OnAngleConstraint {
    private Instant instant;
    private AngleCnec angleCnec;

    OnAngleConstraintImpl(Instant instant, AngleCnec angleCnec) {
        super(UsageMethod.TO_BE_EVALUATED);
        this.instant = instant;
        this.angleCnec = angleCnec;
    }

    @Override
    public AngleCnec getAngleCnec() {
        return angleCnec;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        if (state.isPreventive()) {
            return state.getInstant().equals(instant) ? UsageMethod.TO_BE_EVALUATED : UsageMethod.UNDEFINED;
        } else {
            return state.getInstant().equals(instant) && state.equals(this.angleCnec.getState()) ? UsageMethod.TO_BE_EVALUATED : UsageMethod.UNDEFINED;
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
        OnAngleConstraintImpl rule = (OnAngleConstraintImpl) o;
        return super.equals(o) && rule.getInstant().equals(instant) && rule.getAngleCnec().equals(angleCnec);
    }

    @Override
    public int hashCode() {
        return angleCnec.hashCode() * 19 + instant.hashCode() * 47;
    }
}
