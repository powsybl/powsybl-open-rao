/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.InterTemporalParametersExtension;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LoadFlowAndSensitivityResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class InterTemporalSensitivityAnalysisTest {
    private Crac crac1;
    private Crac crac2;
    private Crac crac3;
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2024, 12, 10, 17, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2024, 12, 10, 18, 21, 0, 0, ZoneOffset.UTC);
    private InterTemporalSensitivityAnalysis sensitivityAnalysis;
    private RaoParameters parameters;

    private static final double DOUBLE_TOLERANCE = 1e-4;
    private static final double AMPERE_MEGAWATT_TOLERANCE = 1.0;

    @BeforeEach
    void setUp() throws IOException {
        Network network1 = Network.read("12Nodes_2_pst.uct", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        Network network2 = Network.read("12Nodes_2_pst.uct", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));
        Network network3 = Network.read("12Nodes_2_pst.uct", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));

        crac1 = Crac.read("small-crac-2pst-1600.json", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/crac/small-crac-2pst-1600.json"), network1);
        crac2 = Crac.read("small-crac-2pst-1700.json", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/crac/small-crac-2pst-1700.json"), network2);
        crac3 = Crac.read("small-crac-2pst-1800.json", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/crac/small-crac-2pst-1800.json"), network3);

        RaoInput raoInput1 = RaoInput.build(network1, crac1).build();
        RaoInput raoInput2 = RaoInput.build(network2, crac2).build();
        RaoInput raoInput3 = RaoInput.build(network3, crac3).build();

        InterTemporalRaoInput input = new InterTemporalRaoInput(new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3)));
        parameters = new RaoParameters();
        parameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().setDc(true);

        sensitivityAnalysis = new InterTemporalSensitivityAnalysis(input, parameters);
    }

    @Test
    void getRangeActionsPerTimestamp() {
        assertEquals(
                Map.of(timestamp1, Set.of(crac1.getRangeAction("pstBe - 1600")),
                        timestamp2, Set.of(crac1.getRangeAction("pstBe - 1600"), crac2.getRangeAction("pstBe - 1700")),
                        timestamp3, Set.of(crac1.getRangeAction("pstBe - 1600"), crac2.getRangeAction("pstBe - 1700"), crac3.getRangeAction("pstBe - 1800"), crac3.getRangeAction("pstDe - 1800"))),
                sensitivityAnalysis.getRangeActionsPerTimestamp());

    }

    @Test
    void getFlowCnecsPerTimestamp() {
        assertEquals(
                Map.of(timestamp1, Set.of(crac1.getFlowCnec("cnecDeNlPrev - 1600"), crac1.getFlowCnec("cnecDeNlOut - 1600")),
                        timestamp2, Set.of(crac2.getFlowCnec("cnecDeNlPrev - 1700"), crac2.getFlowCnec("cnecDeNlOut - 1700")),
                        timestamp3, Set.of(crac3.getFlowCnec("cnecDeNlPrev - 1800"), crac3.getFlowCnec("cnecDeNlOut - 1800"))),
                sensitivityAnalysis.getFlowCnecsPerTimestamp());

    }

    @Test
    void testRunInitialSensitivityAnalysis() throws InterruptedException {
        TemporalData<LoadFlowAndSensitivityResult> result = sensitivityAnalysis.runInitialSensitivityAnalysis();
        assertEquals(3, result.getDataPerTimestamp().size());
        assertEquals(List.of(timestamp1, timestamp2, timestamp3), result.getTimestamps());

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
        assertTrue(result.getData(timestamp1).isPresent());

        FlowResult flowResultTimestamp1 = result.getData(timestamp1).get().flowResult();
        assertFlowValueMw(flowResultTimestamp1, preventiveCnecTimestamp1, 618.0);
        assertFlowValueMw(flowResultTimestamp1, outageCnecTimestamp1, 1000.0);

        SensitivityResult sensitivityResultTimestamp1 = result.getData(timestamp1).get().sensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp1, preventiveCnecTimestamp1, pstBeTimestamp1, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp1, outageCnecTimestamp1, pstBeTimestamp1, 0.0);

        // Timestamp 2
        assertTrue(result.getData(timestamp2).isPresent());

        FlowResult flowResultTimestamp2 = result.getData(timestamp2).get().flowResult();
        assertFlowValueMw(flowResultTimestamp2, preventiveCnecTimestamp2, 618.0);
        assertFlowValueMw(flowResultTimestamp2, outageCnecTimestamp2, 1000.0);

        SensitivityResult sensitivityResultTimestamp2 = result.getData(timestamp2).get().sensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp2, preventiveCnecTimestamp2, pstBeTimestamp1, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp2, outageCnecTimestamp2, pstBeTimestamp1, 0.0);
        assertSensitivityValue(sensitivityResultTimestamp2, preventiveCnecTimestamp2, pstBeTimestamp2, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp2, outageCnecTimestamp2, pstBeTimestamp2, 0.0);

        // Timestamp 3
        assertTrue(result.getData(timestamp3).isPresent());

        FlowResult flowResultTimestamp3 = result.getData(timestamp3).get().flowResult();
        assertFlowValueMw(flowResultTimestamp3, preventiveCnecTimestamp3, 618.0);
        assertFlowValueMw(flowResultTimestamp3, outageCnecTimestamp3, 1000.0);

        SensitivityResult sensitivityResultTimestamp3 = result.getData(timestamp3).get().sensitivityResult();
        assertSensitivityValue(sensitivityResultTimestamp3, preventiveCnecTimestamp3, pstBeTimestamp1, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp3, outageCnecTimestamp3, pstBeTimestamp1, 0.0);
        assertSensitivityValue(sensitivityResultTimestamp3, preventiveCnecTimestamp3, pstBeTimestamp2, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp3, outageCnecTimestamp3, pstBeTimestamp2, 0.0);
        assertSensitivityValue(sensitivityResultTimestamp3, preventiveCnecTimestamp3, pstBeTimestamp3, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp3, outageCnecTimestamp3, pstBeTimestamp3, 0.0);
        assertSensitivityValue(sensitivityResultTimestamp3, preventiveCnecTimestamp3, pstDeTimestamp3, 25.202534);
        assertSensitivityValue(sensitivityResultTimestamp3, outageCnecTimestamp3, pstDeTimestamp3, 0.0);
    }

    private static void assertFlowValueMw(FlowResult flowResult, FlowCnec flowCnec, double expectedFlowMw) {
        assertEquals(expectedFlowMw, flowResult.getFlow(flowCnec, TwoSides.ONE, Unit.MEGAWATT), AMPERE_MEGAWATT_TOLERANCE);
    }

    private static void assertSensitivityValue(SensitivityResult sensitivityResult, FlowCnec flowCnec, RangeAction<?> rangeAction, double expectedSensitivityValue) {
        assertEquals(expectedSensitivityValue, sensitivityResult.getSensitivityValue(flowCnec, TwoSides.ONE, rangeAction, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testNumberOfThreadsForComputation() {
        assertEquals(3, sensitivityAnalysis.getNumberOfThreads());

        InterTemporalParametersExtension extension = new InterTemporalParametersExtension();
        parameters.addExtension(InterTemporalParametersExtension.class, extension);
        assertEquals(1, sensitivityAnalysis.getNumberOfThreads());

        parameters.getExtension(InterTemporalParametersExtension.class).setSensitivityComputationInParallel(4);
        assertEquals(3, sensitivityAnalysis.getNumberOfThreads());
    }
}
