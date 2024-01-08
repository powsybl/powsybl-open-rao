/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.result.impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.*;
import com.powsybl.open_rao.data.crac_api.cnec.AngleCnec;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnec;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.data.rao_result_api.OptimizationStepsExecuted;
import com.powsybl.open_rao.search_tree_rao.result.api.FlowResult;
import com.powsybl.open_rao.search_tree_rao.result.api.ObjectiveFunctionResult;
import com.powsybl.open_rao.search_tree_rao.result.api.OptimizationResult;
import com.powsybl.open_rao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.open_rao.search_tree_rao.result.api.PerimeterResult;
import com.powsybl.open_rao.search_tree_rao.castor.algorithm.BasecaseScenario;
import com.powsybl.open_rao.search_tree_rao.castor.algorithm.ContingencyScenario;
import com.powsybl.open_rao.search_tree_rao.castor.algorithm.StateTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.powsybl.open_rao.data.crac_api.cnec.Side.LEFT;
import static com.powsybl.open_rao.data.crac_api.cnec.Side.RIGHT;
import static com.powsybl.open_rao.commons.Unit.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PreventiveAndCurativesRaoResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;
    private PrePerimeterResult initialResult;
    private PerimeterResult postPrevResult;
    private PrePerimeterResult preCurativeResult;
    private PstRangeAction pstRangeAction = mock(PstRangeAction.class);
    private RangeAction<?> rangeAction;
    private NetworkAction networkAction;
    private FlowCnec cnec1;
    private FlowCnec cnec1auto;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec cnec4;
    private State preventiveState;
    private State outageState;
    private State autoState1;
    private State curativeState1;
    private State curativeState2;
    private State curativeState3;
    private PreventiveAndCurativesRaoResultImpl output;
    private OptimizationResult autoResult1;
    private OptimizationResult curativeResult1;
    private OptimizationResult curativeResult2;
    private StateTree stateTree;

    @BeforeEach
    public void setUp() {
        crac = mock(Crac.class);
        preventiveInstant = mock(Instant.class);
        when(preventiveInstant.isPreventive()).thenReturn(true);
        when(preventiveInstant.getOrder()).thenReturn(0);
        when(preventiveInstant.toString()).thenReturn(PREVENTIVE_INSTANT_ID);
        when(preventiveInstant.getId()).thenReturn(PREVENTIVE_INSTANT_ID);
        outageInstant = mock(Instant.class);
        when(outageInstant.isOutage()).thenReturn(true);
        when(outageInstant.getOrder()).thenReturn(1);
        when(outageInstant.toString()).thenReturn(OUTAGE_INSTANT_ID);
        when(outageInstant.getId()).thenReturn(OUTAGE_INSTANT_ID);
        autoInstant = mock(Instant.class);
        when(autoInstant.isAuto()).thenReturn(true);
        when(autoInstant.getOrder()).thenReturn(2);
        when(autoInstant.toString()).thenReturn(AUTO_INSTANT_ID);
        when(autoInstant.getId()).thenReturn(AUTO_INSTANT_ID);
        when(preventiveInstant.comesBefore(autoInstant)).thenReturn(true);
        when(outageInstant.comesBefore(autoInstant)).thenReturn(true);
        curativeInstant = mock(Instant.class);
        when(curativeInstant.isCurative()).thenReturn(true);
        when(curativeInstant.getOrder()).thenReturn(3);
        when(curativeInstant.toString()).thenReturn(CURATIVE_INSTANT_ID);
        when(preventiveInstant.comesBefore(curativeInstant)).thenReturn(true);
        when(outageInstant.comesBefore(curativeInstant)).thenReturn(true);
        when(autoInstant.comesBefore(curativeInstant)).thenReturn(true);
        cnec1 = mock(FlowCnec.class);
        cnec1auto = mock(FlowCnec.class);
        cnec2 = mock(FlowCnec.class);
        cnec3 = mock(FlowCnec.class);
        cnec4 = mock(FlowCnec.class);
        preventiveState = mock(State.class);
        outageState = mock(State.class);
        autoState1 = mock(State.class);
        curativeState1 = mock(State.class);
        curativeState2 = mock(State.class);
        curativeState3 = mock(State.class);
        when(cnec1.getState()).thenReturn(curativeState1);
        when(cnec1auto.getState()).thenReturn(autoState1);
        when(cnec2.getState()).thenReturn(curativeState2);
        when(cnec4.getState()).thenReturn(outageState);
        when(preventiveState.getInstant()).thenReturn(preventiveInstant);
        Contingency contingency1 = mock(Contingency.class);
        Contingency contingency2 = mock(Contingency.class);
        when(outageState.getInstant()).thenReturn(outageInstant);
        when(outageState.getContingency()).thenReturn(Optional.of(contingency1));
        when(autoState1.getInstant()).thenReturn(autoInstant);
        when(autoState1.getContingency()).thenReturn(Optional.of(contingency1));
        when(curativeState1.getInstant()).thenReturn(curativeInstant);
        when(curativeState1.getContingency()).thenReturn(Optional.of(contingency1));
        when(curativeState2.getInstant()).thenReturn(curativeInstant);
        when(curativeState2.getContingency()).thenReturn(Optional.of(contingency2));
        when(curativeState3.getInstant()).thenReturn(curativeInstant);

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
        when(initialResult.getSetpoint(pstRangeAction)).thenReturn(6.7);
        when(initialResult.getSetpoint(rangeAction)).thenReturn(5.6);
        when(initialResult.getTap(pstRangeAction)).thenReturn(1);
        when(initialResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        mockCnecResults(initialResult, cnec1, -1000, -500, -2000, -1000);
        mockCnecResults(initialResult, cnec2, -500, -250, -1500, -750);

        when(postPrevResult.getFunctionalCost()).thenReturn(-1020.);
        when(postPrevResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        mockVirtualCosts(postPrevResult, -120, -40, List.of(cnec1), -100, List.of(cnec2));
        when(postPrevResult.isActivated(networkAction)).thenReturn(true);
        when(postPrevResult.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(postPrevResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(rangeAction));
        when(postPrevResult.getOptimizedSetpoint(eq(pstRangeAction), any())).thenReturn(28.9);
        when(postPrevResult.getOptimizedSetpoint(eq(rangeAction), any())).thenReturn(25.6);
        when(postPrevResult.getOptimizedTap(eq(pstRangeAction), any())).thenReturn(22);
        when(postPrevResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(postPrevResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(postPrevResult.getOptimizedTapsOnState(any())).thenReturn(Map.of(pstRangeAction, 22));
        when(postPrevResult.getOptimizedSetpointsOnState(any())).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        mockCnecResults(postPrevResult, cnec1, 520, 270, 1520, 770);
        mockCnecResults(postPrevResult, cnec2, 1020, 520, 2020, 1020);

        when(autoResult1.getFunctionalCost()).thenReturn(-1025.);
        when(autoResult1.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        mockVirtualCosts(autoResult1, -125, -45, List.of(cnec1auto), -105, List.of(cnec1));
        when(autoResult1.isActivated(networkAction)).thenReturn(false);
        when(autoResult1.getActivatedNetworkActions()).thenReturn(Set.of());
        when(autoResult1.getOptimizedSetpoint(pstRangeAction, autoState1)).thenReturn(28.9);
        when(autoResult1.getOptimizedSetpoint(rangeAction, autoState1)).thenReturn(25.6);
        when(autoResult1.getOptimizedTap(pstRangeAction, autoState1)).thenReturn(22);
        when(autoResult1.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(autoResult1.getActivatedRangeActions(autoState1)).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(autoResult1.getOptimizedTapsOnState(autoState1)).thenReturn(Map.of(pstRangeAction, 22));
        when(autoResult1.getOptimizedSetpointsOnState(autoState1)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        mockCnecResults(autoResult1, cnec1auto, 523, 273, 1523, 773);
        mockCnecResults(autoResult1, cnec1, 525, 275, 1525, 775);

        when(curativeResult1.getFunctionalCost()).thenReturn(-1030.);
        when(curativeResult1.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        mockVirtualCosts(curativeResult1, -130, -50, List.of(cnec1), -110, List.of(cnec2));
        when(curativeResult1.isActivated(networkAction)).thenReturn(false);
        when(curativeResult1.getActivatedNetworkActions()).thenReturn(Set.of());
        when(curativeResult1.getOptimizedSetpoint(pstRangeAction, curativeState1)).thenReturn(28.9);
        when(curativeResult1.getOptimizedSetpoint(rangeAction, curativeState1)).thenReturn(25.6);
        when(curativeResult1.getOptimizedTap(pstRangeAction, curativeState1)).thenReturn(22);
        when(curativeResult1.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(curativeResult1.getActivatedRangeActions(curativeState1)).thenReturn(Set.of());
        when(curativeResult1.getOptimizedTapsOnState(curativeState1)).thenReturn(Map.of(pstRangeAction, 22));
        when(curativeResult1.getOptimizedSetpointsOnState(curativeState1)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        mockCnecResults(curativeResult1, cnec1, 530, 280, 1530, 780);

        when(curativeResult2.getFunctionalCost()).thenReturn(-1040.);
        when(curativeResult2.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        mockVirtualCosts(curativeResult2, -140, -60, List.of(cnec1), -120, List.of(cnec2));
        when(curativeResult2.isActivated(networkAction)).thenReturn(false);
        when(curativeResult2.getActivatedNetworkActions()).thenReturn(Set.of());
        when(curativeResult2.getOptimizedSetpoint(pstRangeAction, curativeState2)).thenReturn(48.9);
        when(curativeResult2.getOptimizedSetpoint(rangeAction, curativeState2)).thenReturn(25.6);
        when(curativeResult2.getOptimizedTap(pstRangeAction, curativeState2)).thenReturn(42);
        when(curativeResult2.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(curativeResult2.getActivatedRangeActions(curativeState2)).thenReturn(Set.of(pstRangeAction));
        when(curativeResult2.getOptimizedTapsOnState(curativeState2)).thenReturn(Map.of(pstRangeAction, 42));
        when(curativeResult2.getOptimizedSetpointsOnState(curativeState2)).thenReturn(Map.of(pstRangeAction, 28.9, rangeAction, 25.6));
        mockCnecResults(curativeResult2, cnec2, 1040, 540, 2040, 1040);

        when(preCurativeResult.getFunctionalCost()).thenReturn(-1050.);
        when(preCurativeResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec3));
        mockVirtualCosts(preCurativeResult, -150, -70, List.of(cnec3, cnec2), -130, List.of(cnec1, cnec4));
        when(preCurativeResult.getSetpoint(pstRangeAction)).thenReturn(58.9);
        when(preCurativeResult.getSetpoint(rangeAction)).thenReturn(55.6);
        when(preCurativeResult.getTap(pstRangeAction)).thenReturn(52);
        when(preCurativeResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        mockCnecResults(preCurativeResult, cnec2, 1050, 550, 2050, 1050);
        mockCnecResults(preCurativeResult, cnec1, 5050, 300, 1550, 800);

        stateTree = mock(StateTree.class);
        BasecaseScenario basecaseScenario = new BasecaseScenario(preventiveState, null);
        Set<ContingencyScenario> contingencyScenarios = Set.of(
            new ContingencyScenario(contingency1, autoState1, curativeState1),
            new ContingencyScenario(contingency2, null, curativeState2)
        );
        when(stateTree.getBasecaseScenario()).thenReturn(basecaseScenario);
        when(stateTree.getContingencyScenarios()).thenReturn(contingencyScenarios);

        output = new PreventiveAndCurativesRaoResultImpl(
            stateTree,
            initialResult,
            postPrevResult,
            preCurativeResult,
            Map.of(autoState1, autoResult1, curativeState1, curativeResult1, curativeState2, curativeResult2), crac);
    }

    private void mockCnecResults(FlowResult flowResult, FlowCnec cnec, double marginMw, double marginA, double relMarginMw, double relMarginA) {
        when(flowResult.getMargin(cnec, MEGAWATT)).thenReturn(marginMw);
        when(flowResult.getMargin(cnec, AMPERE)).thenReturn(marginA);
        when(flowResult.getRelativeMargin(cnec, MEGAWATT)).thenReturn(relMarginMw);
        when(flowResult.getRelativeMargin(cnec, AMPERE)).thenReturn(relMarginA);
        when(flowResult.getPtdfZonalSum(cnec, Side.LEFT)).thenReturn(1.);
    }

    private void mockVirtualCosts(ObjectiveFunctionResult objectiveFunctionResult, double virtualCost, double mnecCost, List<FlowCnec> mnecCostlyElements, double lfCost, List<FlowCnec> lfCostlyElements) {
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(virtualCost);
        when(objectiveFunctionResult.getVirtualCost("mnec")).thenReturn(mnecCost);
        when(objectiveFunctionResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(mnecCostlyElements);
        when(objectiveFunctionResult.getVirtualCost("lf")).thenReturn(lfCost);
        when(objectiveFunctionResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(lfCostlyElements);
    }

    private void flowResultThrows(Instant instant, FlowCnec cnec) {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getFlow(instant, cnec, Side.LEFT, Unit.MEGAWATT));
        assertEquals(String.format("Trying to access results for instant %s at optimization state %s is not allowed", cnec.getState().getInstant().getId(), instant.toString()), exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(instant, cnec, Unit.MEGAWATT));
        assertEquals(String.format("Trying to access results for instant %s at optimization state %s is not allowed", cnec.getState().getInstant().getId(), instant), exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getRelativeMargin(instant, cnec, Unit.MEGAWATT));
        assertEquals(String.format("Trying to access results for instant %s at optimization state %s is not allowed", cnec.getState().getInstant().getId(), instant), exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getCommercialFlow(instant, cnec, Side.LEFT, Unit.MEGAWATT));
        assertEquals(String.format("Trying to access results for instant %s at optimization state %s is not allowed", cnec.getState().getInstant().getId(), instant), exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getLoopFlow(instant, cnec, Side.LEFT, Unit.MEGAWATT));
        assertEquals(String.format("Trying to access results for instant %s at optimization state %s is not allowed", cnec.getState().getInstant().getId(), instant), exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPtdfZonalSum(instant, cnec, Side.LEFT));
        assertEquals(String.format("Trying to access results for instant %s at optimization state %s is not allowed", cnec.getState().getInstant().getId(), instant), exception.getMessage());
    }

    @Test
    void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult1.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult2.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());

        when(initialResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(postPrevResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());

        when(postPrevResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(curativeResult2.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());
    }

    @Test
    void testGetFunctionalCost() {
        assertEquals(1000., output.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(-1050., output.getFunctionalCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(-1020., output.getFunctionalCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-1020., output.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);

        when(postPrevResult.getFunctionalCost()).thenReturn(-2020.);
        assertEquals(-1025., output.getFunctionalCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-1025., output.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetMostLimitingElements() {
        assertNull(output.getMostLimitingElements());
    }

    @Test
    void testGetVirtualCost() {
        assertEquals(100., output.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(-150., output.getVirtualCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(-125., output.getVirtualCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-270., output.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCost() {
        assertEquals(1100., output.getCost(null), DOUBLE_TOLERANCE);
        assertEquals(-1200., output.getCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(-1145., output.getCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-1290., output.getCost(curativeInstant), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetVirtualCostNames() {
        assertEquals(Set.of("mnec", "lf"), output.getVirtualCostNames());
    }

    @Test
    void testGetVirtualCostByName() {
        assertEquals(20., output.getVirtualCost(null, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(80., output.getVirtualCost(null, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-70., output.getVirtualCost(preventiveInstant, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-130., output.getVirtualCost(preventiveInstant, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-45., output.getVirtualCost(autoInstant, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-105., output.getVirtualCost(autoInstant, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-110., output.getVirtualCost(curativeInstant, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-230., output.getVirtualCost(curativeInstant, "lf"), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCostlyElements() {
        assertEquals(List.of(cnec2), output.getCostlyElements(null, "mnec", 5));
        assertEquals(List.of(cnec1), output.getCostlyElements(null, "lf", 15));

        assertEquals(List.of(cnec3, cnec2), output.getCostlyElements(preventiveInstant, "mnec", 5));
        assertEquals(List.of(cnec1, cnec4), output.getCostlyElements(preventiveInstant, "lf", 15));

        assertNull(output.getCostlyElements(autoInstant, "mnec", 5));
        assertNull(output.getCostlyElements(autoInstant, "lf", 15));

        assertNull(output.getCostlyElements(curativeInstant, "mnec", 5));
        assertNull(output.getCostlyElements(curativeInstant, "lf", 15));
    }

    @Test
    void testWasNetworkActionActivatedBeforeState() {
        assertFalse(output.wasActivatedBeforeState(preventiveState, networkAction));
        assertTrue(output.wasActivatedBeforeState(autoState1, networkAction));
        assertTrue(output.wasActivatedBeforeState(curativeState1, networkAction));
        assertTrue(output.wasActivatedBeforeState(curativeState2, networkAction));
    }

    @Test
    void testIsNetworkActionActivatedDuringState() {
        assertTrue(output.isActivatedDuringState(preventiveState, networkAction));
        assertFalse(output.isActivatedDuringState(autoState1, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState1, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState2, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState3, networkAction));

        assertTrue(output.isActivatedDuringState(preventiveState, (RemedialAction<?>) networkAction));
        assertFalse(output.isActivatedDuringState(autoState1, (RemedialAction<?>) networkAction));
        assertFalse(output.isActivatedDuringState(curativeState1, (RemedialAction<?>) networkAction));
        assertFalse(output.isActivatedDuringState(curativeState2, (RemedialAction<?>) networkAction));
        assertFalse(output.isActivatedDuringState(curativeState3, (RemedialAction<?>) networkAction));
    }

    @Test
    void testGetActivatedNetworkActionsDuringState() {
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
    void testIsRangeActionActivatedDuringState() {
        assertTrue(output.isActivatedDuringState(preventiveState, rangeAction));
        assertTrue(output.isActivatedDuringState(autoState1, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState1, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState2, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState3, rangeAction));

        assertTrue(output.isActivatedDuringState(preventiveState, (RemedialAction<?>) rangeAction));
        assertTrue(output.isActivatedDuringState(autoState1, (RemedialAction<?>) rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState1, (RemedialAction<?>) rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState2, (RemedialAction<?>) rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState3, (RemedialAction<?>) rangeAction));
    }

    @Test
    void testGetPreOptimizationTapOnState() {
        assertEquals(1, output.getPreOptimizationTapOnState(preventiveState, pstRangeAction));
        assertEquals(22, output.getPreOptimizationTapOnState(autoState1, pstRangeAction));
        assertEquals(22, output.getPreOptimizationTapOnState(curativeState1, pstRangeAction));
        assertEquals(22, output.getPreOptimizationTapOnState(curativeState2, pstRangeAction));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationTapOnState(curativeState3, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());
    }

    @Test
    void testGetOptimizedTapOnState() {
        when(postPrevResult.getOptimizedTap(eq(pstRangeAction), any())).thenReturn(202);
        assertEquals(202, output.getOptimizedTapOnState(preventiveState, pstRangeAction));
        assertEquals(22, output.getOptimizedTapOnState(autoState1, pstRangeAction));
        assertEquals(22, output.getOptimizedTapOnState(curativeState1, pstRangeAction));
        assertEquals(42, output.getOptimizedTapOnState(curativeState2, pstRangeAction));
        assertEquals(202, output.getOptimizedTapOnState(curativeState3, pstRangeAction));
    }

    @Test
    void testGetPreOptimizationSetPointOnState() {
        assertEquals(6.7, output.getPreOptimizationSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getPreOptimizationSetPointOnState(autoState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getPreOptimizationSetPointOnState(curativeState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getPreOptimizationSetPointOnState(curativeState2, pstRangeAction), DOUBLE_TOLERANCE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationSetPointOnState(curativeState3, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());
    }

    @Test
    void testGetOptimizedSetPointOnState() {
        when(postPrevResult.getOptimizedSetpoint(eq(pstRangeAction), any())).thenReturn(567.);
        assertEquals(567, output.getOptimizedSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getOptimizedSetPointOnState(autoState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getOptimizedSetPointOnState(curativeState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(48.9, output.getOptimizedSetPointOnState(curativeState2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(567, output.getOptimizedSetPointOnState(curativeState3, pstRangeAction), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetActivatedRangeActionsDuringState() {
        assertEquals(Set.of(pstRangeAction, rangeAction), output.getActivatedRangeActionsDuringState(preventiveState));
        assertEquals(Set.of(pstRangeAction, rangeAction), output.getActivatedRangeActionsDuringState(autoState1));
        assertEquals(Set.of(), output.getActivatedRangeActionsDuringState(curativeState1));
        assertEquals(Set.of(pstRangeAction), output.getActivatedRangeActionsDuringState(curativeState2));
        assertEquals(Set.of(), output.getActivatedRangeActionsDuringState(curativeState3));

        when(postPrevResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(pstRangeAction));
        assertEquals(Set.of(pstRangeAction), output.getActivatedRangeActionsDuringState(preventiveState));
    }

    @Test
    void testGetOptimizedTapsOnState() {
        when(curativeResult1.getOptimizedTapsOnState(curativeState1)).thenReturn(Map.of(pstRangeAction, 333));
        when(curativeResult1.getOptimizedTap(pstRangeAction, curativeState1)).thenReturn(333);
        // with next line, pstRangeAction should be detected as activated in state1
        when(curativeResult1.getOptimizedSetpoint(pstRangeAction, curativeState1)).thenReturn(3330.);

        when(curativeResult2.getOptimizedTapsOnState(curativeState2)).thenReturn(Map.of(pstRangeAction, 444));
        when(curativeResult2.getOptimizedTap(pstRangeAction, curativeState1)).thenReturn(444);
        // with next line, pstRangeAction should not be detected as activated in state2
        when(curativeResult2.getOptimizedSetpoint(pstRangeAction, curativeState1)).thenReturn(18.9);

        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(autoState1));
        assertEquals(Map.of(pstRangeAction, 333), output.getOptimizedTapsOnState(curativeState1));
        assertEquals(Map.of(pstRangeAction, 444), output.getOptimizedTapsOnState(curativeState2));
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(curativeState3));
    }

    @Test
    void testGetOptimizedSetPointsOnState() {
        when(postPrevResult.getOptimizedSetpointsOnState(any())).thenReturn(Map.of(pstRangeAction, 222., rangeAction, 111.));
        when(autoResult1.getOptimizedSetpointsOnState(autoState1)).thenReturn(Map.of(pstRangeAction, 222., rangeAction, 111.));

        when(curativeResult1.getOptimizedSetpointsOnState(curativeState1)).thenReturn(Map.of(pstRangeAction, 333.));
        // with next line, pstRangeAction should be detected as activated in state1
        when(curativeResult1.getOptimizedSetpoint(pstRangeAction, curativeState1)).thenReturn(333.);

        when(curativeResult2.getOptimizedSetpointsOnState(curativeState2)).thenReturn(Map.of(pstRangeAction, 444.));
        // with next line, pstRangeAction should not be detected as activated in state2
        when(curativeResult2.getOptimizedSetpoint(pstRangeAction, curativeState2)).thenReturn(18.9);

        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(autoState1));
        assertEquals(Map.of(pstRangeAction, 333.), output.getOptimizedSetPointsOnState(curativeState1));
        assertEquals(Map.of(pstRangeAction, 444.), output.getOptimizedSetPointsOnState(curativeState2));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(curativeState3));
    }

    @Test
    void testGetFlow() {
        when(initialResult.getFlow(cnec1, LEFT, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getFlow(cnec2, RIGHT, AMPERE)).thenReturn(20.);
        when(postPrevResult.getFlow(cnec3, RIGHT, MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getFlow(null, cnec1, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getFlow(preventiveInstant, cnec2, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getFlow(curativeInstant, cnec3, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetMargin() {
        when(initialResult.getMargin(cnec1, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getMargin(cnec2, AMPERE)).thenReturn(20.);
        when(postPrevResult.getMargin(cnec3, MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getMargin(null, cnec1, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getMargin(preventiveInstant, cnec2, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getMargin(curativeInstant, cnec3, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetRelativeMargin() {
        when(initialResult.getRelativeMargin(cnec1, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getRelativeMargin(cnec2, AMPERE)).thenReturn(20.);
        when(postPrevResult.getRelativeMargin(cnec3, MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getRelativeMargin(null, cnec1, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getRelativeMargin(preventiveInstant, cnec2, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getRelativeMargin(curativeInstant, cnec3, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCommercialFlow() {
        when(initialResult.getCommercialFlow(cnec1, LEFT, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getCommercialFlow(cnec2, RIGHT, AMPERE)).thenReturn(20.);
        when(curativeResult1.getCommercialFlow(cnec3, RIGHT, MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState1);
        assertEquals(10., output.getCommercialFlow(null, cnec1, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getCommercialFlow(preventiveInstant, cnec2, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getCommercialFlow(curativeInstant, cnec3, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetLoopFlow() {
        when(initialResult.getLoopFlow(cnec1, LEFT, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getLoopFlow(cnec2, RIGHT, AMPERE)).thenReturn(20.);
        when(curativeResult2.getFlow(cnec3, RIGHT, MEGAWATT)).thenReturn(90.);
        when(curativeResult2.getCommercialFlow(cnec3, RIGHT, MEGAWATT)).thenReturn(60.);
        when(cnec3.getState()).thenReturn(curativeState2);
        assertEquals(10., output.getLoopFlow(null, cnec1, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getLoopFlow(preventiveInstant, cnec2, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getLoopFlow(curativeInstant, cnec3, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetPtdfZonalSum() {
        when(initialResult.getPtdfZonalSum(cnec1, LEFT)).thenReturn(10.);
        when(preCurativeResult.getPtdfZonalSum(cnec2, RIGHT)).thenReturn(20.);
        when(autoResult1.getPtdfZonalSum(cnec1auto, LEFT)).thenReturn(25.);
        when(curativeResult2.getPtdfZonalSum(cnec3, RIGHT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState2);
        assertEquals(10., output.getPtdfZonalSum(null, cnec1, LEFT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getPtdfZonalSum(preventiveInstant, cnec2, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(30., output.getPtdfZonalSum(curativeInstant, cnec3, RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetFlowResult() {
        flowResultThrows(autoInstant, cnec4);
        flowResultThrows(curativeInstant, cnec4);
        flowResultThrows(curativeInstant, cnec1auto);
    }

    @Test
    void testGetPerimeter() {
        State outageState = mock(State.class);
        when(outageState.getInstant()).thenReturn(outageInstant);

        when(initialResult.getPtdfZonalSum(cnec1, LEFT)).thenReturn(1.);
        when(postPrevResult.getPtdfZonalSum(cnec1, LEFT)).thenReturn(2.);
        when(autoResult1.getPtdfZonalSum(cnec1, LEFT)).thenReturn(3.);
        when(curativeResult1.getPtdfZonalSum(cnec1, LEFT)).thenReturn(4.);
        when(curativeResult2.getPtdfZonalSum(cnec1, LEFT)).thenReturn(5.);

        when(curativeResult2.getSensitivityStatus(curativeState2)).thenReturn(ComputationStatus.DEFAULT);

        PerimeterResult perimeterResult;

        // null
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, preventiveState));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, outageState));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, autoState1));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, curativeState1));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, curativeState2));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());

        // PREVENTIVE
        perimeterResult = output.getPerimeterResult(preventiveInstant, preventiveState);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(preventiveInstant, outageState);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(preventiveInstant, autoState1);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(preventiveInstant, curativeState1);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(preventiveInstant, curativeState2);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);

        // AUTO
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(autoInstant, preventiveState));
        assertEquals("Trying to access results for instant preventive at optimization state auto is not allowed", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(autoInstant, outageState));
        assertEquals("Trying to access results for instant outage at optimization state auto is not allowed", exception.getMessage());
        perimeterResult = output.getPerimeterResult(autoInstant, autoState1);
        assertEquals(3., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(autoInstant, curativeState1);
        assertEquals(3., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        assertNull(output.getPerimeterResult(autoInstant, curativeState2));

        // CURATIVE
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(curativeInstant, preventiveState));
        assertEquals("Trying to access results for instant preventive at optimization state curative is not allowed", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(curativeInstant, outageState));
        assertEquals("Trying to access results for instant outage at optimization state curative is not allowed", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(curativeInstant, autoState1));
        assertEquals("Trying to access results for instant auto at optimization state curative is not allowed", exception.getMessage());
        perimeterResult = output.getPerimeterResult(curativeInstant, curativeState1);
        assertEquals(4., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(curativeInstant, curativeState2);
        assertEquals(5., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);

        assertEquals(ComputationStatus.DEFAULT, output.getComputationStatus(curativeState2));
    }

    @Test
    void testNoPostContingencyResultGetters() {
        // Test if only preventive RAO has been conducted
        output = new PreventiveAndCurativesRaoResultImpl(preventiveState, initialResult, postPrevResult, preCurativeResult, crac);

        // Test get functional cost
        assertEquals(1000., output.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(-1050., output.getFunctionalCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(-1050., output.getFunctionalCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-1050., output.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);

        // Test get most limiting elements
        assertNull(output.getMostLimitingElements());
        assertNull(output.getMostLimitingElements());
        assertNull(output.getMostLimitingElements());
        assertNull(output.getMostLimitingElements());

        // Test get virtual cost
        assertEquals(100., output.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(-150., output.getVirtualCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(-150., output.getVirtualCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-150., output.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);

        // Test get virtual cost names
        assertEquals(Set.of("mnec", "lf"), output.getVirtualCostNames());

        // Test get virtual cost by name
        assertEquals(20., output.getVirtualCost(null, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(80., output.getVirtualCost(null, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-70., output.getVirtualCost(preventiveInstant, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-130., output.getVirtualCost(preventiveInstant, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-70., output.getVirtualCost(autoInstant, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-130., output.getVirtualCost(autoInstant, "lf"), DOUBLE_TOLERANCE);
        assertEquals(-70., output.getVirtualCost(curativeInstant, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(-130., output.getVirtualCost(curativeInstant, "lf"), DOUBLE_TOLERANCE);

        // Test get costly elements
        assertEquals(List.of(cnec2), output.getCostlyElements(null, "mnec", 5));
        assertEquals(List.of(cnec1), output.getCostlyElements(null, "lf", 15));
        assertEquals(List.of(cnec3, cnec2), output.getCostlyElements(preventiveInstant, "mnec", 5));
        assertEquals(List.of(cnec1, cnec4), output.getCostlyElements(preventiveInstant, "lf", 15));
        assertEquals(List.of(cnec3, cnec2), output.getCostlyElements(autoInstant, "mnec", 5));
        assertEquals(List.of(cnec1, cnec4), output.getCostlyElements(autoInstant, "lf", 15));
        assertEquals(List.of(cnec3, cnec2), output.getCostlyElements(curativeInstant, "mnec", 5));
        assertEquals(List.of(cnec1, cnec4), output.getCostlyElements(curativeInstant, "lf", 15));

        // Test was activated before state
        assertFalse(output.wasActivatedBeforeState(preventiveState, networkAction));
        assertTrue(output.wasActivatedBeforeState(autoState1, networkAction));
        assertTrue(output.wasActivatedBeforeState(curativeState1, networkAction));
        assertTrue(output.wasActivatedBeforeState(curativeState2, networkAction));

        // Test is activated during state
        assertTrue(output.isActivatedDuringState(preventiveState, networkAction));
        assertFalse(output.isActivatedDuringState(autoState1, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState1, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState2, networkAction));
        assertFalse(output.isActivatedDuringState(curativeState3, networkAction));

        // Test get network actions activated during state
        assertEquals(Set.of(networkAction), output.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(autoState1));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState1));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState2));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState3));
        when(postPrevResult.getActivatedNetworkActions()).thenReturn(Set.of());
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(preventiveState));
        assertEquals(Set.of(), output.getActivatedNetworkActionsDuringState(curativeState3));

        // Test is activated during state
        assertTrue(output.isActivatedDuringState(preventiveState, rangeAction));
        assertFalse(output.isActivatedDuringState(autoState1, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState1, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState2, rangeAction));
        assertFalse(output.isActivatedDuringState(curativeState3, rangeAction));

        // Test get pre optim tap
        assertEquals(1, output.getPreOptimizationTapOnState(preventiveState, pstRangeAction));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationTapOnState(autoState1, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationTapOnState(curativeState1, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationTapOnState(curativeState2, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationTapOnState(curativeState3, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());

        // Test get optimized tap
        when(postPrevResult.getOptimizedTap(eq(pstRangeAction), any())).thenReturn(202);
        assertEquals(202, output.getOptimizedTapOnState(preventiveState, pstRangeAction));
        assertEquals(202, output.getOptimizedTapOnState(autoState1, pstRangeAction));
        assertEquals(202, output.getOptimizedTapOnState(curativeState1, pstRangeAction));
        assertEquals(202, output.getOptimizedTapOnState(curativeState2, pstRangeAction));
        assertEquals(202, output.getOptimizedTapOnState(curativeState3, pstRangeAction));

        // Test get pre optim setpoint
        assertEquals(6.7, output.getPreOptimizationSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationSetPointOnState(autoState1, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationSetPointOnState(curativeState1, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationSetPointOnState(curativeState2, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPreOptimizationSetPointOnState(curativeState3, pstRangeAction));
        assertEquals("State null was not optimized and does not have pre-optim values", exception.getMessage());

        // Test get optimized setpoint
        when(postPrevResult.getOptimizedSetpoint(eq(pstRangeAction), any())).thenReturn(567.);
        assertEquals(567, output.getOptimizedSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(567, output.getOptimizedSetPointOnState(autoState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(567, output.getOptimizedSetPointOnState(curativeState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(567, output.getOptimizedSetPointOnState(curativeState2, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(567, output.getOptimizedSetPointOnState(curativeState3, pstRangeAction), DOUBLE_TOLERANCE);

        // Test get activate range actions
        assertEquals(Set.of(rangeAction, pstRangeAction), output.getActivatedRangeActionsDuringState(preventiveState));
        assertEquals(Set.of(), output.getActivatedRangeActionsDuringState(autoState1));
        assertEquals(Set.of(), output.getActivatedRangeActionsDuringState(curativeState1));
        assertEquals(Set.of(), output.getActivatedRangeActionsDuringState(curativeState2));
        assertEquals(Set.of(), output.getActivatedRangeActionsDuringState(curativeState3));
        when(postPrevResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(pstRangeAction));
        assertEquals(Set.of(pstRangeAction), output.getActivatedRangeActionsDuringState(preventiveState));

        // Test get optimized taps
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(autoState1));
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(curativeState1));
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(curativeState2));
        assertEquals(Map.of(pstRangeAction, 22), output.getOptimizedTapsOnState(curativeState3));

        // Test get optimized setpoints
        when(postPrevResult.getOptimizedSetpointsOnState(any())).thenReturn(Map.of(pstRangeAction, 222., rangeAction, 111.));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(preventiveState));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(autoState1));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(curativeState1));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(curativeState2));
        assertEquals(Map.of(pstRangeAction, 222., rangeAction, 111.), output.getOptimizedSetPointsOnState(curativeState3));

        // Test get flow
        when(initialResult.getFlow(cnec1, LEFT, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getFlow(cnec2, RIGHT, AMPERE)).thenReturn(20.);
        when(preCurativeResult.getFlow(cnec3, RIGHT, MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getFlow(null, cnec1, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getFlow(preventiveInstant, cnec2, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getFlow(curativeInstant, cnec3, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        // Test get margin
        when(initialResult.getMargin(cnec1, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getMargin(cnec2, AMPERE)).thenReturn(20.);
        when(preCurativeResult.getMargin(cnec3, MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getMargin(null, cnec1, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getMargin(preventiveInstant, cnec2, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getMargin(curativeInstant, cnec3, MEGAWATT), DOUBLE_TOLERANCE);

        // Test get relative margin
        when(initialResult.getRelativeMargin(cnec1, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getRelativeMargin(cnec2, AMPERE)).thenReturn(20.);
        when(preCurativeResult.getRelativeMargin(cnec3, MEGAWATT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState3);
        assertEquals(10., output.getRelativeMargin(null, cnec1, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getRelativeMargin(preventiveInstant, cnec2, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(30., output.getRelativeMargin(curativeInstant, cnec3, MEGAWATT), DOUBLE_TOLERANCE);

        // Test get commercial flow
        when(initialResult.getCommercialFlow(cnec1, LEFT, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getCommercialFlow(cnec2, RIGHT, AMPERE)).thenReturn(20.);
        when(preCurativeResult.getCommercialFlow(cnec3, RIGHT, MEGAWATT)).thenReturn(25.);
        when(cnec3.getState()).thenReturn(curativeState1);
        assertEquals(10., output.getCommercialFlow(null, cnec1, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getCommercialFlow(preventiveInstant, cnec2, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(25., output.getCommercialFlow(curativeInstant, cnec3, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        // Test get loopflow
        when(initialResult.getLoopFlow(cnec1, LEFT, MEGAWATT)).thenReturn(10.);
        when(preCurativeResult.getLoopFlow(cnec2, RIGHT, AMPERE)).thenReturn(20.);
        when(preCurativeResult.getLoopFlow(cnec3, RIGHT, MEGAWATT)).thenReturn(25.);
        when(cnec3.getState()).thenReturn(curativeState2);
        assertEquals(10., output.getLoopFlow(null, cnec1, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getLoopFlow(preventiveInstant, cnec2, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(25., output.getLoopFlow(curativeInstant, cnec3, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        // Test get ptdf zonal sum
        when(initialResult.getPtdfZonalSum(cnec1, LEFT)).thenReturn(10.);
        when(preCurativeResult.getPtdfZonalSum(cnec2, RIGHT)).thenReturn(20.);
        when(preCurativeResult.getPtdfZonalSum(cnec3, RIGHT)).thenReturn(30.);
        when(cnec3.getState()).thenReturn(curativeState2);
        assertEquals(10., output.getPtdfZonalSum(null, cnec1, LEFT), DOUBLE_TOLERANCE);
        assertEquals(20., output.getPtdfZonalSum(preventiveInstant, cnec2, RIGHT), DOUBLE_TOLERANCE);
        assertEquals(30., output.getPtdfZonalSum(curativeInstant, cnec3, RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    void testNoPostContingencyResultGetPerimeterResult() {
        // Test if only preventive RAO has been conducted
        output = new PreventiveAndCurativesRaoResultImpl(preventiveState, initialResult, postPrevResult, preCurativeResult, crac);

        State outageState = mock(State.class);
        when(outageState.getInstant()).thenReturn(outageInstant);

        when(initialResult.getPtdfZonalSum(cnec1, LEFT)).thenReturn(1.);
        when(postPrevResult.getPtdfZonalSum(cnec1, LEFT)).thenReturn(2.);
        when(autoResult1.getPtdfZonalSum(cnec1, LEFT)).thenReturn(3.);
        when(curativeResult1.getPtdfZonalSum(cnec1, LEFT)).thenReturn(4.);
        when(curativeResult2.getPtdfZonalSum(cnec1, LEFT)).thenReturn(5.);

        PerimeterResult perimeterResult;

        // null
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, preventiveState));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, outageState));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, autoState1));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, curativeState1));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(null, curativeState2));
        assertEquals("No PerimeterResult for INITIAL optimization state", exception.getMessage());

        // PREVENTIVE
        perimeterResult = output.getPerimeterResult(preventiveInstant, preventiveState);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(preventiveInstant, outageState);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(preventiveInstant, autoState1);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(preventiveInstant, curativeState1);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);
        perimeterResult = output.getPerimeterResult(preventiveInstant, curativeState2);
        assertEquals(2., perimeterResult.getPtdfZonalSum(cnec1, LEFT), DOUBLE_TOLERANCE);

        // AUTO
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(autoInstant, preventiveState));
        assertEquals("Trying to access results for instant preventive at optimization state auto is not allowed", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(autoInstant, outageState));
        assertEquals("Trying to access results for instant outage at optimization state auto is not allowed", exception.getMessage());
        assertNull(output.getPerimeterResult(autoInstant, autoState1));

        // CURATIVE
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(curativeInstant, preventiveState));
        assertEquals("Trying to access results for instant preventive at optimization state curative is not allowed", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(curativeInstant, outageState));
        assertEquals("Trying to access results for instant outage at optimization state curative is not allowed", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getPerimeterResult(curativeInstant, autoState1));
        assertEquals("Trying to access results for instant auto at optimization state curative is not allowed", exception.getMessage());
        assertNull(output.getPerimeterResult(curativeInstant, curativeState1));
        assertNull(output.getPerimeterResult(curativeInstant, curativeState2));
    }

    @Test
    void testRemedialActionsExcludedFrom2p() {
        PerimeterResult secondPreventivePerimeterResult = Mockito.mock(PerimeterResult.class);
        Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive = Set.of(pstRangeAction);

        output = new PreventiveAndCurativesRaoResultImpl(
            stateTree,
            initialResult,
            postPrevResult,
            secondPreventivePerimeterResult,
            remedialActionsExcludedFromSecondPreventive,
            preCurativeResult,
            Map.of(autoState1, autoResult1, curativeState1, curativeResult1, curativeState2, curativeResult2), crac);

        when(secondPreventivePerimeterResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of(rangeAction));
        when(secondPreventivePerimeterResult.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(secondPreventivePerimeterResult.getOptimizedSetpoint(rangeAction, preventiveState)).thenReturn(-1000.);
        when(secondPreventivePerimeterResult.getOptimizedSetpointsOnState(preventiveState)).thenReturn(Map.of(rangeAction, -1000.));

        assertTrue(output.isActivatedDuringState(preventiveState, rangeAction));
        assertTrue(output.isActivatedDuringState(preventiveState, pstRangeAction));
        assertTrue(output.isActivated(preventiveState, networkAction));
        assertEquals(22, output.getPreOptimizationTapOnState(autoState1, pstRangeAction));
        assertEquals(28.9, output.getOptimizedSetPointOnState(preventiveState, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(28.9, output.getPreOptimizationSetPointOnState(autoState1, pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getOptimizedSetPointOnState(preventiveState, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(-1000., output.getPreOptimizationSetPointOnState(autoState1, rangeAction), DOUBLE_TOLERANCE);
        assertEquals(Map.of(pstRangeAction, 28.9, rangeAction, -1000.), output.getOptimizedSetPointsOnState(preventiveState));

        when(secondPreventivePerimeterResult.getActivatedRangeActions(preventiveState)).thenReturn(Set.of());
        when(secondPreventivePerimeterResult.getActivatedNetworkActions()).thenReturn(Set.of());
        assertFalse(output.isActivatedDuringState(preventiveState, rangeAction));
        assertTrue(output.isActivatedDuringState(preventiveState, pstRangeAction));
        assertFalse(output.isActivated(preventiveState, networkAction));
    }

    @Test
    void testWithFinalCostEvaluator() {
        PerimeterResult secondPreventivePerimeterResult = Mockito.mock(PerimeterResult.class);
        ObjectiveFunctionResult postSecondAraoResults = Mockito.mock(ObjectiveFunctionResult.class);
        Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive = Set.of(pstRangeAction);

        output = new PreventiveAndCurativesRaoResultImpl(
            stateTree,
            initialResult,
            postPrevResult,
            secondPreventivePerimeterResult,
            remedialActionsExcludedFromSecondPreventive,
            preCurativeResult,
            Map.of(autoState1, autoResult1, curativeState1, curativeResult1, curativeState2, curativeResult2),
            postSecondAraoResults, crac);

        when(postSecondAraoResults.getFunctionalCost()).thenReturn(123.);
        mockVirtualCosts(postSecondAraoResults, 456., 400., List.of(cnec2, cnec4), 56., List.of(cnec1, cnec4));

        assertEquals(123., output.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(456., output.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(579., output.getCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(400., output.getVirtualCost(curativeInstant, "mnec"), DOUBLE_TOLERANCE);
        assertEquals(56., output.getVirtualCost(curativeInstant, "lf"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec2, cnec4), output.getCostlyElements(curativeInstant, "mnec", 100));
        assertEquals(List.of(cnec1, cnec4), output.getCostlyElements(curativeInstant, "lf", 100));
    }

    @Test
    void testOptimizedStepsExecuted() {
        setUp();
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
        Instant optimizedInstant = mock(Instant.class);

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optimizedInstant, angleCnec, MEGAWATT));
        assertEquals("Angle cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optimizedInstant, angleCnec, AMPERE));
        assertEquals("Angle cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optimizedInstant, voltageCnec, MEGAWATT));
        assertEquals("Voltage cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optimizedInstant, voltageCnec, AMPERE));
        assertEquals("Voltage cnecs are not computed in the rao", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> output.getVoltage(optimizedInstant, voltageCnec, MEGAWATT));
        assertEquals("Voltage cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getVoltage(optimizedInstant, voltageCnec, AMPERE));
        assertEquals("Voltage cnecs are not computed in the rao", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> output.getAngle(optimizedInstant, angleCnec, MEGAWATT));
        assertEquals("Angle cnecs are not computed in the rao", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> output.getMargin(optimizedInstant, angleCnec, AMPERE));
        assertEquals("Angle cnecs are not computed in the rao", exception.getMessage());
    }
}
