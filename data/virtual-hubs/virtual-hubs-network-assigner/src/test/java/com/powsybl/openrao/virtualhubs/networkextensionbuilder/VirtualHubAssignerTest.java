/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs.networkextensionbuilder;

import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.networkextension.AssignedVirtualHub;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class VirtualHubAssignerTest {

    private Network network;
    private List<VirtualHub> virtualHubs;

    @BeforeEach
    void setUp() {
        network = Network.read("12Nodes_with_Xnodes.xiidm", getClass().getResourceAsStream("/" + "12Nodes_with_Xnodes.xiidm"));
        virtualHubs = new ArrayList<>();
    }

    @Test
    void testAssignerOnRealNode() {
        virtualHubs.add(new VirtualHub("code_vh1", "eic_vh1", true, false, "NNL2AA1 ", new MarketArea("NL", "eic_nl", true, false), null));
        new VirtualHubAssigner(virtualHubs).addVirtualLoads(network);

        Optional<Load> load = network.getLoadStream().filter(l -> l.getExtension(AssignedVirtualHub.class) != null).findFirst();
        assertTrue(load.isPresent());
        assertEquals("eic_vh1_virtualLoad", load.get().getId());

        AssignedVirtualHub virtualHub = load.get().getExtension(AssignedVirtualHub.class);
        assertEquals("eic_vh1", virtualHub.getEic());
        assertEquals("NNL2AA1 ", virtualHub.getNodeName());
    }

    @Test
    void testAssignerOnXNode() {
        virtualHubs.add(new VirtualHub("code_vh2", "eic_vh2", true, false, "X_GBFR1 ", new MarketArea("FR", "eic_fr", true, false), null));
        new VirtualHubAssigner(virtualHubs).addVirtualLoads(network);

        Optional<Load> load = network.getLoadStream().filter(l -> l.getExtension(AssignedVirtualHub.class) != null).findFirst();
        assertTrue(load.isPresent());
        assertEquals("eic_vh2_virtualLoad", load.get().getId());

        AssignedVirtualHub virtualHub = load.get().getExtension(AssignedVirtualHub.class);
        assertEquals("eic_vh2", virtualHub.getEic());
        assertEquals("X_GBFR1 ", virtualHub.getNodeName());
    }

    @Test
    void testAssignerDisconnectedXNode() {

        network.getDanglingLine("FFR1AA1  X_GBFR1  1").getTerminal().disconnect();

        virtualHubs.add(new VirtualHub("code_vh2", "eic_vh2", true, false, "X_GBFR1 ", new MarketArea("FR", "eic_fr", true, false), null));
        new VirtualHubAssigner(virtualHubs).addVirtualLoads(network);

        Optional<Load> load = network.getLoadStream().filter(l -> l.getExtension(AssignedVirtualHub.class) != null).findFirst();
        assertTrue(load.isEmpty());
        assertNull(network.getLoad("eic_vh2_virtualLoad"));
    }

    @Test
    void testAssignerOnSeveralNodes() {
        virtualHubs.add(new VirtualHub("code_vh1", "eic_vh1", true, false, "NNL2AA1 ", new MarketArea("NL", "eic_nl", true, false), null));
        virtualHubs.add(new VirtualHub("code_vh2", "eic_vh2", true, false, "X_GBFR1 ", new MarketArea("FR", "eic_fr", true, false), null));

        new VirtualHubAssigner(virtualHubs).addVirtualLoads(network);

        assertEquals(2L, network.getLoadStream().filter(l -> l.getExtension(AssignedVirtualHub.class) != null).count());
    }

    @Test
    void testAssignerOnNonExistingNode() {
        virtualHubs.add(new VirtualHub("code_vh3", "eic_vh3", true, false, "UNKNOWN_", new MarketArea("FR", "eic_fr", true, false), null));

        new VirtualHubAssigner(virtualHubs).addVirtualLoads(network);

        Optional<Load> load = network.getLoadStream().filter(l -> l.getExtension(AssignedVirtualHub.class) != null).findFirst();
        assertFalse(load.isPresent());
    }
}
