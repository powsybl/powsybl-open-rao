/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class PreventiveOptimizationResultTest {
    private Network network1;
    private Network network2;
    private Network network3;
    private RaoInput raoInput1;
    private RaoInput raoInput2;
    private RaoInput raoInput3;
    private RaoResult raoResult1;
    private RaoResult raoResult2;
    private RaoResult raoResult3;

    @BeforeEach
    void setUp() throws IOException {
        network1 = Network.read("12Nodes_2_pst.uct", PreventiveOptimizationResultTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        network2 = Network.read("12Nodes_2_pst.uct", PreventiveOptimizationResultTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        network3 = Network.read("12Nodes_2_pst.uct", PreventiveOptimizationResultTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));

        createInitialScenarioVariant(network1);
        createInitialScenarioVariant(network2);
        createInitialScenarioVariant(network3);

        Crac crac1 = Crac.read("small-crac-2pst-1600.json", PreventiveOptimizationResultTest.class.getResourceAsStream("/crac/small-crac-2pst-1600.json"), network1);
        Crac crac2 = Crac.read("small-crac-2pst-1700.json", PreventiveOptimizationResultTest.class.getResourceAsStream("/crac/small-crac-2pst-1700.json"), network2);
        Crac crac3 = Crac.read("small-crac-2pst-1800.json", PreventiveOptimizationResultTest.class.getResourceAsStream("/crac/small-crac-2pst-1800.json"), network3);

        raoInput1 = RaoInput.build(network1, crac1).build();
        raoInput2 = RaoInput.build(network2, crac2).build();
        raoInput3 = RaoInput.build(network3, crac3).build();

        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().setDc(true);

        raoResult1 = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult1.getActivatedNetworkActionsDuringState(crac1.getPreventiveState())).thenReturn(Set.of(crac1.getNetworkAction("open_DE1DE2")));
        Mockito.when(raoResult1.getActivatedRangeActionsDuringState(crac1.getPreventiveState())).thenReturn(Set.of(crac1.getRangeAction("pstBe - 1600")));
        Mockito.when(raoResult1.getOptimizedSetPointOnState(crac1.getPreventiveState(), crac1.getRangeAction("pstBe - 1600"))).thenReturn(-4.672743946063913);

        raoResult2 = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult2.getActivatedNetworkActionsDuringState(crac2.getPreventiveState())).thenReturn(Set.of(crac2.getNetworkAction("pst_tap5")));

        raoResult3 = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult3.getActivatedNetworkActionsDuringState(crac3.getPreventiveState())).thenReturn(Set.of(crac3.getNetworkAction("shutDownGenerators")));
    }

    private static void createInitialScenarioVariant(Network network) {
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), "InitialScenario");
        network.getVariantManager().setWorkingVariant("InitialScenario");
    }

    @Test
    void testApplyPreventiveNetworkActions() {
        // timestamp 1
        assertTrue(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal1().isConnected());
        assertTrue(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal2().isConnected());

        PreventiveOptimizationResult topologicalOptimizationResult1 = new PreventiveOptimizationResult(raoInput1, raoResult1);
        topologicalOptimizationResult1.applyPreventiveRemedialActions(false);

        assertEquals("InitialScenario_with_topological_actions", network1.getVariantManager().getWorkingVariantId());
        assertFalse(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal1().isConnected());
        assertFalse(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal2().isConnected());

        // timestamp 2
        assertEquals(12, network2.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        assertEquals(0, network2.getTwoWindingsTransformer("DDE2AA1  DDE3AA1  1").getPhaseTapChanger().getTapPosition());

        PreventiveOptimizationResult preventiveOptimizationResult2 = new PreventiveOptimizationResult(raoInput2, raoResult2);
        preventiveOptimizationResult2.applyPreventiveRemedialActions(false);

        assertEquals("InitialScenario_with_topological_actions", network2.getVariantManager().getWorkingVariantId());
        assertEquals(5, network2.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        assertEquals(5, network2.getTwoWindingsTransformer("DDE2AA1  DDE3AA1  1").getPhaseTapChanger().getTapPosition());

        // timestamp 3
        assertEquals(-1000.0, network3.getGenerator("FFR1AA1 _generator").getTargetP());
        assertEquals(1000.0, network3.getGenerator("NNL1AA1 _generator").getTargetP());

        PreventiveOptimizationResult preventiveOptimizationResult3 = new PreventiveOptimizationResult(raoInput3, raoResult3);
        preventiveOptimizationResult3.applyPreventiveRemedialActions(false);

        assertEquals("InitialScenario_with_topological_actions", network3.getVariantManager().getWorkingVariantId());
        assertEquals(0.0, network3.getGenerator("NNL1AA1 _generator").getTargetP());
        assertEquals(0.0, network3.getGenerator("FFR1AA1 _generator").getTargetP());
    }

    @Test
    void testApplyAllPreventiveActions() {
        // timestamp 1
        assertTrue(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal1().isConnected());
        assertTrue(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal2().isConnected());
        assertEquals(12, network1.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());

        PreventiveOptimizationResult topologicalOptimizationResult1 = new PreventiveOptimizationResult(raoInput1, raoResult1);
        topologicalOptimizationResult1.applyPreventiveRemedialActions(true);

        assertEquals("InitialScenario_with_topological_actions", network1.getVariantManager().getWorkingVariantId());
        assertFalse(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal1().isConnected());
        assertFalse(network1.getLine("DDE1AA1  DDE2AA1  1").getTerminal2().isConnected());
        assertEquals(-12, network1.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());

        // timestamp 2
        assertEquals(12, network2.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        assertEquals(0, network2.getTwoWindingsTransformer("DDE2AA1  DDE3AA1  1").getPhaseTapChanger().getTapPosition());

        PreventiveOptimizationResult preventiveOptimizationResult2 = new PreventiveOptimizationResult(raoInput2, raoResult2);
        preventiveOptimizationResult2.applyPreventiveRemedialActions(true);

        assertEquals("InitialScenario_with_topological_actions", network2.getVariantManager().getWorkingVariantId());
        assertEquals(5, network2.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        assertEquals(5, network2.getTwoWindingsTransformer("DDE2AA1  DDE3AA1  1").getPhaseTapChanger().getTapPosition());

        // timestamp 3
        assertEquals(-1000.0, network3.getGenerator("FFR1AA1 _generator").getTargetP());
        assertEquals(1000.0, network3.getGenerator("NNL1AA1 _generator").getTargetP());

        PreventiveOptimizationResult preventiveOptimizationResult3 = new PreventiveOptimizationResult(raoInput3, raoResult3);
        preventiveOptimizationResult3.applyPreventiveRemedialActions(true);

        assertEquals("InitialScenario_with_topological_actions", network3.getVariantManager().getWorkingVariantId());
        assertEquals(0.0, network3.getGenerator("NNL1AA1 _generator").getTargetP());
        assertEquals(0.0, network3.getGenerator("FFR1AA1 _generator").getTargetP());
    }
}
