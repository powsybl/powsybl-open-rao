/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;

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
        when(fixedCommercialFlows.getCommercialFlow(loopFlowCnec, ONE, Unit.MEGAWATT)).thenReturn(200.);
        when(fixedCommercialFlows.getCommercialFlow(eq(optimizedCnec), eq(TWO), any())).thenThrow(new OpenRaoException("a mock of what would happen if trying to access LF"));

        FlowResult fixedPtdfs = Mockito.mock(FlowResult.class);
        when(fixedPtdfs.getPtdfZonalSums()).thenReturn(Map.of(optimizedCnec, Map.of(TWO, 30.)));
        when(fixedPtdfs.getPtdfZonalSum(optimizedCnec, TWO)).thenReturn(30.);
        when(fixedPtdfs.getPtdfZonalSum(loopFlowCnec, ONE)).thenThrow(new OpenRaoException("a mock of what would happen if trying to access ptdf sum"));

        branchResult = new FlowResultImpl(
                systematicSensitivityResult,
                fixedCommercialFlows,
                fixedPtdfs
        );
    }

    @Test
    void testBasicReturns() {
        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec, ONE)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec, ONE)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, TWO)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, TWO)).thenReturn(235.);

        assertEquals(200, branchResult.getFlow(loopFlowCnec, ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(58, branchResult.getFlow(loopFlowCnec, ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(500, branchResult.getFlow(optimizedCnec, TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(235, branchResult.getFlow(optimizedCnec, TWO, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertThrows(OpenRaoException.class, () -> branchResult.getPtdfZonalSum(loopFlowCnec, ONE));
        assertEquals(30., branchResult.getPtdfZonalSum(optimizedCnec, TWO), DOUBLE_TOLERANCE);
        assertEquals(Map.of(optimizedCnec, Map.of(TWO, 30.)), branchResult.getPtdfZonalSums());

        assertEquals(200, branchResult.getCommercialFlow(loopFlowCnec, ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertThrows(OpenRaoException.class, () -> branchResult.getCommercialFlow(loopFlowCnec, ONE, Unit.AMPERE));
        assertThrows(OpenRaoException.class, () -> branchResult.getCommercialFlow(optimizedCnec, TWO, Unit.MEGAWATT));
    }

    @Test
    void testGetFlowWithInstant() {
        Instant instant = Mockito.mock(Instant.class);

        when(systematicSensitivityResult.getReferenceFlow(loopFlowCnec, ONE, instant)).thenReturn(200.);
        when(systematicSensitivityResult.getReferenceIntensity(loopFlowCnec, ONE, instant)).thenReturn(58.);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, TWO, instant)).thenReturn(500.);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, TWO, instant)).thenReturn(235.);

        assertEquals(200, branchResult.getFlow(loopFlowCnec, ONE, Unit.MEGAWATT, instant), DOUBLE_TOLERANCE);
        assertEquals(58, branchResult.getFlow(loopFlowCnec, ONE, Unit.AMPERE, instant), DOUBLE_TOLERANCE);
        assertEquals(500, branchResult.getFlow(optimizedCnec, TWO, Unit.MEGAWATT, instant), DOUBLE_TOLERANCE);
        assertEquals(235, branchResult.getFlow(optimizedCnec, TWO, Unit.AMPERE, instant), DOUBLE_TOLERANCE);
    }

    @Test
    void testNanFlow() {
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, TWO)).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, TWO)).thenReturn(500.);
        when(optimizedCnec.getNominalVoltage(any())).thenReturn(400.);

        assertEquals(721.69, branchResult.getFlow(optimizedCnec, TWO, Unit.AMPERE), DOUBLE_TOLERANCE);

        Instant instant = Mockito.mock(Instant.class);
        when(systematicSensitivityResult.getReferenceIntensity(optimizedCnec, TWO, instant)).thenReturn(Double.NaN);
        when(systematicSensitivityResult.getReferenceFlow(optimizedCnec, TWO, instant)).thenReturn(500.);
        when(optimizedCnec.getNominalVoltage(any())).thenReturn(400.);

        assertEquals(721.69, branchResult.getFlow(optimizedCnec, TWO, Unit.AMPERE, instant), DOUBLE_TOLERANCE);
    }

    @Test
    void testWrongFlowUnit() {
        Exception e = assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, TWO, Unit.KILOVOLT));
        assertEquals("Unknown unit for flow.", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, TWO, Unit.DEGREE));
        assertEquals("Unknown unit for flow.", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, TWO, Unit.PERCENT_IMAX));
        assertEquals("Unknown unit for flow.", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, TWO, Unit.TAP));
        assertEquals("Unknown unit for flow.", e.getMessage());

        Instant instant = Mockito.mock(Instant.class);
        e = assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, TWO, Unit.KILOVOLT, instant));
        assertEquals("Unknown unit for flow.", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, TWO, Unit.DEGREE, instant));
        assertEquals("Unknown unit for flow.", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, TWO, Unit.PERCENT_IMAX, instant));
        assertEquals("Unknown unit for flow.", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> branchResult.getFlow(optimizedCnec, TWO, Unit.TAP, instant));
        assertEquals("Unknown unit for flow.", e.getMessage());
    }

    @Test
    void testConstructorWrongCases() {
        Map<FlowCnec, Map<Side, Double>> commercialFlows = new HashMap<>();
        FlowResult fixedCommercialFlows = Mockito.mock(FlowResult.class);
        Map<FlowCnec, Map<Side, Double>> ptdfZonalSums = new HashMap<>();
        FlowResult fixedPtdfZonalSums = Mockito.mock(FlowResult.class);

        Exception e = assertThrows(OpenRaoException.class, () -> new FlowResultImpl(systematicSensitivityResult, commercialFlows, fixedCommercialFlows, ptdfZonalSums, null));
        assertEquals("Either commercialFlows or fixedCommercialFlows should be non null", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> new FlowResultImpl(systematicSensitivityResult, null, null, ptdfZonalSums, null));
        assertEquals("Either commercialFlows or fixedCommercialFlows should be non null", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> new FlowResultImpl(systematicSensitivityResult, commercialFlows, null, ptdfZonalSums, fixedPtdfZonalSums));
        assertEquals("Either ptdfZonalSums or fixedPtdfZonalSums should be non null", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> new FlowResultImpl(systematicSensitivityResult, commercialFlows, null, null, null));
        assertEquals("Either ptdfZonalSums or fixedPtdfZonalSums should be non null", e.getMessage());
    }
}
