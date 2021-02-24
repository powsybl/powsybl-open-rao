package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.FaraoException;
import com.powsybl.iidm.network.Country;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ZoneToZonePtdfTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testSimpleZoneToZonePtdfWithCountryCode() {

        ZoneToZonePtdf zTozPtdf = new ZoneToZonePtdf("{FR}-{ES}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackFr = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().equals(new EICode(Country.FR)))
            .findAny();

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackEs = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().equals(new EICode(Country.ES)))
            .findAny();

        assertTrue(zToSlackFr.isPresent());
        assertEquals(1, zToSlackFr.get().getWeight(), DOUBLE_TOLERANCE);
        assertTrue(zToSlackEs.isPresent());
        assertEquals(-1, zToSlackEs.get().getWeight(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSimpleZoneToZonePtdfWithEiCode() {

        ZoneToZonePtdf zTozPtdf = new ZoneToZonePtdf("{22Y201903145---4}-{22Y201903144---9}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackDeAl = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().getAreaCode().equals("22Y201903145---4"))
            .findAny();

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackBeAl = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().getAreaCode().equals("22Y201903144---9"))
            .findAny();

        assertTrue(zToSlackDeAl.isPresent());
        assertEquals(1, zToSlackDeAl.get().getWeight(), DOUBLE_TOLERANCE);
        assertTrue(zToSlackBeAl.isPresent());
        assertEquals(-1, zToSlackBeAl.get().getWeight(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSimpleZoneToZonePtdfWithMixedCode() {

        ZoneToZonePtdf zTozPtdf = new ZoneToZonePtdf("{BE}-{22Y201903144---9}");

        assertEquals(2, zTozPtdf.getZoneToSlackPtdfs().size());

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackBe = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().equals(new EICode(Country.BE)))
            .findAny();

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackBeAl = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().getAreaCode().equals("22Y201903144---9"))
            .findAny();

        assertTrue(zToSlackBe.isPresent());
        assertEquals(1, zToSlackBe.get().getWeight(), DOUBLE_TOLERANCE);
        assertTrue(zToSlackBeAl.isPresent());
        assertEquals(-1, zToSlackBeAl.get().getWeight(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComplexZoneToZonePtdfWithMixedCode() {

        ZoneToZonePtdf zTozPtdf = new ZoneToZonePtdf("{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}");

        assertEquals(4, zTozPtdf.getZoneToSlackPtdfs().size());

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackBe = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().equals(new EICode(Country.BE)))
            .findAny();

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackBeAl = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().getAreaCode().equals("22Y201903144---9"))
            .findAny();

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackDe = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().equals(new EICode(Country.DE)))
            .findAny();

        Optional<ZoneToZonePtdf.WeightedZoneToSlackPtdf> zToSlackDeAl = zTozPtdf.getZoneToSlackPtdfs().stream().
            filter(zToSlackPtdf -> zToSlackPtdf.getPtdfZoneToSlack().getAreaCode().equals("22Y201903145---4"))
            .findAny();

        assertTrue(zToSlackBe.isPresent());
        assertEquals(1, zToSlackBe.get().getWeight(), DOUBLE_TOLERANCE);
        assertTrue(zToSlackBeAl.isPresent());
        assertEquals(-1, zToSlackBeAl.get().getWeight(), DOUBLE_TOLERANCE);
        assertTrue(zToSlackDe.isPresent());
        assertEquals(1, zToSlackBe.get().getWeight(), DOUBLE_TOLERANCE);
        assertTrue(zToSlackDeAl.isPresent());
        assertEquals(-1, zToSlackBeAl.get().getWeight(), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax1() {
        new ZoneToZonePtdf("FR-ES");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax2() {
        new ZoneToZonePtdf("FR/ES");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax3() {
        new ZoneToZonePtdf("{{FR}-{ES}");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax4() {
        new ZoneToZonePtdf("{FR}/{ES}");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax5() {
        new ZoneToZonePtdf("{FRANCE}/{ES}");
    }

    @Test(expected = FaraoException.class)
    public void testWrongSyntax6() {
        new ZoneToZonePtdf("{}/{ES}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongCountryCode() {
        new ZoneToZonePtdf("{XX}/{ES}");
    }

    @Test
    public void testToString() {
        ZoneToZonePtdf zTozPtdf1 = new ZoneToZonePtdf("{FR}-{ES}");
        assertEquals("{FR}-{ES}", zTozPtdf1.toString());

        ZoneToZonePtdf zTozPtdf2 = new ZoneToZonePtdf(new ArrayList<>(Arrays.asList(
            new ZoneToZonePtdf.WeightedZoneToSlackPtdf(new EICode(Country.FR), 1),
            new ZoneToZonePtdf.WeightedZoneToSlackPtdf(new EICode(Country.ES), -1))));
        assertEquals("+{FR}-{ES}", zTozPtdf2.toString());

        ZoneToZonePtdf zTozPtdf3 = new ZoneToZonePtdf(new ArrayList<>(Arrays.asList(
            new ZoneToZonePtdf.WeightedZoneToSlackPtdf(new EICode(Country.FR), -1),
            new ZoneToZonePtdf.WeightedZoneToSlackPtdf(new EICode(Country.ES), -1))));
        assertEquals("-{FR}-{ES}", zTozPtdf3.toString());
    }
}
