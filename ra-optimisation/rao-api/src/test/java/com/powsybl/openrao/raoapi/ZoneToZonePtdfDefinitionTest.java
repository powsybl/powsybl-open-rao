/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class ZoneToZonePtdfDefinitionTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    void testSimpleZoneToZonePtdfWithCountryCode() {

        ZoneToZonePtdfDefinition zTozPtdf = new ZoneToZonePtdfDefinition("{FR}-{ES}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());
        assertEquals(1, zTozPtdf.getWeight(new EICode(Country.FR)), DOUBLE_TOLERANCE);
        assertEquals(-1, zTozPtdf.getWeight(new EICode(Country.ES)), DOUBLE_TOLERANCE);
    }

    @Test
    void testSimpleZoneToZonePtdfWithEiCode() {

        ZoneToZonePtdfDefinition zTozPtdf = new ZoneToZonePtdfDefinition("{22Y201903145---4}-{22Y201903144---9}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());

        assertEquals(1, zTozPtdf.getWeight(new EICode("22Y201903145---4")), DOUBLE_TOLERANCE);
        assertEquals(-1, zTozPtdf.getWeight(new EICode("22Y201903144---9")), DOUBLE_TOLERANCE);
    }

    @Test
    void testSimpleZoneToZonePtdfWithMixedCode() {

        ZoneToZonePtdfDefinition zTozPtdf = new ZoneToZonePtdfDefinition("{BE}-{22Y201903144---9}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());
        assertEquals(1, zTozPtdf.getWeight(new EICode(Country.BE)), DOUBLE_TOLERANCE);
        assertEquals(-1, zTozPtdf.getWeight(new EICode("22Y201903144---9")), DOUBLE_TOLERANCE);
    }

    @Test
    void testComplexZoneToZonePtdfWithMixedCode() {

        ZoneToZonePtdfDefinition zTozPtdf = new ZoneToZonePtdfDefinition("{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}");

        assertEquals(4, zTozPtdf.getZoneToSlackPtdfs().size());
        assertEquals(4, zTozPtdf.getEiCodes().size());
        assertTrue(zTozPtdf.getEiCodes().contains(new EICode(Country.BE)));
        assertTrue(zTozPtdf.getEiCodes().contains(new EICode("22Y201903144---9")));
        assertTrue(zTozPtdf.getEiCodes().contains(new EICode(Country.DE)));
        assertTrue(zTozPtdf.getEiCodes().contains(new EICode("22Y201903145---4")));

        assertEquals(1, zTozPtdf.getWeight(new EICode(Country.BE)), DOUBLE_TOLERANCE);
        assertEquals(-1, zTozPtdf.getWeight(new EICode("22Y201903144---9")), DOUBLE_TOLERANCE);
        assertEquals(-1, zTozPtdf.getWeight(new EICode(Country.DE)), DOUBLE_TOLERANCE);
        assertEquals(1, zTozPtdf.getWeight(new EICode("22Y201903145---4")), DOUBLE_TOLERANCE);
    }

    @Test
    void testWrongSyntax1() {
        assertThrows(OpenRaoException.class, () -> new ZoneToZonePtdfDefinition("FR-ES"));
    }

    @Test
    void testWrongSyntax2() {
        assertThrows(OpenRaoException.class, () -> new ZoneToZonePtdfDefinition("FR/ES"));
    }

    @Test
    void testWrongSyntax3() {
        assertThrows(OpenRaoException.class, () -> new ZoneToZonePtdfDefinition("{{FR}-{ES}"));
    }

    @Test
    void testWrongSyntax4() {
        assertThrows(OpenRaoException.class, () -> new ZoneToZonePtdfDefinition("{FR}/{ES}"));
    }

    @Test
    void testWrongSyntax5() {
        assertThrows(OpenRaoException.class, () -> new ZoneToZonePtdfDefinition("{FRANCE}-{ES}"));
    }

    @Test
    void testWrongSyntax6() {
        assertThrows(OpenRaoException.class, () -> new ZoneToZonePtdfDefinition("{}/{ES}"));
    }

    @Test
    void testWrongCountryCode() {
        assertThrows(IllegalArgumentException.class, () -> new ZoneToZonePtdfDefinition("{XX}/{ES}"));
    }

    @Test
    void testToString() {
        ZoneToZonePtdfDefinition zTozPtdf1 = new ZoneToZonePtdfDefinition("{FR}-{ES}");
        assertEquals("{FR}-{ES}", zTozPtdf1.toString());

        ZoneToZonePtdfDefinition zTozPtdf2 = new ZoneToZonePtdfDefinition(new ArrayList<>(Arrays.asList(
            new ZoneToZonePtdfDefinition.WeightedZoneToSlackPtdf(new EICode(Country.FR), 1),
            new ZoneToZonePtdfDefinition.WeightedZoneToSlackPtdf(new EICode(Country.ES), -1))));
        assertEquals("+{FR}-{ES}", zTozPtdf2.toString());

        ZoneToZonePtdfDefinition zTozPtdf3 = new ZoneToZonePtdfDefinition(new ArrayList<>(Arrays.asList(
            new ZoneToZonePtdfDefinition.WeightedZoneToSlackPtdf(new EICode(Country.FR), -1),
            new ZoneToZonePtdfDefinition.WeightedZoneToSlackPtdf(new EICode(Country.ES), -1))));
        assertEquals("-{FR}-{ES}", zTozPtdf3.toString());
    }
}
