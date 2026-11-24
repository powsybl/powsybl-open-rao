/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoStopCondition;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
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
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(23); // some time during curative optimization

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
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(36); // some time during second preventive optimization

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
    void testEarlyStopFastRao() throws IOException {
        RaoInput raoInput = createRaoInput();
        RaoParameters raoParameters = getRaoParametersWithFastRaoExtension();
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
    void testEarlyStopFastRaoDuringFirstLoop() throws IOException {
        RaoInput raoInput = createRaoInput();
        RaoParameters raoParameters = getRaoParametersWithFastRaoExtension();
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(23); // some time during first iteration

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

    /**
     * MARMOT
     */

    @Test
    void testEarlyStopMarmot() throws IOException {
        TimeCoupledRaoInput raoInput = createTimeCoupledRaoInput();
        RaoParameters raoParameters = getRaoParametersForTimeCoupledComputation();
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(1);

        RaoResult raoResult = new OpenRao().run(raoInput, raoParameters, ReportNode.NO_OP, raoStopCondition).join();
        assertNotNull(raoResult);
        raoInput.getRaoInputs().map(RaoInput::getCrac).getDataPerTimestamp().values().forEach(
            crac -> crac.getStates().forEach(state -> {
                assertTrue(raoResult.getActivatedNetworkActionsDuringState(state).isEmpty());
                assertTrue(raoResult.getActivatedRangeActionsDuringState(state).isEmpty());
            })
        );

        Metadata metadata = raoResult.getExtension(Metadata.class);
        assertNotNull(metadata);
        Optional<String> executionDetails = metadata.getExecutionDetails();
        assertTrue(executionDetails.isPresent());
        assertEquals("RAO was interrupted before any result could be produced.", executionDetails.get());
    }

    @Test
    void testEarlyStopMarmotAfterTopologicalOptimization() throws IOException {
        TimeCoupledRaoInput raoInput = createTimeCoupledRaoInput();
        RaoParameters raoParameters = getRaoParametersForTimeCoupledComputation();
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(300); // some time during first iteration of global MIP

        RaoResult raoResult = new OpenRao().run(raoInput, raoParameters, ReportNode.NO_OP, raoStopCondition).join();
        assertNotNull(raoResult);

        OffsetDateTime timestamp1330 = OffsetDateTime.of(2025, 3, 25, 13, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1430 = OffsetDateTime.of(2025, 3, 25, 14, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1530 = OffsetDateTime.of(2025, 3, 25, 15, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1630 = OffsetDateTime.of(2025, 3, 25, 16, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1730 = OffsetDateTime.of(2025, 3, 25, 17, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1830 = OffsetDateTime.of(2025, 3, 25, 18, 30, 0, 0, ZoneOffset.UTC);

        Crac crac1330 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1330).orElseThrow();
        Crac crac1430 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1430).orElseThrow();
        Crac crac1530 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1530).orElseThrow();
        Crac crac1630 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1630).orElseThrow();
        Crac crac1730 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1730).orElseThrow();
        Crac crac1830 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1830).orElseThrow();

        // set-points violate 500 MW/h gradient
        assertEquals(5000.0, raoResult.getOptimizedSetPointOnState(crac1330.getPreventiveState(), crac1330.getInjectionRangeAction("redispatchingAction")));
        assertEquals(5000.0, raoResult.getOptimizedSetPointOnState(crac1430.getPreventiveState(), crac1430.getInjectionRangeAction("redispatchingAction")));
        assertEquals(5000.0, raoResult.getOptimizedSetPointOnState(crac1530.getPreventiveState(), crac1530.getInjectionRangeAction("redispatchingAction")));
        assertEquals(5000.0, raoResult.getOptimizedSetPointOnState(crac1630.getPreventiveState(), crac1630.getInjectionRangeAction("redispatchingAction")));
        assertEquals(2500.0, raoResult.getOptimizedSetPointOnState(crac1730.getPreventiveState(), crac1730.getInjectionRangeAction("redispatchingAction")));
        assertEquals(2500.0, raoResult.getOptimizedSetPointOnState(crac1830.getPreventiveState(), crac1830.getInjectionRangeAction("redispatchingAction")));

        Metadata metadata = raoResult.getExtension(Metadata.class);
        assertNotNull(metadata);
        Optional<String> executionDetails = metadata.getExecutionDetails();
        assertTrue(executionDetails.isPresent());
        assertEquals("RAO was interrupted after topological optimization but before global linear optimization. Some time-coupled constraints may be violated.", executionDetails.get());
    }

    @Test
    void testNoEarlyStoppingMarmot() throws IOException {
        TimeCoupledRaoInput raoInput = createTimeCoupledRaoInput();
        RaoParameters raoParameters = getRaoParametersForTimeCoupledComputation();
        RaoStopCondition raoStopCondition = new MaxCallsRaoStopCondition(3210); // exceeds number of calls

        RaoResult raoResult = new OpenRao().run(raoInput, raoParameters, ReportNode.NO_OP, raoStopCondition).join();
        assertNotNull(raoResult);

        OffsetDateTime timestamp1330 = OffsetDateTime.of(2025, 3, 25, 13, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1430 = OffsetDateTime.of(2025, 3, 25, 14, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1530 = OffsetDateTime.of(2025, 3, 25, 15, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1630 = OffsetDateTime.of(2025, 3, 25, 16, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1730 = OffsetDateTime.of(2025, 3, 25, 17, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp1830 = OffsetDateTime.of(2025, 3, 25, 18, 30, 0, 0, ZoneOffset.UTC);

        Crac crac1330 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1330).orElseThrow();
        Crac crac1430 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1430).orElseThrow();
        Crac crac1530 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1530).orElseThrow();
        Crac crac1630 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1630).orElseThrow();
        Crac crac1730 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1730).orElseThrow();
        Crac crac1830 = raoInput.getRaoInputs().map(RaoInput::getCrac).getData(timestamp1830).orElseThrow();

        // set-points respect 500 MW/h gradient
        assertEquals(4500.0, raoResult.getOptimizedSetPointOnState(crac1330.getPreventiveState(), crac1330.getInjectionRangeAction("redispatchingAction")));
        assertEquals(4000.0, raoResult.getOptimizedSetPointOnState(crac1430.getPreventiveState(), crac1430.getInjectionRangeAction("redispatchingAction")));
        assertEquals(3500.0, raoResult.getOptimizedSetPointOnState(crac1530.getPreventiveState(), crac1530.getInjectionRangeAction("redispatchingAction")));
        assertEquals(3000.0, raoResult.getOptimizedSetPointOnState(crac1630.getPreventiveState(), crac1630.getInjectionRangeAction("redispatchingAction")));
        assertEquals(2500.0, raoResult.getOptimizedSetPointOnState(crac1730.getPreventiveState(), crac1730.getInjectionRangeAction("redispatchingAction")));
        assertEquals(2500.0, raoResult.getOptimizedSetPointOnState(crac1830.getPreventiveState(), crac1830.getInjectionRangeAction("redispatchingAction")));

        Metadata metadata = raoResult.getExtension(Metadata.class);
        assertNotNull(metadata);
        Optional<String> executionDetails = metadata.getExecutionDetails();
        assertTrue(executionDetails.isPresent());
        // usual execution details -> RAO was not interrupted
        assertEquals("RAO went through independent topological optimizations and global time-coupled linear optimization.", executionDetails.get());
    }

    private static RaoInput createRaoInput() throws IOException {
        Network network = Network.read("small-network-2P.uct", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/network/small-network-2P.uct"));
        Crac crac = Crac.read("small-crac-2P.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/small-crac-2P.json"), network);
        return RaoInput.build(network, crac).build();
    }

    private static RaoParameters getRaoParameters() {
        return JsonRaoParameters.read(OpenRaoEarlyStoppingTest.class.getResourceAsStream("/parameters/RaoParameters_2P.json"), ReportNode.NO_OP);
    }

    private static RaoParameters getRaoParametersWithFastRaoExtension() {
        RaoParameters raoParameters = getRaoParameters();
        FastRaoParameters fastRaoParameters = new FastRaoParameters();
        fastRaoParameters.setNumberOfCnecsToAdd(1);
        raoParameters.addExtension(FastRaoParameters.class, fastRaoParameters);
        return raoParameters;
    }

    private static TimeCoupledRaoInput createTimeCoupledRaoInput() throws IOException {
        String networkPath = "src/test/resources/network/4Nodes_1_PST.xiidm";

        Network network = Network.read("/network/4Nodes_1_PST.xiidm", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/network/4Nodes_1_PST.xiidm"));
        Crac crac1 = Crac.read("/crac/crac-202503251030.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251030.json"), network);
        Crac crac2 = Crac.read("/crac/crac-202503251130.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251130.json"), network);
        Crac crac3 = Crac.read("/crac/crac-202503251230.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251230.json"), network);
        Crac crac4 = Crac.read("/crac/crac-202503251330.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251330.json"), network);
        Crac crac5 = Crac.read("/crac/crac-202503251430.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251430.json"), network);
        Crac crac6 = Crac.read("/crac/crac-202503251530.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251530.json"), network);
        Crac crac7 = Crac.read("/crac/crac-202503251630.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251630.json"), network);
        Crac crac8 = Crac.read("/crac/crac-202503251730.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251730.json"), network);
        Crac crac9 = Crac.read("/crac/crac-202503251830.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251830.json"), network);
        Crac crac10 = Crac.read("/crac/crac-202503251930-key-2.json", OpenRaoEarlyStoppingTest.class.getResourceAsStream("/crac/crac-202503251930-key-2.json"), network);

        RaoParameters raoParameters = JsonRaoParameters.read(OpenRaoEarlyStoppingTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc_with_offset.json"), ReportNode.NO_OP);

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 3, 25, 10, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 3, 25, 11, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 3, 25, 12, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp4 = OffsetDateTime.of(2025, 3, 25, 13, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp5 = OffsetDateTime.of(2025, 3, 25, 14, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp6 = OffsetDateTime.of(2025, 3, 25, 15, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp7 = OffsetDateTime.of(2025, 3, 25, 16, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp8 = OffsetDateTime.of(2025, 3, 25, 17, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp9 = OffsetDateTime.of(2025, 3, 25, 18, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp10 = OffsetDateTime.of(2025, 3, 25, 19, 30, 0, 0, ZoneOffset.UTC);

        Map<OffsetDateTime, RaoInput> inputPerTimestamp = new HashMap<>();
        inputPerTimestamp.put(timestamp1, RaoInput.build(LazyNetwork.of(networkPath), crac1).build());
        inputPerTimestamp.put(timestamp2, RaoInput.build(LazyNetwork.of(networkPath), crac2).build());
        inputPerTimestamp.put(timestamp3, RaoInput.build(LazyNetwork.of(networkPath), crac3).build());
        inputPerTimestamp.put(timestamp4, RaoInput.build(LazyNetwork.of(networkPath), crac4).build());
        inputPerTimestamp.put(timestamp5, RaoInput.build(LazyNetwork.of(networkPath), crac5).build());
        inputPerTimestamp.put(timestamp6, RaoInput.build(LazyNetwork.of(networkPath), crac6).build());
        inputPerTimestamp.put(timestamp7, RaoInput.build(LazyNetwork.of(networkPath), crac7).build());
        inputPerTimestamp.put(timestamp8, RaoInput.build(LazyNetwork.of(networkPath), crac8).build());
        inputPerTimestamp.put(timestamp9, RaoInput.build(LazyNetwork.of(networkPath), crac9).build());
        inputPerTimestamp.put(timestamp10, RaoInput.build(LazyNetwork.of(networkPath), crac10).build());

        TimeCoupledConstraints timeCoupledConstraints = new TimeCoupledConstraints();
        timeCoupledConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("FFR3AA1 _generator").withUpwardPowerGradient(500.0).withDownwardPowerGradient(-500.0).build());

        return new TimeCoupledRaoInput(new TemporalDataImpl<>(inputPerTimestamp), timeCoupledConstraints);
    }

    private static RaoParameters getRaoParametersForTimeCoupledComputation() {
        return JsonRaoParameters.read(OpenRaoEarlyStoppingTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc_with_offset.json"), ReportNode.NO_OP);
    }

}
