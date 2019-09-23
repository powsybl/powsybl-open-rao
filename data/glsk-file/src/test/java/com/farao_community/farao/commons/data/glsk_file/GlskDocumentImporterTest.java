/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file;

import com.farao_community.farao.commons.data.glsk_file.actors.GlskDocumentImporter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class GlskDocumentImporterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskDocumentImporterTest.class);

    private static final String GLSKB42TEST = "/GlskB42test.xml";
    private static final String GLSKB42COUNTRY = "/GlskB42CountryIIDM.xml";
    private static final String GLSKB43TEST = "/GlskB43ParticipationFactorIIDM.xml";
    private static final String GLSKMULTIPOINTSTEST = "/GlskMultiPoints.xml";
    private static final String GLSKB45TEST = "/GlskB45test.xml";

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
    public void testGlskDocumentImporterWithFilePathString() {
        GlskDocument glskDocument = GlskDocumentImporter.importGlsk(getResourceAsPathString(GLSKB42COUNTRY));
        assertEquals("2018-08-28T22:00:00Z", glskDocument.getInstantStart().toString());
        assertEquals("2018-08-29T22:00:00Z", glskDocument.getInstantEnd().toString());
        assertFalse(glskDocument.getCountries().isEmpty());
    }

    @Test
    public void testGlskDocumentImporterWithFilePath() {
        GlskDocument glskDocument = GlskDocumentImporter.importGlsk(getResourceAsPath(GLSKB42COUNTRY));
        assertEquals("2018-08-28T22:00:00Z", glskDocument.getInstantStart().toString());
        assertEquals("2018-08-29T22:00:00Z", glskDocument.getInstantEnd().toString());
        assertFalse(glskDocument.getCountries().isEmpty());
    }

    @Test
    public void testGlskDocumentImportB45()  {
        GlskDocument glskDocument = GlskDocumentImporter.importGlsk(getResourceAsInputStream(GLSKB45TEST));
        List<GlskShiftKey> glskShiftKeys = glskDocument.getGlskPoints().get(0).getGlskShiftKeys();
        assertTrue(!glskShiftKeys.isEmpty());
//        for (GlskShiftKey glskShiftKey : glskShiftKeys) {
//            LOGGER.info("Flow direction:" + glskShiftKey.getFlowDirection());
//            LOGGER.info("Merit order position:" + glskShiftKey.getMeritOrderPosition());
//            LOGGER.info("ID:" + glskShiftKey.getRegisteredResourceArrayList().get(0).getmRID());
//            LOGGER.info("max min: " + glskShiftKey.getRegisteredResourceArrayList().get(0).getMaximumCapacity() + "; " + glskShiftKey.getRegisteredResourceArrayList().get(0).getMinimumCapacity());
//        }
    }

    @Test
    public void testGlskDocumentImporterWithFileName() {
        GlskDocument glskDocument = GlskDocumentImporter.importGlsk(getResourceAsInputStream(GLSKB42TEST));

        List<GlskPoint> glskPointList = glskDocument.getGlskPoints();
        for (GlskPoint point : glskPointList) {
            assertEquals(Interval.parse("2018-08-28T22:00:00Z/2018-08-29T22:00:00Z"), point.getPointInterval());
            assertEquals(Integer.valueOf(1), point.getPosition());
        }

    }

    @Test
    public void testGlskDoucmentImporterGlskMultiPoints() {
        GlskDocument glskDocument = GlskDocumentImporter.importGlsk(getResourceAsInputStream(GLSKMULTIPOINTSTEST));

        List<GlskPoint> glskPointList = glskDocument.getGlskPoints();
        for (GlskPoint point : glskPointList) {
            LOGGER.info("Position: " + point.getPosition() + "; PointInterval: " + point.getPointInterval().toString());
        }
    }
}
