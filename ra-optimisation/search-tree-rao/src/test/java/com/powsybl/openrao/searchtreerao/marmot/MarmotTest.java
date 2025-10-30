/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.RaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.InterTemporalRaoResultImpl;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class MarmotTest {
    /*
    TODO
    For each test check the global MARMOT cost and the individual costs for each timestamp.
    This cannot currently be done since all individual RAO results use the global cost as their cost.
    The costs will be separated in a future PR and will have to be checked here.
     */

    public static String getResourcesPath() {
        return "src/test/resources/";
    }

    @Test
    void testTwoTimestampsAndGradientOnGeneratorWithNoAssociatedRemedialAction() throws IOException {
        // we need to import twice the network to avoid variant names conflicts on the same network object
        String networkFilePath = "/network/2Nodes2ParallelLinesPST.uct";
        Network network1 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        Network network2 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        // Create postIcsNetwork:
        String networkFilePathPostIcsImport = networkFilePath.split(".uct")[0].concat("_modified.jiidm");
        network1.write("JIIDM", new Properties(), Path.of(getResourcesPath().concat(networkFilePathPostIcsImport)));

        Crac crac1 = Crac.read("/crac/crac-20250213.json", MarmotTest.class.getResourceAsStream("/crac/crac-20250213.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-20250214.json", MarmotTest.class.getResourceAsStream("/crac/crac-20250214.json"), network2);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_dc_minObjective_discretePst.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 13, 11, 35, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 35, 0, 0, ZoneOffset.UTC);

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>(
            Map.of(
                timestamp1, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac1).build(),
                timestamp2, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac2).build()
            ));

        InterTemporalRaoInputWithNetworkPaths input = new InterTemporalRaoInputWithNetworkPaths(
            raoInputs,
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR1AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(1000.0).withUpwardPowerGradient(1000.0).withDownwardPowerGradient(-1000.0).build())
        );

        // first RAOs shift tap to -5 for a cost of 55 each
        // MARMOT should also move the tap to -5 for both timestamps with a total cost of 110
        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();
        assertEquals(-5, results.getOptimizedTapOnState(crac1.getPreventiveState(), crac1.getPstRangeAction("pstBeFr2")));
        assertEquals(-5, results.getOptimizedTapOnState(crac2.getPreventiveState(), crac2.getPstRangeAction("pstBeFr2")));

        assertEquals(110., results.getGlobalCost(crac1.getLastInstant()));
        assertEquals(55., results.getCost(crac1.getLastInstant(), timestamp1));
        assertEquals(55., results.getCost(crac2.getLastInstant(), timestamp2));

        // Clean created networks
        cleanExistingNetwork(getResourcesPath().concat(networkFilePathPostIcsImport));
    }

    @Test
    void testWithRedispatchingAndNoGradientOnImplicatedGenerators() throws IOException {
        String networkFilePath = "/network/3Nodes.uct";
        Network network1 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        Network network2 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        Network network3 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        // Create postIcsNetwork:
        String networkFilePathPostIcsImport = networkFilePath.split(".uct")[0].concat("_modified.jiidm");
        network1.write("JIIDM", new Properties(), Path.of(getResourcesPath().concat(networkFilePathPostIcsImport)));

        Crac crac1 = Crac.read("/crac/crac-redispatching-202502141040.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141040.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-redispatching-202502141140.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141140.json"), network2);
        Crac crac3 = Crac.read("/crac/crac-redispatching-202502141240.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141240.json"), network3);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 14, 10, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 2, 14, 12, 40, 0, 0, ZoneOffset.UTC);

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>(
            Map.of(
                timestamp1, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac1).build(),
                timestamp2, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac2).build(),
                timestamp3, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac3).build()
            ));
        InterTemporalRaoInputWithNetworkPaths input = new InterTemporalRaoInputWithNetworkPaths(
            raoInputs,
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR1AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(1000.0).withUpwardPowerGradient(250.0).withDownwardPowerGradient(-250.0).build())
        );

        // no redispatching required during the first timestamp
        // redispatching of 500 MW in both timestamps 2 & 3 with a cost of 26510 each
        // MARMOT should also activate redispatching at 530 MW for second and third timestamps
        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();

        assertEquals(-0.0, results.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));

        assertEquals(53020., results.getGlobalCost(crac1.getLastInstant()));
        assertEquals(0., results.getCost(crac1.getLastInstant(), timestamp1));
        assertEquals(26510, results.getCost(crac2.getLastInstant(), timestamp2));
        assertEquals(26510, results.getCost(crac3.getLastInstant(), timestamp3));

        // Clean created networks
        cleanExistingNetwork(getResourcesPath().concat(networkFilePathPostIcsImport));
    }

    @Test
    void testWithRedispatchingAndNoGradients() throws IOException {
        String networkFilePath = "/network/3Nodes.uct";
        Network network1 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        Network network2 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        Network network3 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        // Create postIcsNetwork:
        String networkFilePathPostIcsImport = networkFilePath.split(".uct")[0].concat("_modified.jiidm");
        network1.write("JIIDM", new Properties(), Path.of(getResourcesPath().concat(networkFilePathPostIcsImport)));

        Crac crac1 = Crac.read("/crac/crac-redispatching-202502141040.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141040.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-redispatching-202502141140.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141140.json"), network2);
        Crac crac3 = Crac.read("/crac/crac-redispatching-202502141240.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141240.json"), network3);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 14, 10, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 2, 14, 12, 40, 0, 0, ZoneOffset.UTC);

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>(
            Map.of(
                timestamp1, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac1).build(),
                timestamp2, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac2).build(),
                timestamp3, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac3).build()
            ));
        InterTemporalRaoInputWithNetworkPaths input = new InterTemporalRaoInputWithNetworkPaths(
            raoInputs,
            Set.of()
        );

        // no redispatching required during the first timestamp
        // redispatching of 500 MW in both timestamps 2 & 3 with a cost of 26510 each
        // MARMOT should also activate redispatching at 530 MW for second and third timestamps
        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();

        assertEquals(-0.0, results.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));

        assertEquals(53020., results.getGlobalCost(crac1.getLastInstant()));
        assertEquals(0., results.getCost(crac1.getLastInstant(), timestamp1));
        assertEquals(26510, results.getCost(crac2.getLastInstant(), timestamp2));
        assertEquals(26510, results.getCost(crac3.getLastInstant(), timestamp3));

        // Clean created networks
        cleanExistingNetwork(getResourcesPath().concat(networkFilePathPostIcsImport));
    }

    @Test
    void testWithRedispatchingAndGradientOnImplicatedGenerators() throws IOException {
        String networkRelativePath = "/network/3Nodes.uct";
        String networkAbsolutePath = getResourcesPath().concat(networkRelativePath);
        Network network = Network.read(networkRelativePath, MarmotTest.class.getResourceAsStream(networkRelativePath));

        // Create postIcsNetwork:
        String networkFilePathPostIcsImport = networkRelativePath.split(".uct")[0].concat("_modified.jiidm");
        network.write("JIIDM", new Properties(), Path.of(getResourcesPath().concat(networkFilePathPostIcsImport)));

        Crac crac1 = Crac.read("/crac/crac-redispatching-202502141040.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141040.json"), network);
        Crac crac2 = Crac.read("/crac/crac-redispatching-202502141140.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141140.json"), network);
        Crac crac3 = Crac.read("/crac/crac-redispatching-202502141240.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141240.json"), network);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 14, 10, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 2, 14, 12, 40, 0, 0, ZoneOffset.UTC);

        InterTemporalRaoInputWithNetworkPaths input = new InterTemporalRaoInputWithNetworkPaths(
            new TemporalDataImpl<>(Map.of(
                timestamp1, RaoInputWithNetworkPaths.build(networkAbsolutePath, networkAbsolutePath, crac1).build(),
                timestamp2, RaoInputWithNetworkPaths.build(networkAbsolutePath, networkAbsolutePath, crac2).build(),
                timestamp3, RaoInputWithNetworkPaths.build(networkAbsolutePath, networkAbsolutePath, crac3).build())),
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR3AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(1000.0).withUpwardPowerGradient(200.0).withDownwardPowerGradient(0.0).build())
        );

        // no redispatching required during the first timestamp
        // MARMOT will activate 330 MW however in timestamp 1 : it is the minimum necessary to be able to activate 500 MW in timestamp 2
        // due to the max gradient of 200. Not activating 530 MW in timestamps 2 and 3 will create an overload and be very costly.
        // redispatching of 530 MW in both timestamps 2 & 3 with a cost of 26510 each
        // MARMOT should also activate redispatching at 530 MW for second and third timestamps
        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();
        assertEquals(330.0, results.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));

        assertEquals(69530., results.getGlobalCost(crac1.getLastInstant()));
        assertEquals(16510., results.getCost(crac1.getLastInstant(), timestamp1));
        assertEquals(26510, results.getCost(crac2.getLastInstant(), timestamp2));
        assertEquals(26510, results.getCost(crac3.getLastInstant(), timestamp3));

        // Clean created networks
        cleanExistingNetwork(getResourcesPath().concat(networkFilePathPostIcsImport));
    }

    @Test
    void testWithPreventiveTopologicalAction() throws IOException {
        String networkFilePath = "/network/2Nodes3ParallelLinesPST2LinesClosed.uct";
        Network network1 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        Network network2 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        // Create postIcsNetwork:
        String networkFilePathPostIcsImport = networkFilePath.split(".uct")[0].concat("_modified.jiidm");
        network1.write("JIIDM", new Properties(), Path.of(getResourcesPath().concat(networkFilePathPostIcsImport)));

        Crac crac1 = Crac.read("/crac/crac-topo-202502181007.json", MarmotTest.class.getResourceAsStream("/crac/crac-topo-202502181007.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-topo-202502191007.json", MarmotTest.class.getResourceAsStream("/crac/crac-topo-202502191007.json"), network2);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 18, 10, 7, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 19, 10, 7, 0, 0, ZoneOffset.UTC);

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>(
            Map.of(
                timestamp1, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac1).build(),
                timestamp2, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkFilePath), getResourcesPath().concat(networkFilePathPostIcsImport), crac2).build()

            ));
        InterTemporalRaoInputWithNetworkPaths input = new InterTemporalRaoInputWithNetworkPaths(
            raoInputs,
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR1AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(1000.0).withUpwardPowerGradient(250.0).withDownwardPowerGradient(-250.0).build())
        );

        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();
        assertTrue(results.isActivated(crac1.getPreventiveState(), crac1.getNetworkAction("closeBeFr2")));
        assertTrue(results.isActivated(crac2.getPreventiveState(), crac2.getNetworkAction("closeBeFr2")));
        assertEquals(40.0, results.getGlobalCost(crac1.getPreventiveInstant()));

        assertEquals(40., results.getGlobalCost(crac1.getLastInstant()));
        assertEquals(20., results.getCost(crac1.getLastInstant(), timestamp1));
        assertEquals(20., results.getCost(crac2.getLastInstant(), timestamp2));

        // Clean created networks
        cleanExistingNetwork(getResourcesPath().concat(networkFilePathPostIcsImport));
    }

    @Test
    void testWithTenTimestampsAndGeneratorConstraints() throws IOException {
        String networkPath = getResourcesPath().concat("/network/4Nodes_1_PST.uct");

        Network network = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Crac crac1 = Crac.read("/crac/crac-202503251030.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251030.json"), network);
        Crac crac2 = Crac.read("/crac/crac-202503251130.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251130.json"), network);
        Crac crac3 = Crac.read("/crac/crac-202503251230.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251230.json"), network);
        Crac crac4 = Crac.read("/crac/crac-202503251330.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251330.json"), network);
        Crac crac5 = Crac.read("/crac/crac-202503251430.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251430.json"), network);
        Crac crac6 = Crac.read("/crac/crac-202503251530.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251530.json"), network);
        Crac crac7 = Crac.read("/crac/crac-202503251630.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251630.json"), network);
        Crac crac8 = Crac.read("/crac/crac-202503251730.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251730.json"), network);
        Crac crac9 = Crac.read("/crac/crac-202503251830.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251830.json"), network);
        Crac crac10 = Crac.read("/crac/crac-202503251930.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251930.json"), network);

        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc_with_offset.json"));

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

        Map<OffsetDateTime, RaoInputWithNetworkPaths> inputPerTimestamp = new HashMap<>();
        inputPerTimestamp.put(timestamp1, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac1).build());
        inputPerTimestamp.put(timestamp2, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac2).build());
        inputPerTimestamp.put(timestamp3, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac3).build());
        inputPerTimestamp.put(timestamp4, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac4).build());
        inputPerTimestamp.put(timestamp5, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac5).build());
        inputPerTimestamp.put(timestamp6, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac6).build());
        inputPerTimestamp.put(timestamp7, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac7).build());
        inputPerTimestamp.put(timestamp8, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac8).build());
        inputPerTimestamp.put(timestamp9, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac9).build());
        inputPerTimestamp.put(timestamp10, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac10).build());

        InterTemporalRaoInputWithNetworkPaths input = new InterTemporalRaoInputWithNetworkPaths(
            new TemporalDataImpl<>(inputPerTimestamp),
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR1AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(5000.0).withUpwardPowerGradient(500.0).withDownwardPowerGradient(-500.0).build())
        );

        InterTemporalRaoResultImpl interTemporalRaoResult = (InterTemporalRaoResultImpl) new Marmot().run(input, raoParameters).join();

        assertEquals(625070.0, interTemporalRaoResult.getGlobalFunctionalCost(crac1.getPreventiveInstant()));

        assertFunctionalCostAndRedispatchingSetPoint(crac1, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac2, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac3, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac4, interTemporalRaoResult, 25010.0, 4500.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac5, interTemporalRaoResult, 50010.0, 4000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac6, interTemporalRaoResult, 75010.0, 3500.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac7, interTemporalRaoResult, 100010.0, 3000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac8, interTemporalRaoResult, 125010.0, 2500.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac9, interTemporalRaoResult, 125010.0, 2500.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac10, interTemporalRaoResult, 125010.0, 2500.0);
    }

    @Test
    void testWithTenTimestampsAndNoGeneratorConstraints() throws IOException {
        String networkPath = getResourcesPath().concat("/network/4Nodes_1_PST.uct");

        Network network = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Crac crac1 = Crac.read("/crac/crac-202503251030.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251030.json"), network);
        Crac crac2 = Crac.read("/crac/crac-202503251130.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251130.json"), network);
        Crac crac3 = Crac.read("/crac/crac-202503251230.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251230.json"), network);
        Crac crac4 = Crac.read("/crac/crac-202503251330.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251330.json"), network);
        Crac crac5 = Crac.read("/crac/crac-202503251430.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251430.json"), network);
        Crac crac6 = Crac.read("/crac/crac-202503251530.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251530.json"), network);
        Crac crac7 = Crac.read("/crac/crac-202503251630.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251630.json"), network);
        Crac crac8 = Crac.read("/crac/crac-202503251730.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251730.json"), network);
        Crac crac9 = Crac.read("/crac/crac-202503251830.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251830.json"), network);
        Crac crac10 = Crac.read("/crac/crac-202503251930.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251930.json"), network);

        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc_with_offset.json"));

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

        Map<OffsetDateTime, RaoInputWithNetworkPaths> inputPerTimestamp = new HashMap<>();
        inputPerTimestamp.put(timestamp1, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac1).build());
        inputPerTimestamp.put(timestamp2, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac2).build());
        inputPerTimestamp.put(timestamp3, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac3).build());
        inputPerTimestamp.put(timestamp4, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac4).build());
        inputPerTimestamp.put(timestamp5, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac5).build());
        inputPerTimestamp.put(timestamp6, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac6).build());
        inputPerTimestamp.put(timestamp7, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac7).build());
        inputPerTimestamp.put(timestamp8, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac8).build());
        inputPerTimestamp.put(timestamp9, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac9).build());
        inputPerTimestamp.put(timestamp10, RaoInputWithNetworkPaths.build(networkPath, networkPath, crac10).build());

        InterTemporalRaoInputWithNetworkPaths input = new InterTemporalRaoInputWithNetworkPaths(new TemporalDataImpl<>(inputPerTimestamp), Set.of());

        InterTemporalRaoResultImpl interTemporalRaoResult = (InterTemporalRaoResultImpl) new Marmot().run(input, raoParameters).join();

        assertEquals(375030.0, interTemporalRaoResult.getGlobalFunctionalCost(crac1.getPreventiveInstant()));

        assertFunctionalCostAndRedispatchingSetPoint(crac1, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac2, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac3, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac4, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac5, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac6, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac7, interTemporalRaoResult, 0.0, 5000.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac8, interTemporalRaoResult, 125010.0, 2500.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac9, interTemporalRaoResult, 125010.0, 2500.0);
        assertFunctionalCostAndRedispatchingSetPoint(crac10, interTemporalRaoResult, 125010.0, 2500.0);
    }

    private static void assertFunctionalCostAndRedispatchingSetPoint(Crac crac, InterTemporalRaoResult interTemporalRaoResult, double expectedFunctionalCost, double expectedRdSetPoint) {
        assertEquals(expectedFunctionalCost, interTemporalRaoResult.getFunctionalCost(crac.getPreventiveInstant(), crac.getTimestamp().orElseThrow()));
        assertEquals(expectedRdSetPoint, interTemporalRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), crac.getRangeAction("redispatchingAction")));
    }

    private void cleanExistingNetwork(String path) {
        File file = new File(Path.of(path).toUri());
        assertTrue(file.exists());
        file.delete();
    }
}
