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
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowResultFromMapImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    SystematicSensitivityResult systematicSensitivityResult;
    FlowCnec loopFlowCnec;
    FlowCnec optimizedCnec;
    FlowResultFromMapImpl branchResultFromMap;

    @Before
    public void setUp() {
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        loopFlowCnec = Mockito.mock(FlowCnec.class);
        optimizedCnec = Mockito.mock(FlowCnec.class);
        branchResultFromMap = new FlowResultFromMapImpl(
                systematicSensitivityResult,
                Map.of(loopFlowCnec, 200.),
                Map.of(optimizedCnec, 30.)
        );
    }

    @Test
    public void testBasicReturns() {
        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec)).thenReturn(235.);

        assertEquals(200, branchResultFromMap.getFlow(loopFlowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58, branchResultFromMap.getFlow(loopFlowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500, branchResultFromMap.getFlow(optimizedCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235, branchResultFromMap.getFlow(optimizedCnec, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertThrows(FaraoException.class, () -> branchResultFromMap.getPtdfZonalSum(loopFlowCnec));
        assertEquals(30., branchResultFromMap.getPtdfZonalSum(optimizedCnec), DOUBLE_TOLERANCE);
        assertEquals(Map.of(optimizedCnec, 30.), branchResultFromMap.getPtdfZonalSums());

        assertEquals(200, branchResultFromMap.getCommercialFlow(loopFlowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(FaraoException.class, () -> branchResultFromMap.getCommercialFlow(loopFlowCnec, Unit.AMPERE));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getCommercialFlow(optimizedCnec, Unit.MEGAWATT));
    }

    @Test
    public void testNanFlow() {
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec)).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec)).thenReturn(500.);
        when(optimizedCnec.getNominalVoltage(any())).thenReturn(400.);

        assertEquals(721.69, branchResultFromMap.getFlow(optimizedCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWrongFlowUnit() {
        assertThrows(FaraoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, Unit.KILOVOLT));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, Unit.DEGREE));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, Unit.PERCENT_IMAX));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, Unit.TAP));
    }
}
