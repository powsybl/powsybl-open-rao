/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class PostOptimizationResultTest {
    private Crac crac;
    private RaoInput raoInput;

    @BeforeEach
    void setUp() throws IOException {
        Network network = Network.read("12Nodes_2_pst.uct", PostOptimizationResultTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        crac = Crac.read("small-crac-2pst-1600.json", PostOptimizationResultTest.class.getResourceAsStream("/crac/small-crac-2pst-1600.json"), network);
        raoInput = RaoInput.build(network, crac).build();
    }

    @Test
    void testMergeResults() {
        FlowCnec cnec = crac.getFlowCnec("cnecDeNlPrev - 1600");
        State preventiveState = crac.getPreventiveState();
        NetworkAction networkAction = crac.getNetworkAction("open_DE1DE2");
        RangeAction<?> rangeAction = crac.getRangeAction("pstBe - 1600");
        RangeAction<?> rangeActionCur = crac.getRangeAction("pstDe - 1600");

        RaoResult raoResult = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(preventiveState)).thenReturn(Set.of(networkAction));

        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Mockito.when(prePerimeterResult.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(12.2);
        Mockito.when(prePerimeterResult.getSetpoint(rangeAction)).thenReturn(4.672743946063913);
        Mockito.when(prePerimeterResult.getSetpoint(rangeActionCur)).thenReturn(4.672743946063913);

        LinearOptimizationResult linearOptimizationResult = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(linearOptimizationResult.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(345.25);
        Mockito.when(linearOptimizationResult.getRangeActions()).thenReturn(Set.of(rangeAction, rangeActionCur));
        Mockito.when(linearOptimizationResult.getOptimizedSetpoint(rangeAction, preventiveState)).thenReturn(6.2276423729910535);
        Mockito.when(linearOptimizationResult.getOptimizedSetpoint(rangeActionCur, preventiveState)).thenReturn(4.672743946063913);

        PostOptimizationResult postOptimizationResult = new PostOptimizationResult(raoInput, prePerimeterResult, linearOptimizationResult, raoResult);

        RaoResult mergedResults = postOptimizationResult.merge();

        assertEquals(12.2, mergedResults.getFlow(null, cnec, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(345.25, mergedResults.getFlow(crac.getPreventiveInstant(), cnec, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(Set.of(networkAction), mergedResults.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(rangeAction), mergedResults.getActivatedRangeActionsDuringState(preventiveState));
    }
}
