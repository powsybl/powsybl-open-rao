/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static com.powsybl.openrao.data.cracapi.cnec.Side.LEFT;
import static com.powsybl.openrao.commons.Unit.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class SensitivityResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    void testSensitivitiesOnRangeAction() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        when(state.getInstant()).thenReturn(instant);
        when(cnec.getState()).thenReturn(state);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction, cnec, LEFT, cnec.getState().getInstant())).thenReturn(8.);

        assertEquals(8, sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);

        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, KILOVOLT));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, DEGREE));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, PERCENT_IMAX));
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, rangeAction, TAP));
    }

    @Test
    void testSensitivitiesOnLinearGLSK() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        when(systematicSensitivityResult.getSensitivityOnFlow(linearGlsk, cnec, LEFT)).thenReturn(8.);

        assertEquals(8, sensitivityResultImpl.getSensitivityValue(cnec, LEFT, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(OpenRaoException.class, () -> sensitivityResultImpl.getSensitivityValue(cnec, LEFT, linearGlsk, AMPERE));
    }

    @Test
    void testStatus() {
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SensitivityResultImpl sensitivityResultImpl = new SensitivityResultImpl(
                systematicSensitivityResult
        );

        when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        assertEquals(ComputationStatus.DEFAULT, sensitivityResultImpl.getSensitivityStatus());
        when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, sensitivityResultImpl.getSensitivityStatus());
    }
}
