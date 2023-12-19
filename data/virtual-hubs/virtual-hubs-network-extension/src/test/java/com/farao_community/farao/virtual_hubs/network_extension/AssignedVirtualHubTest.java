/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs.network_extension;

import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot@rte-france.com>}
 */
class AssignedVirtualHubTest {

    private static final String SMALL_NETWORK_FILE_NAME = "12Nodes_with_Xnodes.xiidm";

    @Test
    void testConstructor() {
        AssignedVirtualHub virtualHub = new AssignedVirtualHubImpl("code", "10XAAAUDHGKAAAAS", false, "12345678", "FR");
        assertEquals("code", virtualHub.getCode());
        assertEquals("10XAAAUDHGKAAAAS", virtualHub.getEic());
        assertFalse(virtualHub.isMcParticipant());
        assertEquals("12345678", virtualHub.getNodeName());
        assertEquals("FR", virtualHub.getRelatedMa());
    }

    @Test
    void testExtensionAdderOnGenerator() {
        Network network = Network.read(SMALL_NETWORK_FILE_NAME, getClass().getResourceAsStream("/" + SMALL_NETWORK_FILE_NAME));
        Generator anyGenerator = network.getGenerators().iterator().next();

        anyGenerator.newExtension(AssignedVirtualHubAdder.class)
            .withCode("CODE__")
            .withEic("19VDUEGOLKAAAAS")
            .withMcParticipant(true)
            .withNodeName("")
            .withRelatedMa("BE")
            .add();

        AssignedVirtualHub<Generator> virtualHub = anyGenerator.getExtension(AssignedVirtualHub.class);

        assertNotNull(virtualHub);
        assertEquals("CODE__", virtualHub.getCode());
        assertEquals("19VDUEGOLKAAAAS", virtualHub.getEic());
        assertTrue(virtualHub.isMcParticipant());
        assertEquals("", virtualHub.getNodeName());
        assertEquals("BE", virtualHub.getRelatedMa());
    }

    @Test
    void testExtensionAdderOnDanglingLine() {
        Network network = Network.read(SMALL_NETWORK_FILE_NAME, getClass().getResourceAsStream("/" + SMALL_NETWORK_FILE_NAME));
        DanglingLine anyDanglingLine = network.getDanglingLines().iterator().next();

        anyDanglingLine.newExtension(AssignedVirtualHubAdder.class)
            .withCode("__CODE")
            .withEic("19VDUEGOLKAAAAS")
            .withMcParticipant(true)
            .withNodeName("UCTENODE")
            .withRelatedMa(null)
            .add();

        AssignedVirtualHub<DanglingLine> virtualHub = anyDanglingLine.getExtension(AssignedVirtualHub.class);

        assertNotNull(virtualHub);
        assertEquals("__CODE", virtualHub.getCode());
        assertEquals("19VDUEGOLKAAAAS", virtualHub.getEic());
        assertTrue(virtualHub.isMcParticipant());
        assertEquals("UCTENODE", virtualHub.getNodeName());
        assertNull(virtualHub.getRelatedMa());
    }
}
