/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BranchResultFromMapTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testBasicReturns() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        BranchCnec loopFlowCnec = Mockito.mock(BranchCnec.class);
        BranchCnec optimizedCnec = Mockito.mock(BranchCnec.class);
        BranchResultFromMap branchResultFromMap = new BranchResultFromMap(
                systematicSensitivityResult,
                Map.of(loopFlowCnec, 200.),
                Map.of(optimizedCnec, 30.)
        );

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

        assertEquals(200, branchResultFromMap.getCommercialFlow(loopFlowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(FaraoException.class, () -> branchResultFromMap.getCommercialFlow(loopFlowCnec, Unit.AMPERE));
        assertThrows(FaraoException.class, () -> branchResultFromMap.getCommercialFlow(optimizedCnec, Unit.MEGAWATT));
    }
}
