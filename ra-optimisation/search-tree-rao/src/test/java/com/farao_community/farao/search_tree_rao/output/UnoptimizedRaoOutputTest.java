/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UnoptimizedRaoOutputTest {
    private PrePerimeterResult initialResult;
    private UnoptimizedRaoOutput output;
    private FlowCnec flowCnec;
    private static final double DOUBLE_TOLERANCE = 1e-6;

    @Before
    public void setUp() {
        initialResult = Mockito.mock(PrePerimeterResult.class);
        output = new UnoptimizedRaoOutput(initialResult);
        flowCnec = Mockito.mock(FlowCnec.class);
    }

    @Test
    public void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.FALLBACK);
        assertEquals(ComputationStatus.FALLBACK, output.getComputationStatus());
    }

    @Test
    public void testGetFlow() {
        when(initialResult.getFlow(flowCnec, Unit.AMPERE)).thenReturn(100.);
        when(initialResult.getFlow(flowCnec, Unit.MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getFlow(OptimizationState.INITIAL, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(OptimizationState.AFTER_PRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(OptimizationState.AFTER_ARA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getFlow(OptimizationState.INITIAL, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(OptimizationState.AFTER_PRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(OptimizationState.AFTER_ARA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(OptimizationState.AFTER_CRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMargin() {
        when(initialResult.getMargin(flowCnec, Unit.AMPERE)).thenReturn(100.);
        when(initialResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getMargin(OptimizationState.INITIAL, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(OptimizationState.AFTER_PRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(OptimizationState.AFTER_ARA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getMargin(OptimizationState.INITIAL, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_PRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_ARA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_CRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetRelativeMargin() {
        when(initialResult.getRelativeMargin(flowCnec, Unit.AMPERE)).thenReturn(100.);
        when(initialResult.getRelativeMargin(flowCnec, Unit.MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getRelativeMargin(OptimizationState.INITIAL, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(OptimizationState.AFTER_PRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(OptimizationState.AFTER_ARA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getRelativeMargin(OptimizationState.INITIAL, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_PRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_ARA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_CRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCommercialFlow() {
        when(initialResult.getCommercialFlow(flowCnec, Unit.AMPERE)).thenReturn(100.);
        when(initialResult.getCommercialFlow(flowCnec, Unit.MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getCommercialFlow(OptimizationState.INITIAL, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(OptimizationState.AFTER_PRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(OptimizationState.AFTER_ARA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getCommercialFlow(OptimizationState.INITIAL, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(OptimizationState.AFTER_PRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(OptimizationState.AFTER_ARA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(OptimizationState.AFTER_CRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLoopFlow() {
        when(initialResult.getLoopFlow(flowCnec, Unit.AMPERE)).thenReturn(100.);
        when(initialResult.getLoopFlow(flowCnec, Unit.MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getLoopFlow(OptimizationState.INITIAL, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(OptimizationState.AFTER_PRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(OptimizationState.AFTER_ARA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getLoopFlow(OptimizationState.INITIAL, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(OptimizationState.AFTER_PRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(OptimizationState.AFTER_ARA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(OptimizationState.AFTER_CRA, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPtdfZonalSum() {
        when(initialResult.getPtdfZonalSum(flowCnec)).thenReturn(100.);

        assertEquals(100., output.getPtdfZonalSum(OptimizationState.INITIAL, flowCnec), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(OptimizationState.AFTER_PRA, flowCnec), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(OptimizationState.AFTER_ARA, flowCnec), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(OptimizationState.AFTER_CRA, flowCnec), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCost() {
        when(initialResult.getCost()).thenReturn(-50.);
        assertEquals(-50., output.getCost(OptimizationState.INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(OptimizationState.AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(OptimizationState.AFTER_ARA), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(OptimizationState.AFTER_CRA), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetFunctionalCost() {
        when(initialResult.getFunctionalCost()).thenReturn(-500.);
        assertEquals(-500., output.getFunctionalCost(OptimizationState.INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(OptimizationState.AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(OptimizationState.AFTER_ARA), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(OptimizationState.AFTER_CRA), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetVirtualCost() {
        when(initialResult.getVirtualCost()).thenReturn(-5000.);
        assertEquals(-5000., output.getVirtualCost(OptimizationState.INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(OptimizationState.AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(OptimizationState.AFTER_ARA), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(OptimizationState.AFTER_CRA), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetVirtualCostNames() {
        when(initialResult.getVirtualCostNames()).thenReturn(Set.of("one", "two"));
        assertEquals(Set.of("one", "two"), output.getVirtualCostNames());
    }

    @Test
    public void testGetVirtualCostWithName() {
        when(initialResult.getVirtualCost("one")).thenReturn(60.);
        when(initialResult.getVirtualCost("two")).thenReturn(600.);

        assertEquals(60., output.getVirtualCost(OptimizationState.INITIAL, "one"), DOUBLE_TOLERANCE);
        assertEquals(60., output.getVirtualCost(OptimizationState.AFTER_PRA, "one"), DOUBLE_TOLERANCE);
        assertEquals(60., output.getVirtualCost(OptimizationState.AFTER_ARA, "one"), DOUBLE_TOLERANCE);
        assertEquals(60., output.getVirtualCost(OptimizationState.AFTER_CRA, "one"), DOUBLE_TOLERANCE);

        assertEquals(600., output.getVirtualCost(OptimizationState.INITIAL, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(OptimizationState.AFTER_PRA, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(OptimizationState.AFTER_ARA, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(OptimizationState.AFTER_CRA, "two"), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWasActivatedBeforeState() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        State state = Mockito.mock(State.class);
        assertFalse(output.wasActivatedBeforeState(state, na));
    }

    @Test
    public void testIsActivatedDuringState() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        State state = Mockito.mock(State.class);
        assertFalse(output.isActivatedDuringState(state, na));
    }

    @Test
    public void testGetActivatedNetworkActionsDuringState() {
        State state = Mockito.mock(State.class);
        assertTrue(output.getActivatedNetworkActionsDuringState(state).isEmpty());
    }

    @Test
    public void testIsActivatedDuringStateRa() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        State state = Mockito.mock(State.class);
        assertFalse(output.isActivatedDuringState(state, rangeAction));
    }

    @Test
    public void testGetPreOptimizationTapOnState() {
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(initialResult.getOptimizedTap(pstRangeAction)).thenReturn(6);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(6, output.getPreOptimizationTapOnState(state1, pstRangeAction));
        assertEquals(6, output.getPreOptimizationTapOnState(state2, pstRangeAction));
    }

    @Test
    public void testGetOptimizedTapOnState() {
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(initialResult.getOptimizedTap(pstRangeAction)).thenReturn(6);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(6, output.getOptimizedTapOnState(state1, pstRangeAction));
        assertEquals(6, output.getOptimizedTapOnState(state2, pstRangeAction));
    }

    @Test
    public void testGetPreOptimizationSetPointOnState() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        when(initialResult.getOptimizedSetPoint(rangeAction)).thenReturn(60.);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(60., output.getPreOptimizationSetPointOnState(state1, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(60., output.getPreOptimizationSetPointOnState(state2, rangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetOptimizedSetPointOnState() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        when(initialResult.getOptimizedSetPoint(rangeAction)).thenReturn(60.);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(60., output.getOptimizedSetPointOnState(state1, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(60., output.getOptimizedSetPointOnState(state2, rangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetActivatedRangeActionsDuringState() {
        State state1 = Mockito.mock(State.class);
        assertTrue(output.getActivatedRangeActionsDuringState(state1).isEmpty());
    }

    @Test
    public void testGetOptimizedTapsOnState() {
        PstRangeAction pstRangeAction1 = Mockito.mock(PstRangeAction.class);
        PstRangeAction pstRangeAction2 = Mockito.mock(PstRangeAction.class);
        when(initialResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction1, 1, pstRangeAction2, 2));
        assertEquals(Map.of(pstRangeAction1, 1, pstRangeAction2, 2), output.getOptimizedTapsOnState(Mockito.mock(State.class)));
    }

    @Test
    public void testGetOptimizedSetPointsOnState() {
        RangeAction rangeAction1 = Mockito.mock(RangeAction.class);
        RangeAction rangeAction2 = Mockito.mock(RangeAction.class);
        when(initialResult.getOptimizedSetPoints()).thenReturn(Map.of(rangeAction1, 19.3, rangeAction2, 25.6));
        assertEquals(Map.of(rangeAction1, 19.3, rangeAction2, 25.6), output.getOptimizedSetPointsOnState(Mockito.mock(State.class)));
    }

}
