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
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.RaoInput;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
            Set.of(new PowerGradient("FFR1AA1 _generator", -1000d, 1000d))
        );

        // first RAOs shift tap to -5 for a cost of 55 each
        // MARMOT should also move the tap to -5 for both timestamps with a total cost of 110
        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();
        assertEquals(-5, results.getOptimizedTapOnState(crac1.getPreventiveState(), crac1.getPstRangeAction("pstBeFr2")));
        assertEquals(-5, results.getOptimizedTapOnState(crac2.getPreventiveState(), crac2.getPstRangeAction("pstBeFr2")));

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
            Set.of(new PowerGradient("FFR1AA1 _generator", -250d, 250d))
        );

        // no redispatching required during the first timestamp
        // redispatching of 500 MW in both timestamps 2 & 3 with a cost of 25010 each
        // MARMOT should also activate redispatching at 500 MW for second and third timestamps
        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();

        assertEquals(-0.0, results.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(509.0, results.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(509.0, results.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));

        // Clean created networks
        cleanExistingNetwork(getResourcesPath().concat(networkFilePathPostIcsImport));
    }

    @Test
    void testWithRedispatchingAndGradientOnImplicatedGenerators() throws IOException {
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
            Set.of(new PowerGradient("FFR3AA1 _generator", 0d, 200d))
        );

        // no redispatching required during the first timestamp
        // MARMOT will activate 3000 MW however in timestamp 1 : it is the minimum necessary to be able to activate 500 MW in timestamp 2
        // due to the max gradient of 200. Not activating 500 MW in timestamps 2 and 3 will create an overload and be very costly.
        // redispatching of 500 MW in both timestamps 2 & 3 with a cost of 25010 each
        // MARMOT should also activate redispatching at 500 MW for second and third timestamps
        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();
        assertEquals(309.0, results.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(509.0, results.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(509.0, results.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));

        // Clean created networks
        cleanExistingNetwork(getResourcesPath().concat(networkFilePathPostIcsImport));
    }

    @Test
    void testWithPreventiveTopologicalAction() throws IOException {
        String networkFilePath = "/network/2Nodes3ParallelLinesPST2LinesClosed.uct";
        Network network1 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
        // Create postIcsNetwork:
        String networkFilePathPostIcsImport = networkFilePath.split(".uct")[0].concat("_modified.jiidm");
        network1.write("JIIDM", new Properties(), Path.of(getResourcesPath().concat(networkFilePathPostIcsImport)));

        Network network2 = Network.read(networkFilePath, MarmotTest.class.getResourceAsStream(networkFilePath));
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
            Set.of(new PowerGradient("FFR1AA1 _generator", -250d, 250d))
        );

        InterTemporalRaoResult results = new Marmot().run(input, raoParameters).join();
        assertTrue(results.isActivated(crac1.getPreventiveState(), crac1.getNetworkAction("closeBeFr2")));
        assertTrue(results.isActivated(crac2.getPreventiveState(), crac2.getNetworkAction("closeBeFr2")));
        assertEquals(40.0, results.getGlobalCost(InstantKind.PREVENTIVE));

        // Clean created networks
        cleanExistingNetwork(getResourcesPath().concat(networkFilePathPostIcsImport));
    }

    @Test
    void test10TS() throws IOException {
        String networkPath = "/network/4Nodes_1_PST.uct";
        String networkFilePathPostIcsImport = networkPath.split(".uct")[0].concat("_modified.jiidm");
        Network network = Network.read(networkPath, MarmotTest.class.getResourceAsStream(networkPath));
        network.write("JIIDM", new Properties(), Path.of(getResourcesPath().concat(networkFilePathPostIcsImport)));

        Network network1 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network2 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network3 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network4 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network5 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network6 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network7 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network8 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network9 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));
        Network network10 = Network.read("/network/4Nodes_1_PST.uct", MarmotTest.class.getResourceAsStream("/network/4Nodes_1_PST.uct"));

        Crac crac1 = Crac.read("/crac/crac-202503251030.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251030.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-202503251130.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251130.json"), network2);
        Crac crac3 = Crac.read("/crac/crac-202503251230.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251230.json"), network3);
        Crac crac4 = Crac.read("/crac/crac-202503251330.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251330.json"), network4);
        Crac crac5 = Crac.read("/crac/crac-202503251430.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251430.json"), network5);
        Crac crac6 = Crac.read("/crac/crac-202503251530.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251530.json"), network6);
        Crac crac7 = Crac.read("/crac/crac-202503251630.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251630.json"), network7);
        Crac crac8 = Crac.read("/crac/crac-202503251730.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251730.json"), network8);
        Crac crac9 = Crac.read("/crac/crac-202503251830.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251830.json"), network9);
        Crac crac10 = Crac.read("/crac/crac-202503251930.json", MarmotTest.class.getResourceAsStream("/crac/crac-202503251930.json"), network10);

        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));

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
        inputPerTimestamp.put(timestamp1, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac1).build());
        inputPerTimestamp.put(timestamp2, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac2).build());
        inputPerTimestamp.put(timestamp3, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac3).build());
        inputPerTimestamp.put(timestamp4, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac4).build());
        inputPerTimestamp.put(timestamp5, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac5).build());
        inputPerTimestamp.put(timestamp6, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac6).build());
        inputPerTimestamp.put(timestamp7, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac7).build());
        inputPerTimestamp.put(timestamp8, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac8).build());
        inputPerTimestamp.put(timestamp9, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac9).build());
        inputPerTimestamp.put(timestamp10, RaoInputWithNetworkPaths.build(getResourcesPath().concat(networkPath), getResourcesPath().concat(networkFilePathPostIcsImport), crac10).build());

        InterTemporalRaoInputWithNetworkPaths input = new InterTemporalRaoInputWithNetworkPaths(
            new TemporalDataImpl<>(inputPerTimestamp),
            Set.of(new PowerGradient("FFR1AA1 _generator", -500.0, 500.0))
        );

        InterTemporalRaoResultImpl interTemporalRaoResult = (InterTemporalRaoResultImpl) new Marmot().run(input, raoParameters).join();

        checkRemedialActionActivation(interTemporalRaoResult, crac1, timestamp1, 11, false, 5000.0);
        checkRemedialActionActivation(interTemporalRaoResult, crac2, timestamp2, 15, false, 5000.0);
        checkRemedialActionActivation(interTemporalRaoResult, crac3, timestamp3, 15, false, 5000.0);
        // checkRemedialActionActivation(interTemporalRaoResult, crac4, timestamp4, 10, false, 4500.0);
        // checkRemedialActionActivation(interTemporalRaoResult, crac5, timestamp5, 5, false, 4000.0);
        // checkRemedialActionActivation(interTemporalRaoResult, crac6, timestamp6, 0, false, 3500.0);
        // checkRemedialActionActivation(interTemporalRaoResult, crac7, timestamp7, 0, false, 3000.0);
        checkRemedialActionActivation(interTemporalRaoResult, crac8, timestamp8, 16, true, 2500.0);
        checkRemedialActionActivation(interTemporalRaoResult, crac9, timestamp9, 16, true, 2500.0);
        checkRemedialActionActivation(interTemporalRaoResult, crac10, timestamp10, 16, true, 2500.0);
    }

    private static void checkRemedialActionActivation(InterTemporalRaoResultImpl interTemporalRaoResult, Crac crac, OffsetDateTime timestamp, int pstTap, boolean topologicalActionActivated, double redispatchingSetPoint) {
        RaoResult raoResult = interTemporalRaoResult.getIndividualRaoResult(timestamp);
        assertEquals(pstTap, raoResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction("PST_FR1_FR3")));
        assertEquals(topologicalActionActivated, raoResult.isActivated(crac.getPreventiveState(), crac.getNetworkAction("closeFr2Fr3-2")));
        assertEquals(redispatchingSetPoint, raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), crac.getRangeAction("redispatchingAction")));
        assertEquals(0.0, raoResult.getVirtualCost(crac.getPreventiveInstant(), "min-margin-violation-evaluator"));
    }

    private void cleanExistingNetwork(String path) {
        File file = new File(Path.of(path).toUri());
        assertTrue(file.exists());
        file.delete();
    }
}
