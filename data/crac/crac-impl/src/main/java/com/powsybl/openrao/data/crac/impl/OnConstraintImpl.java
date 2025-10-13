/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class OnConstraintImpl<T extends Cnec<?>> implements OnConstraint<T> {
    protected Instant instant;
    protected T cnec;

    protected OnConstraintImpl(Instant instant, T cnec) {
        this.instant = instant;
        this.cnec = cnec;
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
        return rule.getInstant().equals(instant) && rule.getCnec().equals(cnec);
    }

    @Override
    public int hashCode() {
        return cnec.hashCode() * 19 + instant.hashCode() * 47;
    }
}
