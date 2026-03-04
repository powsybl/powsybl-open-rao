/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.Country;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Allows filtering elements on a list of countries.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public abstract class AbstractCountriesFilter {
    private Set<Country> countries = null;

    /**
     * Get the country filter. If it has no value, all countries must be considered. If it's an empty set, no country must be considered.
     */
    public Optional<Set<Country>> getCountries() {
        return Optional.ofNullable(countries);
    }

    /**
     * Set the filter. Set it to null to consider all countries. Set it to empty set to filter out all countries.
     */
    public void setCountryFilter(@Nullable Set<Country> countries) {
        this.countries = countries;
    }
}
