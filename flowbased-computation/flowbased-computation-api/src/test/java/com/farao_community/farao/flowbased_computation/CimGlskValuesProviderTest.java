/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.flowbased_computation.glsk_provider.CimGlskValuesProvider;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * FlowBased Glsk Values Provider Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class CimGlskValuesProviderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CimGlskValuesProviderTest.class);

    private Network testNetwork;
    private Instant instant;

    @Test
    public void run() throws ParserConfigurationException, SAXException, IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2018-08-28T22:00:00Z");
        CimGlskValuesProvider cimGlskValuesProvider = new CimGlskValuesProvider(testNetwork, getClass().getResource("/GlskCountry.xml").getPath());
        Map<String, DataChronology<LinearGlsk> > map = cimGlskValuesProvider.createDataChronologyLinearGlskMap(testNetwork,
                getClass().getResource("/GlskCountry.xml").getPath());
        Assert.assertFalse(map.isEmpty());

        LinearGlsk linearGlsk = cimGlskValuesProvider.getCountryLinearGlsk(instant, "10YBE----------2");
        Assert.assertFalse(linearGlsk.getGLSKs().isEmpty());
        Assert.assertFalse(cimGlskValuesProvider.getFilePathString().isEmpty());
        Assert.assertFalse(cimGlskValuesProvider.getMapCountryDataChronologyLinearGlsk().isEmpty());
        Map<String, LinearGlsk> linearGlskMap = cimGlskValuesProvider.getCountryLinearGlskMap(instant);
        Assert.assertFalse(linearGlskMap.isEmpty());
    }

    @Test (expected = FaraoException.class)
    public void runBis() throws ParserConfigurationException, SAXException, IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2018-08-28T22:00:00Z");
        CimGlskValuesProvider cimGlskValuesProvider = new CimGlskValuesProvider();
        cimGlskValuesProvider.setNetwork(testNetwork);
        Network testgetnetwork = cimGlskValuesProvider.getNetwork();
        cimGlskValuesProvider.setFilePathString(getClass().getResource("/GlskCountry.xml").getPath());
        Map<String, DataChronology<LinearGlsk> > map = cimGlskValuesProvider.createDataChronologyLinearGlskMap(testNetwork,
                getClass().getResource("/GlskCountry.xml").getPath());

        cimGlskValuesProvider.setMapCountryDataChronologyLinearGlsk(map);
        Assert.assertFalse(cimGlskValuesProvider.getCountryLinearGlsk(instant, "10YBE----------2").getGLSKs().isEmpty());
        cimGlskValuesProvider.getCountryLinearGlsk(instant, ""); //(expected = FaraoException.class)
    }

    @Test (expected = FaraoException.class)
    public void runTre() throws ParserConfigurationException, SAXException, IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2020-08-28T22:00:00Z");
        CimGlskValuesProvider cimGlskValuesProvider = new CimGlskValuesProvider(testNetwork, getClass().getResource("/GlskCountry.xml").getPath());
        Map<String, DataChronology<LinearGlsk> > map = cimGlskValuesProvider.createDataChronologyLinearGlskMap(testNetwork,
                getClass().getResource("/GlskCountry.xml").getPath());
        Assert.assertFalse(map.isEmpty());

        LinearGlsk linearGlsk = cimGlskValuesProvider.getCountryLinearGlsk(instant, "10YBE----------2"); //(expected = FaraoException.class)
    }
}
