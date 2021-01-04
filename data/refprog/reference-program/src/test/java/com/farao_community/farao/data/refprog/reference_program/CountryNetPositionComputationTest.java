/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CountryNetPositionComputationTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private ReferenceProgramArea referenceProgramAreaFrance;
    private ReferenceProgramArea referenceProgramAreaBelgium;
    private ReferenceProgramArea referenceProgramAreaNetherlands;
    private ReferenceProgramArea referenceProgramAreaGermany;

    @Before
    public void setUp() throws Exception {
        referenceProgramAreaFrance = new ReferenceProgramArea(Country.FR);
        referenceProgramAreaBelgium = new ReferenceProgramArea(Country.BE);
        referenceProgramAreaNetherlands = new ReferenceProgramArea(Country.NL);
        referenceProgramAreaGermany = new ReferenceProgramArea(Country.DE);
    }

    @Test
    public void testLines() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Map<ReferenceProgramArea, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(1000.0, netPositions.get(referenceProgramAreaFrance), DOUBLE_TOLERANCE);
        assertEquals(1500.0, netPositions.get(referenceProgramAreaBelgium), DOUBLE_TOLERANCE);
        assertEquals(0.0, netPositions.get(referenceProgramAreaNetherlands), DOUBLE_TOLERANCE);
        assertEquals(-2500.0, netPositions.get(referenceProgramAreaGermany), DOUBLE_TOLERANCE);
    }

    @Test
    public void testDanglingLines() {
        Network network = Importers.loadNetwork("TestCaseDangling.xiidm", getClass().getResourceAsStream("/TestCaseDangling.xiidm"));
        Map<ReferenceProgramArea, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(0.0, netPositions.get(referenceProgramAreaFrance), DOUBLE_TOLERANCE);
        assertEquals(300.0, netPositions.get(referenceProgramAreaBelgium), DOUBLE_TOLERANCE);
    }

    @Test
    public void testHvdcLines() {
        Network network = Importers.loadNetwork("TestCaseHvdc.xiidm", getClass().getResourceAsStream("/TestCaseHvdc.xiidm"));
        Map<ReferenceProgramArea, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        assertEquals(272.0, netPositions.get(referenceProgramAreaFrance), DOUBLE_TOLERANCE);
        assertEquals(-272.0, netPositions.get(referenceProgramAreaGermany), DOUBLE_TOLERANCE);
    }

}
