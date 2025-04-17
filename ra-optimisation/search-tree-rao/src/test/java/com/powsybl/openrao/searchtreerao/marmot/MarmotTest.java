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
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.RaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));

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
        assertEquals(330.0, results.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(530.0, results.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));

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

    private void cleanExistingNetwork(String path) {
        File file = new File(Path.of(path).toUri());
        assertTrue(file.exists());
        file.delete();
    }
}
