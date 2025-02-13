/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class TopologicalOptimizationResultTest {
    private Crac crac1;
    private Crac crac2;
    private Crac crac3;
    private Network network1;
    private Network network2;
    private Network network3;
    private RaoInput raoInput1;
    private RaoInput raoInput2;
    private RaoInput raoInput3;
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2024, 12, 10, 17, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2024, 12, 10, 18, 21, 0, 0, ZoneOffset.UTC);
    RaoParameters parameters;
    TemporalData<RaoInput> raoInputs;

    @BeforeEach
    void setUp() throws IOException {
        network1 = Network.read("12Nodes_2_pst.uct", TopologicalOptimizationResultTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        network2 = Network.read("12Nodes_2_pst.uct", TopologicalOptimizationResultTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        network3 = Network.read("12Nodes_2_pst.uct", TopologicalOptimizationResultTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));

        crac1 = Crac.read("small-crac-2pst-1600.json", TopologicalOptimizationResultTest.class.getResourceAsStream("/crac/small-crac-2pst-1600.json"), network1);
        crac2 = Crac.read("small-crac-2pst-1700.json", TopologicalOptimizationResultTest.class.getResourceAsStream("/crac/small-crac-2pst-1700.json"), network2);
        crac3 = Crac.read("small-crac-2pst-1800.json", TopologicalOptimizationResultTest.class.getResourceAsStream("/crac/small-crac-2pst-1800.json"), network3);

        raoInput1 = RaoInput.build(network1, crac1).build();
        raoInput2 = RaoInput.build(network2, crac2).build();
        raoInput3 = RaoInput.build(network3, crac3).build();

        raoInputs = new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3));
        parameters = new RaoParameters();
        parameters.getExtension(LoadFlowParameters.class).setDc(true);
    }

    @Test
    void testApplyPreventiveNetworkActions() {
        // timestamp 1
        RaoResult raoResult1 = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult1.getActivatedNetworkActionsDuringState(crac1.getPreventiveState())).thenReturn(Set.of(crac1.getNetworkAction("open_DE1DE2")));

        String networkVariantId1 = network1.getVariantManager().getWorkingVariantId();

        assertTrue(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal1().isConnected());
        assertTrue(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal2().isConnected());

        TopologicalOptimizationResult topologicalOptimizationResult1 = new TopologicalOptimizationResult(raoInput1, raoResult1);
        topologicalOptimizationResult1.applyTopologicalActions();

        assertEquals(networkVariantId1 + "_with_topological_actions", network1.getVariantManager().getWorkingVariantId());
        assertFalse(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal1().isConnected());
        assertFalse(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal2().isConnected());

        // timestamp 2
        RaoResult raoResult2 = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult2.getActivatedNetworkActionsDuringState(crac2.getPreventiveState())).thenReturn(Set.of(crac2.getNetworkAction("pst_tap5")));

        String networkVariantId2 = network2.getVariantManager().getWorkingVariantId();

        assertEquals(12, network2.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        assertEquals(0, network2.getTwoWindingsTransformer("DDE2AA1  DDE3AA1  1").getPhaseTapChanger().getTapPosition());

        TopologicalOptimizationResult topologicalOptimizationResult2 = new TopologicalOptimizationResult(raoInput2, raoResult2);
        topologicalOptimizationResult2.applyTopologicalActions();

        assertEquals(networkVariantId2 + "_with_topological_actions", network2.getVariantManager().getWorkingVariantId());
        assertEquals(5, network2.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        assertEquals(5, network2.getTwoWindingsTransformer("DDE2AA1  DDE3AA1  1").getPhaseTapChanger().getTapPosition());

        // timestamp 3
        RaoResult raoResult3 = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult3.getActivatedNetworkActionsDuringState(crac3.getPreventiveState())).thenReturn(Set.of(crac3.getNetworkAction("shutDownGenerators")));

        String networkVariantId3 = network3.getVariantManager().getWorkingVariantId();

        assertEquals(-1000.0, network3.getGenerator("FFR1AA1 _generator").getTargetP());
        assertEquals(1000.0, network3.getGenerator("NNL1AA1 _generator").getTargetP());

        TopologicalOptimizationResult topologicalOptimizationResult3 = new TopologicalOptimizationResult(raoInput3, raoResult3);
        topologicalOptimizationResult3.applyTopologicalActions();

        assertEquals(networkVariantId3 + "_with_topological_actions", network3.getVariantManager().getWorkingVariantId());
        assertEquals(0.0, network3.getGenerator("NNL1AA1 _generator").getTargetP());
        assertEquals(0.0, network3.getGenerator("FFR1AA1 _generator").getTargetP());

    }
}
