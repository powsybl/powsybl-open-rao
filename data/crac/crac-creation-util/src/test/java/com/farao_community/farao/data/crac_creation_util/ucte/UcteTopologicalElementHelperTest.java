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

        UcteTopogicalElementHelper topoHelper = new UcteTopogicalElementHelper("BBE1AA1", "BBE4AA1", "1", null, networkHelper);
        assertTrue(topoHelper.isValid());
        assertEquals("BBE1AA1  BBE4AA1  1", topoHelper.getIdInNetwork());

        topoHelper = new UcteTopogicalElementHelper("BBE4AA1", "BBE1AA1", "1", null, networkHelper);
        assertTrue(topoHelper.isValid());
        assertEquals("BBE1AA1  BBE4AA1  1", topoHelper.getIdInNetwork());
    }
}
