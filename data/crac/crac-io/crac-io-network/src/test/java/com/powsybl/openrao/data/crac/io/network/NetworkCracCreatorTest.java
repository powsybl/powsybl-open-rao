/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.network.parameters.CriticalElements;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkCracCreatorTest {

    private CracCreationContext creationContext;
    private Crac crac;

    private static CracCreationParameters getCracCreationParameters() {
        CracCreationParameters genericParameters = new CracCreationParameters();
        genericParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        NetworkCracCreationParameters parameters = new NetworkCracCreationParameters();
        genericParameters.addExtension(NetworkCracCreationParameters.class, parameters);

        parameters.getCriticalElements().setThresholdDefinition(CriticalElements.ThresholdDefinition.FROM_OPERATIONAL_LIMITS);
        parameters.getCriticalElements().setLimitMultiplierPerInstant(
            Map.of("preventive", 0.95, "outage", 1., "curative", 1.1)
        );
        parameters.getCriticalElements().setApplicableLimitDurationPerInstant(Map.of("outage", 60., "curative", Double.POSITIVE_INFINITY));
        return genericParameters;
    }

    private void importCracFrom(String networkName) {
        Network network = Network.read(networkName, getClass().getResourceAsStream("/" + networkName));
        creationContext = new NetworkCracCreator().createCrac(network, getCracCreationParameters());
        creationContext.getCreationReport().printCreationReport();
        crac = creationContext.getCrac();
    }

    @Test
    void testImportUcte() {
        importCracFrom("TestCase12Nodes.uct");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
    }

    @Test
    void testImport1() {
        importCracFrom("testNetwork3BusbarSections.xiidm");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
    }

    @Test
    void testImport2() {
        importCracFrom("network_one_voltage_level.xiidm");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
    }

    @Test
    void testImport3() {
        importCracFrom("TestCase16NodesWith2Hvdc.xiidm");
        assertTrue(creationContext.isCreationSuccessful());
        assertNotNull(crac);
    }
}
