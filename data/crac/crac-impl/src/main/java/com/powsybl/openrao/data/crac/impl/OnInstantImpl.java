/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;

/**
 * The OnInstantImpl UsageRule is effective in all the States which
 * are at a given Instant.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class OnInstantImpl implements OnInstant {

    private final Instant instant;

    OnInstantImpl(Instant instant) {
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
        return rule.getInstant().equals(instant);
    }

    @Override
    public int hashCode() {
        return instant.hashCode() * 47;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }
}
