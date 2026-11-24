/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoStopCondition;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class that ensures that interrupting the RAO during the workflow produces a coherent RAO Result.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class OpenRaoEarlyStoppingTest {

    private static class MaxCallsRaoStopCondition implements RaoStopCondition {
        private int maxCalls;
        private int count = 0; // used for debug, delete before opening the PR

        MaxCallsRaoStopCondition(int maxCalls) {
            this.maxCalls = maxCalls;
        }

        @Override
        public boolean shouldStop() {
            count++;
            System.out.println("Count: " + count);
            maxCalls--;
            return maxCalls <= 0;
        }
    }

    /**
     * CASTOR
     */

    @Test
    void testEarlyStopCastor() throws IOException {
        RaoInput raoInput = createRaoInput();
        RaoParameters raoParameters = getRaoParameters();
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(1);

        RaoResult raoResult = new OpenRao().run(raoInput, raoParameters, ReportNode.NO_OP, raoStopCondition).join();
        assertNotNull(raoResult);
        raoInput.getCrac().getStates().forEach(state -> {
            assertTrue(raoResult.getActivatedNetworkActionsDuringState(state).isEmpty());
            assertTrue(raoResult.getActivatedRangeActionsDuringState(state).isEmpty());
        });

        Metadata metadata = raoResult.getExtension(Metadata.class);
        assertNotNull(metadata);
        Optional<String> executionDetails = metadata.getExecutionDetails();
        assertTrue(executionDetails.isPresent());
        assertEquals("RAO was interrupted before any result could be produced.", executionDetails.get());
    }

    @Test
    void testEarlyStopCastorAfterPreventiveOptimization() throws IOException {
        RaoInput raoInput = createRaoInput();
        RaoParameters raoParameters = getRaoParameters();
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(23); // sometime during curative optimization

        RaoResult raoResult = new OpenRao().run(raoInput, raoParameters, ReportNode.NO_OP, raoStopCondition).join();
        assertNotNull(raoResult);

        State preventiveState = raoInput.getCrac().getPreventiveState();
        assertEquals(2, raoResult.getActivatedNetworkActionsDuringState(preventiveState).size());
        assertTrue(raoResult.isActivated(preventiveState, raoInput.getCrac().getNetworkAction("close_de3_de4")));
        assertTrue(raoResult.isActivated(preventiveState, raoInput.getCrac().getNetworkAction("close_fr1_fr5")));

        raoInput.getCrac().getStates(raoInput.getCrac().getInstant("curative")).forEach(state -> {
            assertTrue(raoResult.getActivatedNetworkActionsDuringState(state).isEmpty());
            assertTrue(raoResult.getActivatedRangeActionsDuringState(state).isEmpty());
        });

        Metadata metadata = raoResult.getExtension(Metadata.class);
        assertNotNull(metadata);
        Optional<String> executionDetails = metadata.getExecutionDetails();
        assertTrue(executionDetails.isPresent());
        assertEquals("RAO was interrupted after preventive optimization and before curative optimization.", executionDetails.get());
    }

    @Test
    void testEarlyStopCastorAfterCurativeOptimization() throws IOException {
        RaoInput raoInput = createRaoInput();
        RaoParameters raoParameters = getRaoParameters();
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(36); // sometime during second preventive optimization

        RaoResult raoResult = new OpenRao().run(raoInput, raoParameters, ReportNode.NO_OP, raoStopCondition).join();
        assertNotNull(raoResult);

        State preventiveState = raoInput.getCrac().getPreventiveState();
        assertEquals(2, raoResult.getActivatedNetworkActionsDuringState(preventiveState).size());
        assertTrue(raoResult.isActivated(preventiveState, raoInput.getCrac().getNetworkAction("close_de3_de4")));
        assertTrue(raoResult.isActivated(preventiveState, raoInput.getCrac().getNetworkAction("close_fr1_fr5")));

        State curativeState = raoInput.getCrac().getState("co1_fr2_fr3_1", raoInput.getCrac().getInstant("curative"));
        assertEquals(1, raoResult.getActivatedNetworkActionsDuringState(curativeState).size());
        assertTrue(raoResult.isActivated(curativeState, raoInput.getCrac().getNetworkAction("open_fr1_fr3")));

        Metadata metadata = raoResult.getExtension(Metadata.class);
        assertNotNull(metadata);
        Optional<String> executionDetails = metadata.getExecutionDetails();
        assertTrue(executionDetails.isPresent());
        assertEquals("RAO was interrupted after contingency scenarios optimization.", executionDetails.get());
    }

    /**
     * FastRAO
     */

    @Test
    void testEarlyStopFastRao() {
        // TODO
    }

    @Test
    void testEarlyStopFastRaoDuringLoops() {
        // TODO
    }

    /**
     * MARMOT
     */

    @Test
    void testEarlyStopMarmot() {
        // TODO
    }

    @Test
    void testEarlyStopMarmotAfterTopologicalOptimization() {
        // TODO
    }

    @Test
    void testEarlyStopMarmotDuringLoops() {
        // TODO
    }

    private static RaoInput createRaoInput() throws IOException {
        Network network = Network.read("small-network-2P.uct", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/network/small-network-2P.uct"));
        Crac crac = Crac.read("small-crac-2P.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/small-crac-2P.json"), network);
        return RaoInput.build(network, crac).build();
    }

    private static RaoParameters getRaoParameters() {
        return JsonRaoParameters.read(OpenRaoEarlyStoppingTest.class.getResourceAsStream("/parameters/RaoParameters_2P.json"), ReportNode.NO_OP);
    }

}
