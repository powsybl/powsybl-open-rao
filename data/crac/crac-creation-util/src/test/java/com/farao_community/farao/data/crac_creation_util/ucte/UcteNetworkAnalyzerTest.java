/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util.ucte;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static com.farao_community.farao.data.crac_creation_util.ucte.UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES;
import static com.farao_community.farao.data.crac_creation_util.ucte.UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkAnalyzerTest {

    @Test
    public void testGetter() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes.uct"));
        UcteNetworkAnalyzerProperties properties = new UcteNetworkAnalyzerProperties(COMPLETE_WITH_WILDCARDS);
        UcteNetworkAnalyzer networkAnalyzer = new UcteNetworkAnalyzer(network, properties);

        assertSame(network, networkAnalyzer.getNetwork());
        assertSame(properties, networkAnalyzer.getProperties());
    }

    @Test
    public void testBusIdMatchPolicy() {

        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes_8characters.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes_8characters.uct"));

        UcteNetworkAnalyzerProperties properties = new UcteNetworkAnalyzerProperties(COMPLETE_WITH_WILDCARDS);
        UcteNetworkAnalyzer networkAnalyzer = new UcteNetworkAnalyzer(network, properties);
        UcteMatchingResult result = networkAnalyzer.findCnecElement("BBE1AA1", "BBE2AA1", "1");
        assertTrue(result.hasMatched()); // branch BBE1AA11 BBE2AA11 1

        properties = new UcteNetworkAnalyzerProperties(COMPLETE_WITH_WHITESPACES);
        networkAnalyzer = new UcteNetworkAnalyzer(network, properties);
        result = networkAnalyzer.findCnecElement("BBE1AA1", "BBE2AA1", "1");
        assertFalse(result.hasMatched());
    }

    @Test
    public void testMethodFindDependingOnSoughtElement1() {

        Network network = Importers.loadNetwork("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        UcteNetworkAnalyzer networkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(COMPLETE_WITH_WHITESPACES));

        // internal branch
        assertTrue(networkAnalyzer.findContingencyElement("BBE1AA11", "BBE2AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findCnecElement("BBE1AA11", "BBE2AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findTopologicalElement("BBE1AA11", "BBE2AA11", "1").hasMatched());
        assertFalse(networkAnalyzer.findPstElement("BBE1AA11", "BBE2AA11", "1").hasMatched());
        assertFalse(networkAnalyzer.findHvdcElement("BBE1AA11", "BBE2AA11", "1").hasMatched());

        // pst
        assertTrue(networkAnalyzer.findContingencyElement("BBE2AA11", "BBE3AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findCnecElement("BBE2AA11", "BBE3AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findTopologicalElement("BBE2AA11", "BBE3AA11", "1").hasMatched());
        assertTrue(networkAnalyzer.findPstElement("BBE2AA11", "BBE3AA11", "1").hasMatched());
        assertFalse(networkAnalyzer.findHvdcElement("BBE2AA11", "BBE3AA11", "1").hasMatched());

        // hvdc
        assertTrue(networkAnalyzer.findContingencyElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
        assertFalse(networkAnalyzer.findCnecElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
        assertFalse(networkAnalyzer.findTopologicalElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
        assertFalse(networkAnalyzer.findPstElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
        assertTrue(networkAnalyzer.findHvdcElement("BBE2AA11", "FFR3AA1*", "1").hasMatched());
    }
}
