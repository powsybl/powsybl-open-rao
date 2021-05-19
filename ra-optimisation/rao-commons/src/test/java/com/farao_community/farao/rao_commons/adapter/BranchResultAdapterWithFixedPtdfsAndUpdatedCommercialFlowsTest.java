/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.adapter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.result.BranchResultFromMap;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BranchResultAdapterWithFixedPtdfsAndUpdatedCommercialFlowsTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testBasicReturns() {
        BranchCnec cnec1 = Mockito.mock(BranchCnec.class);
        BranchCnec cnec2 = Mockito.mock(BranchCnec.class);
        LoopFlowComputation loopFlowComputation = Mockito.mock(LoopFlowComputation.class);
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        BranchResultAdapterImpl.BranchResultAdpaterBuilder branchResultAdpaterBuilder = new BranchResultAdapterImpl.BranchResultAdpaterBuilder();
        BranchResult ptdfBranchResult = new BranchResultFromMap(systematicSensitivityResult, new HashMap<>(), Map.of(cnec1, 20.));
        BranchResultAdapter branchResultAdapter = branchResultAdpaterBuilder.withPtdfsResults(ptdfBranchResult)
            .withCommercialFlowsResults(loopFlowComputation, Set.of(cnec2))
            .build();
        /*BranchResultAdapter branchResultAdapter = new BranchResultAdapterWithFixedPtdfsAndUpdatedCommercialFlows(
                Map.of(cnec1, 20.),
                loopFlowComputation,
                Set.of(cnec2)
        );*/

        when(systematicSensitivityResult.getReferenceFlow(cnec1)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(cnec1)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(cnec2)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(cnec2)).thenReturn(235.);

        LoopFlowResult loopFlowResult = Mockito.mock(LoopFlowResult.class);
        when(loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(systematicSensitivityResult, Set.of(cnec2)))
                .thenReturn(loopFlowResult);
        when(loopFlowResult.getCommercialFlow(cnec2)).thenReturn(300.);
        BranchResult branchResult = branchResultAdapter.getResult(systematicSensitivityResult);

        assertEquals(200., branchResult.getFlow(cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58., branchResult.getFlow(cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500., branchResult.getFlow(cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235., branchResult.getFlow(cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(20., branchResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        assertEquals(300., branchResult.getCommercialFlow(cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }
}
