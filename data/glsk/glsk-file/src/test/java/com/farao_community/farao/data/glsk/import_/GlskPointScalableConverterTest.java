/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.data.glsk.import_.cim_glsk_document.CimGlskDocument;
import com.farao_community.farao.data.glsk.import_.converters.GlskPointScalableConverter;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.AbstractGlskPoint;
import com.google.common.math.DoubleMath;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskPointScalableConverterTest {
    private static final String GLSKB45TEST = "/GlskB45test.xml";
    private static final String GLSKB42COUNTRYIIDM = "/GlskB42CountryIIDM.xml";
    private static final String GLSKB42COUNTRYGSKLSK = "/GlskB42CountryGskLsk.xml";
    private static final String GLSKB42COUNTRYQUANTITY = "/GlskB42CountryQuantity.xml";
    private static final String GLSKB42EXPLICITIIDM = "/GlskB42ExplicitIIDM.xml";
    private static final String GLSKB42EXPLICITGSKLSK = "/GlskB42ExplicitGskLsk.xml";
    private static final String GLSKB43 = "/GlskB43ParticipationFactorIIDM.xml";
    private static final String GLSKB43GSKLSK = "/GlskB43ParticipationFactorGskLsk.xml";

    private Network testNetwork;
    private AbstractGlskPoint glskPointCountry;
    private AbstractGlskPoint glskPointCountryGskLsk;
    private AbstractGlskPoint glskPointExplicit;
    private AbstractGlskPoint glskPointExplicitGskLsk;
    private AbstractGlskPoint glskPointParticipationFactor;
    private AbstractGlskPoint glskPointParticipationFactorGskLsk;
    private AbstractGlskPoint glskMeritOrder;

    private InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Before
    public void setUp() throws IOException, SAXException, ParserConfigurationException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        glskPointCountry = new CimGlskDocument(getResourceAsStream(GLSKB42COUNTRYIIDM)).getGlskPoints().get(0);
        glskPointCountryGskLsk = new CimGlskDocument(getResourceAsStream(GLSKB42COUNTRYGSKLSK)).getGlskPoints().get(0);
        glskPointExplicit = new CimGlskDocument(getResourceAsStream(GLSKB42EXPLICITIIDM)).getGlskPoints().get(0);
        glskPointExplicitGskLsk = new CimGlskDocument(getResourceAsStream(GLSKB42EXPLICITGSKLSK)).getGlskPoints().get(0);
        glskPointParticipationFactor = new CimGlskDocument(getResourceAsStream(GLSKB43)).getGlskPoints().get(0);
        glskPointParticipationFactorGskLsk = new CimGlskDocument(getResourceAsStream(GLSKB43GSKLSK)).getGlskPoints().get(0);
        glskMeritOrder = new CimGlskDocument(getResourceAsStream(GLSKB45TEST)).getGlskPoints().get(0);

    }

    /**
     *  test for Scalable
     */
    @Test
    public void testConvertGlskPointToScalableB45MeritOrder() {
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glskMeritOrder);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(6, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB42Country() {
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glskPointCountry);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB42CountryGskLsk() {
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glskPointCountryGskLsk);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB42ExplicitGskLsk() {
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glskPointExplicitGskLsk);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB43GskLsk() {
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glskPointParticipationFactorGskLsk);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB42Explicit() {
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glskPointExplicit);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }

    @Test
    public void testConvertGlskPointToScalableB43() {
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glskPointParticipationFactor);
        double done = scalable.scale(testNetwork, 100.0);
        assertTrue(DoubleMath.fuzzyEquals(100, done, 0.0001));
    }
}
