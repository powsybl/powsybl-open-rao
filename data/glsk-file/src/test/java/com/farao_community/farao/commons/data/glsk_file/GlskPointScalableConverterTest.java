/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file;

import com.farao_community.farao.commons.data.glsk_file.actors.GlskDocumentImporter;
import com.farao_community.farao.commons.data.glsk_file.actors.GlskPointScalableConverter;
import com.farao_community.farao.commons.data.glsk_file.actors.TypeGlskFile;
import com.google.common.math.DoubleMath;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
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
public class GlskPointScalableConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointScalableConverterTest.class);
    private static final String GLSKB42TEST = "/GlskB42test.xml";
    private static final String GLSKB45TEST = "/GlskB45test.xml";
    private static final String GLSKB42COUNTRYIIDM = "/GlskB42CountryIIDM.xml";
    private static final String GLSKB42COUNTRYGSKLSK = "/GlskB42CountryGskLsk.xml";
    private static final String GLSKB42COUNTRYQUANTITY = "/GlskB42CountryQuantity.xml";
    private static final String GLSKB42EXPLICITIIDM = "/GlskB42ExplicitIIDM.xml";
    private static final String GLSKB42EXPLICITGSKLSK = "/GlskB42ExplicitGskLsk.xml";
    private static final String GLSKB43 = "/GlskB43ParticipationFactorIIDM.xml";
    private static final String GLSKB43GSKLSK = "/GlskB43ParticipationFactorGskLsk.xml";

    private Network testNetwork;
    private GlskPoint glskPointCountry;
    private GlskPoint glskPointCountryGskLsk;
    private GlskPoint glskPointCountryQuantity;
    private GlskPoint glskPointExplicit;
    private GlskPoint glskPointExplicitGskLsk;
    private GlskPoint glskPointParticipationFactor;
    private GlskPoint glskPointParticipationFactorGskLsk;
    private GlskPoint glskMeritOrder;

    @Before
    public void setUp() throws ParserConfigurationException, SAXException, IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        GlskDocumentImporter importer = new GlskDocumentImporter();
        glskPointCountry = importer.importGlskDocumentWithFilename(GLSKB42COUNTRYIIDM).getGlskPoints().get(0);
        glskPointCountryGskLsk = importer.importGlskDocumentWithFilename(GLSKB42COUNTRYGSKLSK).getGlskPoints().get(0);
        glskPointCountryQuantity = importer.importGlskDocumentWithFilename(GLSKB42COUNTRYQUANTITY).getGlskPoints().get(0);
        glskPointExplicit = importer.importGlskDocumentWithFilename(GLSKB42EXPLICITIIDM).getGlskPoints().get(0);
        glskPointExplicitGskLsk = importer.importGlskDocumentWithFilename(GLSKB42EXPLICITGSKLSK).getGlskPoints().get(0);
        glskPointParticipationFactor = importer.importGlskDocumentWithFilename(GLSKB43).getGlskPoints().get(0);
        glskPointParticipationFactorGskLsk = importer.importGlskDocumentWithFilename(GLSKB43GSKLSK).getGlskPoints().get(0);
        glskMeritOrder = importer.importGlskDocumentWithFilename(GLSKB45TEST).getGlskPoints().get(0);

    }

    /**
     *  test for Scalable
     */
    @Test
    public void testConvertGlskPointToScalableB45MeritOrder() {
        Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(testNetwork, glskMeritOrder, TypeGlskFile.CIM);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(6, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB42Country() {
        Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(testNetwork, glskPointCountry, TypeGlskFile.CIM);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB42CountryGskLsk() {
        Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(testNetwork, glskPointCountryGskLsk, TypeGlskFile.CIM);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB42ExplicitGskLsk() {
        Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(testNetwork, glskPointExplicitGskLsk, TypeGlskFile.CIM);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB43GskLsk() {
        Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(testNetwork, glskPointParticipationFactorGskLsk, TypeGlskFile.CIM);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB42Explicit() {
        Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(testNetwork, glskPointExplicit, TypeGlskFile.CIM);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB43() {
        Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(testNetwork, glskPointParticipationFactor, TypeGlskFile.CIM);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }
}
