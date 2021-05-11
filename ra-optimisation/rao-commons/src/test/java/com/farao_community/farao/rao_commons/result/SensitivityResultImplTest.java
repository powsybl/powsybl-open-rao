/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SensitivityResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testSensitivitiesOnRangeAction() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        BranchCnec cnec = Mockito.mock(BranchCnec.class);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction, cnec)).thenReturn(8.);
        when(systematicSensitivityResult.getSensitivityOnIntensity(rangeAction, cnec)).thenReturn(10.);

        assertEquals(8, sensitivityResultImpl.getSensitivityValue(cnec, rangeAction, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(10, sensitivityResultImpl.getSensitivityValue(cnec, rangeAction, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSensitivitiesOnLinearGLSK() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        LinearGlsk linearGlsk = Mockito.mock(LinearGlsk.class);
        BranchCnec cnec = Mockito.mock(BranchCnec.class);
        when(systematicSensitivityResult.getSensitivityOnFlow(linearGlsk, cnec)).thenReturn(8.);

        assertEquals(8, sensitivityResultImpl.getSensitivityValue(cnec, linearGlsk, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(FaraoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, linearGlsk, Unit.AMPERE));
    }

    @Test
    public void testStatus() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        assertEquals(SensitivityStatus.DEFAULT, sensitivityResultImpl.getSensitivityStatus());
        when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.FALLBACK);
        assertEquals(SensitivityStatus.FALLBACK, sensitivityResultImpl.getSensitivityStatus());
        when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        assertEquals(SensitivityStatus.FAILURE, sensitivityResultImpl.getSensitivityStatus());
    }
}
