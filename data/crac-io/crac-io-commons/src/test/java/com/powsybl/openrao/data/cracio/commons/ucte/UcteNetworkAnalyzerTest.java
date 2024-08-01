/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UcteNetworkAnalyzerTest {

    @Test
    void testGetter() {
        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes.uct"));
        UcteNetworkAnalyzerProperties properties = new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS);
        UcteNetworkAnalyzer networkAnalyzer = new UcteNetworkAnalyzer(network, properties);

        assertSame(network, networkAnalyzer.getNetwork());
        assertSame(properties, networkAnalyzer.getProperties());
    }

    @Test
    void testBusIdMatchPolicy() {

        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes_8characters.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes_8characters.uct"));

        UcteNetworkAnalyzerProperties properties = new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS);
        UcteNetworkAnalyzer networkAnalyzer = new UcteNetworkAnalyzer(network, properties);
        UcteMatchingResult result = networkAnalyzer.findFlowElement("BBE1AA1", "BBE2AA1", "1");
        assertTrue(result.hasMatched()); // branch BBE1AA11 BBE2AA11 1

        properties = new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES);
        networkAnalyzer = new UcteNetworkAnalyzer(network, properties);
        result = networkAnalyzer.findFlowElement("BBE1AA1", "BBE2AA1", "1");
        assertFalse(result.hasMatched());
    }

    @Test
    void testMethodFindDependingOnSoughtElement1() {

        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        UcteNetworkAnalyzer networkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES));

        // internal branch
        assertTrue(networkAnalyzer.findContingencyElement("BBE1AA11", "BBE2AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findFlowElement("BBE1AA11", "BBE2AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findTopologicalElement("BBE1AA11", "BBE2AA11", "1").hasMatched());
        assertFalse(networkAnalyzer.findPstElement("BBE1AA11", "BBE2AA11", "1").hasMatched());
        assertFalse(networkAnalyzer.findHvdcElement("BBE1AA11", "BBE2AA11", "1").hasMatched());

        // pst
        assertTrue(networkAnalyzer.findContingencyElement("BBE2AA11", "BBE3AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findFlowElement("BBE2AA11", "BBE3AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findTopologicalElement("BBE2AA11", "BBE3AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findPstElement("BBE2AA11", "BBE3AA11", "1").hasMatched());
        assertFalse(networkAnalyzer.findHvdcElement("BBE2AA11", "BBE3AA11", "1").hasMatched());

        // hvdc
        assertTrue(networkAnalyzer.findContingencyElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
        assertFalse(networkAnalyzer.findFlowElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
        assertFalse(networkAnalyzer.findTopologicalElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
        assertFalse(networkAnalyzer.findPstElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
        assertTrue(networkAnalyzer.findHvdcElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
    }
}
