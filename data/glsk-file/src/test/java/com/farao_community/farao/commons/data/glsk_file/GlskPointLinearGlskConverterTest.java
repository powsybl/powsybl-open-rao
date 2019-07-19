/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file;

import com.farao_community.farao.commons.data.glsk_file.actors.GlskDocumentImporter;
import com.farao_community.farao.commons.data.glsk_file.actors.GlskPointLinearGlskConverter;
import com.google.common.math.DoubleMath;
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

import static org.junit.Assert.assertTrue;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskPointLinearGlskConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointLinearGlskConverterTest.class);
    private static final String GLSKB42TEST = "/GlskB42test.xml";
    private static final String GLSKB42COUNTRYIIDM = "/GlskB42CountryIIDM.xml";
    private static final String GLSKB42COUNTRYQUANTITY = "/GlskB42CountryQuantity.xml";
    private static final String GLSKB42EXPLICITIIDM = "/GlskB42ExplicitIIDM.xml";
    private static final String GLSKB42EXPLICITGSKLSK = "/GlskB42ExplicitGskLsk.xml";
    private static final String GLSKB43 = "/GlskB43ParticipationFactorIIDM.xml";
    private static final String GLSKB43GSKLSK = "/GlskB43ParticipationFactorGskLsk.xml";

    private Network testNetwork;
    private GlskPoint glskPointCountry;
    private GlskPoint glskPointCountryQuantity;
    private GlskPoint glskPointExplicit;
    private GlskPoint glskPointExplicitGskLsk;
    private GlskPoint glskPointParticipationFactor;
    private GlskPoint glskPointParticipationFactorGskLsk;

    private static final String TYPE_GLSK_FILE = "CIM";

    @Before
    public void setUp() throws ParserConfigurationException, SAXException, IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        GlskDocumentImporter importer = new GlskDocumentImporter();
        glskPointCountry = importer.importGlskDocumentWithFilename(GLSKB42COUNTRYIIDM).getGlskPoints().get(0);
        glskPointCountryQuantity = importer.importGlskDocumentWithFilename(GLSKB42COUNTRYQUANTITY).getGlskPoints().get(0);
        glskPointExplicit = importer.importGlskDocumentWithFilename(GLSKB42EXPLICITIIDM).getGlskPoints().get(0);
        glskPointExplicitGskLsk = importer.importGlskDocumentWithFilename(GLSKB42EXPLICITGSKLSK).getGlskPoints().get(0);
        glskPointParticipationFactor = importer.importGlskDocumentWithFilename(GLSKB43).getGlskPoints().get(0);
        glskPointParticipationFactorGskLsk = importer.importGlskDocumentWithFilename(GLSKB43GSKLSK).getGlskPoints().get(0);

    }

    /**
     *  tests for LinearGlsk
     */
    @Test
    public void testConvertGlskPointToLinearGlskB42Country() {

        LinearGlsk linearGlsk = new GlskPointLinearGlskConverter().convertGlskPointToLinearGlsk(testNetwork, glskPointCountry, TYPE_GLSK_FILE);
        linearGlsk.getGLSKs().forEach((k, v) -> LOGGER.info("GenCountry: " + k + "; factor = " + v)); //log
        double totalfactor = linearGlsk.getGLSKs().values().stream().mapToDouble(Double::valueOf).sum();
        assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    public void testConvertGlskPointToLinearGlskB42CountryQuantity() {

        LinearGlsk linearGlsk = new GlskPointLinearGlskConverter().convertGlskPointToLinearGlsk(testNetwork, glskPointCountryQuantity, TYPE_GLSK_FILE);
        linearGlsk.getGLSKs().forEach((k, v) -> LOGGER.info("Country: " + k + "; factor = " + v)); //log
        double totalfactor = linearGlsk.getGLSKs().values().stream().mapToDouble(Double::valueOf).sum();
        assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    public void testConvertGlskPointToLinearGlskB42Explicit() {
        LinearGlsk linearGlsk = new GlskPointLinearGlskConverter().convertGlskPointToLinearGlsk(testNetwork, glskPointExplicitGskLsk, TYPE_GLSK_FILE);
        linearGlsk.getGLSKs().forEach((k, v) -> LOGGER.info("Explicit: " + k + "; factor = " + v)); //log
        double totalfactor = linearGlsk.getGLSKs().values().stream().mapToDouble(Double::valueOf).sum();
        assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    public void testConvertGlskPointToLinearGlskB43() {
        LinearGlsk linearGlsk = new GlskPointLinearGlskConverter().convertGlskPointToLinearGlsk(testNetwork, glskPointParticipationFactorGskLsk, TYPE_GLSK_FILE);
        linearGlsk.getGLSKs().forEach((k, v) -> LOGGER.info("Factor: " + k + "; factor = " + v)); //log
        double totalfactor = linearGlsk.getGLSKs().values().stream().mapToDouble(Double::valueOf).sum();
        assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

}
