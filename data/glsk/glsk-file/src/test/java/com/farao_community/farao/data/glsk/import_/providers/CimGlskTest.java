/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.providers;

import com.farao_community.farao.data.glsk.import_.GlskProvider;
import com.farao_community.farao.data.glsk.import_.cim_glsk_document.CimGlskDocument;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * FlowBased Glsk Values Provider Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class CimGlskTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CimGlskTest.class);

    private Network testNetwork;
    private Instant instant;

    @Test
    public void run() throws IOException, SAXException, ParserConfigurationException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2018-08-28T22:00:00Z");
        GlskProvider cimGlskProvider = new CimGlskDocument(getClass().getResourceAsStream("/GlskCountry.xml")).getGlskProvider(testNetwork);
        Map<String, LinearGlsk> map = cimGlskProvider.getLinearGlskPerCountry(instant);
        assertFalse(map.isEmpty());

        LinearGlsk linearGlsk = cimGlskProvider.getLinearGlsk(instant, "10YBE----------2");
        assertFalse(linearGlsk.getGLSKs().isEmpty());
    }

    @Test
    public void runWithInvalidCountry() throws IOException, SAXException, ParserConfigurationException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2020-08-28T22:00:00Z");
        GlskProvider cimGlskProvider = new CimGlskDocument(getClass().getResourceAsStream("/GlskCountry.xml")).getGlskProvider(testNetwork);
        assertNull(cimGlskProvider.getLinearGlsk(instant, "10YBE----------2"));
    }
}
