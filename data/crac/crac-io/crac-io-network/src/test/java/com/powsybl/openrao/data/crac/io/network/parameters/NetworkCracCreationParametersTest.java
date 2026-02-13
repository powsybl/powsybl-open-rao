/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NetworkCracCreationParametersTest {
    @Test
    void testGetName() {
        assertEquals("NetworkCracCreationParameters", new NetworkCracCreationParameters(null, null).getName());
    }

    @Test
    void testWrongInstants() {
        // Mustn't use reserved names
        List<String> instants1 = List.of("preventive");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new NetworkCracCreationParameters(instants1, null));
        assertEquals("Instant names must be unique. Names 'preventive' and 'outage' are reserved for the preventive and outage instants.", exception.getMessage());

        List<String> instants2 = List.of("outage");
        exception = assertThrows(OpenRaoException.class, () -> new NetworkCracCreationParameters(null, instants2));
        assertEquals("Instant names must be unique. Names 'preventive' and 'outage' are reserved for the preventive and outage instants.", exception.getMessage());

        // Mustn't use same names multiple times
        List<String> instants3 = List.of("curative", "curative", "other_curative");
        exception = assertThrows(OpenRaoException.class, () -> new NetworkCracCreationParameters(null, instants3));
        assertEquals("Instant names must be unique. Names 'preventive' and 'outage' are reserved for the preventive and outage instants.", exception.getMessage());

        List<String> instants4 = List.of("one", "two");
        List<String> instants5 = List.of("one", "three");
        exception = assertThrows(OpenRaoException.class, () -> new NetworkCracCreationParameters(instants4, instants5));
        assertEquals("Instant names must be unique. Names 'preventive' and 'outage' are reserved for the preventive and outage instants.", exception.getMessage());

        // Mustn't use null or empty strings
        List<String> instants6 = new ArrayList<>();
        instants6.add(null);
        exception = assertThrows(OpenRaoException.class, () -> new NetworkCracCreationParameters(null, instants6));
        assertEquals("All instant names should be non null and not empty.", exception.getMessage());

        List<String> instants7 = List.of("", "two");
        exception = assertThrows(OpenRaoException.class, () -> new NetworkCracCreationParameters(instants7, null));
        assertEquals("All instant names should be non null and not empty.", exception.getMessage());
    }

    @Test
    void testGeneratorCombination() {
        RedispatchingRangeActions parameters = new NetworkCracCreationParameters(null, null).getRedispatchingRangeActions();
        Map<String, Set<String>> combinations = Map.of("combi1", Set.of("gen1", "gen2"), "combi2", Set.of("gen1", "gen3"));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> parameters.setGeneratorCombinations(combinations));
        assertEquals("A generator can only be used once in generator combinations.", exception.getMessage());
    }

    @Test
    void testIllegalMinMax() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new MinAndMax<>(10., 2.));
        assertEquals("Min should be smaller than max!", exception.getMessage());
    }

    @Test
    void testContingencies() {
        Contingencies parameters = new NetworkCracCreationParameters(null, null).getContingencies();
        assertTrue(parameters.getCountries().isEmpty());
        assertTrue(parameters.getMinV().isEmpty());
        assertTrue(parameters.getMaxV().isEmpty());

        // Country filter
        parameters.setCountryFilter(Set.of(Country.AE, Country.FR));
        assertEquals(Optional.of(Set.of(Country.AE, Country.FR)), parameters.getCountries());

        parameters.setCountryFilter(null);
        assertTrue(parameters.getCountries().isEmpty());

        parameters.setCountryFilter(Set.of());
        assertEquals(Optional.of(Set.of()), parameters.getCountries());

        // Min and max V
        parameters.setMinAndMaxV(-1., 2.);
        assertEquals(Optional.of(-1.), parameters.getMinV());
        assertEquals(Optional.of(2.), parameters.getMaxV());

        parameters.setMinAndMaxV(null, -250.);
        assertTrue(parameters.getMinV().isEmpty());
        assertEquals(Optional.of(-250.), parameters.getMaxV());

        parameters.setMinAndMaxV(50., null);
        assertEquals(Optional.of(50.), parameters.getMinV());
        assertTrue(parameters.getMaxV().isEmpty());

        parameters.setMinAndMaxV(null, null);
        assertTrue(parameters.getMinV().isEmpty());
        assertTrue(parameters.getMaxV().isEmpty());

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> parameters.setMinAndMaxV(10., 2.));
        assertEquals("Min should be smaller than max!", exception.getMessage());
    }
}
