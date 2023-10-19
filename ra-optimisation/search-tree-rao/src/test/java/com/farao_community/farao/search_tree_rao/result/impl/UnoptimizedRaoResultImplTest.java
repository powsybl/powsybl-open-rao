/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.InstantImpl;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UnoptimizedRaoResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);
    private PrePerimeterResult initialResult;
    private UnoptimizedRaoResultImpl output;
    private FlowCnec flowCnec;

    @BeforeEach
    public void setUp() {
        initialResult = Mockito.mock(PrePerimeterResult.class);
        output = new UnoptimizedRaoResultImpl(initialResult);
        flowCnec = Mockito.mock(FlowCnec.class);
    }

    @Test
    void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());
    }

    @Test
    void testGetFlow() {
        when(initialResult.getFlow(flowCnec, LEFT, AMPERE)).thenReturn(100.);
        when(initialResult.getFlow(flowCnec, LEFT, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getFlow(null, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(INSTANT_PREV, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(INSTANT_AUTO, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(INSTANT_CURATIVE, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getFlow(null, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(INSTANT_PREV, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(INSTANT_AUTO, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(INSTANT_CURATIVE, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetMargin() {
        when(initialResult.getMargin(flowCnec, AMPERE)).thenReturn(100.);
        when(initialResult.getMargin(flowCnec, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getMargin(null, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(INSTANT_PREV, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(INSTANT_AUTO, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(INSTANT_CURATIVE, flowCnec, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getMargin(null, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(INSTANT_PREV, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(INSTANT_AUTO, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(INSTANT_CURATIVE, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetRelativeMargin() {
        when(initialResult.getRelativeMargin(flowCnec, AMPERE)).thenReturn(100.);
        when(initialResult.getRelativeMargin(flowCnec, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getRelativeMargin(null, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(INSTANT_PREV, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(INSTANT_AUTO, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(INSTANT_CURATIVE, flowCnec, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getRelativeMargin(null, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(INSTANT_PREV, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(INSTANT_AUTO, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(INSTANT_CURATIVE, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCommercialFlow() {
        when(initialResult.getCommercialFlow(flowCnec, RIGHT, AMPERE)).thenReturn(100.);
        when(initialResult.getCommercialFlow(flowCnec, RIGHT, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getCommercialFlow(null, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(INSTANT_PREV, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(INSTANT_AUTO, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(INSTANT_CURATIVE, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getCommercialFlow(null, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(INSTANT_PREV, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(INSTANT_AUTO, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(INSTANT_CURATIVE, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetLoopFlow() {
        when(initialResult.getLoopFlow(flowCnec, LEFT, AMPERE)).thenReturn(100.);
        when(initialResult.getLoopFlow(flowCnec, LEFT, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getLoopFlow(null, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(INSTANT_PREV, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(INSTANT_AUTO, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(INSTANT_CURATIVE, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getLoopFlow(null, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(INSTANT_PREV, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(INSTANT_AUTO, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(INSTANT_CURATIVE, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetPtdfZonalSum() {
        when(initialResult.getPtdfZonalSum(flowCnec, RIGHT)).thenReturn(100.);

        assertEquals(100., output.getPtdfZonalSum(null, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(INSTANT_PREV, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(INSTANT_AUTO, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(INSTANT_CURATIVE, flowCnec, RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCost() {
        when(initialResult.getCost()).thenReturn(-50.);
        assertEquals(-50., output.getCost(null), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(INSTANT_PREV), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(INSTANT_AUTO), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetFunctionalCost() {
        when(initialResult.getFunctionalCost()).thenReturn(-500.);
        assertEquals(-500., output.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(INSTANT_PREV), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(INSTANT_AUTO), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetVirtualCost() {
        when(initialResult.getVirtualCost()).thenReturn(-5000.);
        assertEquals(-5000., output.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(INSTANT_PREV), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(INSTANT_AUTO), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetVirtualCostNames() {
        when(initialResult.getVirtualCostNames()).thenReturn(Set.of("one", "two"));
        assertEquals(Set.of("one", "two"), output.getVirtualCostNames());
    }

    @Test
    void testGetVirtualCostWithName() {
        when(initialResult.getVirtualCost("one")).thenReturn(60.);
        when(initialResult.getVirtualCost("two")).thenReturn(600.);

        assertEquals(60., output.getVirtualCost(null, "one"), DOUBLE_TOLERANCE);
        assertEquals(60., output.getVirtualCost(INSTANT_PREV, "one"), DOUBLE_TOLERANCE);
        assertEquals(60., output.getVirtualCost(INSTANT_AUTO, "one"), DOUBLE_TOLERANCE);
        assertEquals(60., output.getVirtualCost(INSTANT_CURATIVE, "one"), DOUBLE_TOLERANCE);

        assertEquals(600., output.getVirtualCost(null, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(INSTANT_PREV, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(INSTANT_AUTO, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(INSTANT_CURATIVE, "two"), DOUBLE_TOLERANCE);
    }

    @Test
    void testWasActivatedBeforeState() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        State state = Mockito.mock(State.class);
        assertFalse(output.wasActivatedBeforeState(state, na));
    }

    @Test
    void testIsActivatedDuringState() {
        NetworkAction na = Mockito.mock(NetworkAction.class);
        State state = Mockito.mock(State.class);
        assertFalse(output.isActivatedDuringState(state, na));
    }

    @Test
    void testGetActivatedNetworkActionsDuringState() {
        State state = Mockito.mock(State.class);
        assertTrue(output.getActivatedNetworkActionsDuringState(state).isEmpty());
    }

    @Test
    void testIsActivatedDuringStateRa() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        State state = Mockito.mock(State.class);
        assertFalse(output.isActivatedDuringState(state, rangeAction));
    }

    @Test
    void testGetPreOptimizationTapOnState() {
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(initialResult.getTap(pstRangeAction)).thenReturn(6);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(6, output.getPreOptimizationTapOnState(state1, pstRangeAction));
        assertEquals(6, output.getPreOptimizationTapOnState(state2, pstRangeAction));
    }

    @Test
    void testGetOptimizedTapOnState() {
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(initialResult.getTap(pstRangeAction)).thenReturn(6);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(6, output.getOptimizedTapOnState(state1, pstRangeAction));
        assertEquals(6, output.getOptimizedTapOnState(state2, pstRangeAction));
    }

    @Test
    void testGetPreOptimizationSetPointOnState() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        when(initialResult.getSetpoint(rangeAction)).thenReturn(60.);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(60., output.getPreOptimizationSetPointOnState(state1, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(60., output.getPreOptimizationSetPointOnState(state2, rangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetOptimizedSetPointOnState() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        when(initialResult.getSetpoint(rangeAction)).thenReturn(60.);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        assertEquals(60., output.getOptimizedSetPointOnState(state1, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(60., output.getOptimizedSetPointOnState(state2, rangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetActivatedRangeActionsDuringState() {
        State state1 = Mockito.mock(State.class);
        assertTrue(output.getActivatedRangeActionsDuringState(state1).isEmpty());
    }

    @Test
    void testOptimizedStepsExecuted() {
        setUp();
        assertFalse(output.getOptimizationStepsExecuted().hasRunSecondPreventive());
        output.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION);
        assertTrue(output.getOptimizationStepsExecuted().hasRunSecondPreventive());
        FaraoException exception = assertThrows(FaraoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertEquals("", exception.getMessage());
    }
}
