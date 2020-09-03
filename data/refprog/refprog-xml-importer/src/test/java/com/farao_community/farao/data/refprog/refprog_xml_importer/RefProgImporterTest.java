/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.refprog_xml_importer;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.powsybl.iidm.network.Country;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RefProgImporterTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private OffsetDateTime offsetDateTime = OffsetDateTime.of(2020, 1, 6, 23, 0, 0, 0, ZoneOffset.UTC);

    @Test(expected = FaraoException.class)
    public void testUnexistantFile() {
        RefProgImporter.importRefProg("/refProg_12nodes_doesntexist.xml", offsetDateTime);
    }

    @Test(expected = FaraoException.class)
    public void testWrongXml() {
        RefProgImporter.importRefProg(getClass().getResourceAsStream("/wrong_refprog.xml"), offsetDateTime);
    }

    @Test(expected = FaraoException.class)
    public void testRefProgWithoutInterval() {
        RefProgImporter.importRefProg(getClass().getResourceAsStream("/refprog_noInterval.xml"), offsetDateTime);
    }

    @Test(expected = FaraoException.class)
    public void testWrongTimestamp() {
        offsetDateTime = OffsetDateTime.of(2020, 1, 6, 23, 0, 0, 0, ZoneOffset.UTC);
        RefProgImporter.importRefProg(getClass().getResourceAsStream("/refProg_12nodes.xml"), offsetDateTime);
    }

    @Test
    public void testImportSimpleFile() {
        offsetDateTime = OffsetDateTime.of(2020, 1, 6, 21, 00, 0, 0, ZoneOffset.UTC);
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(getClass().getResourceAsStream("/refProg_12nodes.xml"), offsetDateTime);
        assertEquals(500, referenceProgram.getExchange("10YBE----------2", "10YFR-RTE------C"), DOUBLE_TOLERANCE);
        assertEquals(1300, referenceProgram.getExchange("10YBE----------2", "10YNL----------L"), DOUBLE_TOLERANCE);
        assertEquals(-1600, referenceProgram.getExchange("10YCB-GERMANY--8", "10YFR-RTE------C"), DOUBLE_TOLERANCE);
        assertEquals(-600, referenceProgram.getExchange("10YCB-GERMANY--8", "10YNL----------L"), DOUBLE_TOLERANCE);
        assertEquals(1800, referenceProgram.getGlobalNetPosition("10YBE----------2"), DOUBLE_TOLERANCE);
        assertEquals(1100, referenceProgram.getGlobalNetPosition("10YFR-RTE------C"), DOUBLE_TOLERANCE);
        assertEquals(-2200, referenceProgram.getGlobalNetPosition("10YCB-GERMANY--8"), DOUBLE_TOLERANCE);
        assertEquals(-700, referenceProgram.getGlobalNetPosition("10YNL----------L"), DOUBLE_TOLERANCE);
    }

    @Test
    public void testImportSimpleFileWithoutFlowForTimestamp() {
        offsetDateTime = OffsetDateTime.of(2020, 1, 6, 19, 00, 0, 0, ZoneOffset.UTC);
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(getClass().getResourceAsStream("/refProg_12nodes.xml"), offsetDateTime);
        assertEquals(0, referenceProgram.getExchange("10YBE----------2", "10YFR-RTE------C"), DOUBLE_TOLERANCE);
        assertEquals(0, referenceProgram.getExchange("10YBE----------2", "10YNL----------L"), DOUBLE_TOLERANCE);
        assertEquals(-0, referenceProgram.getExchange("10YCB-GERMANY--8", "10YFR-RTE------C"), DOUBLE_TOLERANCE);
        assertEquals(-0, referenceProgram.getExchange("10YCB-GERMANY--8", "10YNL----------L"), DOUBLE_TOLERANCE);
        assertEquals(0, referenceProgram.getGlobalNetPosition("10YBE----------2"), DOUBLE_TOLERANCE);
        assertEquals(0, referenceProgram.getGlobalNetPosition("10YFR-RTE------C"), DOUBLE_TOLERANCE);
        assertEquals(-0, referenceProgram.getGlobalNetPosition("10YCB-GERMANY--8"), DOUBLE_TOLERANCE);
        assertEquals(-0, referenceProgram.getGlobalNetPosition("10YNL----------L"), DOUBLE_TOLERANCE);
    }

    @Test
    public void testImportLargeFile1() {
        offsetDateTime = OffsetDateTime.of(2015, 1, 11, 6, 30, 0, 0, ZoneOffset.UTC);
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(getClass().getResourceAsStream("/large_refprog.xml"), offsetDateTime);
        assertEquals(76, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(191, referenceProgram.getExchange("10YFR-RTE------C", "10YCB-GERMANY--8"), DOUBLE_TOLERANCE);
        assertEquals(-191, referenceProgram.getExchange("10YCB-GERMANY--8", "10YFR-RTE------C"), DOUBLE_TOLERANCE);
        assertEquals(1756, referenceProgram.getExchange("10YFR-RTE------C", "10YES-REE------0"), DOUBLE_TOLERANCE);
        assertEquals(-288, referenceProgram.getExchange("10YCB-ALBANIA--1", "10YCS-CG-TSO---S"), DOUBLE_TOLERANCE);
        assertEquals(-2218, referenceProgram.getGlobalNetPosition("10YES-REE------0"), DOUBLE_TOLERANCE);
        assertEquals(10198, referenceProgram.getGlobalNetPosition("10YFR-RTE------C"), DOUBLE_TOLERANCE);
    }

    @Test
    public void testImportLargeFile2() {
        offsetDateTime = OffsetDateTime.of(2015, 1, 11, 19, 15, 0, 0, ZoneOffset.UTC);
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(getClass().getResource("/large_refprog.xml").getPath(), offsetDateTime);
        assertEquals(76, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(-1397, referenceProgram.getExchange(Country.CH, Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-147, referenceProgram.getExchange(Country.BA, Country.RS), DOUBLE_TOLERANCE);
        assertEquals(288, referenceProgram.getExchange("10YCS-CG-TSO---S", "10YCB-ALBANIA--1"), DOUBLE_TOLERANCE);
        assertEquals(374, referenceProgram.getExchange(Country.CZ, Country.SK), DOUBLE_TOLERANCE);
        assertEquals(-4249, referenceProgram.getGlobalNetPosition(Country.ES), DOUBLE_TOLERANCE);
        assertEquals(11366, referenceProgram.getGlobalNetPosition(Country.FR), DOUBLE_TOLERANCE);
    }
}
