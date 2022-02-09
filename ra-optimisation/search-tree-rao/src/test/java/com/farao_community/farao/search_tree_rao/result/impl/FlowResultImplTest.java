/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class FlowResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    SystematicSensitivityResult systematicSensitivityResult;
    FlowCnec loopFlowCnec;
    FlowCnec optimizedCnec;
    FlowResultImpl branchResult;

    @Before
    public void setUp() {
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        loopFlowCnec = Mockito.mock(FlowCnec.class);
        optimizedCnec = Mockito.mock(FlowCnec.class);

        FlowResult fixedCommercialFlows = Mockito.mock(FlowResult.class);
        when(fixedCommercialFlows.getCommercialFlow(loopFlowCnec, Unit.MEGAWATT)).thenReturn(200.);
        when(fixedCommercialFlows.getCommercialFlow(eq(optimizedCnec), any())).thenThrow(new FaraoException("a mock of what would happen if trying to access LF"));

        FlowResult fixedPtdfs = Mockito.mock(FlowResult.class);
        when(fixedPtdfs.getPtdfZonalSums()).thenReturn(Map.of(optimizedCnec, 30.));
        when(fixedPtdfs.getPtdfZonalSum(optimizedCnec)).thenReturn(30.);
        when(fixedPtdfs.getPtdfZonalSum(loopFlowCnec)).thenThrow(new FaraoException("a mock of what would happen if trying to access ptdf sum"));

        branchResult = new FlowResultImpl(
                systematicSensitivityResult,
                fixedCommercialFlows,
                fixedPtdfs
        );
    }

    @Test
    public void testBasicReturns() {
        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec)).thenReturn(235.);

        assertEquals(200, branchResult.getFlow(loopFlowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58, branchResult.getFlow(loopFlowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500, branchResult.getFlow(optimizedCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235, branchResult.getFlow(optimizedCnec, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertThrows(FaraoException.class, () -> branchResult.getPtdfZonalSum(loopFlowCnec));
        assertEquals(30., branchResult.getPtdfZonalSum(optimizedCnec), DOUBLE_TOLERANCE);
        assertEquals(Map.of(optimizedCnec, 30.), branchResult.getPtdfZonalSums());

        assertEquals(200, branchResult.getCommercialFlow(loopFlowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(FaraoException.class, () -> branchResult.getCommercialFlow(loopFlowCnec, Unit.AMPERE));
        assertThrows(FaraoException.class, () -> branchResult.getCommercialFlow(optimizedCnec, Unit.MEGAWATT));
    }

    @Test
    public void testNanFlow() {
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec)).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec)).thenReturn(500.);
        when(optimizedCnec.getNominalVoltage(any())).thenReturn(400.);

        assertEquals(721.69, branchResult.getFlow(optimizedCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWrongFlowUnit() {
        assertThrows(FaraoException.class, () -> branchResult.getFlow(optimizedCnec, Unit.KILOVOLT));
        assertThrows(FaraoException.class, () -> branchResult.getFlow(optimizedCnec, Unit.DEGREE));
        assertThrows(FaraoException.class, () -> branchResult.getFlow(optimizedCnec, Unit.PERCENT_IMAX));
        assertThrows(FaraoException.class, () -> branchResult.getFlow(optimizedCnec, Unit.TAP));
    }
}
