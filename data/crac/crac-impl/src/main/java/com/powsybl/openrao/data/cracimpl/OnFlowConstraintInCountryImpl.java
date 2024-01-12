/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.iidm.network.Country;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintInCountryImpl extends AbstractUsageRule implements OnFlowConstraintInCountry {
    private final Instant instant;
    private final Country country;

    OnFlowConstraintInCountryImpl(UsageMethod usageMethod, Instant instant, Country country) {
        super(usageMethod);
        this.instant = instant;
        this.country = country;
    }

    @Override
    public Country getCountry() {
        return country;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        return state.getInstant().equals(instant) ? usageMethod : UsageMethod.UNDEFINED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OnFlowConstraintInCountryImpl rule = (OnFlowConstraintInCountryImpl) o;
        return super.equals(o) && rule.getInstant().equals(instant) && rule.getCountry().equals(country);
    }

    @Override
    public int hashCode() {
        return country.hashCode() * 19 + instant.hashCode() * 47;
    }
}
