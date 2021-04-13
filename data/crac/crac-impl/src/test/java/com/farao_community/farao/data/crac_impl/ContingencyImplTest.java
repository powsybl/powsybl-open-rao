/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ContingencyImplTest {

    private Network network;
    private ComputationManager computationManager;

    @Before
    public void setUp() {
        computationManager = LocalComputationManager.getDefault();
        network = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
    }

    @Test
    public void testDifferentWithDifferentIds() {
        ContingencyImpl contingencyImpl1 = new ContingencyImpl(
            "contingency-1", "contingency",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        ContingencyImpl contingencyImpl2 = new ContingencyImpl(
            "contingency-2", "contingency",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        assertNotEquals(contingencyImpl1, contingencyImpl2);
    }

    @Test
    public void testDifferentWithDifferentObjects() {
        ContingencyImpl contingencyImpl1 = new ContingencyImpl(
            "contingency-1", "contingency-1",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        ContingencyImpl contingencyImpl2 = new ContingencyImpl(
            "contingency-1", "contingency-1",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-5")).collect(Collectors.toSet())
        );

        assertNotEquals(contingencyImpl1, contingencyImpl2);
    }

    @Test
    public void testEqual() {
        ContingencyImpl contingencyImpl1 = new ContingencyImpl(
            "contingency-1", "contingency-1",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        ContingencyImpl contingencyImpl2 = new ContingencyImpl(
            "contingency-1", "contingency-1",
            Stream.of(new NetworkElementImpl("network-element-1"), new NetworkElementImpl("network-element-2")).collect(Collectors.toSet())
        );

        assertEquals(contingencyImpl1, contingencyImpl2);
    }

    @Test(expected = FaraoException.class)
    public void testApplyFails() {
        ContingencyImpl contingencyImpl = new ContingencyImpl("contingency", "contingency", Collections.singleton(new NetworkElementImpl("None")));
        assertEquals(1, contingencyImpl.getNetworkElements().size());
        contingencyImpl.apply(network, computationManager);
    }

    @Test
    public void testApplyOnBranch() {
        ContingencyImpl contingencyImpl = new ContingencyImpl("contingency", "contingency", Collections.singleton(new NetworkElementImpl("FRANCE_BELGIUM_1")));
        assertEquals(1, contingencyImpl.getNetworkElements().size());
        assertFalse(network.getBranch("FRANCE_BELGIUM_1").getTerminal1().connect());
        contingencyImpl.apply(network, computationManager);
        assertTrue(network.getBranch("FRANCE_BELGIUM_1").getTerminal1().connect());
    }

    @Test
    public void testApplyOnGenerator() {
        ContingencyImpl contingencyImpl = new ContingencyImpl("contingency", "contingency", Collections.singleton(new NetworkElementImpl("GENERATOR_FR_2")));
        assertEquals(1, contingencyImpl.getNetworkElements().size());
        assertTrue(network.getGenerator("GENERATOR_FR_2").getTerminal().isConnected());
        contingencyImpl.apply(network, computationManager);
        assertFalse(network.getGenerator("GENERATOR_FR_2").getTerminal().isConnected());
    }

    @Test
    public void testSynchronize() {
        ContingencyImpl contingencyImpl = new ContingencyImpl("contingency", "contingency", Collections.emptySet());
        assertTrue(contingencyImpl.isSynchronized());
        contingencyImpl.desynchronize();
        assertTrue(contingencyImpl.isSynchronized());
        contingencyImpl.synchronize(network);
        assertTrue(contingencyImpl.isSynchronized());
    }

}
