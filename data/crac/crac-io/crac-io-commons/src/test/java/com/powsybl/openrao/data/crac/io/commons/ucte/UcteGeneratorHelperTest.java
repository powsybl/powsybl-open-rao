/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.ucte;

import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UcteGeneratorHelperTest {
    private Network network;
    private UcteNetworkAnalyzer ucteNetworkAnalyzer;

    private void setUp(String networkFileName) {
        network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
    }

    @Test
    void testElementNotInNetwork() {
        setUp("/TestCase12Nodes_forCSE.uct");
        UcteGeneratorHelper generatorHelper = new UcteGeneratorHelper("AAAAAAAA", ucteNetworkAnalyzer);
        assertEquals(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, generatorHelper.getImportStatus());
        assertNull(generatorHelper.getGeneratorId());
        assertEquals("No bus in the network matches bus id AAAAAAAA", generatorHelper.getDetail());
        assertFalse(generatorHelper.isValid());
        assertFalse(generatorHelper.isAltered());
    }

    @Test
    void testOneMatch() {
        setUp("/TestCase12Nodes_forCSE.uct");
        UcteGeneratorHelper generatorHelper = new UcteGeneratorHelper("BBE1AA11", ucteNetworkAnalyzer);
        assertEquals(ImportStatus.IMPORTED, generatorHelper.getImportStatus());
        assertEquals("BBE1AA11_generator", generatorHelper.getGeneratorId());
        assertNull(generatorHelper.getDetail());
        assertTrue(generatorHelper.isValid());
        assertFalse(generatorHelper.isAltered());
        assertEquals(-9000, generatorHelper.getPmin(), 1e-3);
        assertEquals(9000, generatorHelper.getPmax(), 1e-3);
        assertEquals(1500, generatorHelper.getCurrentP(), 1e-3);
    }

    @Test
    void testMultipleBusMatchesButOneGenerator() {
        setUp("/TestCase12Nodes_forCSE.uct");
        UcteGeneratorHelper generatorHelper = new UcteGeneratorHelper("BBE1AA1*", ucteNetworkAnalyzer);
        assertEquals(ImportStatus.IMPORTED, generatorHelper.getImportStatus());
        assertEquals("BBE1AA11_generator", generatorHelper.getGeneratorId());
        assertNull(generatorHelper.getDetail());
        assertTrue(generatorHelper.isValid());
        assertFalse(generatorHelper.isAltered());
    }

    @Test
    void testMultipleGeneratorMatches() {
        setUp("/TestCase12Nodes_forCSE_multipleGenerators.uct");
        UcteGeneratorHelper generatorHelper = new UcteGeneratorHelper("BBE1AA1*", ucteNetworkAnalyzer);
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, generatorHelper.getImportStatus());
        assertNull(generatorHelper.getGeneratorId());
        assertEquals("Too many generators match node name BBE1AA1*", generatorHelper.getDetail());
        assertFalse(generatorHelper.isValid());
        assertFalse(generatorHelper.isAltered());
    }

    @Test
    void testBusHasNoGenerator() {
        setUp("/TestCase12Nodes_forCSE.uct");
        UcteGeneratorHelper generatorHelper = new UcteGeneratorHelper("BBE1AA12", ucteNetworkAnalyzer);
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, generatorHelper.getImportStatus());
        assertNull(generatorHelper.getGeneratorId());
        assertEquals("Buses matching BBE1AA12 in the network do not hold generators", generatorHelper.getDetail());
        assertFalse(generatorHelper.isValid());
        assertFalse(generatorHelper.isAltered());
    }

    @Test
    void testTwoGeneratorsOnOneBus() {
        setUp("/TestCase12Nodes_forCSE.uct");
        ((Bus) network.getIdentifiable("BBE1AA11"))
            .getVoltageLevel()
            .newGenerator()
            .setId("BBE1AA11_second_generator")
            .setBus("BBE1AA11")
            .setMaxP(100)
            .setMinP(0)
            .setTargetP(0)
            .setTargetQ(0)
            .setRatedS(100)
            .setVoltageRegulatorOn(true)
            .setTargetV(430)
            .setConnectableBus("BBE1AA11")
            .setEnsureIdUnicity(true)
            .add();
        UcteGeneratorHelper generatorHelper = new UcteGeneratorHelper("BBE1AA11", ucteNetworkAnalyzer);
        assertEquals(ImportStatus.IMPORTED, generatorHelper.getImportStatus());
        assertEquals("BBE1AA11_generator", generatorHelper.getGeneratorId());
        assertEquals("More than 1 generator associated to BBE1AA11. First generator is selected.", generatorHelper.getDetail());
        assertTrue(generatorHelper.isValid());
        assertTrue(generatorHelper.isAltered());
    }

    @Test
    void testGeneratorNotConnected() {
        setUp("/TestCase12Nodes_forCSE.uct");
        network.getGenerator("BBE1AA11_generator").getTerminal().disconnect();

        UcteGeneratorHelper generatorHelper = new UcteGeneratorHelper("BBE1AA11", ucteNetworkAnalyzer);
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, generatorHelper.getImportStatus());
        assertNull(generatorHelper.getGeneratorId());
        assertEquals("Buses matching BBE1AA11 in the network do not hold generators", generatorHelper.getDetail());
        assertFalse(generatorHelper.isValid());
        assertFalse(generatorHelper.isAltered());
    }

    @Test
    void testGeneratorNotInMainComponent() {
        setUp("/TestCase12Nodes_forCSE.uct");
        network.getBranch("NNL1AA1  NNL2AA1  1").getTerminal1().disconnect();
        network.getBranch("NNL1AA1  NNL3AA1  1").getTerminal1().disconnect();

        UcteGeneratorHelper generatorHelper = new UcteGeneratorHelper("NNL1AA1 ", ucteNetworkAnalyzer);
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, generatorHelper.getImportStatus());
        assertNull(generatorHelper.getGeneratorId());
        assertEquals("Buses matching NNL1AA1  in the network do not hold generators connected to the main grid", generatorHelper.getDetail());
        assertFalse(generatorHelper.isValid());
        assertFalse(generatorHelper.isAltered());
    }
}
