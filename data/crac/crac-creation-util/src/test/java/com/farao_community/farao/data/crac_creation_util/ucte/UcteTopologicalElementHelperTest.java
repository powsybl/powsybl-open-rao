package com.farao_community.farao.data.crac_creation_util.ucte;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class UcteTopologicalElementHelperTest {

    private UcteNetworkAnalyzer networkHelper;

    private void setUp(String networkFile) {
        Network network = Importers.loadNetwork(networkFile, getClass().getResourceAsStream("/" + networkFile));
        networkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
    }

    @Test
    public void testSwitch() {
        setUp("TestCase16Nodes_with_different_imax.uct");

        UcteTopologicalElementHelper topoHelper = new UcteTopologicalElementHelper("BBE1AA1", "BBE4AA1", "1", null, networkHelper);
        assertTrue(topoHelper.isValid());
        assertEquals("BBE1AA1  BBE4AA1  1", topoHelper.getIdInNetwork());

        topoHelper = new UcteTopologicalElementHelper("BBE4AA1", "BBE1AA1", "1", null, networkHelper);
        assertTrue(topoHelper.isValid());
        assertEquals("BBE1AA1  BBE4AA1  1", topoHelper.getIdInNetwork());
    }

    @Test
    public void testOtherValidTopologicalElements() {
        setUp("TestCase_severalVoltageLevels_Xnodes_8characters.uct");

        assertTrue(new UcteTopologicalElementHelper("DDE1AA12", "DDE2AA11", "1", null, networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("XBEFR321", "BBE1AA21", null, "TL BE1X", networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("XDE2AL11", "DDE2AA11", null, "DL AL", networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("FFR3AA11", "FFR3AA21", "1", null, networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("BBE3AA12", "BBE2AA11", null, "PST BE", networkHelper).isValid());
    }

    @Test
    public void testOtherConstructor() {
        setUp("TestCase16Nodes_with_different_imax.uct");
        assertTrue(new UcteTopologicalElementHelper("BBE1AA1 ", "BBE4AA1 ", "1", networkHelper).isValid());
        assertTrue(new UcteTopologicalElementHelper("BBE1AA1  BBE4AA1  1", networkHelper).isValid());
    }
}
