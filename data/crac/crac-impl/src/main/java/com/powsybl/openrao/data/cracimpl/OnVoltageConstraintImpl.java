/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.usagerule.OnVoltageConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public class OnVoltageConstraintImpl extends AbstractUsageRule implements OnVoltageConstraint {
    private final Instant instant;

    private final VoltageCnec voltageCnec;

    OnVoltageConstraintImpl(UsageMethod usageMethod, Instant instant, VoltageCnec angleCnec) {
        super(usageMethod);
        this.instant = instant;
        this.voltageCnec = angleCnec;
    }

    @Override
    public VoltageCnec getVoltageCnec() {
        return voltageCnec;
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
            return state.getInstant().equals(instant) && state.equals(this.voltageCnec.getState()) ? usageMethod : UsageMethod.UNDEFINED;
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
        OnVoltageConstraintImpl rule = (OnVoltageConstraintImpl) o;
        return super.equals(o) && rule.getInstant().equals(instant) && rule.getVoltageCnec().equals(voltageCnec);
    }

    @Override
    public int hashCode() {
        return voltageCnec.hashCode() * 19 + instant.hashCode() * 47;
    }
}
