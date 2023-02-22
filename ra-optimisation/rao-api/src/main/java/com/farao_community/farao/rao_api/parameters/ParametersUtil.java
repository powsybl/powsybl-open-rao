/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.iidm.network.Country;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class ParametersUtil {

    private ParametersUtil() {

    }

    public static Set<Country> convertToCountrySet(List<String> countryStringList) {
        Set<Country> countryList = new HashSet<>();
        for (String countryString : countryStringList) {
            try {
                countryList.add(Country.valueOf(countryString));
            } catch (Exception e) {
                throw new FaraoException(String.format("[%s] in loopflow countries could not be recognized as a country", countryString));
            }
        }
        return countryList;
    }

}
