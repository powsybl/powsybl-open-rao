/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.import_.actors.UcteGlskImporter;
import com.google.common.math.DoubleMath;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskImporterTest {

    private static final String COUNTRYFULL = "/20160729_0000_GSK_allday_full.xml";
    private static final String COUNTRYTEST = "/20160729_0000_GSK_allday_test.xml";

    private Path getResourceAsPath(String resource) {
        return Paths.get(getResourceAsPathString(resource));
    }

    private String getResourceAsPathString(String resource) {
        return new File(getClass().getResource(resource).getFile()).getAbsolutePath();
    }

    private InputStream getResourceAsInputStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    public void testUcteGlskDocumentImporterTest() {
        UcteGlskDocument ucteGlskDocument = UcteGlskImporter.importGlsk(getResourceAsInputStream(COUNTRYTEST));

        List<UcteGlskSeries> list = ucteGlskDocument.getListGlskSeries();
        assertFalse(list.isEmpty());
        assertEquals(1, ucteGlskDocument.getListGlskSeries().size());
        assertEquals(1, ucteGlskDocument.getListUcteGlskBlocks().size());
        assertFalse(ucteGlskDocument.getListGlskSeries().get(0).getUcteGlskBlocks().get(0).glskPointToString().isEmpty());
    }

    @Test
    public void testImportUcteGlskDocumentWithFilePathString() {
        assertFalse(UcteGlskImporter.importGlsk(getResourceAsPathString(COUNTRYTEST)).getListGlskSeries().isEmpty());
    }

    @Test
    public void testImportUcteGlskDocumentWithFilePath() {
        assertFalse(UcteGlskImporter.importGlsk(getResourceAsPath(COUNTRYTEST)).getListGlskSeries().isEmpty());
    }

    @Test
    public void testUcteGlskDocumentImporterFull() {
        UcteGlskDocument ucteGlskDocument = UcteGlskImporter.importGlsk(getResourceAsInputStream(COUNTRYFULL));

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

        Interval documentGSKTimeInterval = ucteGlskDocument.getGSKTimeInterval(); // <GSKTimeInterval v="2016-07-28T22:00Z/2016-07-29T22:00Z"/>
        assertEquals("2016-07-28T22:00:00Z", documentGSKTimeInterval.getStart().toString());
    }

    @Test
    public void testExceptionCases() {
        try {
            UcteGlskImporter.importGlsk("/nonExistingFile.xml");
            fail();
        } catch (FaraoException e) {
            // Should throw FaraoException
        }

        try {
            UcteGlskImporter.importGlsk(Paths.get("/nonExistingFile.xml"));
            fail();
        } catch (FaraoException e) {
            // Should throw FaraoException
        }

        try {
            byte[] nonXmlBytes = "{ should not be imported }".getBytes();
            UcteGlskImporter.importGlsk(new ByteArrayInputStream(nonXmlBytes));
            fail();
        } catch (FaraoException e) {
            // Should throw FaraoException
        }
    }

    @Test
    public void testGetGlskPointForInstant() {
        UcteGlskDocument ucteGlskDocument = UcteGlskImporter.importGlsk(getResourceAsInputStream(COUNTRYFULL));
        Map<String, GlskPoint> result = ucteGlskDocument.getGlskPointsForInstant(Instant.parse("2016-07-28T23:30:00Z"));
        Instant instant = Instant.parse("2016-07-28T23:30:00Z");
        assertTrue(result.get("10YNL----------L").getPointInterval().getStart().isBefore(instant));
        assertTrue(result.get("10YNL----------L").getPointInterval().getEnd().isAfter(instant));
        assertEquals(1, result.get("10YNL----------L").getGlskShiftKeys().size());

        Double factor = result.get("10YNL----------L").getGlskShiftKeys().get(0)
                .getRegisteredResourceArrayList()
                .stream()
                .filter(glskRegisteredResource -> glskRegisteredResource.getmRID().equals("N_EC-42 "))
                .mapToDouble(glskRegisteredResource -> glskRegisteredResource.getParticipationFactor())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Not good"));

        assertEquals(0.009878, factor, 0.);
    }

    @Test
    public void testGetGlskPointForIncorrectInstant() {
        UcteGlskDocument ucteGlskDocument = UcteGlskImporter.importGlsk(getResourceAsInputStream(COUNTRYFULL));
        try {
            ucteGlskDocument.getGlskPointsForInstant(Instant.parse("2016-07-29T22:00:00Z"));
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }
}
