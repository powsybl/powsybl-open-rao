/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class NeighbouringCountryPairsInCore {

    private NeighbouringCountryPairsInCore() {
        throw new AssertionError("Static class should not have constructor");
    }

    private static final List<Pair<String, String>> NEIGHBOURING_COUNTRY_PAIR_LIST = getNeighbouringCountryPairList();

    private static List<Pair<String, String>> getNeighbouringCountryPairList() {
        List<Pair<String, String>> ncpl = new ArrayList<>();
        ncpl.add(Pair.of(Country.BE.getName(), Country.FR.getName()));
        ncpl.add(Pair.of(Country.BE.getName(), Country.NL.getName()));
        ncpl.add(Pair.of(Country.DE.getName(), Country.FR.getName()));
        ncpl.add(Pair.of(Country.DE.getName(), Country.NL.getName()));
        ncpl.add(Pair.of(Country.BE.getName(), Country.DE.getName()));
        ncpl.add(Pair.of(Country.DE.getName(), Country.PL.getName()));
        ncpl.add(Pair.of(Country.CZ.getName(), Country.PL.getName()));
        ncpl.add(Pair.of(Country.CZ.getName(), Country.DE.getName()));
        ncpl.add(Pair.of(Country.PL.getName(), Country.SK.getName()));
        ncpl.add(Pair.of(Country.AT.getName(), Country.DE.getName()));
        ncpl.add(Pair.of(Country.AT.getName(), Country.CZ.getName()));
        ncpl.add(Pair.of(Country.AT.getName(), Country.SI.getName()));
        ncpl.add(Pair.of(Country.SI.getName(), Country.HR.getName()));
        ncpl.add(Pair.of(Country.HR.getName(), Country.HU.getName()));
        ncpl.add(Pair.of(Country.AT.getName(), Country.HU.getName()));
        ncpl.add(Pair.of(Country.HU.getName(), Country.SK.getName()));
        ncpl.add(Pair.of(Country.HU.getName(), Country.RO.getName()));
        ncpl.add(Pair.of(Country.CZ.getName(), Country.SK.getName()));
        ncpl.add(Pair.of(Country.SI.getName(), Country.HU.getName()));
        return ncpl;
    }

    static boolean belongs(String country1, String country2) {
        return NEIGHBOURING_COUNTRY_PAIR_LIST.contains(Pair.of(country1, country2))
                || NEIGHBOURING_COUNTRY_PAIR_LIST.contains(Pair.of(country2, country1));
    }

}
