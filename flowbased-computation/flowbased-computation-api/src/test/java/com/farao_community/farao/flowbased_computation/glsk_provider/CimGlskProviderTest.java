/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.glsk_provider;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * FlowBased Glsk Values Provider Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class CimGlskProviderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CimGlskProviderTest.class);

    private Network testNetwork;
    private Instant instant;

    @Test
    public void run() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2018-08-28T22:00:00Z");
        CimGlskProvider cimGlskProvider = new CimGlskProvider(getClass().getResourceAsStream("/GlskCountry.xml"), testNetwork);
        Map<String, LinearGlsk> map = cimGlskProvider.selectInstant(instant).getAllGlsk(testNetwork);
        assertFalse(map.isEmpty());

        LinearGlsk linearGlsk = cimGlskProvider.getGlsk(testNetwork, "10YBE----------2");
        assertFalse(linearGlsk.getGLSKs().isEmpty());
    }

    @Test
    public void runWithInvalidCountry() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2020-08-28T22:00:00Z");
        CimGlskProvider cimGlskProvider = new CimGlskProvider(getClass().getResourceAsStream("/GlskCountry.xml"), testNetwork, instant);
        assertNull(cimGlskProvider.getGlsk(testNetwork, "10YBE----------2"));
    }
}
