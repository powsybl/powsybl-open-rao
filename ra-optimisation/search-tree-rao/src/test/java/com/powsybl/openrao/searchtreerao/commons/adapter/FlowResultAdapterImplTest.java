/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.adapter;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.loopflowcomputation.LoopFlowResult;
import com.powsybl.openrao.searchtreerao.commons.AbsolutePtdfSumsComputation;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.impl.FlowResultImpl;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class FlowResultAdapterImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;
    private static final double PTDF_SUM_LOWER_BOUND = 0.01;

    private Network network;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private SystematicSensitivityResult systematicSensitivityResult;
    private BranchResultAdapterImpl.BranchResultAdpaterBuilder branchResultAdpaterBuilder;

    @BeforeEach
    public void setUp() {
        cnec1 = Mockito.mock(FlowCnec.class);
        when(cnec1.getMonitoredSides()).thenReturn(Collections.singleton(ONE));
        cnec2 = Mockito.mock(FlowCnec.class);
        when(cnec1.getMonitoredSides()).thenReturn(Collections.singleton(TWO));
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        branchResultAdpaterBuilder = BranchResultAdapterImpl.create();
        network = Mockito.mock(Network.class);
    }

    @Test
    void testBasicReturns() {
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .build();

        when(systematicSensitivityResult.getReferenceFlow(cnec1, ONE)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(cnec1, ONE)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(cnec2, TWO)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(cnec2, TWO)).thenReturn(235.);
        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult, network);

        assertEquals(200., flowResult.getFlow(cnec1, ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58., flowResult.getFlow(cnec1, ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500., flowResult.getFlow(cnec2, TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235., flowResult.getFlow(cnec2, TWO, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(flowResult.getPtdfZonalSum(cnec1, ONE)));
    }

    @Test
    void testWithFixedPtdfs() {
        FlowResult fixedPtdfFlowResult = new FlowResultImpl(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, Map.of(ONE, 20.)), PTDF_SUM_LOWER_BOUND);
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
            .withPtdfsResults(fixedPtdfFlowResult)
            .build();

        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult, network);

        assertEquals(20., flowResult.getPtdfZonalSum(cnec1, ONE), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithFixedPtdfsAndCommercialFlows() {
        FlowResult ptdfFlowResult = new FlowResultImpl(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, Map.of(ONE, 20.)), PTDF_SUM_LOWER_BOUND);
        FlowResult commercialFlowFlowResult = new FlowResultImpl(systematicSensitivityResult, Map.of(cnec2, Map.of(TWO, 300.)), new HashMap<>(), PTDF_SUM_LOWER_BOUND);
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .withPtdfsResults(ptdfFlowResult)
                .withCommercialFlowsResults(commercialFlowFlowResult)
                .build();

        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult, network);

        assertEquals(20., flowResult.getPtdfZonalSum(cnec1, ONE), DOUBLE_TOLERANCE);
        assertEquals(300., flowResult.getCommercialFlow(cnec2, TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithFixedPtdfsAndUpdatedCommercialFlows() {
        LoopFlowComputation loopFlowComputation = Mockito.mock(LoopFlowComputation.class);
        FlowResult ptdfFlowResult = new FlowResultImpl(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, Map.of(ONE, 20.)), PTDF_SUM_LOWER_BOUND);
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder.withPtdfsResults(ptdfFlowResult)
                .withCommercialFlowsResults(loopFlowComputation, Set.of(cnec2))
                .build();

        LoopFlowResult loopFlowResult = Mockito.mock(LoopFlowResult.class);
        when(loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(systematicSensitivityResult, Set.of(cnec2), network))
                .thenReturn(loopFlowResult);
        when(loopFlowResult.getCommercialFlow(cnec2, TWO)).thenReturn(300.);
        when(loopFlowResult.getCommercialFlowsMap()).thenReturn(Map.of(cnec2, Map.of(TWO, 300.)));
        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult, network);

        assertEquals(20., flowResult.getPtdfZonalSum(cnec1, ONE), DOUBLE_TOLERANCE);
        assertEquals(300., flowResult.getCommercialFlow(cnec2, TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithAbsolutePtdfSumsComputation() {
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = Mockito.mock(AbsolutePtdfSumsComputation.class);
        Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums = Map.of(cnec1, Map.of(ONE, 1.63), cnec2, Map.of(TWO, 0.57));
        when(absolutePtdfSumsComputation.computeAbsolutePtdfSums(any(), any())).thenReturn(ptdfZonalSums);
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .withPtdfsResults(absolutePtdfSumsComputation, Set.of(cnec1, cnec2))
                .build();
        FlowResult flowResult = branchResultAdapter.getResult(systematicSensitivityResult, network);
        assertEquals(1.63, flowResult.getPtdfZonalSum(cnec1, ONE), DOUBLE_TOLERANCE);
        assertEquals(0.57, flowResult.getPtdfZonalSum(cnec2, TWO), DOUBLE_TOLERANCE);
        assertEquals(ptdfZonalSums, flowResult.getPtdfZonalSums());
        assertThrows(OpenRaoException.class, () -> flowResult.getPtdfZonalSum(Mockito.mock(FlowCnec.class), ONE));
    }
}
