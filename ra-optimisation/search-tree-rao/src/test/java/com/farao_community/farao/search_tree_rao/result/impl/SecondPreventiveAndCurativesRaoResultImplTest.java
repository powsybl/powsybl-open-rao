/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.PerimeterResult;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SecondPreventiveAndCurativesRaoResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private PrePerimeterResult initialResult;
    private PerimeterResult post1PResult; // post 1st preventive result
    private PerimeterResult post2PResult; // post 2nd preventive result
    private PrePerimeterResult preCurativeResult;
    private Map<State, OptimizationResult> postCurativeResults;
    private PstRangeAction pstRangeAction = mock(PstRangeAction.class);
    private RangeAction rangeAction;
    private NetworkAction networkAction;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec cnec4;
    private State state1;
    private State state2;
    private State state3;
    private State preventiveState;
    private SecondPreventiveAndCurativesRaoResultImpl outputRaIn2p;
    private SecondPreventiveAndCurativesRaoResultImpl outputRaExcludedFrom2p;
    private OptimizationResult curativeResult1;
    private OptimizationResult curativeResult2;

    @Before
    public void setUp() {
        cnec1 = mock(FlowCnec.class);
        cnec2 = mock(FlowCnec.class);
        cnec3 = mock(FlowCnec.class);
        cnec4 = mock(FlowCnec.class);
        state1 = mock(State.class);
        state2 = mock(State.class);
        state3 = mock(State.class);
        preventiveState = mock(State.class);
        when(cnec1.getState()).thenReturn(state1);
        when(cnec2.getState()).thenReturn(state2);
        when(preventiveState.getInstant()).thenReturn(Instant.PREVENTIVE);
        when(state1.getInstant()).thenReturn(Instant.CURATIVE);
        when(state2.getInstant()).thenReturn(Instant.CURATIVE);
        when(state3.getInstant()).thenReturn(Instant.CURATIVE);

        initialResult = mock(PrePerimeterResult.class);
        post1PResult = mock(PerimeterResult.class);
        post2PResult = mock(PerimeterResult.class);
        preCurativeResult = mock(PrePerimeterResult.class);

        curativeResult1 = mock(OptimizationResult.class);
        curativeResult2 = mock(OptimizationResult.class);
        postCurativeResults = Map.of(state1, curativeResult1, state2, curativeResult2);

        pstRangeAction = mock(PstRangeAction.class);
        rangeAction = mock(RangeAction.class);
        networkAction = mock(NetworkAction.class);

        when(initialResult.getFunctionalCost()).thenReturn(1000.);
        when(initialResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1));
        when(initialResult.getVirtualCost()).thenReturn(100.);
        when(initialResult.getVirtualCost("mnec")).thenReturn(20.);
        when(initialResult.getVirtualCost("lf")).thenReturn(80.);
        when(initialResult.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));
        when(initialResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec2));
        when(initialResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec1));
        when(initialResult.getSetpoint(pstRangeAction)).thenReturn(6.7);
        when(initialResult.getSetpoint(rangeAction)).thenReturn(5.6);
        when(initialResult.getTap(pstRangeAction)).thenReturn(1);
        when(initialResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(initialResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-1000.);
        when(initialResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(-500.);
        when(initialResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(-2000.);
        when(initialResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(-1000.);
        when(initialResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(-500.);
        when(initialResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(-250.);
        when(initialResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(-1500.);
        when(initialResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(-750.);

        when(post1PResult.getFunctionalCost()).thenReturn(-1010.);
        when(post1PResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(post1PResult.getVirtualCost()).thenReturn(-110.);
        when(post1PResult.getVirtualCost("mnec")).thenReturn(-30.);
        when(post1PResult.getVirtualCost("lf")).thenReturn(-90.);
        when(post1PResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(post1PResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(post1PResult.isActivated(networkAction)).thenReturn(false);
        when(post1PResult.getActivatedNetworkActions()).thenReturn(Set.of());
        when(post1PResult.getActivatedRangeActions(any())).thenReturn(Set.of());
        when(post1PResult.getOptimizedSetpoint(eq(pstRangeAction), any())).thenReturn(18.9);
        when(post1PResult.getOptimizedSetpoint(eq(rangeAction), any())).thenReturn(15.6);
        when(post1PResult.getOptimizedTap(eq(pstRangeAction), any())).thenReturn(12);
        when(post1PResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(post1PResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(post1PResult.getOptimizedTapsOnState(preventiveState)).thenReturn(Map.of(pstRangeAction, 12));
        when(post1PResult.getOptimizedSetpointsOnState(preventiveState)).thenReturn(Map.of(pstRangeAction, 18.9, rangeAction, 15.6));
        when(post1PResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1010.);
        when(post1PResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(510.);
        when(post1PResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2010.);
        when(post1PResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1010.);
        when(post1PResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(510.);
        when(post1PResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(260.);
        when(post1PResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1510.);
        when(post1PResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(760.);

        when(post2PResult.getFunctionalCost()).thenReturn(-1020.);
        when(post2PResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(post2PResult.getVirtualCost()).thenReturn(-120.);
        when(post2PResult.getVirtualCost("mnec")).thenReturn(-40.);
        when(post2PResult.getVirtualCost("lf")).thenReturn(-100.);
        when(post2PResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(post2PResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(post2PResult.isActivated(networkAction)).thenReturn(true);
        when(post2PResult.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(post2PResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(rangeAction));
        when(post2PResult.getOptimizedSetpoint(pstRangeAction, preventiveState)).thenReturn(28.9);
        when(post2PResult.getOptimizedSetpoint(pstRangeAction, state1)).thenReturn(10.2);
        when(post2PResult.getOptimizedSetpoint(pstRangeAction, state2)).thenReturn(28.9);
        when(post2PResult.getOptimizedTap(pstRangeAction, preventiveState)).thenReturn(22);
        when(post2PResult.getOptimizedTap(pstRangeAction, state1)).thenReturn(10);
        when(post2PResult.getOptimizedTap(pstRangeAction, state2)).thenReturn(22);
        when(post2PResult.getOptimizedSetpoint(rangeAction, preventiveState)).thenReturn(25.6);
        when(post2PResult.getOptimizedSetpoint(rangeAction, state1)).thenReturn(25.6);
        when(post2PResult.getOptimizedSetpoint(rangeAction, state2)).thenReturn(-14.2);
        when(post2PResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(post2PResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(post2PResult.getActivatedRangeActions(state1)).thenReturn(Set.of(pstRangeAction));
        when(post2PResult.getActivatedRangeActions(state2)).thenReturn(Set.of(rangeAction));
        when(post2PResult.getOptimizedTapsOnState(preventiveState)).thenReturn(Map.of(pstRangeAction, 22));
        when(post2PResult.getOptimizedTapsOnState(state1)).thenReturn(Map.of(pstRangeAction, 10));
        when(post2PResult.getOptimizedTapsOnState(state2)).thenReturn(Map.of(pstRangeAction, 22));
        when(post2PResult.getOptimizedSetpointsOnState(preventiveState)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        when(post2PResult.getOptimizedSetpointsOnState(state1)).thenReturn(Map.of(pstRangeAction, 10.2, rangeAction, 25.6));
        when(post2PResult.getOptimizedSetpointsOnState(state2)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, -14.2));
        when(post2PResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1020.);
        when(post2PResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(520.);
        when(post2PResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2020.);
        when(post2PResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1020.);
        when(post2PResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(520.);
        when(post2PResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(270.);
        when(post2PResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1520.);
        when(post2PResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(770.);

        when(curativeResult1.getFunctionalCost()).thenReturn(-1030.);
        when(curativeResult1.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(curativeResult1.getVirtualCost()).thenReturn(-130.);
        when(curativeResult1.getVirtualCost("mnec")).thenReturn(-50.);
        when(curativeResult1.getVirtualCost("lf")).thenReturn(-110.);
        when(curativeResult1.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(curativeResult1.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(curativeResult1.isActivated(networkAction)).thenReturn(false);
        when(curativeResult1.getActivatedNetworkActions()).thenReturn(Set.of());
        when(curativeResult1.getOptimizedSetpoint(pstRangeAction, state1)).thenReturn(28.9);
        when(curativeResult1.getOptimizedSetpoint(rangeAction, state1)).thenReturn(25.6);
        when(curativeResult1.getOptimizedTap(pstRangeAction, state1)).thenReturn(22);
        when(curativeResult1.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(curativeResult1.getActivatedRangeActions(state1)).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(curativeResult1.getOptimizedTapsOnState(state1)).thenReturn(Map.of(pstRangeAction, 22));
        when(curativeResult1.getOptimizedSetpointsOnState(state1)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        when(curativeResult1.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1030.);
        when(curativeResult1.getMargin(cnec2, Unit.AMPERE)).thenReturn(530.);
        when(curativeResult1.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2030.);
        when(curativeResult1.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1030.);
        when(curativeResult1.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(530.);
        when(curativeResult1.getMargin(cnec1, Unit.AMPERE)).thenReturn(280.);
        when(curativeResult1.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1530.);
        when(curativeResult1.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(780.);

        when(curativeResult2.getFunctionalCost()).thenReturn(-1040.);
        when(curativeResult2.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(curativeResult2.getVirtualCost()).thenReturn(-140.);
        when(curativeResult2.getVirtualCost("mnec")).thenReturn(-60.);
        when(curativeResult2.getVirtualCost("lf")).thenReturn(-120.);
        when(curativeResult2.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(curativeResult2.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(curativeResult2.isActivated(networkAction)).thenReturn(false);
        when(curativeResult2.getActivatedNetworkActions()).thenReturn(Set.of());
        when(curativeResult2.getOptimizedSetpoint(pstRangeAction, state2)).thenReturn(48.9);
        when(curativeResult2.getOptimizedSetpoint(rangeAction, state2)).thenReturn(25.6);
        when(curativeResult2.getOptimizedTap(pstRangeAction, state2)).thenReturn(42);
        when(curativeResult2.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(curativeResult1.getActivatedRangeActions(state2)).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(curativeResult2.getOptimizedTapsOnState(state2)).thenReturn(Map.of(pstRangeAction, 42));
        when(curativeResult2.getOptimizedSetpointsOnState(state2)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        when(curativeResult2.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1040.);
        when(curativeResult2.getMargin(cnec2, Unit.AMPERE)).thenReturn(540.);
        when(curativeResult2.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2040.);
        when(curativeResult2.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1040.);
        when(curativeResult2.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(540.);
        when(curativeResult2.getMargin(cnec1, Unit.AMPERE)).thenReturn(290.);
        when(curativeResult2.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1540.);
        when(curativeResult2.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(790.);

        when(preCurativeResult.getFunctionalCost()).thenReturn(-1050.);
        when(preCurativeResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec3));
        when(preCurativeResult.getVirtualCost()).thenReturn(-150.);
        when(preCurativeResult.getVirtualCost("mnec")).thenReturn(-70.);
        when(preCurativeResult.getVirtualCost("lf")).thenReturn(-130.);
        when(preCurativeResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec3, cnec2));
        when(preCurativeResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec1, cnec4));
        when(preCurativeResult.getSetpoint(pstRangeAction)).thenReturn(18.9);
        when(preCurativeResult.getSetpoint(rangeAction)).thenReturn(15.6);
        when(preCurativeResult.getTap(pstRangeAction)).thenReturn(52);
        when(preCurativeResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(preCurativeResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1050.);
        when(preCurativeResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(550.);
        when(preCurativeResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2050.);
        when(preCurativeResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1050.);
        when(preCurativeResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(550.);
        when(preCurativeResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(300.);
        when(preCurativeResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1550.);
        when(preCurativeResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(800.);

        outputRaIn2p = new SecondPreventiveAndCurativesRaoResultImpl(initialResult,
                preventiveState,
                post1PResult,
                post2PResult,
                preCurativeResult,
                Map.of(state1, curativeResult1, state2, curativeResult2),
                Set.of());
        outputRaExcludedFrom2p = new SecondPreventiveAndCurativesRaoResultImpl(initialResult,
            preventiveState,
            post1PResult,
            post2PResult,
            preCurativeResult,
            Map.of(state1, curativeResult1, state2, curativeResult2),
            Set.of(rangeAction, pstRangeAction));
    }

    @Test
    public void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(post2PResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult1.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult2.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, outputRaIn2p.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, outputRaIn2p.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(post2PResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, outputRaIn2p.getComputationStatus());

        when(post2PResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult2.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, outputRaIn2p.getComputationStatus());
    }

    @Test
    public void testUnimplementedGetResult() {
        assertThrows(NotImplementedException.class, () -> outputRaIn2p.getPerimeterResult(INITIAL, state1));
        assertThrows(NotImplementedException.class, () -> outputRaIn2p.getPostPreventivePerimeterResult());
        assertThrows(NotImplementedException.class, () -> outputRaIn2p.getInitialResult());
    }

    @Test
    public void testGetFunctionalCost() {
        assertEquals(1000., outputRaIn2p.getFunctionalCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-1050., outputRaIn2p.getFunctionalCost(AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(-1020., outputRaIn2p.getFunctionalCost(AFTER_CRA), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMostLimitingElements() {
        assertEquals(List.of(cnec1), outputRaIn2p.getMostLimitingElements(INITIAL, 5));
        assertEquals(List.of(cnec3), outputRaIn2p.getMostLimitingElements(AFTER_PRA, 15));
        assertEquals(List.of(cnec2), outputRaIn2p.getMostLimitingElements(AFTER_CRA, 445));
    }

    @Test
    public void testGetVirtualCost() {
        assertEquals(100., outputRaIn2p.getVirtualCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-150., outputRaIn2p.getVirtualCost(AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(-120., outputRaIn2p.getVirtualCost(AFTER_CRA), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetVirtualCostNames() {
        assertEquals(Set.of("mnec", "lf"), outputRaIn2p.getVirtualCostNames());
    }

    @Test
    public void testGetVirtualCostByName() {
        assertEquals(20., outputRaIn2p.getVirtualCost(INITIAL, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(80., outputRaIn2p.getVirtualCost(INITIAL, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-70., outputRaIn2p.getVirtualCost(AFTER_PRA, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-130., outputRaIn2p.getVirtualCost(AFTER_PRA, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-40., outputRaIn2p.getVirtualCost(AFTER_CRA, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-100., outputRaIn2p.getVirtualCost(AFTER_CRA, "lf"), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCostlyElements() {
        assertEquals(List.of(cnec2), outputRaIn2p.getCostlyElements(INITIAL, "mnec", 5));
        assertEquals(List.of(cnec1), outputRaIn2p.getCostlyElements(INITIAL, "lf", 15));

        assertEquals(List.of(cnec3, cnec2), outputRaIn2p.getCostlyElements(AFTER_PRA, "mnec", 5));
        assertEquals(List.of(cnec1, cnec4), outputRaIn2p.getCostlyElements(AFTER_PRA, "lf", 15));

        assertEquals(List.of(cnec1), outputRaIn2p.getCostlyElements(AFTER_CRA, "mnec", 5));
        assertEquals(List.of(cnec2), outputRaIn2p.getCostlyElements(AFTER_CRA, "lf", 15));
    }

    @Test
    public void testWasNetworkActionActivatedBeforeState() {
        assertFalse(outputRaIn2p.wasActivatedBeforeState(preventiveState, networkAction));
        assertTrue(outputRaIn2p.wasActivatedBeforeState(state1, networkAction));
        assertTrue(outputRaIn2p.wasActivatedBeforeState(state2, networkAction));

        outputRaIn2p = new SecondPreventiveAndCurativesRaoResultImpl(initialResult,
                preventiveState,
                post1PResult,
                post2PResult,
                preCurativeResult,
                Map.of(state1, curativeResult1, state2, curativeResult2),
                Set.of(networkAction));
        assertFalse(outputRaIn2p.wasActivatedBeforeState(state1, networkAction));
        assertFalse(outputRaIn2p.wasActivatedBeforeState(state2, networkAction));
    }

    @Test
    public void testIsNetworkActionActivatedDuringState() {
        assertTrue(outputRaIn2p.isActivatedDuringState(preventiveState, networkAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(state1, networkAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(state2, networkAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(state3, networkAction));

        when(curativeResult1.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        outputRaIn2p = new SecondPreventiveAndCurativesRaoResultImpl(initialResult,
                preventiveState,
                post1PResult,
                post2PResult,
                preCurativeResult,
                Map.of(state1, curativeResult1, state2, curativeResult2),
                Set.of(networkAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(preventiveState, networkAction));
        assertTrue(outputRaIn2p.isActivatedDuringState(state1, networkAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(state2, networkAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(state3, networkAction));
    }

    @Test
    public void testGetActivatedNetworkActionsDuringState() {
        assertEquals(Set.of(networkAction), outputRaIn2p.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(), outputRaIn2p.getActivatedNetworkActionsDuringState(state3));
        assertEquals(Set.of(), outputRaIn2p.getActivatedNetworkActionsDuringState(state1));
        assertEquals(Set.of(), outputRaIn2p.getActivatedNetworkActionsDuringState(state2));

        when(curativeResult2.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        outputRaIn2p = new SecondPreventiveAndCurativesRaoResultImpl(initialResult,
                preventiveState,
                post1PResult,
                post2PResult,
                preCurativeResult,
                Map.of(state1, curativeResult1, state2, curativeResult2),
                Set.of(networkAction));
        assertEquals(Set.of(networkAction), outputRaIn2p.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(), outputRaIn2p.getActivatedNetworkActionsDuringState(state3));
        assertEquals(Set.of(), outputRaIn2p.getActivatedNetworkActionsDuringState(state1));
        assertEquals(Set.of(networkAction), outputRaIn2p.getActivatedNetworkActionsDuringState(state2));

        when(post2PResult.getActivatedNetworkActions()).thenReturn(Set.of());
        assertEquals(Set.of(), outputRaIn2p.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(), outputRaIn2p.getActivatedNetworkActionsDuringState(state3));
    }

    @Test
    public void testIsRangeActionActivatedDuringState() {
        // with - by default - ranges actions included in 2P
        when(post2PResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(pstRangeAction));
        when(post2PResult.getActivatedRangeActions(state1)).thenReturn(Set.of(rangeAction));
        when(post2PResult.getActivatedRangeActions(state2)).thenReturn(Set.of(pstRangeAction, rangeAction));
        when(post2PResult.getActivatedRangeActions(state3)).thenReturn(Set.of());

        assertTrue(outputRaIn2p.isActivatedDuringState(preventiveState, pstRangeAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(state1, pstRangeAction));
        assertTrue(outputRaIn2p.isActivatedDuringState(state2, pstRangeAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(state3, pstRangeAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(preventiveState, rangeAction));
        assertTrue(outputRaIn2p.isActivatedDuringState(state1, rangeAction));
        assertTrue(outputRaIn2p.isActivatedDuringState(state2, rangeAction));
        assertFalse(outputRaIn2p.isActivatedDuringState(state3, rangeAction));

        // with range Action excluded from 2p
        when(post1PResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(pstRangeAction, rangeAction));
        when(curativeResult1.getActivatedRangeActions(state1)).thenReturn(Set.of(pstRangeAction));
        when(curativeResult2.getActivatedRangeActions(state2)).thenReturn(Set.of(rangeAction));

        assertTrue(outputRaExcludedFrom2p.isActivatedDuringState(preventiveState, pstRangeAction));
        assertTrue(outputRaExcludedFrom2p.isActivatedDuringState(state1, pstRangeAction));
        assertFalse(outputRaExcludedFrom2p.isActivatedDuringState(state2, pstRangeAction));
        assertFalse(outputRaExcludedFrom2p.isActivatedDuringState(state3, pstRangeAction));
        assertTrue(outputRaExcludedFrom2p.isActivatedDuringState(preventiveState, rangeAction));
        assertFalse(outputRaExcludedFrom2p.isActivatedDuringState(state1, rangeAction));
        assertTrue(outputRaExcludedFrom2p.isActivatedDuringState(state2, rangeAction));
        assertFalse(outputRaExcludedFrom2p.isActivatedDuringState(state3, rangeAction));
    }

    @Test
    public void testGetPreOptimizationTapOnState() {
        // with - by default - ranges actions included in 2P
        when(initialResult.getTap(pstRangeAction)).thenReturn(1);
        when(post2PResult.getOptimizedTap(pstRangeAction, preventiveState)).thenReturn(22);
        assertEquals(1, outputRaIn2p.getPreOptimizationTapOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(22, outputRaIn2p.getPreOptimizationTapOnState(state1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(22, outputRaIn2p.getPreOptimizationTapOnState(state2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(22, outputRaIn2p.getPreOptimizationTapOnState(state3, pstRangeAction), DOUBLE_TOLERANCE);

        // with range Action excluded from 2p
        when(post1PResult.getOptimizedTap(pstRangeAction, preventiveState)).thenReturn(-12);
        assertEquals(1, outputRaExcludedFrom2p.getPreOptimizationTapOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-12, outputRaExcludedFrom2p.getPreOptimizationTapOnState(state1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-12, outputRaExcludedFrom2p.getPreOptimizationTapOnState(state2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-12, outputRaExcludedFrom2p.getPreOptimizationTapOnState(state3, pstRangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetOptimizedTapOnState() {
        // with - by default - ranges actions included in 2P
        when(post2PResult.getOptimizedTap(pstRangeAction, preventiveState)).thenReturn(10);
        when(post2PResult.getOptimizedTap(pstRangeAction, state1)).thenReturn(11);
        when(post2PResult.getOptimizedTap(pstRangeAction, state2)).thenReturn(12);
        when(post2PResult.getOptimizedTap(pstRangeAction, state3)).thenReturn(13);

        assertEquals(10, outputRaIn2p.getOptimizedTapOnState(preventiveState, pstRangeAction));
        assertEquals(11, outputRaIn2p.getOptimizedTapOnState(state1, pstRangeAction));
        assertEquals(12, outputRaIn2p.getOptimizedTapOnState(state2, pstRangeAction));
        assertEquals(13, outputRaIn2p.getOptimizedTapOnState(state3, pstRangeAction));

        // with range Action excluded from 2p
        when(post1PResult.getOptimizedTap(eq(pstRangeAction), any())).thenReturn(20);
        when(curativeResult1.getOptimizedTap(pstRangeAction, state1)).thenReturn(21);
        when(curativeResult2.getOptimizedTap(pstRangeAction, state2)).thenReturn(22);

        assertEquals(20, outputRaExcludedFrom2p.getOptimizedTapOnState(preventiveState, pstRangeAction));
        assertEquals(21, outputRaExcludedFrom2p.getOptimizedTapOnState(state1, pstRangeAction));
        assertEquals(22, outputRaExcludedFrom2p.getOptimizedTapOnState(state2, pstRangeAction));
        assertEquals(20, outputRaExcludedFrom2p.getOptimizedTapOnState(state3, pstRangeAction));
    }

    @Test
    public void testGetPreOptimizationSetPointOnState() {
        // with - by default - ranges actions included in 2P
        when(initialResult.getSetpoint(pstRangeAction)).thenReturn(6.7);
        when(post2PResult.getOptimizedSetpoint(pstRangeAction, preventiveState)).thenReturn(28.9);
        assertEquals(6.7, outputRaIn2p.getPreOptimizationSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, outputRaIn2p.getPreOptimizationSetPointOnState(state1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, outputRaIn2p.getPreOptimizationSetPointOnState(state2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, outputRaIn2p.getPreOptimizationSetPointOnState(state3, pstRangeAction), DOUBLE_TOLERANCE);

        // with range Action excluded from 2p
        when(post1PResult.getOptimizedSetpoint(pstRangeAction, preventiveState)).thenReturn(-10.3);
        assertEquals(6.7, outputRaExcludedFrom2p.getPreOptimizationSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-10.3, outputRaExcludedFrom2p.getPreOptimizationSetPointOnState(state1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-10.3, outputRaExcludedFrom2p.getPreOptimizationSetPointOnState(state2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-10.3, outputRaExcludedFrom2p.getPreOptimizationSetPointOnState(state3, pstRangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetOptimizedSetPointOnState() {
        // with - by default - ranges actions included in 2P
        when(post2PResult.getOptimizedSetpoint(pstRangeAction, preventiveState)).thenReturn(567.);
        when(post2PResult.getOptimizedSetpoint(pstRangeAction, state1)).thenReturn(28.9);
        when(post2PResult.getOptimizedSetpoint(pstRangeAction, state2)).thenReturn(48.9);
        when(post2PResult.getOptimizedSetpoint(pstRangeAction, state3)).thenReturn(567.);
        assertEquals(567, outputRaIn2p.getOptimizedSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, outputRaIn2p.getOptimizedSetPointOnState(state1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(48.9, outputRaIn2p.getOptimizedSetPointOnState(state2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(567, outputRaIn2p.getOptimizedSetPointOnState(state3, pstRangeAction), DOUBLE_TOLERANCE);

        // with range Action excluded from 2p
        when(post1PResult.getOptimizedSetpoint(eq(pstRangeAction), any())).thenReturn(40.2);
        when(curativeResult1.getOptimizedSetpoint(pstRangeAction, state1)).thenReturn(450.2);
        when(curativeResult2.getOptimizedSetpoint(pstRangeAction, state2)).thenReturn(-100.5);

        assertEquals(40.2, outputRaExcludedFrom2p.getOptimizedSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(450.2, outputRaExcludedFrom2p.getOptimizedSetPointOnState(state1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-100.5, outputRaExcludedFrom2p.getOptimizedSetPointOnState(state2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(40.2, outputRaExcludedFrom2p.getOptimizedSetPointOnState(state3, pstRangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetActivatedRangeActionsDuringState() {
        assertEquals(Set.of(pstRangeAction, rangeAction), outputRaIn2p.getActivatedRangeActionsDuringState(preventiveState));
        assertEquals(Set.of(pstRangeAction), outputRaIn2p.getActivatedRangeActionsDuringState(state1));
        assertEquals(Set.of(rangeAction), outputRaIn2p.getActivatedRangeActionsDuringState(state2));
        assertEquals(Set.of(), outputRaIn2p.getActivatedRangeActionsDuringState(state3));

        when(post2PResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of());
        when(post1PResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(pstRangeAction));
        outputRaIn2p = new SecondPreventiveAndCurativesRaoResultImpl(initialResult,
                preventiveState,
                post1PResult,
                post2PResult,
                preCurativeResult,
                Map.of(state1, curativeResult1, state2, curativeResult2),
                Set.of(pstRangeAction));
        assertEquals(Set.of(pstRangeAction), outputRaIn2p.getActivatedRangeActionsDuringState(preventiveState));
    }

    @Test
    public void testGetOptimizedTapsOnState() {
        // with - by default - ranges actions included in 2P
        when(post2PResult.getOptimizedTapsOnState(preventiveState)).thenReturn(Map.of(pstRangeAction, 0));
        when(post2PResult.getOptimizedTapsOnState(state1)).thenReturn(Map.of(pstRangeAction, 0));
        when(post2PResult.getOptimizedTapsOnState(state2)).thenReturn(Map.of(pstRangeAction, -15));
        when(post2PResult.getOptimizedTapsOnState(state3)).thenReturn(Map.of(pstRangeAction, 0));

        assertEquals(Map.of(pstRangeAction, 0), outputRaIn2p.getOptimizedTapsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 0), outputRaIn2p.getOptimizedTapsOnState(state1));
        assertEquals(Map.of(pstRangeAction, -15), outputRaIn2p.getOptimizedTapsOnState(state2));
        assertEquals(Map.of(pstRangeAction, 0), outputRaIn2p.getOptimizedTapsOnState(state3));

        // with range Action excluded from 2p
        when(post1PResult.getOptimizedTapsOnState(any())).thenReturn(Map.of(pstRangeAction, 4));
        when(curativeResult1.getOptimizedTapsOnState(state1)).thenReturn(Map.of(pstRangeAction, -10));
        when(curativeResult2.getOptimizedTapsOnState(state2)).thenReturn(Map.of(pstRangeAction, 4));

        assertEquals(Map.of(pstRangeAction, 4), outputRaExcludedFrom2p.getOptimizedTapsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, -10), outputRaExcludedFrom2p.getOptimizedTapsOnState(state1));
        assertEquals(Map.of(pstRangeAction, 4), outputRaExcludedFrom2p.getOptimizedTapsOnState(state2));
        assertEquals(Map.of(pstRangeAction, 4), outputRaExcludedFrom2p.getOptimizedTapsOnState(state3));
    }

    @Test
    public void testGetOptimizedSetPointsOnState() {
        // with - by default - ranges actions included in 2P
        when(post2PResult.getOptimizedSetpointsOnState(preventiveState)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        when(post2PResult.getOptimizedSetpointsOnState(state1)).thenReturn(Map.of(pstRangeAction, 10.2, rangeAction, 25.6));
        when(post2PResult.getOptimizedSetpointsOnState(state2)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, -14.2));
        when(post2PResult.getOptimizedSetpointsOnState(state3)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));

        assertEquals(Map.of(pstRangeAction, 28.9, rangeAction, 25.6), outputRaIn2p.getOptimizedSetPointsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 10.2, rangeAction, 25.6), outputRaIn2p.getOptimizedSetPointsOnState(state1));
        assertEquals(Map.of(pstRangeAction, 28.9, rangeAction, -14.2), outputRaIn2p.getOptimizedSetPointsOnState(state2));
        assertEquals(Map.of(pstRangeAction, 28.9, rangeAction, 25.6), outputRaIn2p.getOptimizedSetPointsOnState(state3));

        // with range Action excluded from 2p
        when(post1PResult.getOptimizedSetpointsOnState(any())).thenReturn(Map.of(pstRangeAction, 0., rangeAction, 1000.));
        when(curativeResult1.getOptimizedSetpointsOnState(state1)).thenReturn(Map.of(pstRangeAction, 333., rangeAction, 1000.));
        when(curativeResult2.getOptimizedSetpointsOnState(state2)).thenReturn(Map.of(pstRangeAction, 444., rangeAction, 111.));

        assertEquals(Map.of(pstRangeAction, 0., rangeAction, 1000.), outputRaExcludedFrom2p.getOptimizedSetPointsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 333., rangeAction, 1000.), outputRaExcludedFrom2p.getOptimizedSetPointsOnState(state1));
        assertEquals(Map.of(pstRangeAction, 444., rangeAction, 111.), outputRaExcludedFrom2p.getOptimizedSetPointsOnState(state2));
        assertEquals(Map.of(pstRangeAction, 0., rangeAction, 1000.), outputRaExcludedFrom2p.getOptimizedSetPointsOnState(state3));
    }

    @Test
    public void testGetFlow() {
        when(initialResult.getFlow(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getFlow(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(post2PResult.getFlow(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        assertEquals(10., outputRaIn2p.getFlow(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., outputRaIn2p.getFlow(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., outputRaIn2p.getFlow(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMargin() {
        when(initialResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(post2PResult.getMargin(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        assertEquals(10., outputRaIn2p.getMargin(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., outputRaIn2p.getMargin(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., outputRaIn2p.getMargin(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetRelativeMargin() {
        when(initialResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(post2PResult.getRelativeMargin(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        assertEquals(10., outputRaIn2p.getRelativeMargin(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., outputRaIn2p.getRelativeMargin(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., outputRaIn2p.getRelativeMargin(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCommercialFlow() {
        when(initialResult.getCommercialFlow(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getCommercialFlow(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(post2PResult.getCommercialFlow(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        assertEquals(10., outputRaIn2p.getCommercialFlow(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., outputRaIn2p.getCommercialFlow(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., outputRaIn2p.getCommercialFlow(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLoopFlow() {
        when(initialResult.getLoopFlow(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getLoopFlow(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(post2PResult.getLoopFlow(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        assertEquals(10., outputRaIn2p.getLoopFlow(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., outputRaIn2p.getLoopFlow(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., outputRaIn2p.getLoopFlow(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPtdfZonalSum() {
        when(initialResult.getPtdfZonalSum(cnec1)).thenReturn(10.);
        when(preCurativeResult.getPtdfZonalSum(cnec2)).thenReturn(20.);
        when(post2PResult.getPtdfZonalSum(cnec3)).thenReturn(30.);
        assertEquals(10., outputRaIn2p.getPtdfZonalSum(INITIAL, cnec1), DOUBLE_TOLERANCE);
        assertEquals(20., outputRaIn2p.getPtdfZonalSum(AFTER_PRA, cnec2), DOUBLE_TOLERANCE);
        assertEquals(30., outputRaIn2p.getPtdfZonalSum(AFTER_CRA, cnec3), DOUBLE_TOLERANCE);
    }
}
