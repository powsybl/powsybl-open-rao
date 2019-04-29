/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.generatorloadshiftkeys;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.data.generatorloadshiftkeys.actors.GlskDocumentImporter;
import com.farao_community.farao.commons.data.generatorloadshiftkeys.actors.GlskDocumentLinearGlskConverter;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GlskDocumentLinearGlskConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskDocumentLinearGlskConverterTest.class);

    private static final String GLSKB42TEST = "/GlskB42test.xml";
    private static final String GLSKB42COUNTRYIIDM = "/GlskB42CountryIIDM.xml";
    private static final String GLSKB42COUNTRYQUANTITY = "/GlskB42CountryQuantity.xml";
    private static final String GLSKB42EXPLICITIIDM = "/GlskB42ExplicitIIDM.xml";
    private static final String GLSKB42EXPLICITGSKLSK = "/GlskB42ExplicitGskLsk.xml";
    private static final String GLSKB43 = "/GlskB43ParticipationFactorIIDM.xml";
    private static final String GLSKB43GSKLSK = "/GlskB43ParticipationFactorGskLsk.xml";

    private Network testNetwork;

    @Before
    public void setUp() throws ParserConfigurationException, SAXException, IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        GlskDocumentImporter importer = new GlskDocumentImporter();
        GlskPoint glskPointCountry = importer.importGlskDocumentWithFilename(GLSKB42COUNTRYIIDM).getGlskPoints().get(0);
        GlskPoint glskPointCountryQuantity = importer.importGlskDocumentWithFilename(GLSKB42COUNTRYQUANTITY).getGlskPoints().get(0);
        GlskPoint glskPointExplicit = importer.importGlskDocumentWithFilename(GLSKB42EXPLICITIIDM).getGlskPoints().get(0);
        GlskPoint glskPointExplicitGskLsk = importer.importGlskDocumentWithFilename(GLSKB42EXPLICITGSKLSK).getGlskPoints().get(0);
        GlskPoint glskPointParticipationFactor = importer.importGlskDocumentWithFilename(GLSKB43).getGlskPoints().get(0);
        GlskPoint glskPointParticipationFactorGskLsk = importer.importGlskDocumentWithFilename(GLSKB43GSKLSK).getGlskPoints().get(0);

    }

    @Test
    public void testMappingGlskDocumentToDataChronology() throws ParserConfigurationException, SAXException, IOException {
        GlskDocumentLinearGlskConverter mapper = new GlskDocumentLinearGlskConverter();
        Map<String, DataChronology<GlskPoint>> map = mapper.convertGlskDocumentToGlskPointDataChronologyFromFileName("/GlskB42test.xml");

//        for (String country : map.keySet()) {
//            LOGGER.info("Country: " + country);
//            LOGGER.info("   DataChronology: Interval: " + map.get(country).getDataForInstant(Instant.parse("2018-08-28T23:00:00Z")).get().getUcteGlskNodes().get(0).getSubjectDomainmRID());
//            LOGGER.info("   DataChronology: Interval: " + map.get(country).getDataForInstant(Instant.parse("2018-08-28T23:00:00Z")).get().getPointInterval().toString());
//        }

    }

    @Test
    public void testConvertGlskDocumentToLinearGlskDataChronology() throws ParserConfigurationException, SAXException, IOException {
        Map<String, DataChronology<LinearGlsk>> mapGlskDocLinearGlsk = new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFileName(GLSKB42COUNTRYIIDM, testNetwork);
        assertTrue(!mapGlskDocLinearGlsk.isEmpty());

        for (String country : mapGlskDocLinearGlsk.keySet()) {
            DataChronology<LinearGlsk> dataChronology = mapGlskDocLinearGlsk.get(country);
            assertTrue(dataChronology.getDataForInstant(Instant.parse("2018-08-29T21:00:00Z")).isPresent());
            assertFalse(dataChronology.getDataForInstant(Instant.parse("2018-08-26T21:00:00Z")).isPresent());
        }
    }

    @Test
    public void testConvertGlskDocumentToLinearGlskDataChronologyFromFilePathString() throws ParserConfigurationException, SAXException, IOException {
        String filepathstring = "src/test/resources/GlskB42CountryIIDM.xml";
        assertTrue(!new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFilePathString(filepathstring, testNetwork).isEmpty());
    }

    @Test
    public void testConvertGlskDocumentToLinearGlskDataChronologyFromFilePath() throws ParserConfigurationException, SAXException, IOException {
        Path pathtest = Paths.get("src/test/resources/GlskB42CountryIIDM.xml");
        assertTrue(!new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFilePath(pathtest, testNetwork).isEmpty());
    }
}
