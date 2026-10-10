/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
class TimeCoupledCurativeSynchronizationTest {
    private static final String CONTINGENCY = "Contingency L1";

    /**
     * This is a test case with 2 timestamps, sharing the exact same network, 1 curative PST and 1 preventive redispatching
     * action. Each timestamp has one preventive CNEC that is not critical and one critical curative CNEC.
     * <p>
     *     Before the loss of line 1 (Contingency L1). The 1000 MW flow is shared by three lines. After the loss, there
     *     are only 2 paths remaining : line 2 and the line with the PST each one of them carrying 500 MW. But in curative,
     *     these two lines are CNECs with a 450 MW threshold (at 19:30 the line with the PST is the CNEC, at 20:30, line 2
     *     is CNEC) both timestamps are therefore overloaded by 50 MW.
     * </p>
     * <p>
     *     Nothing happens in preventive. The curative synchronization does not solve the overloads because relieving one
     *     line overloads the other and therefore the tap stays at 0. It is only the redispatching that can secure both
     *     timestamps but since preventive CNECs are secured, it is not activated during the preventive optimizations.
     *     It is the global MIP (launched when second preventive is parameters are enabled) that solves the situation
     *     by activating 120 MW of redispatching on each timestamp to bring the flow down to 440 MW for both CNECs.
     * </p>
     */
    @Test
    void testGlobalMipSavesTheOverload() throws IOException {
        String networkFilePath = "/network/3NodesPST.uct";
        Network network1 = Network.read(networkFilePath, TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream(networkFilePath));
        Network network2 = Network.read(networkFilePath, TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream(networkFilePath));

        Crac crac1 = Crac.read("/crac/crac-202511041930.json", TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream("/crac/crac-202511041930.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-202511042030.json", TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream("/crac/crac-202511042030.json"), network2);
        RaoParameters raoParameters = JsonRaoParameters.read(
            TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc_with_2P.json"),
            ReportNode.NO_OP
        );

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 11, 4, 19, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 11, 4, 20, 30, 0, 0, ZoneOffset.UTC);

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>(
            Map.of(
                timestamp1, RaoInput.build(network1, crac1).build(),
                timestamp2, RaoInput.build(network2, crac2).build()
            )
        );
        TimeCoupledRaoInput inputs = new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints());
        TimeCoupledRaoResult results = new Marmot().run(inputs, raoParameters, ReportNode.NO_OP).join();

        // the common pst is not activated at all.
        assertEquals(0, results.getOptimizedSetPointOnState(getCurativeState(crac1), crac1.getRangeAction("pstCurativeAction")));
        assertEquals(0, results.getOptimizedSetPointOnState(getCurativeState(crac2), crac2.getRangeAction("pstCurativeAction")));
        // global MIP activates preventive redispatching to solve the overloadss
        assertTrue(results.isActivatedDuringState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertTrue(results.isActivatedDuringState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        // 120 MW per timestamp
        assertEquals(120.0, results.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("redispatchingAction")));
        assertEquals(120.0, results.getOptimizedSetPointOnState(crac2.getPreventiveState(), crac2.getRangeAction("redispatchingAction")));
        // global cost = 120 * 10 (variation cost) + 120 * 10 (variation cost) + 10 (activation cost) + 10 (activation cost)
        assertEquals(2420.0, results.getGlobalCost(crac1.getLastInstant()));
    }

    /**
     * Same case a above, the oly difference is that both timestamps have the same curative CNEC which is the line with the PST
     * with a slightly different threshold. Meaning it's almost the same situation at both timestamps.
     * <p>
     *     Both timestamps now need the PST to behave in the same way. In this case, the curative synchronization is useful and
     *     sets the PST at the same tap on both timestamps.
     *     Since the PST is not enough to solve the overload, the global MIP intervenes by activating preventive redispatching.
     * </p>
     */
    @Test
    void testCurativeSynchronizationAndGlobalMip() throws IOException {
        String networkFilePath = "/network/3NodesPST.uct";
        Network network1 = Network.read(networkFilePath, TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream(networkFilePath));
        Network network2 = Network.read(networkFilePath, TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream(networkFilePath));

        Crac crac1 = Crac.read("/crac/crac-202511041930-same-cnec.json", TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream("/crac/crac-202511041930-same-cnec.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-202511042030-same-cnec.json", TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream("/crac/crac-202511042030-same-cnec.json"), network2);
        RaoParameters raoParameters = JsonRaoParameters.read(
            TimeCoupledCurativeSynchronizationTest.class.getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc_with_2P.json"),
            ReportNode.NO_OP
        );

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 11, 4, 19, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 11, 4, 20, 30, 0, 0, ZoneOffset.UTC);

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>(
                Map.of(
                        timestamp1, RaoInput.build(network1, crac1).build(),
                        timestamp2, RaoInput.build(network2, crac2).build()
                )
        );
        TimeCoupledRaoInput inputs = new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints());
        TimeCoupledRaoResult results = new Marmot().run(inputs, raoParameters, ReportNode.NO_OP).join();
        assertEquals(3, results.getOptimizedTapOnState(getCurativeState(crac1), crac1.getPstRangeAction("pstCurativeAction")));
        assertEquals(3, results.getOptimizedTapOnState(getCurativeState(crac2), crac2.getPstRangeAction("pstCurativeAction")));
        // global cost = 10 * 125 + 10 * 25 + 10 + 10
        assertEquals(1520.0, results.getGlobalCost(crac1.getLastInstant()));
    }

    private static State getCurativeState(Crac crac) {
        return crac.getState(CONTINGENCY, crac.getLastInstant());
    }
}
