/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.Pair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static com.powsybl.iidm.network.Country.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CountryUtilTest {

    List<Pair<Country, Country>> boundaries;

    @Before
    public void setUp() {
        // CORE region boundaries
        boundaries = Arrays.asList(
                new ImmutablePair<>(FR, BE),
                new ImmutablePair<>(FR, DE),
                new ImmutablePair<>(BE, NL),
                new ImmutablePair<>(NL, DE),
                new ImmutablePair<>(DE, PL),
                new ImmutablePair<>(DE, CZ),
                new ImmutablePair<>(DE, AT),
                new ImmutablePair<>(PL, CZ),
                new ImmutablePair<>(PL, SK),
                new ImmutablePair<>(CZ, SK),
                new ImmutablePair<>(CZ, AT),
                new ImmutablePair<>(AT, HU),
                new ImmutablePair<>(AT, SI),
                new ImmutablePair<>(SI, HR),
                new ImmutablePair<>(SK, HU),
                new ImmutablePair<>(HU, RO),
                new ImmutablePair<>(HU, HR)
                );
    }

    @Test
    public void testNeighborsNoBoundaries() {
        assertTrue(CountryUtil.areNeighbors(FR, FR, 0, boundaries));
        assertTrue(CountryUtil.areNeighbors(RO, RO, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(FR, BE, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(BE, FR, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(FR, CZ, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(CZ, FR, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(AT, FR, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(FR, SK, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(SK, FR, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(HU, FR, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(HU, DE, 0, boundaries));
        assertFalse(CountryUtil.areNeighbors(ES, FR, 0, boundaries)); // ES is not in the tested region
        assertFalse(CountryUtil.areNeighbors(HR, RO, 0, boundaries));
    }

    @Test
    public void testNeighborsOneBoundary() {
        assertTrue(CountryUtil.areNeighbors(FR, FR, 1, boundaries));
        assertTrue(CountryUtil.areNeighbors(RO, RO, 1, boundaries));
        assertTrue(CountryUtil.areNeighbors(FR, BE, 1, boundaries));
        assertTrue(CountryUtil.areNeighbors(BE, FR, 1, boundaries));
        assertFalse(CountryUtil.areNeighbors(FR, CZ, 1, boundaries));
        assertFalse(CountryUtil.areNeighbors(CZ, FR, 1, boundaries));
        assertFalse(CountryUtil.areNeighbors(AT, FR, 1, boundaries));
        assertFalse(CountryUtil.areNeighbors(FR, SK, 1, boundaries));
        assertFalse(CountryUtil.areNeighbors(SK, FR, 1, boundaries));
        assertFalse(CountryUtil.areNeighbors(HU, FR, 1, boundaries));
        assertFalse(CountryUtil.areNeighbors(HU, DE, 1, boundaries));
        assertFalse(CountryUtil.areNeighbors(ES, FR, 1, boundaries)); // ES is not in the tested region
        assertFalse(CountryUtil.areNeighbors(HR, RO, 1, boundaries));
        assertTrue(CountryUtil.areNeighbors(HU, RO, 1, boundaries));
        assertTrue(CountryUtil.areNeighbors(PL, DE, 1, boundaries));
    }

    @Test
    public void testNeighborsTwoBoundaries() {
        assertTrue(CountryUtil.areNeighbors(FR, FR, 2, boundaries));
        assertTrue(CountryUtil.areNeighbors(RO, RO, 2, boundaries));
        assertTrue(CountryUtil.areNeighbors(FR, BE, 2, boundaries));
        assertTrue(CountryUtil.areNeighbors(BE, FR, 2, boundaries));
        assertTrue(CountryUtil.areNeighbors(FR, CZ, 2, boundaries));
        assertTrue(CountryUtil.areNeighbors(CZ, FR, 2, boundaries));
        assertTrue(CountryUtil.areNeighbors(AT, FR, 2, boundaries));
        assertFalse(CountryUtil.areNeighbors(FR, SK, 2, boundaries));
        assertFalse(CountryUtil.areNeighbors(SK, FR, 2, boundaries));
        assertFalse(CountryUtil.areNeighbors(HU, FR, 2, boundaries));
        assertTrue(CountryUtil.areNeighbors(HU, DE, 2, boundaries));
        assertFalse(CountryUtil.areNeighbors(ES, FR, 2, boundaries)); // ES is not in the tested region
        assertTrue(CountryUtil.areNeighbors(HR, RO, 2, boundaries));
    }

    @Test
    public void testNeighborsThreeBoundaries() {
        assertTrue(CountryUtil.areNeighbors(FR, FR, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(RO, RO, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(FR, BE, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(BE, FR, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(FR, CZ, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(CZ, FR, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(AT, FR, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(FR, SK, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(SK, FR, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(HU, FR, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(HU, DE, 3, boundaries));
        assertFalse(CountryUtil.areNeighbors(ES, FR, 3, boundaries)); // ES is not in the tested region
        assertFalse(CountryUtil.areNeighbors(RO, FR, 3, boundaries));
        assertTrue(CountryUtil.areNeighbors(HR, RO, 3, boundaries));
    }
}
