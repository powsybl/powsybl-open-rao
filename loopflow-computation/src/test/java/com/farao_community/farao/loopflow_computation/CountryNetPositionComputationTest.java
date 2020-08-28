package com.farao_community.farao.loopflow_computation;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CountryNetPositionComputationTest {

    @Test
    public void testLines() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Map<Country, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(1000.0, netPositions.get(Country.FR), 1e-3);
        assertEquals(1500.0, netPositions.get(Country.BE), 1e-3);
        assertEquals(0.0, netPositions.get(Country.NL), 1e-3);
        assertEquals(-2500.0, netPositions.get(Country.DE), 1e-3);
    }

    @Test
    public void testDanglingLines() {
        Network network = Importers.loadNetwork("TestCaseDangling.xiidm", getClass().getResourceAsStream("/TestCaseDangling.xiidm"));
        Map<Country, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(0.0, netPositions.get(Country.FR), 1e-3);
        assertEquals(300.0, netPositions.get(Country.BE), 1e-3);
    }

    @Test
    public void testHvdcLines() {
        Network network = Importers.loadNetwork("TestCaseHvdc.xiidm", getClass().getResourceAsStream("/TestCaseHvdc.xiidm"));
        Map<Country, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(272.0, netPositions.get(Country.FR), 1e-3);
        assertEquals(-272.0, netPositions.get(Country.DE), 1e-3);
    }

}
