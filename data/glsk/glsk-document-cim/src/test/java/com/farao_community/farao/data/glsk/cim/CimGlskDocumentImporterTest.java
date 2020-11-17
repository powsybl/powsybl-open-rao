/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.cim;

import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.AbstractGlskShiftKey;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.GlskException;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class CimGlskDocumentImporterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimGlskDocumentImporterTest.class);

    private static final String GLSKB42TEST = "/GlskB42test.xml";
    private static final String GLSKB42COUNTRY = "/GlskB42CountryIIDM.xml";
    private static final String GLSKMULTIPOINTSTEST = "/GlskMultiPoints.xml";
    private static final String GLSKB45TEST = "/GlskB45test.xml";

    private InputStream getResourceAsInputStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    public void testGlskDocumentImporterWithFilePathString() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB42COUNTRY));
        assertEquals("2018-08-28T22:00:00Z", cimGlskDocument.getInstantStart().toString());
        assertEquals("2018-08-29T22:00:00Z", cimGlskDocument.getInstantEnd().toString());
        assertFalse(cimGlskDocument.getZones().isEmpty());
    }

    @Test
    public void testGlskDocumentImporterWithFilePath() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB42COUNTRY));
        assertEquals("2018-08-28T22:00:00Z", cimGlskDocument.getInstantStart().toString());
        assertEquals("2018-08-29T22:00:00Z", cimGlskDocument.getInstantEnd().toString());
        assertFalse(cimGlskDocument.getZones().isEmpty());
    }

    @Test
    public void testGlskDocumentImportB45() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB45TEST));
        List<AbstractGlskShiftKey> glskShiftKeys = cimGlskDocument.getGlskPoints().get(0).getGlskShiftKeys();
        assertFalse(glskShiftKeys.isEmpty());
    }

    @Test
    public void testGlskDocumentImporterWithFileName() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB42TEST));

        List<AbstractGlskPoint> glskPointList = cimGlskDocument.getGlskPoints();
        for (AbstractGlskPoint point : glskPointList) {
            assertEquals(Interval.parse("2018-08-28T22:00:00Z/2018-08-29T22:00:00Z"), point.getPointInterval());
            assertEquals(Integer.valueOf(1), point.getPosition());
        }

    }

    @Test
    public void testGlskDocumentImporterGlskMultiPoints() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKMULTIPOINTSTEST));

        List<AbstractGlskPoint> glskPointList = cimGlskDocument.getGlskPoints();
        for (AbstractGlskPoint point : glskPointList) {
            LOGGER.info("Position: " + point.getPosition() + "; PointInterval: " + point.getPointInterval().toString());
        }
        assertFalse(glskPointList.isEmpty());
    }

    @Test(expected = GlskException.class)
    public void testExceptionCases() {
        byte[] nonXmlBytes = "{ should not be imported }".getBytes();
        new CimGlskDocumentImporter().importGlsk(new ByteArrayInputStream(nonXmlBytes));
    }

    @Test(expected = FileNotFoundException.class)
    public void testFileNotFound() throws FileNotFoundException {
        GlskDocumentImporters.importGlsk("/nonExistingFile.xml");
    }

    @Test
    public void existsTrue() {
        CimGlskDocumentImporter importer = new CimGlskDocumentImporter();
        assertTrue(importer.canImport(getResourceAsInputStream(GLSKB45TEST)));
    }

    @Test
    public void existsFalse() {
        CimGlskDocumentImporter importer = new CimGlskDocumentImporter();
        assertFalse(importer.canImport(getResourceAsInputStream("/GlskB45wrong.xml")));
    }

    @Test
    public void fullImport() {
        GlskDocument document = GlskDocumentImporters.importGlsk(getResourceAsInputStream(GLSKB42COUNTRY));
        assertEquals(1, document.getZones().size());
    }
}
