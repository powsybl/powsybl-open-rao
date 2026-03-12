/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.refprog.referenceprogram;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.EICode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CountryNetPositionComputationTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private EICode eiCodeFrance;
    private EICode eiCodeBelgium;
    private EICode eiCodeNetherlands;
    private EICode eiCodeGermany;

    @BeforeEach
    public void setUp() {
        eiCodeFrance = new EICode(Country.FR);
        eiCodeBelgium = new EICode(Country.BE);
        eiCodeNetherlands = new EICode(Country.NL);
        eiCodeGermany = new EICode(Country.DE);
    }

    @Test
    void testLines() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Map<EICode, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(1000.0, netPositions.get(eiCodeFrance), DOUBLE_TOLERANCE);
        assertEquals(1500.0, netPositions.get(eiCodeBelgium), DOUBLE_TOLERANCE);
        assertEquals(0.0, netPositions.get(eiCodeNetherlands), DOUBLE_TOLERANCE);
        assertEquals(-2500.0, netPositions.get(eiCodeGermany), DOUBLE_TOLERANCE);
    }

    @Test
    void testDanglingLines() {
        Network network = Network.read("TestCaseDangling.xiidm", getClass().getResourceAsStream("/TestCaseDangling.xiidm"));
        Map<EICode, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(0.0, netPositions.get(eiCodeFrance), DOUBLE_TOLERANCE);
        assertEquals(300.0, netPositions.get(eiCodeBelgium), DOUBLE_TOLERANCE);
    }

    @Test
    void testHvdcLines() {
        Network network = Network.read("TestCaseHvdc.xiidm", getClass().getResourceAsStream("/TestCaseHvdc.xiidm"));
        Map<EICode, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(272.0, netPositions.get(eiCodeFrance), DOUBLE_TOLERANCE);
        assertEquals(-272.0, netPositions.get(eiCodeGermany), DOUBLE_TOLERANCE);
    }

}
