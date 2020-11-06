/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.providers;

import com.farao_community.farao.data.glsk.import_.GlskProvider;
import com.farao_community.farao.data.glsk.import_.ucte_glsk_document.UcteGlskDocument;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * FlowBased Glsk Values Provider Test for Ucte format
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteGlskValueProviderTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void testProvideOkUcteGlsk() throws IOException, SAXException, ParserConfigurationException {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        GlskProvider ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml")).getGlskProvider(network);
        assertEquals(3, ucteGlskProvider.getLinearGlsk(instant, "10YFR-RTE------C").getGLSKs().size());
        assertEquals(0.3, ucteGlskProvider.getLinearGlsk(instant, "10YFR-RTE------C").getGLSKs().get("FFR1AA1 _generator"), EPSILON);
    }

    @Test
    public void testProvideUcteGlskEmptyInstant() throws IOException, SAXException, ParserConfigurationException {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2020-07-29T10:00:00Z");

        GlskProvider ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml")).getGlskProvider(network);

        assertTrue(ucteGlskProvider.getLinearGlskPerCountry(instant).isEmpty());
    }

    @Test
    public void testProvideUcteGlskUnknownCountry() throws IOException, SAXException, ParserConfigurationException {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        GlskProvider ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml")).getGlskProvider(network);

        assertNull(ucteGlskProvider.getLinearGlsk(instant, "unknowncountry"));
    }

    @Test
    public void testProvideUcteGlskWithWrongFormat() throws IOException, SAXException, ParserConfigurationException {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        GlskProvider ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml")).getGlskProvider(network);
        assertTrue(ucteGlskProvider.getLinearGlskPerCountry(instant).isEmpty());
    }

}
