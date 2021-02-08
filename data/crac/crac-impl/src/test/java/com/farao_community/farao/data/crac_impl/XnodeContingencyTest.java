/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
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

    @Test
    public void testApply() {
        xnodeContingency.synchronize(network);
        assertFalse(network.getDanglingLine("DDE3AA1  XLI_OB1A 1").getTerminal().connect());
        assertFalse(network.getDanglingLine("BBE2AA1  XLI_OB1B 1").getTerminal().connect());

        xnodeContingency.apply(network, computationManager);
        assertTrue(network.getDanglingLine("DDE3AA1  XLI_OB1A 1").getTerminal().connect());
        assertTrue(network.getDanglingLine("BBE2AA1  XLI_OB1B 1").getTerminal().connect());
    }

    @Test
    public void testEquals() {
        assertEquals(xnodeContingency, xnodeContingency);
        assertNotEquals(xnodeContingency, null);
        assertNotEquals(xnodeContingency, new ComplexContingency(xnodeContingency.getId()));
        assertNotEquals(xnodeContingency, new XnodeContingency("alegro1", Set.of("XLI_OB1A", "XLI_OB1B")));
        assertNotEquals(xnodeContingency, new XnodeContingency("alegro", Set.of("XLI_OB1C", "XLI_OB1B")));

        XnodeContingency xnodeContingency2;
        xnodeContingency2 = new XnodeContingency("alegro", Set.of("XLI_OB1B", "XLI_OB1A"));
        assertEquals(xnodeContingency, xnodeContingency2);

        xnodeContingency2.synchronize(network);
        assertNotEquals(xnodeContingency, xnodeContingency2);

        xnodeContingency.synchronize(network);
        xnodeContingency2.desynchronize();
        assertNotEquals(xnodeContingency, xnodeContingency2);

        xnodeContingency2.synchronize(network);
        assertEquals(xnodeContingency, xnodeContingency2);

        xnodeContingency2 = new XnodeContingency("alegro", Set.of("XLI_OB1A"));
        xnodeContingency2.synchronize(network);
        assertNotEquals(xnodeContingency, xnodeContingency2);
    }

    @Test
    public void testAddXnode() {
        xnodeContingency = new XnodeContingency("alegro", Set.of("XLI_OB1A"));
        xnodeContingency.synchronize(network);
        assertTrue(xnodeContingency.isSynchronized());
        assertEquals(1, xnodeContingency.getNetworkElements().size());
        xnodeContingency.addXnode("XLI_OB1B");
        assertFalse(xnodeContingency.isSynchronized());
        xnodeContingency.synchronize(network);
        assertEquals(2, xnodeContingency.getNetworkElements().size());
    }

    @Test(expected = FaraoException.class)
    public void testApplyException() {
        xnodeContingency.apply(network, computationManager);
    }

    @Test
    public void testWrongDanglingLine() {
        xnodeContingency = new XnodeContingency("alegro", Set.of("XLI_OB1A_WRONG"));
        xnodeContingency.synchronize(network);
        assertTrue(xnodeContingency.isSynchronized());
        assertEquals(0, xnodeContingency.getNetworkElements().size());
    }

    @Test
    public void testFourArgumentConstructor() {
        NetworkElement ne1 = new NetworkElement("ne1");
        NetworkElement ne2 = new NetworkElement("ne2");
        xnodeContingency = new XnodeContingency("alegro", "alegro", Set.of("XLI_OB1B", "XLI_OB1A"), Set.of(ne1, ne2));
        assertTrue(xnodeContingency.isSynchronized());
        assertEquals(2, xnodeContingency.getXnodeIds().size());
        assertEquals(2, xnodeContingency.getNetworkElements().size());
        assertTrue(xnodeContingency.getNetworkElements().contains(ne1));
        assertTrue(xnodeContingency.getNetworkElements().contains(ne2));
    }

    @Test(expected = FaraoException.class)
    public void testFourArgumentConstructorError() {
        NetworkElement ne = new NetworkElement("ne");
        xnodeContingency = new XnodeContingency("alegro", "alegro", Set.of("XLI_OB1B", "XLI_OB1A"), Set.of(ne));
    }

    @Test
    public void testResynchronize1() {
        NetworkElement ne1 = new NetworkElement("ne1");
        NetworkElement ne2 = new NetworkElement("ne2");
        xnodeContingency = new XnodeContingency("alegro", "alegro", Set.of("XLI_OB1B", "XLI_OB1A"), Set.of(ne1, ne2));
        assertTrue(xnodeContingency.isSynchronized());
        xnodeContingency.synchronize(network);
        assertTrue(xnodeContingency.isSynchronized());
        assertEquals(2, xnodeContingency.getNetworkElements().size());
        assertTrue(xnodeContingency.getNetworkElements().contains(ne1));
        assertTrue(xnodeContingency.getNetworkElements().contains(ne2));
    }

    @Test
    public void testResynchronize2() {
        NetworkElement ne1 = new NetworkElement("ne1");
        NetworkElement ne2 = new NetworkElement("ne2");
        xnodeContingency = new XnodeContingency("alegro", "alegro", Set.of("XLI_OB1B", "XLI_OB1A"), Set.of(ne1, ne2));
        assertTrue(xnodeContingency.isSynchronized());
        xnodeContingency.desynchronize();
        assertFalse(xnodeContingency.isSynchronized());
        xnodeContingency.synchronize(network);
        assertTrue(xnodeContingency.isSynchronized());
        assertEquals(2, xnodeContingency.getNetworkElements().size());
        assertTrue(xnodeContingency.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("DDE3AA1  XLI_OB1A 1")));
        assertTrue(xnodeContingency.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("BBE2AA1  XLI_OB1B 1")));
    }
}
