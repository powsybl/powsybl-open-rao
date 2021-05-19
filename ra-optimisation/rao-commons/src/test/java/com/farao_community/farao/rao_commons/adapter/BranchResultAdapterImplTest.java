/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.adapter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.AbsolutePtdfSumsComputation;
import com.farao_community.farao.rao_commons.result.BranchResultFromMap;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BranchResultAdapterImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private BranchCnec cnec1;
    private BranchCnec cnec2;
    private SystematicSensitivityResult systematicSensitivityResult;
    private BranchResultAdapterImpl.BranchResultAdpaterBuilder branchResultAdpaterBuilder;

    @Before
    public void setUp() {
        cnec1 = Mockito.mock(BranchCnec.class);
        cnec2 = Mockito.mock(BranchCnec.class);
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        branchResultAdpaterBuilder = BranchResultAdapterImpl.create();
    }

    @Test
    public void testBasicReturns() {
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .build();

        when(systematicSensitivityResult.getReferenceFlow(cnec1)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(cnec1)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(cnec2)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(cnec2)).thenReturn(235.);
        BranchResult branchResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(200., branchResult.getFlow(cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58., branchResult.getFlow(cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500., branchResult.getFlow(cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235., branchResult.getFlow(cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(branchResult.getPtdfZonalSum(cnec1)));
    }

    @Test
    public void testWithFixedPtdfs() {
        BranchResult fixedPtdfBranchResult = new BranchResultFromMap(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, 20.));
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
            .withPtdfsResults(fixedPtdfBranchResult)
            .build();

        BranchResult branchResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(20., branchResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithFixedPtdfsAndCommercialFlows() {
        BranchResult ptdfBranchResult = new BranchResultFromMap(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, 20.));
        BranchResult commercialFlowBranchResult = new BranchResultFromMap(systematicSensitivityResult, Map.of(cnec2, 300.), new HashMap<>());
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .withPtdfsResults(ptdfBranchResult)
                .withCommercialFlowsResults(commercialFlowBranchResult)
                .build();

        BranchResult branchResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(20., branchResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        assertEquals(300., branchResult.getCommercialFlow(cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithFixedPtdfsAndUpdatedCommercialFlows() {
        LoopFlowComputation loopFlowComputation = Mockito.mock(LoopFlowComputation.class);
        BranchResult ptdfBranchResult = new BranchResultFromMap(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, 20.));
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder.withPtdfsResults(ptdfBranchResult)
                .withCommercialFlowsResults(loopFlowComputation, Set.of(cnec2))
                .build();

        LoopFlowResult loopFlowResult = Mockito.mock(LoopFlowResult.class);
        when(loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(systematicSensitivityResult, Set.of(cnec2)))
                .thenReturn(loopFlowResult);
        when(loopFlowResult.getCommercialFlow(cnec2)).thenReturn(300.);
        BranchResult branchResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(20., branchResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        assertEquals(300., branchResult.getCommercialFlow(cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithAbsolutePtdfSumsComputation() {
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = Mockito.mock(AbsolutePtdfSumsComputation.class);
        Map<BranchCnec, Double> ptdfZonalSums = Map.of(cnec1, 1.63, cnec2, 0.57);
        when(absolutePtdfSumsComputation.computeAbsolutePtdfSums(any(), any())).thenReturn(ptdfZonalSums);
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder
                .withPtdfsResults(absolutePtdfSumsComputation, Set.of(cnec1, cnec2))
                .build();
        BranchResult branchResult = branchResultAdapter.getResult(systematicSensitivityResult);
        assertEquals(1.63, branchResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        assertEquals(0.57, branchResult.getPtdfZonalSum(cnec2), DOUBLE_TOLERANCE);
        assertEquals(ptdfZonalSums, branchResult.getPtdfZonalSums());
        assertThrows(FaraoException.class, () -> branchResult.getPtdfZonalSum(Mockito.mock(BranchCnec.class)));
    }
}
