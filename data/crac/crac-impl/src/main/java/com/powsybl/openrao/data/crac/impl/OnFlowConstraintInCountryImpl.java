/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.iidm.network.Country;

import java.util.Optional;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintInCountryImpl implements OnFlowConstraintInCountry {
    private final Instant instant;
    private final Optional<Contingency> contingency;
    private final Country country;

    OnFlowConstraintInCountryImpl(Instant instant, Optional<Contingency> contingency, Country country) {
        this.instant = instant;
        this.contingency = contingency;
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
    public Optional<Contingency> getContingency() {
        return contingency;
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
        return rule.getInstant().equals(instant)
            && rule.getContingency().equals(contingency)
            && rule.getCountry().equals(country);
    }

    @Override
    public int hashCode() {
        return country.hashCode() * 19
            + instant.hashCode() * 47
            + contingency.hashCode() * 59;
    }
}
