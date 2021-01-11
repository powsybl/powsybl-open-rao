/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CountryUtil {
    private CountryUtil() {
    }

    public static boolean areNeighbors(Country country1, Country country2, int maxNumberOfBoundaries, List<Pair<Country, Country>> boundaries) {
        if (country1.equals(country2)) {
            return true;
        } else if (maxNumberOfBoundaries <= 0) {
            return false;
        } else {
            boolean neighbors = false;
            for (Pair<Country, Country> countryCountryPair : boundaries) {
                if ((countryCountryPair.getLeft().equals(country1) && areNeighbors(countryCountryPair.getRight(), country2, maxNumberOfBoundaries - 1, boundaries))
                        || (countryCountryPair.getRight().equals(country1) && areNeighbors(countryCountryPair.getLeft(), country2, maxNumberOfBoundaries - 1, boundaries))) {
                    neighbors = true;
                    break;
                }
            }
            return neighbors;
        }
    }
}
