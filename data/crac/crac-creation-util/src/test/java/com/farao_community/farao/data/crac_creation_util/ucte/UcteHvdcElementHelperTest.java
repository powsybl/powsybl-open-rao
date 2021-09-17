package com.farao_community.farao.data.crac_creation_util.ucte;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

public class UcteHvdcElementHelperTest {

    private UcteNetworkAnalyzer networkHelper;

    private void setUp(String networkFile) {
        Network network = Importers.loadNetwork(networkFile, getClass().getResourceAsStream("/" + networkFile));
        networkHelper = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
    }

    @Test
    public void testValidHvdc() {
        setUp("TestCase16NodesWithHvdc.xiidm");

        // from/to same as network
        UcteHvdcElementHelper hvdcHelper = new UcteHvdcElementHelper("BBE2AA11", "FFR3AA11", "1", null, networkHelper);
        assertTrue(hvdcHelper.isValid());
        assertEquals("BBE2AA11 FFR3AA11 1", hvdcHelper.getIdInNetwork());
        assertFalse("BBE2AA11 FFR3AA11 1", hvdcHelper.isInvertedInNetwork());

        // inverted from/to
        hvdcHelper = new UcteHvdcElementHelper("FFR3AA11", "BBE2AA11", "1", null, networkHelper);
        assertTrue(hvdcHelper.isValid());
        assertEquals("BBE2AA11 FFR3AA11 1", hvdcHelper.getIdInNetwork());
        assertTrue("BBE2AA11 FFR3AA11 1", hvdcHelper.isInvertedInNetwork());
    }

    @Test
    public void testInvalidHvdc() {
        setUp("TestCase16NodesWithHvdc.xiidm");

        // invalid order code
        assertFalse(new UcteHvdcElementHelper("BBE2AA11", "FFR3AA11", "2", null, networkHelper).isValid());

        // not an hvdc
        assertFalse(new UcteHvdcElementHelper("BBE1AA11", "BBE2AA11", "1", null, networkHelper).isValid());
    }

    @Test
    public void testOtherConstructor() {
        setUp("TestCase16NodesWithHvdc.xiidm");
        assertTrue(new UcteHvdcElementHelper("FFR3AA11", "BBE2AA11", "1", networkHelper).isValid());
        assertTrue(new UcteHvdcElementHelper("BBE2AA11 FFR3AA11 1", networkHelper).isValid());
    }

}
