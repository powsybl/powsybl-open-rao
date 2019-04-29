/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.generatorloadshiftkeys;

import com.farao_community.farao.commons.data.generatorloadshiftkeys.actors.UcteGlskDocumentImporter;
import com.google.common.math.DoubleMath;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UcteGlskDocumentImporterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteGlskDocumentImporterTest.class);

    private static final String COUNTRYFULL = "/20160729_0000_GSK_allday_full.xml";
    private static final String COUNTRYTEST = "/20160729_0000_GSK_allday_test.xml";

    @Test
    public void testUcteGlskDocumentImporterTest() throws ParserConfigurationException, SAXException, IOException {
        UcteGlskDocumentImporter importer = new UcteGlskDocumentImporter();
        UcteGlskDocument ucteGlskDocument = importer.importUcteGlskDocumentWithFilename(COUNTRYTEST);

        List<UcteGlskSeries> list = ucteGlskDocument.getListGlskSeries();
        assertFalse(list.isEmpty());
        assertEquals(1, ucteGlskDocument.getListGlskSeries().size());
        assertEquals(1, ucteGlskDocument.getListUcteGlskBlocks().size());
        assertFalse(ucteGlskDocument.getListGlskSeries().get(0).getUcteGlskBlocks().get(0).glskPointToString().isEmpty());
    }

    @Test
    public void testImportUcteGlskDocumentWithFilePathString() throws ParserConfigurationException, SAXException, IOException {
        assertFalse(new UcteGlskDocumentImporter().importUcteGlskDocumentWithFilePathString("src/test/resources/20160729_0000_GSK_allday_test.xml").getListGlskSeries().isEmpty());
    }

    @Test
    public void testImportUcteGlskDocumentWithFilePath() throws ParserConfigurationException, SAXException, IOException {
        assertFalse(new UcteGlskDocumentImporter().importUcteGlskDocumentWithFilePath(Paths.get("src/test/resources/20160729_0000_GSK_allday_test.xml")).getListGlskSeries().isEmpty());
    }

    @Test
    public void testUcteGlskDocumentImporterFull() throws ParserConfigurationException, SAXException, IOException {
        UcteGlskDocumentImporter importer = new UcteGlskDocumentImporter();
        UcteGlskDocument ucteGlskDocument = importer.importUcteGlskDocumentWithFilename(COUNTRYFULL);

        //test Merged List GlskSeries
        assertEquals(12, ucteGlskDocument.getListGlskSeries().size());
        //test merged List GlskPoints
        assertEquals(67, ucteGlskDocument.getListUcteGlskBlocks().size());

        //test factor LSK + GSK = 1
        for (int i = 0; i < ucteGlskDocument.getListGlskSeries().size(); i++) {
            for (int j = 0; j < ucteGlskDocument.getListGlskSeries().get(i).getUcteGlskBlocks().size(); j++) {
                assertTrue(DoubleMath.fuzzyEquals(1.0,
                        ucteGlskDocument.getListGlskSeries().get(i).getUcteGlskBlocks().get(j).getGlskShiftKeys().stream().mapToDouble(GlskShiftKey::getQuantity).sum(),
                        1e-5));
//                LOGGER.info(ucteGlskDocument.getListGlskSeries().get(i).getUcteGlskBlocks().get(j).glskPointToString());
            }
        }

        //test map <CountryID, List<Point>>
        Map<String, List<GlskPoint>> listPoints = ucteGlskDocument.getUcteGlskPointsByCountry();
        int sum = 0;
        for (String s : listPoints.keySet()) {
            sum += listPoints.get(s).size();
        }
        assertEquals(67, sum);

        //test countries list
        List<String> countries = ucteGlskDocument.getCountries();
        assertEquals(12, countries.size());
    }
}
