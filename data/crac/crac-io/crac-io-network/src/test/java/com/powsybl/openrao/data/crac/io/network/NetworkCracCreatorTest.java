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
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkCracCreatorTest {

    @Test
    void testCorrectImport() {
        Network network = Network.read("TestCase16NodesWith2Hvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWith2Hvdc.xiidm"));
        CracCreationParameters genericParameters = new CracCreationParameters();
        genericParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
        NetworkCracCreationParameters parameters = new NetworkCracCreationParameters();
        genericParameters.addExtension(NetworkCracCreationParameters.class, parameters);
        CracCreationContext ccc = new NetworkCracCreator().createCrac(network, genericParameters);
        Crac crac = ccc.getCrac();
        ccc.getCreationReport().printCreationReport();
        int x = 1;
    }
}
