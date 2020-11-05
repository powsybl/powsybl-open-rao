/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.data.glsk.import_.converters.GlskPointLinearGlskConverter;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.TypeGlskFile;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskPoint;
import com.google.common.math.DoubleMath;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.junit.Assert.assertTrue;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class GlskPointLinearGlskConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointLinearGlskConverterTest.class);
    private static final String GLSKB42COUNTRYIIDM = "/GlskB42CountryIIDM.xml";
    private static final String GLSKB42COUNTRYQUANTITY = "/GlskB42CountryQuantity.xml";
    private static final String GLSKB42EXPLICITIIDM = "/GlskB42ExplicitIIDM.xml";
    private static final String GLSKB42EXPLICITGSKLSK = "/GlskB42ExplicitGskLsk.xml";
    private static final String GLSKB43 = "/GlskB43ParticipationFactorIIDM.xml";
    private static final String GLSKB43GSKLSK = "/GlskB43ParticipationFactorGskLsk.xml";

    private Network testNetwork;
    private GlskPoint glskPointCountry;
    private GlskPoint glskPointCountryQuantity;
    private GlskPoint glskPointExplicitGskLsk;
    private GlskPoint glskPointParticipationFactorGskLsk;

    private InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        glskPointCountry = CimGlskImporter.importGlsk(getResourceAsStream(GLSKB42COUNTRYIIDM)).getGlskPoints().get(0);
        glskPointCountryQuantity = CimGlskImporter.importGlsk(getResourceAsStream(GLSKB42COUNTRYQUANTITY)).getGlskPoints().get(0);
        glskPointExplicitGskLsk = CimGlskImporter.importGlsk(getResourceAsStream(GLSKB42EXPLICITGSKLSK)).getGlskPoints().get(0);
        glskPointParticipationFactorGskLsk = CimGlskImporter.importGlsk(getResourceAsStream(GLSKB43GSKLSK)).getGlskPoints().get(0);
    }

    /**
     *  tests for LinearGlsk
     */
    @Test
    public void testConvertGlskPointToLinearGlskB42Country() {

        LinearGlsk linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glskPointCountry, TypeGlskFile.CIM);
        linearGlsk.getGLSKs().forEach((k, v) -> LOGGER.info("GenCountry: " + k + "; factor = " + v)); //log
        double totalfactor = linearGlsk.getGLSKs().values().stream().mapToDouble(Double::valueOf).sum();
        assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    public void testConvertGlskPointToLinearGlskB42CountryQuantity() {

        LinearGlsk linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glskPointCountryQuantity, TypeGlskFile.CIM);
        linearGlsk.getGLSKs().forEach((k, v) -> LOGGER.info("Country: " + k + "; factor = " + v)); //log
        double totalfactor = linearGlsk.getGLSKs().values().stream().mapToDouble(Double::valueOf).sum();
        assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    public void testConvertGlskPointToLinearGlskB42Explicit() {
        LinearGlsk linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glskPointExplicitGskLsk, TypeGlskFile.CIM);
        linearGlsk.getGLSKs().forEach((k, v) -> LOGGER.info("Explicit: " + k + "; factor = " + v)); //log
        double totalfactor = linearGlsk.getGLSKs().values().stream().mapToDouble(Double::valueOf).sum();
        assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    public void testConvertGlskPointToLinearGlskB43() {
        LinearGlsk linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glskPointParticipationFactorGskLsk, TypeGlskFile.CIM);
        linearGlsk.getGLSKs().forEach((k, v) -> LOGGER.info("Factor: " + k + "; factor = " + v)); //log
        double totalfactor = linearGlsk.getGLSKs().values().stream().mapToDouble(Double::valueOf).sum();
        assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

}
