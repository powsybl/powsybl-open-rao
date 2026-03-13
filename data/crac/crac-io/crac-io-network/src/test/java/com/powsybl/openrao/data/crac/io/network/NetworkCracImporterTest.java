/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkCracImporterTest {
    @Test
    void testExists() {
        // xiidm
        assertTrue(
            new NetworkCracImporter().exists("TestCase16NodesWith2Hvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWith2Hvdc.xiidm"))
        );
        // ucte
        assertTrue(
            new NetworkCracImporter().exists("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"))
        );
        // wrong ucte
        assertFalse(
            new NetworkCracImporter().exists("TestCase12Nodes_wrong.uct", getClass().getResourceAsStream("/TestCase12Nodes_wrong.uct"))
        );
    }

}
