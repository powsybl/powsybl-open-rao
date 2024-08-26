/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.powsybl.openrao.commons.Unit.AMPERE;
import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OneStateOnlyRaoResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private State optimizedState;
    private PrePerimeterResult initialResult;
    private OptimizationResult postOptimizationResult;
    private PstRangeAction pstRangeAction;
    private RangeAction<?> rangeAction;
    private NetworkAction networkAction;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private State cnec1state;
    private State cnec2state;
    private OneStateOnlyRaoResultImpl output;
    private Instant preventiveInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.isPreventive()).thenReturn(true);
        curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isCurative()).thenReturn(true);
        optimizedState = mock(State.class);

        initialResult = mock(PrePerimeterResult.class);
        postOptimizationResult = mock(OptimizationResult.class);
        pstRangeAction = mock(PstRangeAction.class);
        optimizedState = mock(State.class);
        rangeAction = mock(RangeAction.class);
        networkAction = mock(NetworkAction.class);
        cnec1 = mock(FlowCnec.class);
        cnec2 = mock(FlowCnec.class);
        cnec1state = mock(State.class);
        cnec2state = mock(State.class);
        when(cnec1.getState()).thenReturn(cnec1state);
        when(cnec2.getState()).thenReturn(cnec2state);

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
        when(initialResult.getFlow(cnec1, TwoSides.ONE, MEGAWATT)).thenReturn(-300.);
        when(initialResult.getFlow(cnec1, TwoSides.TWO, Unit.AMPERE)).thenReturn(-150.);
        when(initialResult.getCommercialFlow(cnec1, TwoSides.ONE, MEGAWATT)).thenReturn(-300.);
        when(initialResult.getCommercialFlow(cnec1, TwoSides.TWO, Unit.AMPERE)).thenReturn(-150.);
        when(initialResult.getLoopFlow(cnec1, TwoSides.ONE, MEGAWATT)).thenReturn(0.);
        when(initialResult.getLoopFlow(cnec1, TwoSides.TWO, Unit.AMPERE)).thenReturn(-0.);
        when(initialResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(-500.);
        when(initialResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(-250.);
        when(initialResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(-1500.);
        when(initialResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(-750.);
        when(initialResult.getFlow(cnec2, TwoSides.ONE, MEGAWATT)).thenReturn(-1000.);
        when(initialResult.getFlow(cnec2, TwoSides.TWO, Unit.AMPERE)).thenReturn(500.);
        when(initialResult.getCommercialFlow(cnec2, TwoSides.ONE, MEGAWATT)).thenReturn(-700.);
        when(initialResult.getCommercialFlow(cnec2, TwoSides.TWO, Unit.AMPERE)).thenReturn(450.);
        when(initialResult.getLoopFlow(cnec2, TwoSides.ONE, MEGAWATT)).thenReturn(-300.);
        when(initialResult.getLoopFlow(cnec2, TwoSides.TWO, Unit.AMPERE)).thenReturn(50.);

        when(postOptimizationResult.getFunctionalCost()).thenReturn(-1000.);
        when(postOptimizationResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(postOptimizationResult.getVirtualCost()).thenReturn(-100.);
        when(postOptimizationResult.getVirtualCost("mnec")).thenReturn(-20.);
        when(postOptimizationResult.getVirtualCost("lf")).thenReturn(-80.);
        when(postOptimizationResult.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));
        when(postOptimizationResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(postOptimizationResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(postOptimizationResult.isActivated(networkAction)).thenReturn(true);
        when(postOptimizationResult.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(postOptimizationResult.getOptimizedSetpoint(pstRangeAction, optimizedState)).thenReturn(8.9);
        when(postOptimizationResult.getOptimizedSetpoint(rangeAction, optimizedState)).thenReturn(5.6);
        when(postOptimizationResult.getOptimizedTap(pstRangeAction, optimizedState)).thenReturn(2);
        when(postOptimizationResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(postOptimizationResult.getOptimizedTapsOnState(optimizedState)).thenReturn(Map.of(pstRangeAction, 2));
        when(postOptimizationResult.getOptimizedSetpointsOnState(optimizedState)).thenReturn(Map.of(pstRangeAction, 8.9, rangeAction, 5.6));
        when(postOptimizationResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1000.);
        when(postOptimizationResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(500.);
        when(postOptimizationResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2000.);
        when(postOptimizationResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1000.);
        when(postOptimizationResult.getFlow(cnec2, TwoSides.ONE, MEGAWATT)).thenReturn(-700.);
        when(postOptimizationResult.getFlow(cnec2, TwoSides.TWO, Unit.AMPERE)).thenReturn(300.);
        when(postOptimizationResult.getCommercialFlow(cnec2, TwoSides.ONE, MEGAWATT)).thenReturn(-600.);
        when(postOptimizationResult.getCommercialFlow(cnec2, TwoSides.TWO, Unit.AMPERE)).thenReturn(250.);
        when(postOptimizationResult.getLoopFlow(cnec2, TwoSides.ONE, MEGAWATT)).thenReturn(-100.);
        when(postOptimizationResult.getLoopFlow(cnec2, TwoSides.TWO, Unit.AMPERE)).thenReturn(50.);
        when(postOptimizationResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(500.);
        when(postOptimizationResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(250.);
        when(postOptimizationResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1500.);
        when(postOptimizationResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(750.);
        when(postOptimizationResult.getFlow(cnec1, TwoSides.ONE, MEGAWATT)).thenReturn(20.);
        when(postOptimizationResult.getFlow(cnec1, TwoSides.TWO, Unit.AMPERE)).thenReturn(10.);
        when(postOptimizationResult.getCommercialFlow(cnec1, TwoSides.ONE, MEGAWATT)).thenReturn(20.);
        when(postOptimizationResult.getCommercialFlow(cnec1, TwoSides.TWO, Unit.AMPERE)).thenReturn(10.);
        when(postOptimizationResult.getLoopFlow(cnec1, TwoSides.ONE, MEGAWATT)).thenReturn(0.);
        when(postOptimizationResult.getLoopFlow(cnec1, TwoSides.TWO, Unit.AMPERE)).thenReturn(0.);

        Set<FlowCnec> cnecs = new HashSet<>();
        cnecs.add(cnec1);
        cnecs.add(cnec2);

        output = new OneStateOnlyRaoResultImpl(optimizedState, initialResult, postOptimizationResult, cnecs);
    }

    @Test
    void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());
        when(postOptimizationResult.getSensitivityStatus(optimizedState)).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus(optimizedState));

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());
    }

    @Test
    void testPreventiveCase() {
        when(optimizedState.getInstant()).thenReturn(preventiveInstant);
        when(optimizedState.isPreventive()).thenReturn(true);

        assertSame(initialResult, output.getInitialResult());
        assertNotNull(output.getOptimizationResult(optimizedState));

        assertEquals(1000., output.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getFunctionalCost(preventiveInstant), DOUBLE_TOLERANCE);

        assertEquals(List.of(cnec1), output.getMostLimitingElements(null, 10));
        assertEquals(List.of(cnec2), output.getMostLimitingElements(preventiveInstant, 100));

        assertEquals(100., output.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(-100., output.getVirtualCost(preventiveInstant), DOUBLE_TOLERANCE);

        assertEquals(Set.of("mnec", "lf"), output.getVirtualCostNames());

        assertEquals(20., output.getVirtualCost(null, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(80., output.getVirtualCost(null, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-20., output.getVirtualCost(preventiveInstant, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-80., output.getVirtualCost(preventiveInstant, "lf"), DOUBLE_TOLERANCE);

        assertEquals(List.of(cnec2), output.getCostlyElements(null, "mnec", 10));
        assertEquals(List.of(cnec1), output.getCostlyElements(null, "lf", 100));
        assertEquals(List.of(cnec1), output.getCostlyElements(preventiveInstant, "mnec", 1000));
        assertEquals(List.of(cnec2), output.getCostlyElements(preventiveInstant, "lf", 10000));

        assertFalse(output.wasActivatedBeforeState(optimizedState, networkAction));
        assertTrue(output.isActivatedDuringState(optimizedState, networkAction));
        assertEquals(Set.of(networkAction), output.getActivatedNetworkActionsDuringState(optimizedState));

        assertTrue(output.isActivatedDuringState(optimizedState, pstRangeAction));
        assertFalse(output.isActivatedDuringState(optimizedState, rangeAction));
        assertEquals(1, output.getPreOptimizationTapOnState(optimizedState, pstRangeAction));
        assertEquals(2, output.getOptimizedTapOnState(optimizedState, pstRangeAction));
        assertEquals(6.7, output.getPreOptimizationSetPointOnState(optimizedState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(5.6, output.getPreOptimizationSetPointOnState(optimizedState, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(8.9, output.getOptimizedSetPointOnState(optimizedState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(5.6, output.getOptimizedSetPointOnState(optimizedState, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(Set.of(pstRangeAction), output.getActivatedRangeActionsDuringState(optimizedState));
        assertEquals(Map.of(pstRangeAction, 2), output.getOptimizedTapsOnState(optimizedState));
        assertEquals(Map.of(pstRangeAction, 8.9, rangeAction, 5.6), output.getOptimizedSetPointsOnState(optimizedState));

        // cnec1 initial
        assertEquals(-1000., output.getMargin(null, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(null, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(null, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(null, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getFlow(null, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, output.getFlow(null, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getCommercialFlow(null, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, output.getCommercialFlow(null, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(null, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(null, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(0, output.getPtdfZonalSum(null, cnec1, TwoSides.TWO), DOUBLE_TOLERANCE);

        // cnec2 initial
        assertEquals(-500., output.getMargin(null, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(null, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(null, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(null, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1000, output.getFlow(null, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500, output.getFlow(null, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-700, output.getCommercialFlow(null, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(450, output.getCommercialFlow(null, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getLoopFlow(null, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(50, output.getLoopFlow(null, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);

        // cnec1 afterPRA
        assertEquals(500., output.getMargin(preventiveInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250., output.getMargin(preventiveInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1500., output.getRelativeMargin(preventiveInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(750., output.getRelativeMargin(preventiveInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(20, output.getFlow(preventiveInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(10, output.getFlow(preventiveInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(20, output.getCommercialFlow(preventiveInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(10, output.getCommercialFlow(preventiveInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(preventiveInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(preventiveInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);

        // cnec2 afterPRA
        assertEquals(1000., output.getMargin(preventiveInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., output.getMargin(preventiveInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(2000., output.getRelativeMargin(preventiveInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(preventiveInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-700, output.getFlow(preventiveInstant, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(300, output.getFlow(preventiveInstant, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-600, output.getCommercialFlow(preventiveInstant, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250, output.getCommercialFlow(preventiveInstant, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-100, output.getLoopFlow(preventiveInstant, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(50, output.getLoopFlow(preventiveInstant, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);

        // cnec1 afterCRA
        assertEquals(500., output.getMargin(curativeInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250., output.getMargin(curativeInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1500., output.getRelativeMargin(curativeInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(750., output.getRelativeMargin(curativeInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(20, output.getFlow(curativeInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(10, output.getFlow(curativeInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(20, output.getCommercialFlow(curativeInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(10, output.getCommercialFlow(curativeInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(curativeInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(curativeInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);

        // cnec2 afterCRA
        assertEquals(1000., output.getMargin(curativeInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., output.getMargin(curativeInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(2000., output.getRelativeMargin(curativeInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(curativeInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-700, output.getFlow(curativeInstant, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(300, output.getFlow(curativeInstant, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-600, output.getCommercialFlow(curativeInstant, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250, output.getCommercialFlow(curativeInstant, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-100, output.getLoopFlow(curativeInstant, cnec2, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(50, output.getLoopFlow(curativeInstant, cnec2, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);

        // using another state
        State otherState = mock(State.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getOptimizationResult(otherState));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.wasActivatedBeforeState(otherState, networkAction));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.isActivatedDuringState(otherState, networkAction));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getActivatedNetworkActionsDuringState(otherState));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> output.isActivatedDuringState(otherState, rangeAction));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationTapOnState(otherState, pstRangeAction));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getOptimizedTapOnState(otherState, pstRangeAction));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationSetPointOnState(otherState, rangeAction));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getOptimizedSetPointOnState(otherState, rangeAction));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getActivatedRangeActionsDuringState(otherState));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getOptimizedTapsOnState(otherState));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getOptimizedSetPointsOnState(otherState));
        assertEquals("Trying to access perimeter result for the wrong state.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(curativeInstant, mock(FlowCnec.class), Unit.MEGAWATT));
        assertEquals("Cnec not optimized in this perimeter.", exception.getMessage());
    }

    @Test
    void testCurativeCase1() {
        when(optimizedState.getInstant()).thenReturn(curativeInstant);
        when(optimizedState.isPreventive()).thenReturn(false);

        // margins
        when(cnec1state.isPreventive()).thenReturn(true);
        when(cnec2state.isPreventive()).thenReturn(false);
        Contingency contingency = mock(Contingency.class);
        when(optimizedState.getContingency()).thenReturn(Optional.of(contingency));
        when(cnec2state.getContingency()).thenReturn(Optional.of(contingency));
        when(cnec2state.compareTo(optimizedState)).thenReturn(0);

        // cnec1
        assertEquals(-1000., output.getMargin(null, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(null, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(null, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(null, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getFlow(null, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, output.getFlow(null, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getCommercialFlow(null, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, output.getCommercialFlow(null, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(null, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(null, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getMargin(preventiveInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(preventiveInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(preventiveInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(preventiveInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getFlow(preventiveInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, output.getFlow(preventiveInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getCommercialFlow(preventiveInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, output.getCommercialFlow(preventiveInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(preventiveInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(preventiveInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getMargin(curativeInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(curativeInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(curativeInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(curativeInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getFlow(curativeInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, output.getFlow(curativeInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-300, output.getCommercialFlow(curativeInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, output.getCommercialFlow(curativeInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(curativeInstant, cnec1, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0, output.getLoopFlow(curativeInstant, cnec1, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);

        // cnec2
        assertEquals(-500., output.getMargin(null, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(null, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(null, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(null, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(preventiveInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., output.getMargin(preventiveInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(2000., output.getRelativeMargin(preventiveInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(preventiveInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(curativeInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., output.getMargin(curativeInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(2000., output.getRelativeMargin(curativeInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(curativeInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    void testCurativeCase2() {
        when(optimizedState.getInstant()).thenReturn(curativeInstant);
        when(optimizedState.isPreventive()).thenReturn(false);

        // margins
        when(cnec1state.isPreventive()).thenReturn(false);
        when(cnec2state.isPreventive()).thenReturn(false);
        Contingency contingency = mock(Contingency.class);
        when(optimizedState.getContingency()).thenReturn(Optional.of(contingency));
        when(cnec1state.getContingency()).thenReturn(Optional.of(mock(Contingency.class)));
        when(cnec2state.getContingency()).thenReturn(Optional.of(contingency));
        when(cnec2state.compareTo(optimizedState)).thenReturn(-1);

        assertEquals(-1000., output.getMargin(null, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(null, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(null, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(null, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(null, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(null, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(null, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(null, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(-1000., output.getMargin(preventiveInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(preventiveInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(preventiveInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(preventiveInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(preventiveInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(preventiveInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(preventiveInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(preventiveInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(-1000., output.getMargin(curativeInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(curativeInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(curativeInstant, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(curativeInstant, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(curativeInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(curativeInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(curativeInstant, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(curativeInstant, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    void testOptimizedStepsExecuted() {
        assertFalse(output.getOptimizationStepsExecuted().hasRunSecondPreventive());
        output.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST);
        assertTrue(output.getOptimizationStepsExecuted().hasRunSecondPreventive());
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
    }

    @Test
    void testAngleAndVoltageCnec() {
        AngleCnec angleCnec = mock(AngleCnec.class);
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        Instant optInstant = mock(Instant.class);

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optInstant, angleCnec, MEGAWATT));
        assertEquals("Angle cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optInstant, angleCnec, AMPERE));
        assertEquals("Angle cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optInstant, voltageCnec, MEGAWATT));
        assertEquals("Voltage cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optInstant, voltageCnec, AMPERE));
        assertEquals("Voltage cnecs are not computed in the rao", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> output.getVoltage(optInstant, voltageCnec, MEGAWATT));
        assertEquals("Voltage cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getVoltage(optInstant, voltageCnec, AMPERE));
        assertEquals("Voltage cnecs are not computed in the rao", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> output.getAngle(optInstant, angleCnec, MEGAWATT));
        assertEquals("Angle cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optInstant, angleCnec, AMPERE));
        assertEquals("Angle cnecs are not computed in the rao", exception.getMessage());
    }

    @Test
    void testIsSecureOnSecureCase() {
        when(optimizedState.getInstant()).thenReturn(curativeInstant);
        when(output.getFunctionalCost(curativeInstant)).thenReturn(-10.);
        assertTrue(output.isSecure(PhysicalParameter.FLOW));

        String expectedErrorMessage = "This is a flow RaoResult, flows are secure but other physical parameters' security status is unknown";
        OpenRaoException angleException = assertThrows(OpenRaoException.class, () -> output.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
        assertEquals(expectedErrorMessage, angleException.getMessage());
        OpenRaoException voltageException = assertThrows(OpenRaoException.class, () -> output.isSecure(PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
        assertEquals(expectedErrorMessage, voltageException.getMessage());
    }

    @Test
    void testIsSecureOnFailureCase() {
        when(optimizedState.getInstant()).thenReturn(curativeInstant);
        when(output.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);
        assertFalse(output.isSecure());
    }

    @Test
    void testIsSecureOnUnsecureCase() {
        when(optimizedState.getInstant()).thenReturn(curativeInstant);
        when(output.getFunctionalCost(curativeInstant)).thenReturn(10.);
        assertFalse(output.isSecure());
    }
}
