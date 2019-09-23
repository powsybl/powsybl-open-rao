/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file;

import com.farao_community.farao.commons.data.glsk_file.actors.UcteGlskDocumentImporter;
import com.google.common.math.DoubleMath;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskDocumentImporterTest {

    private static final String COUNTRYFULL = "/20160729_0000_GSK_allday_full.xml";
    private static final String COUNTRYTEST = "/20160729_0000_GSK_allday_test.xml";

    private Path getResourceAsPath(String resource) {
        return Paths.get(getResourceAsPathString(resource));
    }

    private String getResourceAsPathString(String resource) {
        return getClass().getResource(resource).getPath();
    }

    private InputStream getResourceAsInputStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    public void testUcteGlskDocumentImporterTest() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocumentImporter.importGlsk(getResourceAsInputStream(COUNTRYTEST));

        List<UcteGlskSeries> list = ucteGlskDocument.getListGlskSeries();
        assertFalse(list.isEmpty());
        assertEquals(1, ucteGlskDocument.getListGlskSeries().size());
        assertEquals(1, ucteGlskDocument.getListUcteGlskBlocks().size());
        assertFalse(ucteGlskDocument.getListGlskSeries().get(0).getUcteGlskBlocks().get(0).glskPointToString().isEmpty());
    }

    @Test
    public void testImportUcteGlskDocumentWithFilePathString() {
        assertFalse(UcteGlskDocumentImporter.importGlsk(getResourceAsPathString(COUNTRYTEST)).getListGlskSeries().isEmpty());
    }

    @Test
    public void testImportUcteGlskDocumentWithFilePath() {
        assertFalse(UcteGlskDocumentImporter.importGlsk(getResourceAsPath(COUNTRYTEST)).getListGlskSeries().isEmpty());
    }

    @Test
    public void testUcteGlskDocumentImporterFull() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocumentImporter.importGlsk(getResourceAsInputStream(COUNTRYFULL));

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
