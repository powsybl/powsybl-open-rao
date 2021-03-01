package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.FaraoException;
import com.powsybl.iidm.network.Country;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ZoneToZonePtdfDefinitionTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testSimpleZoneToZonePtdfWithCountryCode() {

        ZoneToZonePtdfDefinition zTozPtdf = new ZoneToZonePtdfDefinition("{FR}-{ES}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());
        assertEquals(1, zTozPtdf.getWeight(new EICode(Country.FR)), DOUBLE_TOLERANCE);
        assertEquals(-1, zTozPtdf.getWeight(new EICode(Country.ES)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSimpleZoneToZonePtdfWithEiCode() {

        ZoneToZonePtdfDefinition zTozPtdf = new ZoneToZonePtdfDefinition("{22Y201903145---4}-{22Y201903144---9}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());

        assertEquals(1, zTozPtdf.getWeight(new EICode("22Y201903145---4")), DOUBLE_TOLERANCE);
        assertEquals(-1, zTozPtdf.getWeight(new EICode("22Y201903144---9")), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSimpleZoneToZonePtdfWithMixedCode() {

        ZoneToZonePtdfDefinition zTozPtdf = new ZoneToZonePtdfDefinition("{BE}-{22Y201903144---9}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());
        assertEquals(1, zTozPtdf.getWeight(new EICode(Country.BE)), DOUBLE_TOLERANCE);
        assertEquals(-1, zTozPtdf.getWeight(new EICode("22Y201903144---9")), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComplexZoneToZonePtdfWithMixedCode() {

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

    @Test(expected = FaraoException.class)
    public void testWrongSyntax1() {
        new ZoneToZonePtdfDefinition("FR-ES");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax2() {
        new ZoneToZonePtdfDefinition("FR/ES");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax3() {
        new ZoneToZonePtdfDefinition("{{FR}-{ES}");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax4() {
        new ZoneToZonePtdfDefinition("{FR}/{ES}");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax5() {
        new ZoneToZonePtdfDefinition("{FRANCE}-{ES}");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax6() {
        new ZoneToZonePtdfDefinition("{}/{ES}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongCountryCode() {
        new ZoneToZonePtdfDefinition("{XX}/{ES}");
    }

    @Test
    public void testToString() {
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
