/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class OnConstraintImpl<T extends Cnec<?>> extends AbstractUsageRule implements OnConstraint<T> {
    protected Instant instant;
    protected T cnec;

    protected OnConstraintImpl(UsageMethod usageMethod, Instant instant, T cnec) {
        super(usageMethod);
        this.instant = instant;
        this.cnec = cnec;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        if (state.isPreventive()) {
            return state.getInstant().equals(instant) ? usageMethod : UsageMethod.UNDEFINED;
        } else {
            return state.getInstant().equals(instant) && state.equals(cnec.getState()) ? usageMethod : UsageMethod.UNDEFINED;
        }
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    public T getCnec() {
        return cnec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OnConstraintImpl<?> rule = (OnConstraintImpl<?>) o;
        return super.equals(o) && rule.getInstant().equals(instant) && rule.getCnec().equals(cnec);
    }

    @Override
    public int hashCode() {
        return cnec.hashCode() * 19 + instant.hashCode() * 47;
    }
}
