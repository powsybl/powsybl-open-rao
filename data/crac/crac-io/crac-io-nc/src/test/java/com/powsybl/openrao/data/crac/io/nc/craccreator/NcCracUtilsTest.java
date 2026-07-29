/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class NcCracUtilsTest {

    @Test
    void testDurationConversionNoYearNoMonth() {
        assertEquals(90063, NcCracUtils.convertDurationToSeconds("P1DT1H1M3S"));
        assertEquals(10920, NcCracUtils.convertDurationToSeconds("PT3H2M0S"));
        assertEquals(315, NcCracUtils.convertDurationToSeconds("P0DT5M15S"));
        assertEquals(172800, NcCracUtils.convertDurationToSeconds("P2DT0H0S"));
        assertEquals(3600, NcCracUtils.convertDurationToSeconds("P0DT1H0M"));
        assertEquals(62, NcCracUtils.convertDurationToSeconds("PT1M2S"));
        assertEquals(2, NcCracUtils.convertDurationToSeconds("PT0H2S"));
        assertEquals(2, NcCracUtils.convertDurationToSeconds("P0DT2S"));
        assertEquals(240, NcCracUtils.convertDurationToSeconds("PT0H4M"));
        assertEquals(60, NcCracUtils.convertDurationToSeconds("P0DT1M"));
        assertEquals(3600, NcCracUtils.convertDurationToSeconds("P0DT1H"));
        assertEquals(5, NcCracUtils.convertDurationToSeconds("PT5S"));
        assertEquals(60, NcCracUtils.convertDurationToSeconds("PT1M"));
        assertEquals(0, NcCracUtils.convertDurationToSeconds("PT0H"));
        assertEquals(86400, NcCracUtils.convertDurationToSeconds("P1D"));
    }

    @Test
    void testDurationConversionNoMonth() {
        assertEquals(90063, NcCracUtils.convertDurationToSeconds("P0Y1DT1H1M3S"));
        assertEquals(10920, NcCracUtils.convertDurationToSeconds("P0YT3H2M0S"));
        assertEquals(315, NcCracUtils.convertDurationToSeconds("P0Y0DT5M15S"));
        assertEquals(172800, NcCracUtils.convertDurationToSeconds("P0Y2DT0H0S"));
        assertEquals(3600, NcCracUtils.convertDurationToSeconds("P0Y0DT1H0M"));
        assertEquals(62, NcCracUtils.convertDurationToSeconds("P0YT1M2S"));
        assertEquals(2, NcCracUtils.convertDurationToSeconds("P0YT0H2S"));
        assertEquals(2, NcCracUtils.convertDurationToSeconds("P0Y0DT2S"));
        assertEquals(240, NcCracUtils.convertDurationToSeconds("P0YT0H4M"));
        assertEquals(60, NcCracUtils.convertDurationToSeconds("P0Y0DT1M"));
        assertEquals(3600, NcCracUtils.convertDurationToSeconds("P0Y0DT1H"));
        assertEquals(5, NcCracUtils.convertDurationToSeconds("P0YT5S"));
        assertEquals(60, NcCracUtils.convertDurationToSeconds("P0YT1M"));
        assertEquals(0, NcCracUtils.convertDurationToSeconds("P0YT0H"));
        assertEquals(86400, NcCracUtils.convertDurationToSeconds("P0Y1D"));
    }

    @Test
    void testDurationConversionNoYear() {
        assertEquals(90063, NcCracUtils.convertDurationToSeconds("P0M1DT1H1M3S"));
        assertEquals(10920, NcCracUtils.convertDurationToSeconds("P0MT3H2M0S"));
        assertEquals(315, NcCracUtils.convertDurationToSeconds("P0M0DT5M15S"));
        assertEquals(172800, NcCracUtils.convertDurationToSeconds("P0M2DT0H0S"));
        assertEquals(3600, NcCracUtils.convertDurationToSeconds("P0M0DT1H0M"));
        assertEquals(62, NcCracUtils.convertDurationToSeconds("P0MT1M2S"));
        assertEquals(2, NcCracUtils.convertDurationToSeconds("P0MT0H2S"));
        assertEquals(2, NcCracUtils.convertDurationToSeconds("P0M0DT2S"));
        assertEquals(240, NcCracUtils.convertDurationToSeconds("P0MT0H4M"));
        assertEquals(60, NcCracUtils.convertDurationToSeconds("P0M0DT1M"));
        assertEquals(3600, NcCracUtils.convertDurationToSeconds("P0M0DT1H"));
        assertEquals(5, NcCracUtils.convertDurationToSeconds("P0MT5S"));
        assertEquals(60, NcCracUtils.convertDurationToSeconds("P0MT1M"));
        assertEquals(0, NcCracUtils.convertDurationToSeconds("P0MT0H"));
        assertEquals(86400, NcCracUtils.convertDurationToSeconds("P0M1D"));
    }

    @Test
    void testDurationConversionWithYearAndMonth() {
        assertEquals(90063, NcCracUtils.convertDurationToSeconds("P0Y0M1DT1H1M3S"));
        assertEquals(10920, NcCracUtils.convertDurationToSeconds("P0Y0MT3H2M0S"));
        assertEquals(315, NcCracUtils.convertDurationToSeconds("P0Y0M0DT5M15S"));
        assertEquals(172800, NcCracUtils.convertDurationToSeconds("P0Y0M2DT0H0S"));
        assertEquals(3600, NcCracUtils.convertDurationToSeconds("P0Y0M0DT1H0M"));
        assertEquals(62, NcCracUtils.convertDurationToSeconds("P0Y0MT1M2S"));
        assertEquals(2, NcCracUtils.convertDurationToSeconds("P0Y0MT0H2S"));
        assertEquals(2, NcCracUtils.convertDurationToSeconds("P0Y0M0DT2S"));
        assertEquals(240, NcCracUtils.convertDurationToSeconds("P0Y0MT0H4M"));
        assertEquals(60, NcCracUtils.convertDurationToSeconds("P0Y0M0DT1M"));
        assertEquals(3600, NcCracUtils.convertDurationToSeconds("P0Y0M0DT1H"));
        assertEquals(5, NcCracUtils.convertDurationToSeconds("P0Y0MT5S"));
        assertEquals(60, NcCracUtils.convertDurationToSeconds("P0Y0MT1M"));
        assertEquals(0, NcCracUtils.convertDurationToSeconds("P0Y0MT0H"));
        assertEquals(86400, NcCracUtils.convertDurationToSeconds("P0Y0M1D"));
    }

    @Test
    void testInvalidDurationPattern() {
        assertThrows(OpenRaoException.class, () -> NcCracUtils.convertDurationToSeconds("P1R"));
        assertThrows(OpenRaoException.class, () -> NcCracUtils.convertDurationToSeconds("P2Y"));
        assertThrows(OpenRaoException.class, () -> NcCracUtils.convertDurationToSeconds("P1YT3S"));
        assertThrows(OpenRaoException.class, () -> NcCracUtils.convertDurationToSeconds("P5Y4M"));
        assertThrows(OpenRaoException.class, () -> NcCracUtils.convertDurationToSeconds("P0Y5MT3S"));
    }

    @Test
    void testEicFromUrl() {
        assertEquals("10XES-REE------E", NcCracUtils.getEicFromUrl("http://energy.referencedata.eu/EIC/10XES-REE------E"));
        assertEquals("10XPT-REN------9", NcCracUtils.getEicFromUrl("http://energy.referencedata.eu/EIC/10XPT-REN------9"));
        assertEquals("10XFR-RTE------Q", NcCracUtils.getEicFromUrl("http://energy.referencedata.eu/EIC/10XFR-RTE------Q"));
        assertNull(NcCracUtils.getEicFromUrl("Hello world!"));
    }
}
