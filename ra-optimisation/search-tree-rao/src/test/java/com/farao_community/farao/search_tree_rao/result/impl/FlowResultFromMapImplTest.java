/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
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
                Map.of(loopFlowCnec, Map.of(LEFT, 200., RIGHT, 250.)),
                Map.of(optimizedCnec, Map.of(LEFT, 30., RIGHT, 35.))
        );
    }

    @Test
    public void testBasicReturns() {
        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec, LEFT)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec, RIGHT)).thenReturn(250.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec, LEFT)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec, RIGHT)).thenReturn(63.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, LEFT)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, RIGHT)).thenReturn(550.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, LEFT)).thenReturn(235.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, RIGHT)).thenReturn(285.);

        assertEquals(200, branchResultFromMap.getFlow(loopFlowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250, branchResultFromMap.getFlow(loopFlowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58, branchResultFromMap.getFlow(loopFlowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(63, branchResultFromMap.getFlow(loopFlowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500, branchResultFromMap.getFlow(optimizedCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(550, branchResultFromMap.getFlow(optimizedCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235, branchResultFromMap.getFlow(optimizedCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(285, branchResultFromMap.getFlow(optimizedCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);

        assertThrows(FaraoException.class, () -> branchResultFromMap.getPtdfZonalSum(loopFlowCnec, LEFT));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getPtdfZonalSum(loopFlowCnec, RIGHT));
        assertEquals(30., branchResultFromMap.getPtdfZonalSum(optimizedCnec, LEFT), DOUBLE_TOLERANCE);
        assertEquals(35., branchResultFromMap.getPtdfZonalSum(optimizedCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(Map.of(optimizedCnec, Map.of(LEFT, 30., RIGHT, 35.)), branchResultFromMap.getPtdfZonalSums());

        assertEquals(200, branchResultFromMap.getCommercialFlow(loopFlowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250, branchResultFromMap.getCommercialFlow(loopFlowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(FaraoException.class, () -> branchResultFromMap.getCommercialFlow(loopFlowCnec, LEFT, AMPERE));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getCommercialFlow(loopFlowCnec, RIGHT, AMPERE));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getCommercialFlow(optimizedCnec, LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getCommercialFlow(optimizedCnec, RIGHT, MEGAWATT));
    }

    @Test
    public void testNanFlow() {
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, LEFT)).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, RIGHT)).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, LEFT)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, RIGHT)).thenReturn(550.);
        when(optimizedCnec.getNominalVoltage(any())).thenReturn(400.);

        assertEquals(721.69, branchResultFromMap.getFlow(optimizedCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(793.86, branchResultFromMap.getFlow(optimizedCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWrongFlowUnit() {
        assertThrows(FaraoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, LEFT, KILOVOLT));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, RIGHT, DEGREE));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, LEFT, PERCENT_IMAX));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, RIGHT, TAP));
    }
}
