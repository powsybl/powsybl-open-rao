package com.farao_community.farao.loopflow_computation;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class CountryNetPositionComputationTest {

    private Network network;

    @Before
    public void setUp() {
        network = Importers.loadNetwork("testCase.xiidm",
                getClass().getResourceAsStream("/testCase.xiidm"));
    }

    @Test
    public void testGetNetPosition() {
        HashMap<Country, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(1000.0, netPositions.get(Country.FR), 1e-3);
        assertEquals(1500.0, netPositions.get(Country.BE), 1e-3);
        assertEquals(0.0, netPositions.get(Country.NL), 1e-3);
        assertEquals(-2500.0, netPositions.get(Country.DE), 1e-3);
    }

}
