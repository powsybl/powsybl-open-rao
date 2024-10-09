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

class UcteTopologicalElementHelperTest {

    private UcteNetworkAnalyzer networkHelper;

    private void setUp(String networkFile) {
        Network network = Network.read(networkFile, getClass().getResourceAsStream("/" + networkFile));
        networkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
    }

    @Test
    void testSwitch() {
        setUp("TestCase16Nodes_with_different_imax.uct");

        UcteTopologicalElementHelper topoHelper = new UcteTopologicalElementHelper("BBE1AA1", "BBE4AA1", "1", null, networkHelper);
        assertTrue(topoHelper.isValid());
        assertEquals("BBE1AA1  BBE4AA1  1", topoHelper.getIdInNetwork());

        topoHelper = new UcteTopologicalElementHelper("BBE4AA1", "BBE1AA1", "1", null, networkHelper);
        assertTrue(topoHelper.isValid());
        assertEquals("BBE1AA1  BBE4AA1  1", topoHelper.getIdInNetwork());
    }

    @Test
    void testOtherValidTopologicalElements() {
        setUp("TestCase_severalVoltageLevels_Xnodes_8characters.uct");

        assertTrue(new UcteTopologicalElementHelper("DDE1AA12", "DDE2AA11", "1", null, networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("XBEFR321", "BBE1AA21", null, "TL BE1X", networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("XDE2AL11", "DDE2AA11", null, "DL AL", networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("FFR3AA11", "FFR3AA21", "1", null, networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("BBE3AA12", "BBE2AA11", null, "PST BE", networkHelper).isValid());
    }

    @Test
    void testOtherConstructor() {
        setUp("TestCase16Nodes_with_different_imax.uct");
        assertTrue(new UcteTopologicalElementHelper("BBE1AA1 ", "BBE4AA1 ", "1", networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("BBE1AA1  BBE4AA1  1", networkHelper).isValid());
    }
}
