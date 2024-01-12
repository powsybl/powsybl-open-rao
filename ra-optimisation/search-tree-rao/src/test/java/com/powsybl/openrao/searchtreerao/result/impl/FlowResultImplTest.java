/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static com.powsybl.openrao.data.cracapi.cnec.Side.LEFT;
import static com.powsybl.openrao.data.cracapi.cnec.Side.RIGHT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class FlowResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    SystematicSensitivityResult systematicSensitivityResult;
    FlowCnec loopFlowCnec;
    FlowCnec optimizedCnec;
    FlowResultImpl branchResult;

    @BeforeEach
    public void setUp() {
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        loopFlowCnec = Mockito.mock(FlowCnec.class);
        optimizedCnec = Mockito.mock(FlowCnec.class);

        FlowResult fixedCommercialFlows = Mockito.mock(FlowResult.class);
        when(fixedCommercialFlows.getCommercialFlow(loopFlowCnec, LEFT, Unit.MEGAWATT)).thenReturn(200.);
        when(fixedCommercialFlows.getCommercialFlow(eq(optimizedCnec), eq(RIGHT), any())).thenThrow(new OpenRaoException("a mock of what would happen if trying to access LF"));

        FlowResult fixedPtdfs = Mockito.mock(FlowResult.class);
        when(fixedPtdfs.getPtdfZonalSums()).thenReturn(Map.of(optimizedCnec, Map.of(RIGHT, 30.)));
        when(fixedPtdfs.getPtdfZonalSum(optimizedCnec, RIGHT)).thenReturn(30.);
        when(fixedPtdfs.getPtdfZonalSum(loopFlowCnec, LEFT)).thenThrow(new OpenRaoException("a mock of what would happen if trying to access ptdf sum"));

        branchResult = new FlowResultImpl(
                systematicSensitivityResult,
                fixedCommercialFlows,
                fixedPtdfs
        );
    }

    @Test
    void testBasicReturns() {
        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec, LEFT)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec, LEFT)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, RIGHT)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, RIGHT)).thenReturn(235.);

        assertEquals(200, branchResult.getFlow(loopFlowCnec, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58, branchResult.getFlow(loopFlowCnec, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500, branchResult.getFlow(optimizedCnec, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235, branchResult.getFlow(optimizedCnec, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertThrows(OpenRaoException.class, () -> branchResult.getPtdfZonalSum(loopFlowCnec, LEFT));
        assertEquals(30., branchResult.getPtdfZonalSum(optimizedCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(Map.of(optimizedCnec, Map.of(RIGHT, 30.)), branchResult.getPtdfZonalSums());

        assertEquals(200, branchResult.getCommercialFlow(loopFlowCnec, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(OpenRaoException.class, () -> branchResult.getCommercialFlow(loopFlowCnec, LEFT, Unit.AMPERE));
        assertThrows(OpenRaoException.class, () -> branchResult.getCommercialFlow(optimizedCnec, RIGHT, Unit.MEGAWATT));
    }

    @Test
    void testNanFlow() {
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, RIGHT)).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, RIGHT)).thenReturn(500.);
        when(optimizedCnec.getNominalVoltage(any())).thenReturn(400.);

        assertEquals(721.69, branchResult.getFlow(optimizedCnec, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    void testWrongFlowUnit() {
        assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, RIGHT, Unit.KILOVOLT));
        assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, RIGHT, Unit.DEGREE));
        assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, RIGHT, Unit.PERCENT_IMAX));
        assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, RIGHT, Unit.TAP));
    }
}
