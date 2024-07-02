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
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static com.powsybl.openrao.commons.Unit.*;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class FlowResultFromMapImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    SystematicSensitivityResult systematicSensitivityResult;
    FlowCnec loopFlowCnec;
    FlowCnec optimizedCnec;
    FlowResultImpl branchResultFromMap;

    @BeforeEach
    public void setUp() {
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Instant instant = mock(Instant.class);
        State state = mock(State.class);
        when(state.getInstant()).thenReturn(instant);
        loopFlowCnec = Mockito.mock(FlowCnec.class);
        when(loopFlowCnec.getState()).thenReturn(state);
        optimizedCnec = Mockito.mock(FlowCnec.class);
        when(optimizedCnec.getState()).thenReturn(state);
        branchResultFromMap = new FlowResultImpl(
                systematicSensitivityResult,
                Map.of(loopFlowCnec, Map.of(ONE, 200., TWO, 250.)),
                Map.of(optimizedCnec, Map.of(ONE, 30., TWO, 35.))
        );
    }

    @Test
    void testBasicReturns() {
        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec, ONE, loopFlowCnec.getState().getInstant())).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec, TWO, loopFlowCnec.getState().getInstant())).thenReturn(250.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec, ONE, loopFlowCnec.getState().getInstant())).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec, TWO, loopFlowCnec.getState().getInstant())).thenReturn(63.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, ONE, optimizedCnec.getState().getInstant())).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, TWO, optimizedCnec.getState().getInstant())).thenReturn(550.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, ONE, optimizedCnec.getState().getInstant())).thenReturn(235.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, TWO, optimizedCnec.getState().getInstant())).thenReturn(285.);

        assertEquals(200, branchResultFromMap.getFlow(loopFlowCnec, ONE, MEGAWATT, loopFlowCnec.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(250, branchResultFromMap.getFlow(loopFlowCnec, TWO, MEGAWATT, loopFlowCnec.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(58, branchResultFromMap.getFlow(loopFlowCnec, ONE, AMPERE, loopFlowCnec.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(63, branchResultFromMap.getFlow(loopFlowCnec, TWO, AMPERE, loopFlowCnec.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(500, branchResultFromMap.getFlow(optimizedCnec, ONE, MEGAWATT, optimizedCnec.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(550, branchResultFromMap.getFlow(optimizedCnec, TWO, MEGAWATT, optimizedCnec.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(235, branchResultFromMap.getFlow(optimizedCnec, ONE, AMPERE, optimizedCnec.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(285, branchResultFromMap.getFlow(optimizedCnec, TWO, AMPERE, optimizedCnec.getState().getInstant()), DOUBLE_TOLERANCE);

        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getPtdfZonalSum(loopFlowCnec, ONE));
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getPtdfZonalSum(loopFlowCnec, TWO));
        assertEquals(30., branchResultFromMap.getPtdfZonalSum(optimizedCnec, ONE), DOUBLE_TOLERANCE);
        assertEquals(35., branchResultFromMap.getPtdfZonalSum(optimizedCnec, TWO), DOUBLE_TOLERANCE);
        assertEquals(Map.of(optimizedCnec, Map.of(ONE, 30., TWO, 35.)), branchResultFromMap.getPtdfZonalSums());

        assertEquals(200, branchResultFromMap.getCommercialFlow(loopFlowCnec, ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250, branchResultFromMap.getCommercialFlow(loopFlowCnec, TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getCommercialFlow(loopFlowCnec, ONE, AMPERE));
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getCommercialFlow(loopFlowCnec, TWO, AMPERE));
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getCommercialFlow(optimizedCnec, ONE, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getCommercialFlow(optimizedCnec, TWO, MEGAWATT));
    }

    @Test
    void testNanFlow() {
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, ONE, optimizedCnec.getState().getInstant())).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, TWO, optimizedCnec.getState().getInstant())).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, ONE, optimizedCnec.getState().getInstant())).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, TWO, optimizedCnec.getState().getInstant())).thenReturn(550.);
        when(optimizedCnec.getNominalVoltage(any())).thenReturn(400.);

        assertEquals(721.69, branchResultFromMap.getFlow(optimizedCnec, ONE, AMPERE, optimizedCnec.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(793.86, branchResultFromMap.getFlow(optimizedCnec, TWO, AMPERE, optimizedCnec.getState().getInstant()), DOUBLE_TOLERANCE);
    }

    @Test
    void testWrongFlowUnit() {
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, ONE, KILOVOLT, optimizedCnec.getState().getInstant()));
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, TWO, DEGREE, optimizedCnec.getState().getInstant()));
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, ONE, PERCENT_IMAX, optimizedCnec.getState().getInstant()));
        assertThrows(OpenRaoException.class, () -> branchResultFromMap.getFlow(optimizedCnec, TWO, TAP, optimizedCnec.getState().getInstant()));
    }
}
