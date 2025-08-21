/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;

/**
 * The UsageMethod of the OnInstantImpl UsageRule is effective in all the States which
 * are at a given Instant.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class OnInstantImpl extends AbstractUsageRule implements OnInstant {

    private Instant instant;

    OnInstantImpl(UsageMethod usageMethod, Instant instant) {
        super(usageMethod);
        this.instant = instant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OnInstantImpl rule = (OnInstantImpl) o;
        return super.equals(o) && rule.getInstant().equals(instant);
    }

    @Override
    public int hashCode() {
        return usageMethod.hashCode() * 19 + instant.hashCode() * 47;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        return state.getInstant().equals(instant) ? usageMethod : UsageMethod.UNDEFINED;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }
}
