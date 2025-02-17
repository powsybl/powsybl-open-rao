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
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MarmotTest {
    @Test
    void runCaseWithTwoTimestampsAndNoGradient() throws IOException {
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
            Set.of(new PowerGradient("FFR1AA1 _generator", -1000d, 1000d))
        );

        // first RAOs shift tap to -5 for a cost of 55 each
        // MARMOT should also move the tap to -5 for both timestamps with a total cost of 110
        TemporalData<RaoResult> results = new Marmot().run(input, raoParameters).join();
        assertEquals(-5, results.getData(timestamp1).get().getOptimizedTapOnState(crac1.getPreventiveState(), crac1.getPstRangeAction("pstBeFr2")));
        assertEquals(-5, results.getData(timestamp2).get().getOptimizedTapOnState(crac2.getPreventiveState(), crac2.getPstRangeAction("pstBeFr2")));
    }

    @Test
    void testWithRedispatchingAndNoGradient() throws IOException {
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
            Set.of(new PowerGradient("FFR1AA1 _generator", -250d, 250d))
        );

        // no redispatching required during the first timestamp
        // redispatching of 500 MW in both timestamps 2 & 3 with a cost of 25010 each
        // MARMOT should also activate redispatching at 500 MW for second and third timestamps
        TemporalData<RaoResult> results = new Marmot().run(input, raoParameters).join();

        assertEquals(-0.0, results.getData(timestamp1).get().getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(500.0, results.getData(timestamp2).get().getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(500.0, results.getData(timestamp3).get().getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));
    }

    @Test
    void testWithRedispatchingAndGradient() throws IOException {
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
            Set.of(new PowerGradient("FFR3AA1 _generator", -250d, 250d))
        );

        // no redispatching required during the first timestamp
        // MARMOT will activate 250 MW however since it helps anticipating the power gradient without creating an overload at the end of timestamp 2
        // redispatching of 500 MW in both timestamps 2 & 3 with a cost of 25010 each
        // MARMOT should also activate redispatching at 500 MW for second and third timestamps
        TemporalData<RaoResult> results = new Marmot().run(input, raoParameters).join();

        assertEquals(250.0, results.getData(timestamp1).get().getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(500.0, results.getData(timestamp2).get().getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        assertEquals(500.0, results.getData(timestamp3).get().getOptimizedSetPointOnState(crac3.getPreventiveState(), crac3.getRangeAction("redispatchingAction")));
    }
}
