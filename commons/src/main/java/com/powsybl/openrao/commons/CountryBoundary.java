/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.google.common.collect.Sets;
import com.powsybl.iidm.network.Country;

import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CountryBoundary {

    private Country country1;
    private Country country2;

    public CountryBoundary(Country country1, Country country2) {
        if (country1.equals(country2)) {
            throw new OpenRaoException("Boundary should delimit two different countries");
        }
        this.country1 = country1;
        this.country2 = country2;
    }

    public Country getCountryLeft() {
        return country1;
    }

    public Country getCountryRight() {
        return country2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(country1, country2);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CountryBoundary other) {
            return Sets.newHashSet(country1, country2).equals(Sets.newHashSet(other.country1, other.country2));
        }
        return false;
    }

    @Override
    public String toString() {
        return country1 + "/" + country2;
    }
}
