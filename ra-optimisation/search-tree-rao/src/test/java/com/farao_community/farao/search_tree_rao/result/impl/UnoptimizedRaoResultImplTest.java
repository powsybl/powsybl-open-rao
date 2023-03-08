/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.commons.Unit.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UnoptimizedRaoResultImplTest {
    private PrePerimeterResult initialResult;
    private UnoptimizedRaoResultImpl output;
    private FlowCnec flowCnec;
    private static final double DOUBLE_TOLERANCE = 1e-6;

    @Before
    public void setUp() {
        initialResult = Mockito.mock(PrePerimeterResult.class);
        output = new UnoptimizedRaoResultImpl(initialResult);
        flowCnec = Mockito.mock(FlowCnec.class);
    }

    @Test
    public void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());
    }

    @Test
    public void testGetFlow() {
        when(initialResult.getFlow(flowCnec, LEFT, AMPERE)).thenReturn(100.);
        when(initialResult.getFlow(flowCnec, LEFT, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getFlow(OptimizationState.INITIAL, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(OptimizationState.AFTER_PRA, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(OptimizationState.AFTER_ARA, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(OptimizationState.AFTER_CRA, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getFlow(OptimizationState.INITIAL, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(OptimizationState.AFTER_PRA, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(OptimizationState.AFTER_ARA, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(OptimizationState.AFTER_CRA, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMargin() {
        when(initialResult.getMargin(flowCnec, AMPERE)).thenReturn(100.);
        when(initialResult.getMargin(flowCnec, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getMargin(OptimizationState.INITIAL, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(OptimizationState.AFTER_PRA, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(OptimizationState.AFTER_ARA, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(OptimizationState.AFTER_CRA, flowCnec, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getMargin(OptimizationState.INITIAL, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_PRA, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_ARA, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_CRA, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetRelativeMargin() {
        when(initialResult.getRelativeMargin(flowCnec, AMPERE)).thenReturn(100.);
        when(initialResult.getRelativeMargin(flowCnec, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getRelativeMargin(OptimizationState.INITIAL, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(OptimizationState.AFTER_PRA, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(OptimizationState.AFTER_ARA, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(OptimizationState.AFTER_CRA, flowCnec, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getRelativeMargin(OptimizationState.INITIAL, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_PRA, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_ARA, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_CRA, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCommercialFlow() {
        when(initialResult.getCommercialFlow(flowCnec, RIGHT, AMPERE)).thenReturn(100.);
        when(initialResult.getCommercialFlow(flowCnec, RIGHT, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getCommercialFlow(OptimizationState.INITIAL, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(OptimizationState.AFTER_PRA, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(OptimizationState.AFTER_ARA, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(OptimizationState.AFTER_CRA, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getCommercialFlow(OptimizationState.INITIAL, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(OptimizationState.AFTER_PRA, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(OptimizationState.AFTER_ARA, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(OptimizationState.AFTER_CRA, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLoopFlow() {
        when(initialResult.getLoopFlow(flowCnec, LEFT, AMPERE)).thenReturn(100.);
        when(initialResult.getLoopFlow(flowCnec, LEFT, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getLoopFlow(OptimizationState.INITIAL, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(OptimizationState.AFTER_PRA, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(OptimizationState.AFTER_ARA, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(OptimizationState.AFTER_CRA, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getLoopFlow(OptimizationState.INITIAL, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(OptimizationState.AFTER_PRA, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(OptimizationState.AFTER_ARA, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(OptimizationState.AFTER_CRA, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPtdfZonalSum() {
        when(initialResult.getPtdfZonalSum(flowCnec, RIGHT)).thenReturn(100.);

        assertEquals(100., output.getPtdfZonalSum(OptimizationState.INITIAL, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(OptimizationState.AFTER_PRA, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(OptimizationState.AFTER_ARA, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(OptimizationState.AFTER_CRA, flowCnec, RIGHT), DOUBLE_TOLERANCE);
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
        when(initialResult.getTap(pstRangeAction)).thenReturn(6);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(6, output.getPreOptimizationTapOnState(state1, pstRangeAction));
        assertEquals(6, output.getPreOptimizationTapOnState(state2, pstRangeAction));
    }

    @Test
    public void testGetOptimizedTapOnState() {
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(initialResult.getTap(pstRangeAction)).thenReturn(6);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(6, output.getOptimizedTapOnState(state1, pstRangeAction));
        assertEquals(6, output.getOptimizedTapOnState(state2, pstRangeAction));
    }

    @Test
    public void testGetPreOptimizationSetPointOnState() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        when(initialResult.getSetpoint(rangeAction)).thenReturn(60.);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(60., output.getPreOptimizationSetPointOnState(state1, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(60., output.getPreOptimizationSetPointOnState(state2, rangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetOptimizedSetPointOnState() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        when(initialResult.getSetpoint(rangeAction)).thenReturn(60.);
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
    public void testOptimizedStepsExecuted() {
        setUp();
        assertFalse(output.getOptimizationStepsExecuted().hasRunSecondPreventive());
        output.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION);
        assertTrue(output.getOptimizationStepsExecuted().hasRunSecondPreventive());
        assertThrows(FaraoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertThrows(FaraoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertThrows(FaraoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
    }
}
