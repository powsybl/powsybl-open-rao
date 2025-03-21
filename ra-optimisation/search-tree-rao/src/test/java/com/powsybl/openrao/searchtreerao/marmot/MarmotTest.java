/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.generatorconstraints.GeneratorConstraints;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.InterTemporalRaoResultImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class MarmotTest {

    @Test
    void testTwoTimestampsAndGradientOnGeneratorWithNoAssociatedRemedialAction() throws IOException {
        // we need to import twice the network to avoid variant names conflicts on the same network object
        Network network1 = Network.read("/network/2Nodes2ParallelLinesPST.uct", MarmotTest.class.getResourceAsStream("/network/2Nodes2ParallelLinesPST.uct"));
        Network network2 = Network.read("/network/2Nodes2ParallelLinesPST.uct", MarmotTest.class.getResourceAsStream("/network/2Nodes2ParallelLinesPST.uct"));
        Crac crac1 = Crac.read("/crac/crac-20250213.json", MarmotTest.class.getResourceAsStream("/crac/crac-20250213.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-20250214.json", MarmotTest.class.getResourceAsStream("/crac/crac-20250214.json"), network2);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_dc_minObjective_discretePst.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 13, 11, 35, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 35, 0, 0, ZoneOffset.UTC);

        InterTemporalRaoInput input = new InterTemporalRaoInput(
            new TemporalDataImpl<>(Map.of(timestamp1, RaoInput.build(network1, crac1).build(), timestamp2, RaoInput.build(network2, crac2).build())),
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR1AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(1000.0).withUpwardPowerGradient(1000.0).withDownwardPowerGradient(-1000.0).build())
        );

        // first RAOs shift tap to -5 for a cost of 55 each
        // MARMOT should also move the tap to -5 for both timestamps with a total cost of 110
        InterTemporalRaoResultImpl interTemporalRaoResult = (InterTemporalRaoResultImpl) new Marmot().run(input, raoParameters).join();
        assertEquals(110.0, interTemporalRaoResult.getGlobalCost());

        assertEquals(55.0, interTemporalRaoResult.getCost(crac1.getPreventiveInstant(), timestamp1));
        assertEquals(-5, interTemporalRaoResult.getOptimizedTapOnState(crac1.getPreventiveState(), crac1.getPstRangeAction("pstBeFr2")));

        assertEquals(55.0, interTemporalRaoResult.getCost(crac2.getPreventiveInstant(), timestamp2));
        assertEquals(-5, interTemporalRaoResult.getOptimizedTapOnState(crac2.getPreventiveState(), crac2.getPstRangeAction("pstBeFr2")));
    }

    @Test
    void testWithRedispatchingAndNoGradientOnImplicatedGenerators() throws IOException {
        Network network1 = Network.read("/network/3Nodes.uct", MarmotTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Network network2 = Network.read("/network/3Nodes.uct", MarmotTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Network network3 = Network.read("/network/3Nodes.uct", MarmotTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Crac crac1 = Crac.read("/crac/crac-redispatching-202502141040.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141040.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-redispatching-202502141140.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141140.json"), network2);
        Crac crac3 = Crac.read("/crac/crac-redispatching-202502141240.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141240.json"), network3);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 14, 10, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 2, 14, 12, 40, 0, 0, ZoneOffset.UTC);

        InterTemporalRaoInput input = new InterTemporalRaoInput(
            new TemporalDataImpl<>(Map.of(timestamp1, RaoInput.build(network1, crac1).build(), timestamp2, RaoInput.build(network2, crac2).build(), timestamp3, RaoInput.build(network3, crac3).build())),
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR1AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(1000.0).withUpwardPowerGradient(250.0).withDownwardPowerGradient(-250.0).build())
        );

        // no redispatching required during the first timestamp
        // redispatching of 500 MW in both timestamps 2 & 3 with a cost of 25010 each
        // MARMOT should also activate redispatching at 500 MW for second and third timestamps
        InterTemporalRaoResultImpl interTemporalRaoResult = (InterTemporalRaoResultImpl) new Marmot().run(input, raoParameters).join();
        assertEquals(50020.0, interTemporalRaoResult.getGlobalCost());

        assertEquals(0.0, interTemporalRaoResult.getCost(crac1.getPreventiveInstant(), timestamp1));
        assertEquals(-0.0, interTemporalRaoResult.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));

        assertEquals(25010.0, interTemporalRaoResult.getCost(crac2.getPreventiveInstant(), timestamp2));
        assertEquals(500.0, interTemporalRaoResult.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));

        assertEquals(25010.0, interTemporalRaoResult.getCost(crac3.getPreventiveInstant(), timestamp3));
        assertEquals(500.0, interTemporalRaoResult.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));
    }

    @Test
    void testWithRedispatchingAndGradientOnImplicatedGenerators() throws IOException {
        Network network1 = Network.read("/network/3Nodes.uct", MarmotTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Network network2 = Network.read("/network/3Nodes.uct", MarmotTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Network network3 = Network.read("/network/3Nodes.uct", MarmotTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Crac crac1 = Crac.read("/crac/crac-redispatching-202502141040.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141040.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-redispatching-202502141140.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141140.json"), network2);
        Crac crac3 = Crac.read("/crac/crac-redispatching-202502141240.json", MarmotTest.class.getResourceAsStream("/crac/crac-redispatching-202502141240.json"), network3);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 14, 10, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 2, 14, 12, 40, 0, 0, ZoneOffset.UTC);

        InterTemporalRaoInput input = new InterTemporalRaoInput(
            new TemporalDataImpl<>(Map.of(timestamp1, RaoInput.build(network1, crac1).build(), timestamp2, RaoInput.build(network2, crac2).build(), timestamp3, RaoInput.build(network3, crac3).build())),
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR3AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(1000.0).withUpwardPowerGradient(200.0).withDownwardPowerGradient(0.0).build())
        );

        // no redispatching required during the first timestamp
        // MARMOT will activate 300 MW however in timestamp 1 : it is the minimum necessary to be able to activate 500 MW in timestamp 2
        // due to the max gradient of 200. Not activating 500 MW in timestamps 2 and 3 will create an overload and be very costly.
        // redispatching of 500 MW in both timestamps 2 & 3 with a cost of 25010 each
        // MARMOT should also activate redispatching at 500 MW for second and third timestamps
        InterTemporalRaoResultImpl interTemporalRaoResult = (InterTemporalRaoResultImpl) new Marmot().run(input, raoParameters).join();
        assertEquals(65030.0, interTemporalRaoResult.getGlobalCost());

        assertEquals(15010.0, interTemporalRaoResult.getCost(crac1.getPreventiveInstant(), timestamp1));
        assertEquals(300.0, interTemporalRaoResult.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));

        assertEquals(25010.0, interTemporalRaoResult.getCost(crac2.getPreventiveInstant(), timestamp2));
        assertEquals(500.0, interTemporalRaoResult.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));

        assertEquals(25010.0, interTemporalRaoResult.getCost(crac3.getPreventiveInstant(), timestamp3));
        assertEquals(500.0, interTemporalRaoResult.getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));
    }

    @Test
    void testWithPreventiveTopologicalAction() throws IOException {
        Network network1 = Network.read("/network/2Nodes3ParallelLinesPST2LinesClosed.uct", MarmotTest.class.getResourceAsStream("/network/2Nodes3ParallelLinesPST2LinesClosed.uct"));
        Network network2 = Network.read("/network/2Nodes3ParallelLinesPST2LinesClosed.uct", MarmotTest.class.getResourceAsStream("/network/2Nodes3ParallelLinesPST2LinesClosed.uct"));
        Crac crac1 = Crac.read("/crac/crac-topo-202502181007.json", MarmotTest.class.getResourceAsStream("/crac/crac-topo-202502181007.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-topo-202502191007.json", MarmotTest.class.getResourceAsStream("/crac/crac-topo-202502191007.json"), network2);
        RaoParameters raoParameters = JsonRaoParameters.read(MarmotTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 18, 10, 7, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 19, 10, 7, 0, 0, ZoneOffset.UTC);

        InterTemporalRaoInput input = new InterTemporalRaoInput(
            new TemporalDataImpl<>(Map.of(timestamp1, RaoInput.build(network1, crac1).build(), timestamp2, RaoInput.build(network2, crac2).build())),
            Set.of(GeneratorConstraints.create().withGeneratorId("FFR1AA1 _generator").withLeadTime(0.0).withLagTime(0.0).withPMin(0.0).withPMax(1000.0).withUpwardPowerGradient(250.0).withDownwardPowerGradient(-250.0).build())
        );

        InterTemporalRaoResultImpl interTemporalRaoResult = (InterTemporalRaoResultImpl) new Marmot().run(input, raoParameters).join();
        assertEquals(40.0, interTemporalRaoResult.getGlobalCost());

        assertEquals(20.0, interTemporalRaoResult.getCost(crac1.getPreventiveInstant(), timestamp1));
        assertTrue(interTemporalRaoResult.isActivated(crac1.getPreventiveState(), crac1.getNetworkAction("closeBeFr2")));

        assertEquals(20.0, interTemporalRaoResult.getCost(crac2.getPreventiveInstant(), timestamp2));
        assertTrue(interTemporalRaoResult.isActivated(crac2.getPreventiveState(), crac2.getNetworkAction("closeBeFr2")));
    }
}
