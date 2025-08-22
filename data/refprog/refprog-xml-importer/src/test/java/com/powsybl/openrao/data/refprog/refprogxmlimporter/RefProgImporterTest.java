/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.refprog.refprogxmlimporter;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RefProgImporterTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private OffsetDateTime offsetDateTime = OffsetDateTime.of(2020, 1, 6, 23, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void testUnexistantFile() {
        Path path = Paths.get("/refProg_12nodes_doesntexist.xml");
        assertThrows(OpenRaoException.class, () -> RefProgImporter.importRefProg(path, offsetDateTime));
    }

    @Test
    void testWrongXml() {
        InputStream inputStream = getClass().getResourceAsStream("/wrong_refProg.xml");
        assertThrows(OpenRaoException.class, () -> RefProgImporter.importRefProg(inputStream, offsetDateTime));
    }

    @Test
    void testRefProgWithoutInterval() {
        InputStream inputStream = getClass().getResourceAsStream("/refProg_noInterval.xml");
        assertThrows(OpenRaoException.class, () -> RefProgImporter.importRefProg(inputStream, offsetDateTime));
    }

    @Test
    void testWrongTimestamp() {
        offsetDateTime = OffsetDateTime.of(2020, 1, 6, 23, 0, 0, 0, ZoneOffset.UTC);
        InputStream inputStream = getClass().getResourceAsStream("/refProg_12nodes.xml");
        assertThrows(OpenRaoException.class, () -> RefProgImporter.importRefProg(inputStream, offsetDateTime));
    }

    @Test
    void testImportSimpleFile() {
        offsetDateTime = OffsetDateTime.of(2020, 1, 6, 21, 00, 0, 0, ZoneOffset.UTC);
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(getClass().getResourceAsStream("/refProg_12nodes.xml"), offsetDateTime);
        assertEquals(4, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(4, referenceProgram.getListOfAreas().size());
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
    void testImportSimpleFileWithoutFlowForTimestamp() {
        offsetDateTime = OffsetDateTime.of(2020, 1, 6, 19, 00, 0, 0, ZoneOffset.UTC);
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(getClass().getResourceAsStream("/refProg_12nodes.xml"), offsetDateTime);
        assertEquals(4, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(4, referenceProgram.getListOfAreas().size());
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
    void testImportLargeFile1() {
        offsetDateTime = OffsetDateTime.of(2015, 1, 11, 6, 30, 0, 0, ZoneOffset.UTC);
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(getClass().getResourceAsStream("/large_refProg.xml"), offsetDateTime);
        assertEquals(77, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(54, referenceProgram.getListOfAreas().size());
        assertEquals(191, referenceProgram.getExchange("10YFR-RTE------C", "10YCB-GERMANY--8"), DOUBLE_TOLERANCE);
        assertEquals(-191, referenceProgram.getExchange("10YCB-GERMANY--8", "10YFR-RTE------C"), DOUBLE_TOLERANCE);
        assertEquals(1756, referenceProgram.getExchange("10YFR-RTE------C", "10YES-REE------0"), DOUBLE_TOLERANCE);
        assertEquals(-288, referenceProgram.getExchange("10YCB-ALBANIA--1", "10YCS-CG-TSO---S"), DOUBLE_TOLERANCE);
        assertEquals(-2218, referenceProgram.getGlobalNetPosition("10YES-REE------0"), DOUBLE_TOLERANCE);
        assertEquals(10198, referenceProgram.getGlobalNetPosition("10YFR-RTE------C"), DOUBLE_TOLERANCE);
    }

    @Test
    void testImportLargeFile2() {
        offsetDateTime = OffsetDateTime.of(2015, 1, 11, 19, 15, 0, 0, ZoneOffset.UTC);
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(getClass().getResourceAsStream("/large_refProg.xml"), offsetDateTime);
        assertEquals(77, referenceProgram.getReferenceExchangeDataList().size());
        assertEquals(54, referenceProgram.getListOfAreas().size());
        EICode areaCh = new EICode(Country.CH);
        EICode areaFr = new EICode(Country.FR);
        EICode areaBa = new EICode(Country.BA);
        EICode areaRs = new EICode(Country.RS);
        EICode areaCz = new EICode(Country.CZ);
        EICode areaSk = new EICode(Country.SK);
        EICode areaEs = new EICode(Country.ES);
        assertEquals(-1397, referenceProgram.getExchange(areaCh, areaFr), DOUBLE_TOLERANCE);
        assertEquals(-147, referenceProgram.getExchange(areaBa, areaRs), DOUBLE_TOLERANCE);
        assertEquals(288, referenceProgram.getExchange("10YCS-CG-TSO---S", "10YCB-ALBANIA--1"), DOUBLE_TOLERANCE);
        assertEquals(374, referenceProgram.getExchange(areaCz, areaSk), DOUBLE_TOLERANCE);
        assertEquals(-4249, referenceProgram.getGlobalNetPosition(areaEs), DOUBLE_TOLERANCE);
        assertEquals(11366, referenceProgram.getGlobalNetPosition(areaFr), DOUBLE_TOLERANCE);
    }
}
