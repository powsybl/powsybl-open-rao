/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.*;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class ContingencyTest {

    private Network network;
    private ComputationManager computationManager;

    @BeforeEach
    public void setUp() {
        computationManager = LocalComputationManager.getDefault();
        network = Network.read("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
    }

    @Test
    void testEquals() {
        // Rao expect contingency to be uniquely identified by their id, and if so, their name and set (java set) of contingency elements should be equals,
        // which is verified in ContingencyAdder by raising exception when equals is false while id are equals,
        // so check that the equals is false in case of same id but different names or set of elements
        Contingency co1 = new Contingency("co", "coName", new BusbarSectionContingency("bbs"), new LineContingency("l", "vl"));

        Contingency co2 = new Contingency("co", "coName", new BusbarSectionContingency("bbs"), new LineContingency("l", "vl"));
        assertEquals(co1, co2);
        assertEquals(co1.hashCode(), co2.hashCode());

        // diff in id
        Contingency co3 = new Contingency("co2", "coName", new BusbarSectionContingency("bbs"), new LineContingency("l", "vl"));
        assertNotEquals(co1, co3);
        assertNotEquals(co1.hashCode(), co3.hashCode());
        // diff in name
        Contingency co4 = new Contingency("co", "coName2", new BusbarSectionContingency("bbs"), new LineContingency("l", "vl"));
        assertNotEquals(co1, co4);
        assertNotEquals(co1.hashCode(), co4.hashCode());
        // without name
        Contingency co5 = new Contingency("co", new BusbarSectionContingency("bbs"), new LineContingency("l", "vl"));
        assertNotEquals(co1, co5);
        assertNotEquals(co1.hashCode(), co5.hashCode());
        // without elements
        Contingency co6 = new Contingency("co", "coName");
        assertNotEquals(co1, co6);
        assertNotEquals(co1.hashCode(), co6.hashCode());
        // change elements type
        Contingency co9 = new Contingency("co", "coName", new GeneratorContingency("bbs"), new LineContingency("l", "vl"));
        assertNotEquals(co1, co9);
        assertNotEquals(co1.hashCode(), co9.hashCode());
        // change elements id
        Contingency co10 = new Contingency("co", "coName", new BusbarSectionContingency("bbs2"), new LineContingency("l", "vl"));
        assertNotEquals(co1, co10);
        assertNotEquals(co1.hashCode(), co10.hashCode());
    }

    @Test
    void testApplyFails() {
        Contingency contingencyImpl = new Contingency("contingency", "contingency", Collections.singletonList(new BranchContingency("None")));
        assertEquals(1, contingencyImpl.getElements().size());
        assertFalse(contingencyImpl.isValid(network));
    }

    @Test
    void testApplyOnBranch() {
        Contingency contingencyImpl = new Contingency("contingency", "contingency", Collections.singletonList(new BranchContingency("FRANCE_BELGIUM_1")));
        assertEquals(1, contingencyImpl.getElements().size());
        assertFalse(network.getBranch("FRANCE_BELGIUM_1").getTerminal1().connect());
        contingencyImpl.isValid(network);
        contingencyImpl.toModification().apply(network, computationManager);
        assertTrue(network.getBranch("FRANCE_BELGIUM_1").getTerminal1().connect());
    }

    @Test
    void testApplyOnGenerator() {
        Contingency contingencyImpl = new Contingency("contingency", "contingency", Collections.singletonList(ContingencyElement.of(network.getIdentifiable("GENERATOR_FR_2"))));
        assertEquals(1, contingencyImpl.getElements().size());
        assertTrue(network.getGenerator("GENERATOR_FR_2").getTerminal().isConnected());
        contingencyImpl.isValid(network);
        contingencyImpl.toModification().apply(network, computationManager);
        assertFalse(network.getGenerator("GENERATOR_FR_2").getTerminal().isConnected());
    }

    @Test
    void testApplyOnDanglingLine() {
        network = Network.read("TestCase12NodesHvdc.uct", getClass().getResourceAsStream("/TestCase12NodesHvdc.uct"));
        Contingency contingencyImpl = new Contingency("contingency", "contingency", Collections.singletonList(ContingencyElement.of(network.getIdentifiable("BBE2AA1  XLI_OB1B 1"))));
        assertEquals(1, contingencyImpl.getElements().size());
        assertTrue(network.getDanglingLine("BBE2AA1  XLI_OB1B 1").getTerminal().isConnected());
        contingencyImpl.isValid(network);
        contingencyImpl.toModification().apply(network, computationManager);
        assertFalse(network.getDanglingLine("BBE2AA1  XLI_OB1B 1").getTerminal().isConnected());
    }
}
