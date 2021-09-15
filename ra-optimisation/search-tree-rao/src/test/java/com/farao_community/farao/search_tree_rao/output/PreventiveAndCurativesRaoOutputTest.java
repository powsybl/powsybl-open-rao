/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.state_tree.BasecaseScenario;
import com.farao_community.farao.search_tree_rao.state_tree.ContingencyScenario;
import com.farao_community.farao.search_tree_rao.state_tree.StateTree;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PreventiveAndCurativesRaoOutputTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    PrePerimeterResult initialResult;
    PerimeterResult postPrevResult;
    PrePerimeterResult preCurativeResult;
    PstRangeAction pstRangeAction = mock(PstRangeAction.class);
    RangeAction rangeAction;
    NetworkAction networkAction;
    FlowCnec cnec1;
    FlowCnec cnec1auto;
    FlowCnec cnec2;
    FlowCnec cnec3;
    FlowCnec cnec4;
    State preventiveState;
    State autoState1;
    State curativeState1;
    State curativeState2;
    State curativeState3;
    PreventiveAndCurativesRaoOutput output;
    OptimizationResult autoResult1;
    OptimizationResult curativeResult1;
    OptimizationResult curativeResult2;

    @Before
    public void setUp() {
        cnec1 = mock(FlowCnec.class);
        cnec1auto = mock(FlowCnec.class);
        cnec2 = mock(FlowCnec.class);
        cnec3 = mock(FlowCnec.class);
        cnec4 = mock(FlowCnec.class);
        preventiveState = mock(State.class);
        autoState1 = mock(State.class);
        curativeState1 = mock(State.class);
        curativeState2 = mock(State.class);
        curativeState3 = mock(State.class);
        when(cnec1.getState()).thenReturn(curativeState1);
        when(cnec1auto.getState()).thenReturn(autoState1);
        when(cnec2.getState()).thenReturn(curativeState2);
        when(preventiveState.getInstant()).thenReturn(Instant.PREVENTIVE);
        Contingency contingency1 = mock(Contingency.class);
        Contingency contingency2 = mock(Contingency.class);
        when(autoState1.getInstant()).thenReturn(Instant.AUTO);
        when(autoState1.getContingency()).thenReturn(Optional.of(contingency1));
        when(curativeState1.getInstant()).thenReturn(Instant.CURATIVE);
        when(curativeState1.getContingency()).thenReturn(Optional.of(contingency1));
        when(curativeState2.getInstant()).thenReturn(Instant.CURATIVE);
        when(curativeState2.getContingency()).thenReturn(Optional.of(contingency2));
        when(curativeState3.getInstant()).thenReturn(Instant.CURATIVE);

        initialResult = mock(PrePerimeterResult.class);
        postPrevResult = mock(PerimeterResult.class);
        preCurativeResult = mock(PrePerimeterResult.class);

        autoResult1 = mock(OptimizationResult.class);
        curativeResult1 = mock(OptimizationResult.class);
        curativeResult2 = mock(OptimizationResult.class);

        pstRangeAction = mock(PstRangeAction.class);
        rangeAction = mock(RangeAction.class);
        networkAction = mock(NetworkAction.class);

        when(initialResult.getFunctionalCost()).thenReturn(1000.);
        when(initialResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1));
        when(initialResult.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));
        mockVirtualCosts(initialResult, 100, 20, List.of(cnec2), 80, List.of(cnec1));
        when(initialResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(6.7);
        when(initialResult.getOptimizedSetPoint(rangeAction)).thenReturn(5.6);
        when(initialResult.getOptimizedTap(pstRangeAction)).thenReturn(1);
        when(initialResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(initialResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 1));
        when(initialResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 6.7, rangeAction, 5.6));
        mockCnecResults(initialResult, cnec1, -1000, -500, -2000, -1000);
        mockCnecResults(initialResult, cnec2, -500, -250, -1500, -750);

        when(postPrevResult.getFunctionalCost()).thenReturn(-1020.);
        when(postPrevResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        mockVirtualCosts(postPrevResult, -120, -40, List.of(cnec1), -100, List.of(cnec2));
        when(postPrevResult.isActivated(networkAction)).thenReturn(true);
        when(postPrevResult.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(postPrevResult.getActivatedRangeActions()).thenReturn(Set.of(rangeAction));
        when(postPrevResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(28.9);
        when(postPrevResult.getOptimizedSetPoint(rangeAction)).thenReturn(25.6);
        when(postPrevResult.getOptimizedTap(pstRangeAction)).thenReturn(22);
        when(postPrevResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(postPrevResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 22));
        when(postPrevResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        mockCnecResults(postPrevResult, cnec1, 520, 270, 1520, 770);
        mockCnecResults(postPrevResult, cnec2, 1020, 520, 2020, 1020);

        when(autoResult1.getFunctionalCost()).thenReturn(-1025.);
        when(autoResult1.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        mockVirtualCosts(autoResult1, -125, -45, List.of(cnec1auto), -105, List.of(cnec1));
        when(autoResult1.isActivated(networkAction)).thenReturn(false);
        when(autoResult1.getActivatedNetworkActions()).thenReturn(Set.of());
        when(autoResult1.getOptimizedSetPoint(pstRangeAction)).thenReturn(28.9);
        when(autoResult1.getOptimizedSetPoint(rangeAction)).thenReturn(25.6);
        when(autoResult1.getOptimizedTap(pstRangeAction)).thenReturn(22);
        when(autoResult1.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(autoResult1.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 22));
        when(autoResult1.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        mockCnecResults(autoResult1, cnec1auto, 523, 273, 1523, 773);
        mockCnecResults(autoResult1, cnec1, 525, 275, 1525, 775);

        when(curativeResult1.getFunctionalCost()).thenReturn(-1030.);
        when(curativeResult1.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        mockVirtualCosts(curativeResult1, -130, -50, List.of(cnec1), -110, List.of(cnec2));
        when(curativeResult1.isActivated(networkAction)).thenReturn(false);
        when(curativeResult1.getActivatedNetworkActions()).thenReturn(Set.of());
        when(curativeResult1.getOptimizedSetPoint(pstRangeAction)).thenReturn(28.9);
        when(curativeResult1.getOptimizedSetPoint(rangeAction)).thenReturn(25.6);
        when(curativeResult1.getOptimizedTap(pstRangeAction)).thenReturn(22);
        when(curativeResult1.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(curativeResult1.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 22));
        when(curativeResult1.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        mockCnecResults(curativeResult1, cnec1, 530, 280, 1530, 780);

        when(curativeResult2.getFunctionalCost()).thenReturn(-1040.);
        when(curativeResult2.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        mockVirtualCosts(curativeResult2, -140, -60, List.of(cnec1), -120, List.of(cnec2));
        when(curativeResult2.isActivated(networkAction)).thenReturn(false);
        when(curativeResult2.getActivatedNetworkActions()).thenReturn(Set.of());
        when(curativeResult2.getOptimizedSetPoint(pstRangeAction)).thenReturn(48.9);
        when(curativeResult2.getOptimizedSetPoint(rangeAction)).thenReturn(25.6);
        when(curativeResult2.getOptimizedTap(pstRangeAction)).thenReturn(42);
        when(curativeResult2.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(curativeResult2.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 42));
        when(curativeResult2.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        mockCnecResults(curativeResult2, cnec2, 1040, 540, 2040, 1040);

        when(preCurativeResult.getFunctionalCost()).thenReturn(-1050.);
        when(preCurativeResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec3));
        mockVirtualCosts(preCurativeResult, -150, -70, List.of(cnec3, cnec2), -130, List.of(cnec1, cnec4));
        when(preCurativeResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(58.9);
        when(preCurativeResult.getOptimizedSetPoint(rangeAction)).thenReturn(55.6);
        when(preCurativeResult.getOptimizedTap(pstRangeAction)).thenReturn(52);
        when(preCurativeResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(preCurativeResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 52));
        when(preCurativeResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 58.9, rangeAction, 55.6));
        mockCnecResults(preCurativeResult, cnec2, 1050, 550, 2050, 1050);
        mockCnecResults(preCurativeResult, cnec1, 5050, 300, 1550, 800);

        StateTree stateTree = mock(StateTree.class);
        BasecaseScenario basecaseScenario = new BasecaseScenario(preventiveState, null);
        Set<ContingencyScenario> contingencyScenarios = Set.of(
            new ContingencyScenario(contingency1, autoState1, curativeState1),
            new ContingencyScenario(contingency2, null, curativeState2)
        );
        when(stateTree.getBasecaseScenario()).thenReturn(basecaseScenario);
        when(stateTree.getContingencyScenarios()).thenReturn(contingencyScenarios);

        output = new PreventiveAndCurativesRaoOutput(
            stateTree,
            initialResult,
            postPrevResult,
            preCurativeResult,
            Map.of(autoState1, autoResult1, curativeState1, curativeResult1, curativeState2, curativeResult2));
    }

    private void mockCnecResults(FlowResult flowResult, FlowCnec cnec, double marginMw, double marginA, double relMarginMw, double relMarginA) {
        when(flowResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(marginMw);
        when(flowResult.getMargin(cnec, Unit.AMPERE)).thenReturn(marginA);
        when(flowResult.getRelativeMargin(cnec, Unit.MEGAWATT)).thenReturn(relMarginMw);
        when(flowResult.getRelativeMargin(cnec, Unit.AMPERE)).thenReturn(relMarginA);
        when(flowResult.getPtdfZonalSum(cnec)).thenReturn(1.);
    }

    private void mockVirtualCosts(ObjectiveFunctionResult objectiveFunctionResult, double virtualCost, double mnecCost, List<FlowCnec> mnecCostlyElements, double lfCost, List<FlowCnec> lfCostlyElements) {
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(virtualCost);
        when(objectiveFunctionResult.getVirtualCost("mnec")).thenReturn(mnecCost);
        when(objectiveFunctionResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(mnecCostlyElements);
        when(objectiveFunctionResult.getVirtualCost("lf")).thenReturn(lfCost);
        when(objectiveFunctionResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(lfCostlyElements);
    }

    @Test
    public void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult1.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult2.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(postPrevResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());

        when(postPrevResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult2.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());
    }

    @Test
    public void testGetFunctionalCost() {
        assertEquals(1000., output.getFunctionalCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-1020., output.getFunctionalCost(AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(-1020., output.getFunctionalCost(AFTER_CRA), DOUBLE_TOLERANCE);

        when(postPrevResult.getFunctionalCost()).thenReturn(-2020.);
        assertEquals(-1025., output.getFunctionalCost(AFTER_CRA), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMostLimitingElements() {
        assertNull(output.getMostLimitingElements(INITIAL, 5));
        assertNull(output.getMostLimitingElements(AFTER_PRA, 15));
        assertNull(output.getMostLimitingElements(AFTER_ARA, 20));
        assertNull(output.getMostLimitingElements(AFTER_CRA, 445));
    }

    @Test
    public void testGetVirtualCost() {
        assertEquals(100., output.getVirtualCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(-120., output.getVirtualCost(AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(-125., output.getVirtualCost(AFTER_ARA), DOUBLE_TOLERANCE);
        assertEquals(-270., output.getVirtualCost(AFTER_CRA), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetVirtualCostNames() {
        assertEquals(Set.of("mnec", "lf"), output.getVirtualCostNames());
    }

    @Test
    public void testGetVirtualCostByName() {
        assertEquals(20., output.getVirtualCost(INITIAL, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(80., output.getVirtualCost(INITIAL, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-40., output.getVirtualCost(AFTER_PRA, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-100., output.getVirtualCost(AFTER_PRA, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-45., output.getVirtualCost(AFTER_ARA, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-105., output.getVirtualCost(AFTER_ARA, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-110., output.getVirtualCost(AFTER_CRA, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-230., output.getVirtualCost(AFTER_CRA, "lf"), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCostlyElements() {
        assertNull(output.getCostlyElements(INITIAL, "mnec", 5));
        assertNull(output.getCostlyElements(INITIAL, "lf", 15));

        assertNull(output.getCostlyElements(AFTER_PRA, "mnec", 5));
        assertNull(output.getCostlyElements(AFTER_PRA, "lf", 15));

        assertNull(output.getCostlyElements(AFTER_ARA, "mnec", 5));
        assertNull(output.getCostlyElements(AFTER_ARA, "lf", 15));

        assertNull(output.getCostlyElements(AFTER_CRA, "mnec", 5));
        assertNull(output.getCostlyElements(AFTER_CRA, "lf", 15));
    }

    @Test
    public void testWasNetworkActionActivatedBeforeState() {
        assertFalse(output.wasActivatedBeforeState(preventiveState, networkAction));
        assertTrue(output.wasActivatedBeforeState(autoState1, networkAction));
        assertTrue(output.wasActivatedBeforeState(curativeState1, networkAction));
        assertTrue(output.wasActivatedBeforeState(curativeState2, networkAction));
    }

    @Test
    public void testIsNetworkActionActivatedDuringState() {
        assertTrue(output.isActivatedDuringState(preventiveState, networkAction));
        assertFalse(output.isActivatedDuringState(autoState1, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState1, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState2, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState3, networkAction));
    }

    @Test
    public void testGetActivatedNetworkActionsDuringState() {
        assertEquals(Set.of(networkAction), output.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(autoState1));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState1));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState2));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState3));

        when(curativeResult2.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        assertEquals(Set.of(networkAction), output.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(autoState1));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState1));
        assertEquals(Set.of(networkAction), output.getActivatedNetworkActionsDuringState(curativeState2));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState3));

        when(curativeResult2.getActivatedNetworkActions()).thenReturn(Set.of());
        when(autoResult1.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        assertEquals(Set.of(networkAction), output.getActivatedNetworkActionsDuringState(autoState1));

        when(postPrevResult.getActivatedNetworkActions()).thenReturn(Set.of());
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState3));
    }

    @Test
    public void testIsRangeActionActivatedDuringState() {
        assertTrue(output.isActivatedDuringState(preventiveState, rangeAction));
        assertTrue(output.isActivatedDuringState(autoState1, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState1, rangeAction));
        assertTrue(output.isActivatedDuringState(curativeState2, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState3, rangeAction));

        when(curativeResult2.getOptimizedSetPoint(rangeAction)).thenReturn(55.6);
        assertTrue(output.isActivatedDuringState(preventiveState, rangeAction));
        assertTrue(output.isActivatedDuringState(autoState1, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState1, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState2, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState3, rangeAction));
    }

    @Test
    public void testGetPreOptimizationTapOnState() {
        assertEquals(1, output.getPreOptimizationTapOnState(preventiveState, pstRangeAction));
        assertEquals(22, output.getPreOptimizationTapOnState(autoState1, pstRangeAction));
        assertEquals(22, output.getPreOptimizationTapOnState(curativeState1, pstRangeAction));
        assertEquals(22, output.getPreOptimizationTapOnState(curativeState2, pstRangeAction));
        assertThrows(FaraoException.class, () -> output.getPreOptimizationTapOnState(curativeState3, pstRangeAction));
    }

    @Test
    public void testGetOptimizedTapOnState() {
        when(postPrevResult.getOptimizedTap(pstRangeAction)).thenReturn(202);
        assertEquals(202, output.getOptimizedTapOnState(preventiveState, pstRangeAction));
        assertEquals(22, output.getOptimizedTapOnState(autoState1, pstRangeAction));
        assertEquals(22, output.getOptimizedTapOnState(curativeState1, pstRangeAction));
        assertEquals(42, output.getOptimizedTapOnState(curativeState2, pstRangeAction));
        assertEquals(202, output.getOptimizedTapOnState(curativeState3, pstRangeAction));
    }

    @Test
    public void testGetPreOptimizationSetPointOnState() {
        assertEquals(6.7, output.getPreOptimizationSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getPreOptimizationSetPointOnState(autoState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getPreOptimizationSetPointOnState(curativeState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getPreOptimizationSetPointOnState(curativeState2, pstRangeAction), DOUBLE_TOLERANCE);
        assertThrows(FaraoException.class, () -> output.getPreOptimizationSetPointOnState(curativeState3, pstRangeAction));
    }

    @Test
    public void testGetOptimizedSetPointOnState() {
        when(postPrevResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(567.);
        assertEquals(567, output.getOptimizedSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getOptimizedSetPointOnState(autoState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getOptimizedSetPointOnState(curativeState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(48.9, output.getOptimizedSetPointOnState(curativeState2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(567, output.getOptimizedSetPointOnState(curativeState3, pstRangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetActivatedRangeActionsDuringState() {
        assertEquals(Set.of(rangeAction), output.getActivatedRangeActionsDuringState(preventiveState));
        assertEquals(Set.of(pstRangeAction, rangeAction), output.getActivatedRangeActionsDuringState(autoState1));
        assertEquals(Set.of(), output.getActivatedRangeActionsDuringState(curativeState1));
        assertEquals(Set.of(pstRangeAction, rangeAction), output.getActivatedRangeActionsDuringState(curativeState2));
        assertEquals(Set.of(), output.getActivatedRangeActionsDuringState(curativeState3));

        when(postPrevResult.getActivatedRangeActions()).thenReturn(Set.of(pstRangeAction));
        assertEquals(Set.of(pstRangeAction), output.getActivatedRangeActionsDuringState(preventiveState));
    }

    @Test
    public void testGetOptimizedTapsOnState() {
        when(curativeResult1.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 333));
        when(curativeResult1.getOptimizedTap(pstRangeAction)).thenReturn(333);
        // with next line, pstRangeAction should be detected as activated in state1
        when(curativeResult1.getOptimizedSetPoint(pstRangeAction)).thenReturn(3330.);

        when(curativeResult2.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 444));
        when(curativeResult2.getOptimizedTap(pstRangeAction)).thenReturn(444);
        // with next line, pstRangeAction should not be detected as activated in state2
        when(curativeResult2.getOptimizedSetPoint(pstRangeAction)).thenReturn(18.9);

        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(autoState1));
        assertEquals(Map.of(pstRangeAction, 333), output.getOptimizedTapsOnState(curativeState1));
        assertEquals(Map.of(pstRangeAction, 444), output.getOptimizedTapsOnState(curativeState2));
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(curativeState3));
    }

    @Test
    public void testGetOptimizedSetPointsOnState() {
        when(postPrevResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 222., rangeAction, 111.));
        when(autoResult1.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 222., rangeAction, 111.));

        when(curativeResult1.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 333.));
        // with next line, pstRangeAction should be detected as activated in state1
        when(curativeResult1.getOptimizedSetPoint(pstRangeAction)).thenReturn(333.);

        when(curativeResult2.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 444.));
        // with next line, pstRangeAction should not be detected as activated in state2
        when(curativeResult2.getOptimizedSetPoint(pstRangeAction)).thenReturn(18.9);

        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(autoState1));
        assertEquals(Map.of(pstRangeAction, 333.), output.getOptimizedSetPointsOnState(curativeState1));
        assertEquals(Map.of(pstRangeAction, 444.), output.getOptimizedSetPointsOnState(curativeState2));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(curativeState3));
    }

    @Test
    public void testGetFlow() {
        when(initialResult.getFlow(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(postPrevResult.getFlow(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(postPrevResult.getFlow(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getFlow(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getFlow(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getFlow(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMargin() {
        when(initialResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(postPrevResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(postPrevResult.getMargin(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getMargin(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getMargin(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getMargin(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetRelativeMargin() {
        when(initialResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(postPrevResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(postPrevResult.getRelativeMargin(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getRelativeMargin(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getRelativeMargin(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getRelativeMargin(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCommercialFlow() {
        when(initialResult.getCommercialFlow(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(postPrevResult.getCommercialFlow(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(curativeResult1.getCommercialFlow(cnec3, Unit.MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState1);
        assertEquals(10., output.getCommercialFlow(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getCommercialFlow(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getCommercialFlow(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLoopFlow() {
        when(initialResult.getLoopFlow(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(postPrevResult.getLoopFlow(cnec2, Unit.AMPERE)).thenReturn(20.);
        when(curativeResult2.getFlow(cnec3, Unit.MEGAWATT)).thenReturn(90.);
        when(curativeResult2.getCommercialFlow(cnec3, Unit.MEGAWATT)).thenReturn(60.);
        when(cnec3.getState()).thenReturn(curativeState2);
        assertEquals(10., output.getLoopFlow(INITIAL, cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getLoopFlow(AFTER_PRA, cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getLoopFlow(AFTER_CRA, cnec3, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPtdfZonalSum() {
        when(initialResult.getPtdfZonalSum(cnec1)).thenReturn(10.);
        when(postPrevResult.getPtdfZonalSum(cnec2)).thenReturn(20.);
        when(autoResult1.getPtdfZonalSum(cnec1auto)).thenReturn(25.);
        when(curativeResult2.getPtdfZonalSum(cnec3)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState2);
        assertEquals(10., output.getPtdfZonalSum(INITIAL, cnec1), DOUBLE_TOLERANCE);
        assertEquals(20., output.getPtdfZonalSum(AFTER_PRA, cnec2), DOUBLE_TOLERANCE);
        assertEquals(30., output.getPtdfZonalSum(AFTER_CRA, cnec3), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPerimeter() {
        State outageState = mock(State.class);
        when(outageState.getInstant()).thenReturn(Instant.OUTAGE);

        when(initialResult.getPtdfZonalSum(cnec1)).thenReturn(1.);
        when(postPrevResult.getPtdfZonalSum(cnec1)).thenReturn(2.);
        when(autoResult1.getPtdfZonalSum(cnec1)).thenReturn(3.);
        when(curativeResult1.getPtdfZonalSum(cnec1)).thenReturn(4.);
        when(curativeResult2.getPtdfZonalSum(cnec1)).thenReturn(5.);

        PerimeterResult perimeterResult;

        // INITIAL
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(INITIAL, preventiveState));
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(INITIAL, outageState));
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(INITIAL, autoState1));
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(INITIAL, curativeState1));
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(INITIAL, curativeState2));

        // AFTER_PRA
        perimeterResult = output.getPerimeterResult(AFTER_PRA, preventiveState);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(AFTER_PRA, outageState);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(AFTER_PRA, autoState1);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(AFTER_PRA, curativeState1);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(AFTER_PRA, curativeState2);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);

        // AFTER_ARA
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(AFTER_ARA, preventiveState));
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(AFTER_ARA, outageState));
        perimeterResult = output.getPerimeterResult(AFTER_ARA, autoState1);
        assertEquals(3., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(AFTER_ARA, curativeState1);
        assertEquals(3., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(AFTER_ARA, curativeState2);
        assertEquals(5., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);

        // AFTER_CRA
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(AFTER_CRA, preventiveState));
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(AFTER_CRA, outageState));
        assertThrows(FaraoException.class, () -> output.getPerimeterResult(AFTER_CRA, autoState1));
        perimeterResult = output.getPerimeterResult(AFTER_CRA, curativeState1);
        assertEquals(4., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(AFTER_CRA, curativeState2);
        assertEquals(5., perimeterResult.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
    }
}
