/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.marmot.OptimizationResultsMerger.mergeResults;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class OptimizationResultsMergerTest {
    private Crac crac1;
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
    RaoParameters parameters;
    TemporalData<RaoInput> inputs;

    @BeforeEach
    void setUp() throws IOException {
        Network network1 = Network.read("12Nodes_2_pst.uct", InterTemporalPrePerimeterSensitivityAnalysisTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        crac1 = Crac.read("small-crac-2pst-1600.json", InterTemporalPrePerimeterSensitivityAnalysisTest.class.getResourceAsStream("/crac/small-crac-2pst-1600.json"), network1);
        RaoInput raoInput1 = RaoInput.build(network1, crac1).build();
        inputs = new TemporalDataImpl<>(Map.of(timestamp1, raoInput1));
        parameters = new RaoParameters();
        parameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().setDc(true);
    }

    @Test
    void testMergeResults() {
        FlowCnec cnec = crac1.getFlowCnec("cnecDeNlPrev - 1600");
        State preventiveState = crac1.getPreventiveState();
        NetworkAction networkAction = crac1.getNetworkAction("open_DE1DE2");
        RangeAction<?> rangeAction = crac1.getRangeAction("pstBe - 1600");
        RangeAction<?> rangeActionCur = crac1.getRangeAction("pstDe - 1600");

        RaoResult raoResult = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(preventiveState)).thenReturn(Set.of(networkAction));

        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Mockito.when(prePerimeterResult.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(12.2);
        Mockito.when(prePerimeterResult.getSetpoint(rangeAction)).thenReturn(4.672743946063913);
        Mockito.when(prePerimeterResult.getSetpoint(rangeActionCur)).thenReturn(4.672743946063913);

        LinearOptimizationResult linearOptimizationResult1 = Mockito.mock(LinearOptimizationResult.class);
        Mockito.when(linearOptimizationResult1.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(345.25);
        Mockito.when(linearOptimizationResult1.getRangeActions()).thenReturn(Set.of(rangeAction, rangeActionCur));
        Mockito.when(linearOptimizationResult1.getOptimizedSetpoint(rangeAction, preventiveState)).thenReturn(6.2276423729910535);
        Mockito.when(linearOptimizationResult1.getOptimizedSetpoint(rangeActionCur, preventiveState)).thenReturn(4.672743946063913);

        TemporalData<RaoResult> mergedResult = mergeResults(new TemporalDataImpl<>(Map.of(timestamp1, raoResult)), new TemporalDataImpl<>(Map.of(timestamp1, linearOptimizationResult1)), inputs, new TemporalDataImpl<>(Map.of(timestamp1, prePerimeterResult)));

        assertEquals(12.2, mergedResult.getData(timestamp1).get().getFlow(null, cnec, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(345.25, mergedResult.getData(timestamp1).get().getFlow(crac1.getPreventiveInstant(), cnec, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(Set.of(networkAction), mergedResult.getData(timestamp1).get().getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(rangeAction), mergedResult.getData(timestamp1).get().getActivatedRangeActionsDuringState(preventiveState));
    }
}
