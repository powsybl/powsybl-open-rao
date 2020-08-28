/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
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
