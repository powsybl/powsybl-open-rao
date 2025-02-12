/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPostOptimizationResults;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPreventivePerimeterCnecs;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getTopologicalOptimizationResult;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runInitialPrePerimeterSensitivityAnalysis;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class MarmotUtilsTest {
    private Crac crac1;
    private Crac crac2;
    private Crac crac3;
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2024, 12, 10, 17, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2024, 12, 10, 18, 21, 0, 0, ZoneOffset.UTC);
    RaoParameters parameters;
    TemporalData<RaoInput> inputs;
    private static final double DOUBLE_TOLERANCE = 1e-4;
    private static final double AMPERE_MEGAWATT_TOLERANCE = 1.0;

    @BeforeEach
    void setUp() throws IOException {
        Network network1 = Network.read("12Nodes_2_pst.uct", MarmotUtilsTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        Network network2 = Network.read("12Nodes_2_pst.uct", MarmotUtilsTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        Network network3 = Network.read("12Nodes_2_pst.uct", MarmotUtilsTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));

        crac1 = Crac.read("small-crac-2pst-1600.json", MarmotUtilsTest.class.getResourceAsStream("/crac/small-crac-2pst-1600.json"), network1);
        crac2 = Crac.read("small-crac-2pst-1700.json", MarmotUtilsTest.class.getResourceAsStream("/crac/small-crac-2pst-1700.json"), network2);
        crac3 = Crac.read("small-crac-2pst-1800.json", MarmotUtilsTest.class.getResourceAsStream("/crac/small-crac-2pst-1800.json"), network3);

        RaoInput raoInput1 = RaoInput.build(network1, crac1).build();
        RaoInput raoInput2 = RaoInput.build(network2, crac2).build();
        RaoInput raoInput3 = RaoInput.build(network3, crac3).build();

        inputs = new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3));
        parameters = new RaoParameters();
        parameters.getExtension(LoadFlowParameters.class).setDc(true);
    }

    @Test
    void testGetPreventivePerimeterCnecs() {
        FlowCnec preventiveCnecTimestamp1 = crac1.getFlowCnec("cnecDeNlPrev - 1600");
        FlowCnec outageCnecTimestamp1 = crac1.getFlowCnec("cnecDeNlOut - 1600");
        assertEquals(Set.of(preventiveCnecTimestamp1, outageCnecTimestamp1), getPreventivePerimeterCnecs(crac1));
    }

    @Test
    void testRunInitialSensitivityAnalysis() {
        FlowCnec preventiveCnecTimestamp1 = crac1.getFlowCnec("cnecDeNlPrev - 1600");
        FlowCnec outageCnecTimestamp1 = crac1.getFlowCnec("cnecDeNlOut - 1600");
        FlowCnec preventiveCnecTimestamp2 = crac2.getFlowCnec("cnecDeNlPrev - 1700");
        FlowCnec outageCnecTimestamp2 = crac2.getFlowCnec("cnecDeNlOut - 1700");
        FlowCnec preventiveCnecTimestamp3 = crac3.getFlowCnec("cnecDeNlPrev - 1800");
        FlowCnec outageCnecTimestamp3 = crac3.getFlowCnec("cnecDeNlOut - 1800");

        RangeAction<?> pstBeTimestamp1 = crac1.getRangeAction("pstBe - 1600");
        RangeAction<?> pstBeTimestamp2 = crac2.getRangeAction("pstBe - 1700");
        RangeAction<?> pstBeTimestamp3 = crac3.getRangeAction("pstBe - 1800");
        RangeAction<?> pstDeTimestamp3 = crac3.getRangeAction("pstDe - 1800");

        // Timestamp 1
        PrePerimeterResult prePerimeterResult1 = runInitialPrePerimeterSensitivityAnalysis(inputs.getData(timestamp1).get(), parameters);

        FlowResult flowResultTimestamp1 = prePerimeterResult1.getFlowResult();
        assertFlowValueMw(flowResultTimestamp1, preventiveCnecTimestamp1, -382.0);
        assertFlowValueMw(flowResultTimestamp1, outageCnecTimestamp1, -1000.0);

        SensitivityResult sensitivityResultTimestamp1 = prePerimeterResult1.getSensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp1, preventiveCnecTimestamp1, pstBeTimestamp1, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp1, outageCnecTimestamp1, pstBeTimestamp1, 0.0);

        RangeActionSetpointResult setPointResultTimestamp1 = prePerimeterResult1.getRangeActionSetpointResult();
        assertEquals(12, setPointResultTimestamp1.getTap((PstRangeAction) pstBeTimestamp1));

        // Timestamp 2
        PrePerimeterResult prePerimeterResult2 = runInitialPrePerimeterSensitivityAnalysis(inputs.getData(timestamp2).get(), parameters);

        FlowResult flowResultTimestamp2 = prePerimeterResult2.getFlowResult();
        assertFlowValueMw(flowResultTimestamp2, preventiveCnecTimestamp2, -382.0);
        assertFlowValueMw(flowResultTimestamp2, outageCnecTimestamp2, -1000.0);

        SensitivityResult sensitivityResultTimestamp2 = prePerimeterResult2.getSensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp2, preventiveCnecTimestamp2, pstBeTimestamp2, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp2, outageCnecTimestamp2, pstBeTimestamp2, 0.0);

        RangeActionSetpointResult setPointResultTimestamp2 = prePerimeterResult2.getRangeActionSetpointResult();
        assertEquals(12, setPointResultTimestamp2.getTap((PstRangeAction) pstBeTimestamp2));

        // Timestamp 3
        PrePerimeterResult prePerimeterResult3 = runInitialPrePerimeterSensitivityAnalysis(inputs.getData(timestamp3).get(), parameters);

        FlowResult flowResultTimestamp3 = prePerimeterResult3.getFlowResult();
        assertFlowValueMw(flowResultTimestamp3, preventiveCnecTimestamp3, -382.0);
        assertFlowValueMw(flowResultTimestamp3, outageCnecTimestamp3, -1000.0);

        SensitivityResult sensitivityResultTimestamp3 = prePerimeterResult3.getSensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp3, preventiveCnecTimestamp3, pstBeTimestamp3, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp3, outageCnecTimestamp3, pstBeTimestamp3, 0.0);
        assertSensitivityValue(sensitivityResultTimestamp3, preventiveCnecTimestamp3, pstDeTimestamp3, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp3, outageCnecTimestamp3, pstDeTimestamp3, 0.0);

        RangeActionSetpointResult setPointResultTimestamp3 = prePerimeterResult3.getRangeActionSetpointResult();
        assertEquals(12, setPointResultTimestamp3.getTap((PstRangeAction) pstBeTimestamp3));
        assertEquals(0, setPointResultTimestamp3.getTap((PstRangeAction) pstDeTimestamp3));

    }

    private static void assertFlowValueMw(FlowResult flowResult, FlowCnec flowCnec, double expectedFlowMw) {
        assertEquals(expectedFlowMw, flowResult.getFlow(flowCnec, TwoSides.ONE, Unit.MEGAWATT), AMPERE_MEGAWATT_TOLERANCE);
    }

    private static void assertSensitivityValue(SensitivityResult sensitivityResult, FlowCnec flowCnec, RangeAction<?> rangeAction, double expectedSensitivityValue) {
        assertEquals(expectedSensitivityValue, sensitivityResult.getSensitivityValue(flowCnec, TwoSides.ONE, rangeAction, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetTopologicalOptimizationResult() {
        RaoResult raoResult1 = Mockito.mock(RaoResult.class);
        RaoResult raoResult2 = Mockito.mock(RaoResult.class);
        RaoResult raoResult3 = Mockito.mock(RaoResult.class);
        TemporalData<RaoResult> raoResults = new TemporalDataImpl<>(Map.of(timestamp1, raoResult1, timestamp2, raoResult2, timestamp3, raoResult3));

        TemporalData<TopologicalOptimizationResult> topologicalOptimizationResults = getTopologicalOptimizationResult(inputs, raoResults);
        assertEquals(List.of(timestamp1, timestamp2, timestamp3), topologicalOptimizationResults.getTimestamps());

        TopologicalOptimizationResult topologicalOptimizationResult1 = topologicalOptimizationResults.getData(timestamp1).get();
        assertEquals(crac1, topologicalOptimizationResult1.raoInput().getCrac());
        assertEquals(raoResult1, topologicalOptimizationResult1.topologicalOptimizationResult());

        TopologicalOptimizationResult topologicalOptimizationResult2 = topologicalOptimizationResults.getData(timestamp2).get();
        assertEquals(crac2, topologicalOptimizationResult2.raoInput().getCrac());
        assertEquals(raoResult2, topologicalOptimizationResult2.topologicalOptimizationResult());

        TopologicalOptimizationResult topologicalOptimizationResult3 = topologicalOptimizationResults.getData(timestamp3).get();
        assertEquals(crac3, topologicalOptimizationResult3.raoInput().getCrac());
        assertEquals(raoResult3, topologicalOptimizationResult3.topologicalOptimizationResult());
    }

    @Test
    void testGetPostOptimizationResults() {
        RaoResult raoResult1 = Mockito.mock(RaoResult.class);
        RaoResult raoResult2 = Mockito.mock(RaoResult.class);
        RaoResult raoResult3 = Mockito.mock(RaoResult.class);
        TemporalData<RaoResult> raoResults = new TemporalDataImpl<>(Map.of(timestamp1, raoResult1, timestamp2, raoResult2, timestamp3, raoResult3));

        PrePerimeterResult prePerimeterResult1 = Mockito.mock(PrePerimeterResult.class);
        PrePerimeterResult prePerimeterResult2 = Mockito.mock(PrePerimeterResult.class);
        PrePerimeterResult prePerimeterResult3 = Mockito.mock(PrePerimeterResult.class);
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>(Map.of(timestamp1, prePerimeterResult1, timestamp2, prePerimeterResult2, timestamp3, prePerimeterResult3));

        LinearOptimizationResult linearOptimizationResult1 = Mockito.mock(LinearOptimizationResult.class);
        LinearOptimizationResult linearOptimizationResult2 = Mockito.mock(LinearOptimizationResult.class);
        LinearOptimizationResult linearOptimizationResult3 = Mockito.mock(LinearOptimizationResult.class);
        TemporalData<LinearOptimizationResult> linearOptimizationResults = new TemporalDataImpl<>(Map.of(timestamp1, linearOptimizationResult1, timestamp2, linearOptimizationResult2, timestamp3, linearOptimizationResult3));

        TemporalData<PostOptimizationResult> postOptimizationResults = getPostOptimizationResults(inputs, prePerimeterResults, linearOptimizationResults, raoResults);
        assertEquals(List.of(timestamp1, timestamp2, timestamp3), postOptimizationResults.getTimestamps());

        PostOptimizationResult postOptimizationResult1 = postOptimizationResults.getData(timestamp1).get();
        assertEquals(crac1, postOptimizationResult1.raoInput().getCrac());
        assertEquals(prePerimeterResult1, postOptimizationResult1.initialResult());
        assertEquals(linearOptimizationResult1, postOptimizationResult1.linearOptimizationResult());
        assertEquals(raoResult1, postOptimizationResult1.topologicalOptimizationResult());

        PostOptimizationResult postOptimizationResult2 = postOptimizationResults.getData(timestamp2).get();
        assertEquals(crac2, postOptimizationResult2.raoInput().getCrac());
        assertEquals(prePerimeterResult2, postOptimizationResult2.initialResult());
        assertEquals(linearOptimizationResult2, postOptimizationResult2.linearOptimizationResult());
        assertEquals(raoResult2, postOptimizationResult2.topologicalOptimizationResult());

        PostOptimizationResult postOptimizationResult3 = postOptimizationResults.getData(timestamp3).get();
        assertEquals(crac3, postOptimizationResult3.raoInput().getCrac());
        assertEquals(prePerimeterResult3, postOptimizationResult3.initialResult());
        assertEquals(linearOptimizationResult3, postOptimizationResult3.linearOptimizationResult());
        assertEquals(raoResult3, postOptimizationResult3.topologicalOptimizationResult());
    }
}
