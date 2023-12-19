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
import com.powsybl.iidm.xml.NetworkXml;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot@rte-france.com>}
 */
class AssignedVirtualHubXmlSerializerTest {

    private static final String SMALL_NETWORK_FILE_NAME = "12Nodes_with_Xnodes.xiidm";

    @Test
    void roundTripTest() {
        // load network
        Network originalNetwork = Network.read(SMALL_NETWORK_FILE_NAME, getClass().getResourceAsStream("/" + SMALL_NETWORK_FILE_NAME));

        // add extensions
        DanglingLine dl = originalNetwork.getDanglingLine("FFR1AA1  X_GBFR1  1");
        Generator g = originalNetwork.getGenerator("NNL1AA1 _generator");

        dl.newExtension(AssignedVirtualHubAdder.class).
            withCode("code1").
            withEic("17YXTYUDHGKAAAAS").
            withMcParticipant(true).
            withNodeName("X_GBFR1 ").
            withRelatedMa("FR").
            add();

        g.newExtension(AssignedVirtualHubAdder.class).
            withCode("code2").
            withEic("15XGDYRHKLKAAAAS").
            withMcParticipant(false).
            withNodeName("NNL3AA1 ").
            withRelatedMa(null).
            add();

        // round trip
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NetworkXml.write(originalNetwork, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        Network roundTripedNetwork = NetworkXml.read(is);

        // check results
        AssignedVirtualHub<DanglingLine> avh1 = roundTripedNetwork.getDanglingLine("FFR1AA1  X_GBFR1  1").getExtension(AssignedVirtualHub.class);
        AssignedVirtualHub<Generator> avh2 = originalNetwork.getGenerator("NNL1AA1 _generator").getExtension(AssignedVirtualHub.class);

        assertNotNull(avh1);
        assertEquals("code1", avh1.getCode());
        assertEquals("17YXTYUDHGKAAAAS", avh1.getEic());
        assertTrue(avh1.isMcParticipant());
        assertEquals("X_GBFR1 ", avh1.getNodeName());
        assertEquals("FR", avh1.getRelatedMa());

        assertNotNull(avh2);
        assertEquals("code2", avh2.getCode());
        assertEquals("15XGDYRHKLKAAAAS", avh2.getEic());
        assertFalse(avh2.isMcParticipant());
        assertEquals("NNL3AA1 ", avh2.getNodeName());
        assertNull(avh2.getRelatedMa());
    }
}

