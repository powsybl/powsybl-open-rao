/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.optimization.linear.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.optimization.linear.algorithms.Loutre;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the test cases of {@code CastorFullOptimizationTest} whose CRACs contain no network action
 * with the {@link Loutre} and checks that the results are the same as with Castor.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class LoutreTest {
    private Crac crac;
    private RaoInput raoInput;

    private void setup(String networkFile, String cracFile) throws IOException {
        Network network = Network.read(networkFile, getClass().getResourceAsStream("/network/" + networkFile));
        crac = Crac.read(cracFile, getClass().getResourceAsStream("/crac/" + cracFile), network);
        raoInput = RaoInput.build(network, crac).build();
    }

    private RaoResult runLinearRao(String parametersFile) {
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/" + parametersFile), ReportNode.NO_OP);
        return new Loutre().run(raoInput, raoParameters, null, ReportNode.NO_OP).join();
    }

    @Test
    void testRaoWithEmptyCrac() throws IOException {
        setup("4Nodes.uct", "empty-crac.json");
        RaoResult raoResult = runLinearRao("RaoParameters_2P_v2.json");
        assertNotNull(raoResult);
        // When no cnec is present, a default value of -1e9 is returned
        assertEquals(-1e9, raoResult.getCost(null));
    }

    @Test
    void preventiveOnlyPstOptimization() throws IOException {
        // CRAC without network action, curative remedial action nor HVDC range action (US 4.3.1)
        setup("TestCase12Nodes.uct", "SL_ep4us3.json");
        RaoResult raoResult = runLinearRao("RaoParameters_posMargin_ampere.json");
        assertNotNull(raoResult);
        assertEquals(-143.83, raoResult.getFunctionalCost(crac.getLastInstant()), 1e-1);
        assertTrue(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).isEmpty());
        assertTrue(raoResult.getCost(crac.getLastInstant()) <= raoResult.getCost(null) + 1e-6);
    }

    @Test
    void preventiveOnlyPstAndRedispatchingOptimization() throws IOException {
        // CRAC without network action, curative remedial action nor HVDC range action, with a PST and a redispatching range action
        setup("2Nodes2ParallelLinesPST_1000MW.uct", "crac-pst-rd-0030.json");
        RaoResult raoResult = runLinearRao("RaoParameters_minCost_megawatt_dc_with_offset.json");
        assertNotNull(raoResult);
        assertTrue(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).isEmpty());
        assertTrue(raoResult.getCost(crac.getLastInstant()) <= raoResult.getCost(crac.getInstant("preventive")) + 1e-6);
        assertTrue(raoResult.getCost(crac.getLastInstant()) <= raoResult.getCost(null) + 1e-6);
    }

    @Test
    void purelyVirtualCurative() throws IOException {
        setup("small-network-2P.uct", "small-crac-purely-virtual-curative.json");
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_secure.json"), ReportNode.NO_OP);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);
        RaoResult raoResult = new Loutre().run(raoInput, raoParameters, null, ReportNode.NO_OP).join();
        assertEquals(-13, raoResult.getOptimizedTapOnState(crac.getState("N-1 NL1-NL3", crac.getLastInstant()), crac.getPstRangeAction("CRA_PST_BE")));
    }
}
