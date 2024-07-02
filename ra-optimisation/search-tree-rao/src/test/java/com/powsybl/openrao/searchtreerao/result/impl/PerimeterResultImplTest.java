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
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PerimeterResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private PerimeterResultImpl perimeterResultImpl;
    private RangeActionSetpointResult prePerimeterRangeActionActivationResult;
    private OptimizationResult optimizationResult;
    private State mainOptimizationState;
    private RangeAction<?> ra1;
    private RangeAction<?> ra2;
    private FlowCnec flowCnec1;
    private FlowCnec flowCnec2;
    private State state1;
    private State state2;
    private Instant instant1;
    private Instant instant2;
    private NetworkAction na1;
    private NetworkAction na2;
    private PstRangeAction pst1;
    private PstRangeAction pst2;

    @BeforeEach
    public void setUp() {
        prePerimeterRangeActionActivationResult = mock(RangeActionSetpointResult.class);
        optimizationResult = mock(OptimizationResult.class);

        mainOptimizationState = mock(State.class);
        ra1 = mock(RangeAction.class);
        ra2 = mock(RangeAction.class);
        when(prePerimeterRangeActionActivationResult.getRangeActions()).thenReturn(Set.of(ra1, ra2));
        when(optimizationResult.getRangeActions()).thenReturn(Set.of(ra1, ra2));

        flowCnec1 = mock(FlowCnec.class);
        flowCnec2 = mock(FlowCnec.class);

        state1 = mock(State.class);
        state2 = mock(State.class);

        instant1 = mock(Instant.class);
        instant2 = mock(Instant.class);

        when(state1.getInstant()).thenReturn(instant1);
        when(state2.getInstant()).thenReturn(instant2);

        when(flowCnec1.getState()).thenReturn(state1);
        when(flowCnec2.getState()).thenReturn(state2);

        na1 = mock(NetworkAction.class);
        na2 = mock(NetworkAction.class);

        pst1 = mock(PstRangeAction.class);
        pst2 = mock(PstRangeAction.class);

        perimeterResultImpl = new PerimeterResultImpl(prePerimeterRangeActionActivationResult, optimizationResult);
    }

    @Test
    void testGetActivatedRangeActions() {
        when(prePerimeterRangeActionActivationResult.getSetpoint(ra1)).thenReturn(5.);
        when(optimizationResult.getOptimizedSetpoint(ra1, mainOptimizationState)).thenReturn(5.);
        when(prePerimeterRangeActionActivationResult.getSetpoint(ra2)).thenReturn(15.);
        when(optimizationResult.getOptimizedSetpoint(ra2, mainOptimizationState)).thenReturn(50.);
        when(optimizationResult.getActivatedRangeActions(mainOptimizationState)).thenReturn(Set.of(ra2));
        assertEquals(Set.of(ra2), perimeterResultImpl.getActivatedRangeActions(mainOptimizationState));
    }

    @Test
    void testGetFlow() {
        when(optimizationResult.getFlow(flowCnec1, ONE, Unit.MEGAWATT, flowCnec1.getState().getInstant())).thenReturn(100.);
        when(optimizationResult.getFlow(flowCnec2, TWO, Unit.AMPERE, flowCnec2.getState().getInstant())).thenReturn(200.);
        assertEquals(100., perimeterResultImpl.getFlow(flowCnec1, ONE, Unit.MEGAWATT, flowCnec1.getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(200., perimeterResultImpl.getFlow(flowCnec2, TWO, Unit.AMPERE, flowCnec2.getState().getInstant()), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCommercialFlow() {
        when(optimizationResult.getCommercialFlow(flowCnec1, ONE, Unit.MEGAWATT)).thenReturn(100.);
        when(optimizationResult.getCommercialFlow(flowCnec2, TWO, Unit.AMPERE)).thenReturn(200.);
        assertEquals(100., perimeterResultImpl.getCommercialFlow(flowCnec1, ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200., perimeterResultImpl.getCommercialFlow(flowCnec2, TWO, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetPtdfZonalSum() {
        when(optimizationResult.getPtdfZonalSum(flowCnec1, ONE)).thenReturn(100.);
        when(optimizationResult.getPtdfZonalSum(flowCnec2, TWO)).thenReturn(200.);
        assertEquals(100., perimeterResultImpl.getPtdfZonalSum(flowCnec1, ONE), DOUBLE_TOLERANCE);
        assertEquals(200., perimeterResultImpl.getPtdfZonalSum(flowCnec2, TWO), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetPtdfZonalSums() {
        Map<FlowCnec, Map<TwoSides, Double>> map = Map.of(flowCnec1, Map.of(ONE, 100.), flowCnec2, Map.of(TWO, 200.));
        when(optimizationResult.getPtdfZonalSums()).thenReturn(map);
        assertEquals(map, perimeterResultImpl.getPtdfZonalSums());
    }

    @Test
    void testIsActivated() {
        when(optimizationResult.isActivated(na1)).thenReturn(true);
        when(optimizationResult.isActivated(na2)).thenReturn(false);
        assertTrue(perimeterResultImpl.isActivated(na1));
        assertFalse(perimeterResultImpl.isActivated(na2));
    }

    @Test
    void testGetActivatedNetworkActions() {
        when(optimizationResult.getActivatedNetworkActions()).thenReturn(Set.of(na1));
        assertEquals(Set.of(na1), perimeterResultImpl.getActivatedNetworkActions());
    }

    @Test
    void testGetFunctionalCost() {
        when(optimizationResult.getFunctionalCost()).thenReturn(1000.);
        assertEquals(1000., perimeterResultImpl.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetMostLimitingElements() {
        when(optimizationResult.getMostLimitingElements(anyInt())).thenReturn(List.of(flowCnec2, flowCnec1));
        assertEquals(List.of(flowCnec2, flowCnec1), perimeterResultImpl.getMostLimitingElements(100));
    }

    @Test
    void testGetVirtualCost() {
        when(optimizationResult.getVirtualCost()).thenReturn(1000.);
        assertEquals(1000., perimeterResultImpl.getVirtualCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetVirtualCostNames() {
        when(optimizationResult.getVirtualCostNames()).thenReturn(Set.of("lf", "mnec"));
        assertEquals(Set.of("lf", "mnec"), perimeterResultImpl.getVirtualCostNames());
    }

    @Test
    void testGetVirtualCostByName() {
        when(optimizationResult.getVirtualCost("lf")).thenReturn(1000.);
        when(optimizationResult.getVirtualCost("mnec")).thenReturn(2000.);
        assertEquals(1000., perimeterResultImpl.getVirtualCost("lf"), DOUBLE_TOLERANCE);
        assertEquals(2000., perimeterResultImpl.getVirtualCost("mnec"), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCostlyElements() {
        when(optimizationResult.getCostlyElements("lf", 100)).thenReturn(List.of(flowCnec2, flowCnec1));
        when(optimizationResult.getCostlyElements("mnec", 100)).thenReturn(List.of(flowCnec1));
        assertEquals(List.of(flowCnec2, flowCnec1), perimeterResultImpl.getCostlyElements("lf", 100));
        assertEquals(List.of(flowCnec1), perimeterResultImpl.getCostlyElements("mnec", 100));
    }

    @Test
    void testGetRangeActions() {
        assertEquals(Set.of(ra1, ra2), perimeterResultImpl.getRangeActions());
    }

    @Test
    void testGetOptimizedTap() {
        when(optimizationResult.getRangeActions()).thenReturn(Set.of(pst1));

        when(optimizationResult.getOptimizedTap(pst1, mainOptimizationState)).thenReturn(10);
        when(optimizationResult.getOptimizedTap(pst2, mainOptimizationState)).thenThrow(new OpenRaoException("absent mock"));
        when(prePerimeterRangeActionActivationResult.getTap(pst2)).thenReturn(3);

        assertEquals(10, perimeterResultImpl.getOptimizedTap(pst1, mainOptimizationState));
        assertEquals(3, perimeterResultImpl.getOptimizedTap(pst2, mainOptimizationState));
    }

    @Test
    void testGetOptimizedSetPoint() {
        when(optimizationResult.getRangeActions()).thenReturn(Set.of(ra1));
        when(optimizationResult.getOptimizedSetpoint(ra1, mainOptimizationState)).thenReturn(10.7);
        when(prePerimeterRangeActionActivationResult.getSetpoint(ra2)).thenReturn(3.5);

        assertEquals(10.7, perimeterResultImpl.getOptimizedSetpoint(ra1, mainOptimizationState), DOUBLE_TOLERANCE);
        assertEquals(3.5, perimeterResultImpl.getOptimizedSetpoint(ra2, mainOptimizationState), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetOptimizedTaps() {
        Map<PstRangeAction, Integer> map = Map.of(pst1, 10, pst2, 5);
        when(optimizationResult.getOptimizedTapsOnState(mainOptimizationState)).thenReturn(map);
        assertEquals(map, perimeterResultImpl.getOptimizedTapsOnState(mainOptimizationState));
    }

    @Test
    void testGetOptimizedSetPoints() {
        Map<RangeAction<?>, Double> map = Map.of(pst1, 10.6, pst2, 5.8, ra1, 52.5, ra2, 100.6);
        when(optimizationResult.getOptimizedSetpointsOnState(mainOptimizationState)).thenReturn(map);
        assertEquals(map, perimeterResultImpl.getOptimizedSetpointsOnState(mainOptimizationState));
    }

    @Test
    void testGetSensitivityStatus() {
        when(optimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, perimeterResultImpl.getSensitivityStatus());
    }

    @Test
    void testGetSensitivityValueOnRa() {
        assertThrows(NotImplementedException.class, () -> perimeterResultImpl.getSensitivityValue(flowCnec1, ONE, ra1, Unit.MEGAWATT));
        assertThrows(NotImplementedException.class, () -> perimeterResultImpl.getSensitivityValue(flowCnec1, ONE, ra2, Unit.AMPERE));
    }

    @Test
    void testGetSensitivityValueOnGlsk() {
        assertThrows(NotImplementedException.class, () -> perimeterResultImpl.getSensitivityValue(flowCnec1, ONE, mock(SensitivityVariableSet.class), Unit.MEGAWATT));
        assertThrows(NotImplementedException.class, () -> perimeterResultImpl.getSensitivityValue(flowCnec1, ONE, mock(SensitivityVariableSet.class), Unit.AMPERE));
    }
}
