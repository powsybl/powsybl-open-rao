/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.commons.Unit.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UnoptimizedRaoResultImplTest {
    private PrePerimeterResult initialResult;
    private UnoptimizedRaoResultImpl output;
    private FlowCnec flowCnec;
    private Instant preventiveInstant;
    private Instant autoInstant;
    private Instant curativeInstant;
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @BeforeEach
    public void setUp() {
        Crac crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
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
        assertEquals(100., output.getFlow(preventiveInstant, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(autoInstant, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getFlow(curativeInstant, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getFlow(null, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(preventiveInstant, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(autoInstant, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getFlow(curativeInstant, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetMargin() {
        when(initialResult.getMargin(flowCnec, AMPERE)).thenReturn(100.);
        when(initialResult.getMargin(flowCnec, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getMargin(null, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(preventiveInstant, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(autoInstant, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getMargin(curativeInstant, flowCnec, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getMargin(null, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(preventiveInstant, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(autoInstant, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(curativeInstant, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetRelativeMargin() {
        when(initialResult.getRelativeMargin(flowCnec, AMPERE)).thenReturn(100.);
        when(initialResult.getRelativeMargin(flowCnec, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getRelativeMargin(null, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(preventiveInstant, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(autoInstant, flowCnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getRelativeMargin(curativeInstant, flowCnec, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getRelativeMargin(null, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(preventiveInstant, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(autoInstant, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(curativeInstant, flowCnec, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCommercialFlow() {
        when(initialResult.getCommercialFlow(flowCnec, RIGHT, AMPERE)).thenReturn(100.);
        when(initialResult.getCommercialFlow(flowCnec, RIGHT, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getCommercialFlow(null, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(preventiveInstant, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(autoInstant, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getCommercialFlow(curativeInstant, flowCnec, RIGHT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getCommercialFlow(null, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(preventiveInstant, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(autoInstant, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getCommercialFlow(curativeInstant, flowCnec, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetLoopFlow() {
        when(initialResult.getLoopFlow(flowCnec, LEFT, AMPERE)).thenReturn(100.);
        when(initialResult.getLoopFlow(flowCnec, LEFT, MEGAWATT)).thenReturn(1000.);

        assertEquals(100., output.getLoopFlow(null, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(preventiveInstant, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(autoInstant, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., output.getLoopFlow(curativeInstant, flowCnec, LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000., output.getLoopFlow(null, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(preventiveInstant, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(autoInstant, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getLoopFlow(curativeInstant, flowCnec, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetPtdfZonalSum() {
        when(initialResult.getPtdfZonalSum(flowCnec, RIGHT)).thenReturn(100.);

        assertEquals(100., output.getPtdfZonalSum(null, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(preventiveInstant, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(autoInstant, flowCnec, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(100., output.getPtdfZonalSum(curativeInstant, flowCnec, RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCost() {
        when(initialResult.getCost()).thenReturn(-50.);
        assertEquals(-50., output.getCost(null), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-50., output.getCost(curativeInstant), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetFunctionalCost() {
        when(initialResult.getFunctionalCost()).thenReturn(-500.);
        assertEquals(-500., output.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetVirtualCost() {
        when(initialResult.getVirtualCost()).thenReturn(-5000.);
        assertEquals(-5000., output.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-5000., output.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
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
        assertEquals(60., output.getVirtualCost(preventiveInstant, "one"), DOUBLE_TOLERANCE);
        assertEquals(60., output.getVirtualCost(autoInstant, "one"), DOUBLE_TOLERANCE);
        assertEquals(60., output.getVirtualCost(curativeInstant, "one"), DOUBLE_TOLERANCE);

        assertEquals(600., output.getVirtualCost(null, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(preventiveInstant, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(autoInstant, "two"), DOUBLE_TOLERANCE);
        assertEquals(600., output.getVirtualCost(curativeInstant, "two"), DOUBLE_TOLERANCE);
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
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
    }
}
