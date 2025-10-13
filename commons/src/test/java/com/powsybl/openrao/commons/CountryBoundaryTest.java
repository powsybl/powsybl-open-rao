/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CountryBoundaryTest {

    private CountryBoundary boundaryFrBe;

    @BeforeEach
    public void setUp() {
        boundaryFrBe = new CountryBoundary(Country.FR, Country.BE);
    }

    @Test
    void testGetCountry() {
        assertTrue(boundaryFrBe.getCountryLeft().equals(Country.FR) || boundaryFrBe.getCountryRight().equals(Country.FR));
        assertTrue(boundaryFrBe.getCountryLeft().equals(Country.BE) || boundaryFrBe.getCountryRight().equals(Country.BE));
    }

    @Test
    void testEquals() {
        assertEquals(boundaryFrBe, new CountryBoundary(Country.FR, Country.BE));
        assertEquals(boundaryFrBe, new CountryBoundary(Country.BE, Country.FR));
        assertNotEquals(boundaryFrBe, new CountryBoundary(Country.BE, Country.DE));
        assertNotEquals(boundaryFrBe, new CountryBoundary(Country.ES, Country.FR));
        assertNotEquals(boundaryFrBe, new CountryBoundary(Country.ES, Country.PT));
    }

    @Test
    void testToString() {
        assertEquals("FR/BE", boundaryFrBe.toString());
    }
}
