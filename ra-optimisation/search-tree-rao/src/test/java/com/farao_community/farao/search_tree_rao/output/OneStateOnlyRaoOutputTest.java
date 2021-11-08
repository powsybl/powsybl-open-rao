/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OneStateOnlyRaoOutputTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    State optimizedState;
    PrePerimeterResult initialResult;
    OptimizationResult postOptimizationResult;
    PstRangeAction pstRangeAction = mock(PstRangeAction.class);
    RangeAction rangeAction;
    NetworkAction networkAction;
    FlowCnec cnec1;
    FlowCnec cnec2;
    State cnec1state;
    State cnec2state;
    OneStateOnlyRaoOutput output;

    @Before
    public void setUp() {
        optimizedState = mock(State.class);

        initialResult = mock(PrePerimeterResult.class);
        postOptimizationResult = mock(OptimizationResult.class);
        pstRangeAction = mock(PstRangeAction.class);
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
        when(initialResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(6.7);
        when(initialResult.getOptimizedSetPoint(rangeAction)).thenReturn(5.6);
        when(initialResult.getOptimizedTap(pstRangeAction)).thenReturn(1);
        when(initialResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(initialResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 1));
        when(initialResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 6.7, rangeAction, 5.6));
        when(initialResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-1000.);
        when(initialResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(-500.);
        when(initialResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(-2000.);
        when(initialResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(-1000.);
        when(initialResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(-500.);
        when(initialResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(-250.);
        when(initialResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(-1500.);
        when(initialResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(-750.);

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
        when(postOptimizationResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(8.9);
        when(postOptimizationResult.getOptimizedSetPoint(rangeAction)).thenReturn(5.6);
        when(postOptimizationResult.getOptimizedTap(pstRangeAction)).thenReturn(2);
        when(postOptimizationResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(postOptimizationResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 2));
        when(postOptimizationResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 8.9, rangeAction, 5.6));
        when(postOptimizationResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1000.);
        when(postOptimizationResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(500.);
        when(postOptimizationResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2000.);
        when(postOptimizationResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1000.);
        when(postOptimizationResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(500.);
        when(postOptimizationResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(250.);
        when(postOptimizationResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1500.);
        when(postOptimizationResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(750.);

        Set<FlowCnec> cnecs = new HashSet<>();
        cnecs.add(cnec1);
        cnecs.add(cnec2);

        output = new OneStateOnlyRaoOutput(optimizedState, initialResult, postOptimizationResult, cnecs);
    }

    @Test
    public void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.FALLBACK);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.FALLBACK);
        assertEquals(ComputationStatus.FALLBACK, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.FALLBACK);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(postOptimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.FALLBACK);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());
    }

    @Test
    public void testPreventiveCase() {
        when(optimizedState.getInstant()).thenReturn(Instant.PREVENTIVE);
        when(optimizedState.isPreventive()).thenReturn(true);

        assertSame(initialResult, output.getInitialResult());
        assertNotNull(output.getPostPreventivePerimeterResult());
        assertNotNull(output.getPerimeterResult(OptimizationState.INITIAL, optimizedState));

        assertEquals(1000., output.getFunctionalCost(OptimizationState.INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getFunctionalCost(OptimizationState.AFTER_PRA), DOUBLE_TOLERANCE);

        assertEquals(List.of(cnec1), output.getMostLimitingElements(OptimizationState.INITIAL, 10));
        assertEquals(List.of(cnec2), output.getMostLimitingElements(OptimizationState.AFTER_PRA, 100));

        assertEquals(100., output.getVirtualCost(OptimizationState.INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-100., output.getVirtualCost(OptimizationState.AFTER_PRA), DOUBLE_TOLERANCE);

        assertEquals(Set.of("mnec", "lf"), output.getVirtualCostNames());

        assertEquals(20., output.getVirtualCost(OptimizationState.INITIAL, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(80., output.getVirtualCost(OptimizationState.INITIAL, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-20., output.getVirtualCost(OptimizationState.AFTER_PRA, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-80., output.getVirtualCost(OptimizationState.AFTER_PRA, "lf"), DOUBLE_TOLERANCE);

        assertEquals(List.of(cnec2), output.getCostlyElements(OptimizationState.INITIAL, "mnec", 10));
        assertEquals(List.of(cnec1), output.getCostlyElements(OptimizationState.INITIAL, "lf", 100));
        assertEquals(List.of(cnec1), output.getCostlyElements(OptimizationState.AFTER_PRA, "mnec", 1000));
        assertEquals(List.of(cnec2), output.getCostlyElements(OptimizationState.AFTER_PRA, "lf", 10000));

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

        // margins
        assertEquals(-1000., output.getMargin(OptimizationState.INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.INITIAL, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(OptimizationState.INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(OptimizationState.INITIAL, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.INITIAL, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(OptimizationState.INITIAL, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(OptimizationState.INITIAL, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(OptimizationState.INITIAL, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(500., output.getMargin(OptimizationState.AFTER_PRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250., output.getMargin(OptimizationState.AFTER_PRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1500., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(750., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_PRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., output.getMargin(OptimizationState.AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(2000., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(500., output.getMargin(OptimizationState.AFTER_CRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(250., output.getMargin(OptimizationState.AFTER_CRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1500., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(750., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_CRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., output.getMargin(OptimizationState.AFTER_CRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(2000., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        // using another state
        State otherState = mock(State.class);
        assertNull(output.getPerimeterResult(OptimizationState.INITIAL, otherState));
        assertThrows(FaraoException.class, () -> output.wasActivatedBeforeState(otherState, networkAction));
        assertThrows(FaraoException.class, () -> output.isActivatedDuringState(otherState, networkAction));
        assertThrows(FaraoException.class, () -> output.getActivatedNetworkActionsDuringState(otherState));

        assertThrows(FaraoException.class, () -> output.isActivatedDuringState(otherState, rangeAction));
        assertThrows(FaraoException.class, () -> output.getPreOptimizationTapOnState(otherState, pstRangeAction));
        assertThrows(FaraoException.class, () -> output.getOptimizedTapOnState(otherState, pstRangeAction));
        assertThrows(FaraoException.class, () -> output.getPreOptimizationSetPointOnState(otherState, rangeAction));
        assertThrows(FaraoException.class, () -> output.getOptimizedSetPointOnState(otherState, rangeAction));
        assertThrows(FaraoException.class, () -> output.getActivatedRangeActionsDuringState(otherState));
        assertThrows(FaraoException.class, () -> output.getOptimizedTapsOnState(otherState));
        assertThrows(FaraoException.class, () -> output.getOptimizedSetPointsOnState(otherState));
        assertThrows("Cnec not optimized in this perimeter.", FaraoException.class, () -> output.getMargin(OptimizationState.AFTER_CRA, mock(FlowCnec.class), Unit.MEGAWATT));
    }

    @Test
    public void testCurativeCase1() {
        when(optimizedState.getInstant()).thenReturn(Instant.CURATIVE);
        when(optimizedState.isPreventive()).thenReturn(false);

        assertThrows(FaraoException.class, output::getPostPreventivePerimeterResult);

        // margins
        when(cnec1state.isPreventive()).thenReturn(true);
        when(cnec2state.isPreventive()).thenReturn(false);
        Contingency contingency = mock(Contingency.class);
        when(optimizedState.getContingency()).thenReturn(Optional.of(contingency));
        when(cnec2state.getContingency()).thenReturn(Optional.of(contingency));
        when(cnec2state.compareTo(optimizedState)).thenReturn(0);

        assertEquals(-1000., output.getMargin(OptimizationState.INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.INITIAL, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(OptimizationState.INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(OptimizationState.INITIAL, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.INITIAL, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(OptimizationState.INITIAL, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(OptimizationState.INITIAL, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(OptimizationState.INITIAL, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(-1000., output.getMargin(OptimizationState.AFTER_PRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.AFTER_PRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_PRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., output.getMargin(OptimizationState.AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(2000., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(-1000., output.getMargin(OptimizationState.AFTER_CRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.AFTER_CRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getMargin(OptimizationState.AFTER_CRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., output.getMargin(OptimizationState.AFTER_CRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(2000., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCurativeCase2() {
        when(optimizedState.getInstant()).thenReturn(Instant.CURATIVE);
        when(optimizedState.isPreventive()).thenReturn(false);

        // margins
        when(cnec1state.isPreventive()).thenReturn(false);
        when(cnec2state.isPreventive()).thenReturn(false);
        Contingency contingency = mock(Contingency.class);
        when(optimizedState.getContingency()).thenReturn(Optional.of(contingency));
        when(cnec1state.getContingency()).thenReturn(Optional.of(mock(Contingency.class)));
        when(cnec2state.getContingency()).thenReturn(Optional.of(contingency));
        when(cnec2state.compareTo(optimizedState)).thenReturn(-1);

        assertEquals(-1000., output.getMargin(OptimizationState.INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.INITIAL, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(OptimizationState.INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(OptimizationState.INITIAL, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.INITIAL, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(OptimizationState.INITIAL, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(OptimizationState.INITIAL, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(OptimizationState.INITIAL, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(-1000., output.getMargin(OptimizationState.AFTER_PRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.AFTER_PRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.AFTER_PRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(OptimizationState.AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(OptimizationState.AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(-1000., output.getMargin(OptimizationState.AFTER_CRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.AFTER_CRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-2000., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-500., output.getMargin(OptimizationState.AFTER_CRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-250., output.getMargin(OptimizationState.AFTER_CRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-1500., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec2, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-750., output.getRelativeMargin(OptimizationState.AFTER_CRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
    }
}
