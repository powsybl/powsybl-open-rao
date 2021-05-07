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
public class BranchResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testBasicReturns() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        BranchCnec loopFlowCnec = Mockito.mock(BranchCnec.class);
        BranchCnec optimizedCnec = Mockito.mock(BranchCnec.class);
        BranchResultImpl branchResultImpl = new BranchResultImpl(
                systematicSensitivityResult,
                Map.of(loopFlowCnec, 200.),
                Map.of(optimizedCnec, 30.)
        );

        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec)).thenReturn(235.);

        assertEquals(200, branchResultImpl.getFlow(loopFlowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58, branchResultImpl.getFlow(loopFlowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500, branchResultImpl.getFlow(optimizedCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235, branchResultImpl.getFlow(optimizedCnec, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertThrows(FaraoException.class, () -> branchResultImpl.getPtdfZonalSum(loopFlowCnec));
        assertEquals(30., branchResultImpl.getPtdfZonalSum(optimizedCnec), DOUBLE_TOLERANCE);

        assertEquals(200, branchResultImpl.getCommercialFlow(loopFlowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(FaraoException.class, () -> branchResultImpl.getCommercialFlow(loopFlowCnec, Unit.AMPERE));
        assertThrows(FaraoException.class, () -> branchResultImpl.getCommercialFlow(optimizedCnec, Unit.MEGAWATT));
    }

}