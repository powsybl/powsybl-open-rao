/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class XnodeContingencyTest {
    private Network network;
    private ComputationManager computationManager;
    private XnodeContingency xnodeContingency;

    @Before
    public void setUp() {
        computationManager = LocalComputationManager.getDefault();
        network = Importers.loadNetwork("TestCase12NodesHvdc.uct", getClass().getResourceAsStream("/TestCase12NodesHvdc.uct"));
        xnodeContingency = new XnodeContingency("alegro", Set.of("XLI_OB1A", "XLI_OB1B"));
    }

    @Test
    public void testSynchronizedFlag() {
        XnodeContingency xnodeContingency = new XnodeContingency("test");
        assertTrue(xnodeContingency.getXnodeIds().isEmpty());
        assertFalse(xnodeContingency.isSynchronized());
    }

    @Test(expected = NotSynchronizedException.class)
    public void testUnsynchronizedException() {
        xnodeContingency.getNetworkElements();
    }

    @Test
    public void testSynchronize() {
        xnodeContingency.synchronize(network);
        assertTrue(xnodeContingency.isSynchronized());
        assertEquals(2, xnodeContingency.getNetworkElements().size());
        assertTrue(xnodeContingency.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("DDE3AA1  XLI_OB1A 1")));
        assertTrue(xnodeContingency.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("BBE2AA1  XLI_OB1B 1")));
    }

    @Test
    public void testDesynchronize() {
        xnodeContingency.synchronize(network);
        xnodeContingency.desynchronize();
        assertFalse(xnodeContingency.isSynchronized());
    }

    @Test(expected = NotSynchronizedException.class)
    public void testDesynchronizeException() {
        xnodeContingency.synchronize(network);
        xnodeContingency.desynchronize();
        xnodeContingency.getNetworkElements();
    }

    @Test(expected = AlreadySynchronizedException.class)
    public void testResynchronizeException() {
        xnodeContingency.synchronize(network);
        xnodeContingency.synchronize(network);
    }

    @Test
    public void testResynchronizeOk() {
        xnodeContingency.synchronize(network);
        xnodeContingency.desynchronize();
        assertFalse(xnodeContingency.isSynchronized());
        xnodeContingency.synchronize(network);
        assertTrue(xnodeContingency.isSynchronized());
    }

    @Test
    public void testApply() {
        xnodeContingency.synchronize(network);
        assertFalse(network.getDanglingLine("DDE3AA1  XLI_OB1A 1").getTerminal().connect());
        assertFalse(network.getDanglingLine("BBE2AA1  XLI_OB1B 1").getTerminal().connect());

        xnodeContingency.apply(network, computationManager);
        assertTrue(network.getDanglingLine("DDE3AA1  XLI_OB1A 1").getTerminal().connect());
        assertTrue(network.getDanglingLine("BBE2AA1  XLI_OB1B 1").getTerminal().connect());
    }
}
