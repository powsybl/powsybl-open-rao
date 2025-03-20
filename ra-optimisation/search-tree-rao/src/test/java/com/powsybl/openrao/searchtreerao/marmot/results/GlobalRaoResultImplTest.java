/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.searchtreerao.marmot.TestsUtils;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GlobalRaoResultImplTest {
    private State stateTimestamp1;
    private State stateTimestamp2;
    private State stateTimestamp3;
    private Instant instant;
    private FlowCnec flowCnecTimestamp1;
    private FlowCnec flowCnecTimestamp2;
    private FlowCnec flowCnecTimestamp3;
    private PstRangeAction pstRangeAction;
    private NetworkAction networkAction;
    private RaoResult raoResultTimestamp1;
    private RaoResult raoResultTimestamp2;
    private RaoResult raoResultTimestamp3;
    private GlobalRaoResultImpl globalRaoResult;

    @BeforeEach
    void setUp() {
        stateTimestamp1 = TestsUtils.mockState(TestsUtils.TIMESTAMP_1);
        stateTimestamp2 = TestsUtils.mockState(TestsUtils.TIMESTAMP_2);
        stateTimestamp3 = TestsUtils.mockState(TestsUtils.TIMESTAMP_3);

        instant = Mockito.mock(Instant.class);

        flowCnecTimestamp1 = TestsUtils.mockFlowCnec(stateTimestamp1);
        flowCnecTimestamp2 = TestsUtils.mockFlowCnec(stateTimestamp2);
        flowCnecTimestamp3 = TestsUtils.mockFlowCnec(stateTimestamp3);

        pstRangeAction = Mockito.mock(PstRangeAction.class);
        networkAction = Mockito.mock(NetworkAction.class);

        ObjectiveFunctionResult initialObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(initialObjectiveFunctionResult.getFunctionalCost()).thenReturn(0.);
        Mockito.when(initialObjectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        Mockito.when(initialObjectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("virtual"));
        Mockito.when(initialObjectiveFunctionResult.getVirtualCost("virtual")).thenReturn(0.);

        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(objectiveFunctionResult.getFunctionalCost()).thenReturn(900.);
        Mockito.when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);
        Mockito.when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("virtual"));
        Mockito.when(objectiveFunctionResult.getVirtualCost("virtual")).thenReturn(100.);

        raoResultTimestamp1 = mockRaoResult(true, "RAO 1 succeeded.", 450., 0., flowCnecTimestamp1, 850., 10., stateTimestamp1, 0, 0, 0., 0., true);
        raoResultTimestamp2 = mockRaoResult(true, "RAO 2 succeeded.", 250., 90., flowCnecTimestamp2, 510., 45., stateTimestamp2, 0, 5, 0., 10.2, false);
        raoResultTimestamp3 = mockRaoResult(false, "RAO 3 failed.", 200., 10., flowCnecTimestamp3, 1000., -60., stateTimestamp3, 0, 16, 0., 35.32, true);

        globalRaoResult = new GlobalRaoResultImpl(initialObjectiveFunctionResult, objectiveFunctionResult, new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, raoResultTimestamp1, TestsUtils.TIMESTAMP_2, raoResultTimestamp2, TestsUtils.TIMESTAMP_3, raoResultTimestamp3)));
    }

    @Test
    void testCosts() {
        assertEquals(Set.of("virtual"), globalRaoResult.getVirtualCostNames());

        assertEquals(0., globalRaoResult.getGlobalCost(null));
        assertEquals(0., globalRaoResult.getGlobalFunctionalCost(null));
        assertEquals(0., globalRaoResult.getGlobalVirtualCost(null));
        assertEquals(0., globalRaoResult.getGlobalVirtualCost(null, "virtual"));

        assertEquals(1000., globalRaoResult.getGlobalCost(InstantKind.PREVENTIVE));
        assertEquals(900., globalRaoResult.getGlobalFunctionalCost(InstantKind.PREVENTIVE));
        assertEquals(100., globalRaoResult.getGlobalVirtualCost(InstantKind.PREVENTIVE));
        assertEquals(100., globalRaoResult.getGlobalVirtualCost(InstantKind.PREVENTIVE, "virtual"));

        assertEquals(450., globalRaoResult.getCost(instant, TestsUtils.TIMESTAMP_1));
        assertEquals(450., globalRaoResult.getFunctionalCost(instant, TestsUtils.TIMESTAMP_1));
        assertEquals(0., globalRaoResult.getVirtualCost(instant, TestsUtils.TIMESTAMP_1));
        assertEquals(0., globalRaoResult.getVirtualCost(instant, "virtual", TestsUtils.TIMESTAMP_1));

        assertEquals(340., globalRaoResult.getCost(instant, TestsUtils.TIMESTAMP_2));
        assertEquals(250., globalRaoResult.getFunctionalCost(instant, TestsUtils.TIMESTAMP_2));
        assertEquals(90., globalRaoResult.getVirtualCost(instant, TestsUtils.TIMESTAMP_2));
        assertEquals(90., globalRaoResult.getVirtualCost(instant, "virtual", TestsUtils.TIMESTAMP_2));

        assertEquals(210., globalRaoResult.getCost(instant, TestsUtils.TIMESTAMP_3));
        assertEquals(200., globalRaoResult.getFunctionalCost(instant, TestsUtils.TIMESTAMP_3));
        assertEquals(10., globalRaoResult.getVirtualCost(instant, TestsUtils.TIMESTAMP_3));
        assertEquals(10., globalRaoResult.getVirtualCost(instant, "virtual", TestsUtils.TIMESTAMP_3));

        OpenRaoException exception1 = assertThrows(OpenRaoException.class, () -> globalRaoResult.getFunctionalCost(instant));
        assertEquals("Calling getFunctionalCost with an instant alone is ambiguous. For the global functional cost, use getGlobalFunctionalCost. Otherwise, please provide a timestamp.", exception1.getMessage());

        OpenRaoException exception2 = assertThrows(OpenRaoException.class, () -> globalRaoResult.getVirtualCost(instant));
        assertEquals("Calling getVirtualCost with an instant alone is ambiguous. For the global virtual cost, use getGlobalVirtualCost. Otherwise, please provide a timestamp.", exception2.getMessage());

        OpenRaoException exception3 = assertThrows(OpenRaoException.class, () -> globalRaoResult.getVirtualCost(instant, "virtual"));
        assertEquals("Calling getVirtualCost with an instant and a name alone is ambiguous. For the global virtual cost, use getGlobalVirtualCost. Otherwise, please provide a timestamp.", exception3.getMessage());
    }

    @Test
    void testTimestamps() {
        assertEquals(List.of(TestsUtils.TIMESTAMP_1, TestsUtils.TIMESTAMP_2, TestsUtils.TIMESTAMP_3), globalRaoResult.getTimestamps());
    }

    @Test
    void testIsSecure() {
        assertFalse(globalRaoResult.isSecure());
        assertTrue(globalRaoResult.isSecure(TestsUtils.TIMESTAMP_1));
        assertTrue(globalRaoResult.isSecure(TestsUtils.TIMESTAMP_2));
        assertFalse(globalRaoResult.isSecure(TestsUtils.TIMESTAMP_3));

        assertFalse(globalRaoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
        assertTrue(globalRaoResult.isSecure(TestsUtils.TIMESTAMP_1, PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
        assertTrue(globalRaoResult.isSecure(TestsUtils.TIMESTAMP_2, PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
        assertFalse(globalRaoResult.isSecure(TestsUtils.TIMESTAMP_3, PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));

        assertTrue(globalRaoResult.isSecure(instant, TestsUtils.TIMESTAMP_1, PhysicalParameter.FLOW));
        assertTrue(globalRaoResult.isSecure(instant, TestsUtils.TIMESTAMP_2, PhysicalParameter.FLOW));
        assertFalse(globalRaoResult.isSecure(instant, TestsUtils.TIMESTAMP_3, PhysicalParameter.FLOW));

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> globalRaoResult.isSecure(instant, PhysicalParameter.FLOW));
        assertEquals("Calling isSecure with an instant and physical parameters alone is ambiguous. Please provide a timestamp.", exception.getMessage());
    }

    @Test
    void testExecutionDetails() {
        assertEquals("2025-02-17T13:33:00Z: RAO 1 succeeded. - 2025-02-18T13:33:00Z: RAO 2 succeeded. - 2025-02-19T13:33:00Z: RAO 3 failed.", globalRaoResult.getExecutionDetails());
    }

    @Test
    void testFlow() {
        assertEquals(850., globalRaoResult.getFlow(instant, flowCnecTimestamp1, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(510., globalRaoResult.getFlow(instant, flowCnecTimestamp2, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(1000., globalRaoResult.getFlow(instant, flowCnecTimestamp3, TwoSides.ONE, Unit.MEGAWATT));
    }

    @Test
    void testMargin() {
        assertEquals(10., globalRaoResult.getMargin(instant, flowCnecTimestamp1, Unit.MEGAWATT));
        assertEquals(45., globalRaoResult.getMargin(instant, flowCnecTimestamp2, Unit.MEGAWATT));
        assertEquals(-60., globalRaoResult.getMargin(instant, flowCnecTimestamp3, Unit.MEGAWATT));
    }

    private RaoResult mockRaoResult(boolean isSecure, String executionDetails, double functionalCost, double virtualCost, FlowCnec flowCnec, double flow, double margin, State state, int initialTap, int optimizedTap, double initialSetPoint, double optimizedSetPoint, boolean isNetworkActionActivated) {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE)).thenReturn(isSecure);
        Mockito.when(raoResult.isSecure(instant, PhysicalParameter.FLOW)).thenReturn(isSecure);
        Mockito.when(raoResult.getExecutionDetails()).thenReturn(executionDetails);
        Mockito.when(raoResult.getFunctionalCost(instant)).thenReturn(functionalCost);
        Mockito.when(raoResult.getVirtualCost(instant)).thenReturn(virtualCost);
        Mockito.when(raoResult.getVirtualCost(instant, "virtual")).thenReturn(virtualCost);
        Mockito.when(raoResult.getFlow(instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(flow);
        Mockito.when(raoResult.getMargin(instant, flowCnec, Unit.MEGAWATT)).thenReturn(margin);
        Mockito.when(raoResult.getPreOptimizationTapOnState(state, pstRangeAction)).thenReturn(initialTap);
        Mockito.when(raoResult.getOptimizedTapOnState(state, pstRangeAction)).thenReturn(optimizedTap);
        Mockito.when(raoResult.getOptimizedTapsOnState(state)).thenReturn(Map.of(pstRangeAction, optimizedTap));
        Mockito.when(raoResult.isActivatedDuringState(state, pstRangeAction)).thenReturn(initialTap != optimizedTap);
        Mockito.when(raoResult.getPreOptimizationSetPointOnState(state, pstRangeAction)).thenReturn(initialSetPoint);
        Mockito.when(raoResult.getOptimizedSetPointOnState(state, pstRangeAction)).thenReturn(optimizedSetPoint);
        Mockito.when(raoResult.getOptimizedSetPointsOnState(state)).thenReturn(Map.of(pstRangeAction, optimizedSetPoint));
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(state)).thenReturn(isNetworkActionActivated ? Set.of(networkAction) : Set.of());
        Mockito.when(raoResult.isActivatedDuringState(state, networkAction)).thenReturn(isNetworkActionActivated);
        return raoResult;
    }

    @Test
    void testRangeActionActivation() {
        assertFalse(globalRaoResult.isActivatedDuringState(stateTimestamp1, pstRangeAction));
        assertEquals(0, globalRaoResult.getPreOptimizationTapOnState(stateTimestamp1, pstRangeAction));
        assertEquals(0, globalRaoResult.getOptimizedTapOnState(stateTimestamp1, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 0), globalRaoResult.getOptimizedTapsOnState(stateTimestamp1));
        assertEquals(0., globalRaoResult.getPreOptimizationSetPointOnState(stateTimestamp1, pstRangeAction));
        assertEquals(0., globalRaoResult.getOptimizedSetPointOnState(stateTimestamp1, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 0.), globalRaoResult.getOptimizedSetPointsOnState(stateTimestamp1));

        assertTrue(globalRaoResult.isActivatedDuringState(stateTimestamp2, pstRangeAction));
        assertEquals(0, globalRaoResult.getPreOptimizationTapOnState(stateTimestamp2, pstRangeAction));
        assertEquals(5, globalRaoResult.getOptimizedTapOnState(stateTimestamp2, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 5), globalRaoResult.getOptimizedTapsOnState(stateTimestamp2));
        assertEquals(0., globalRaoResult.getPreOptimizationSetPointOnState(stateTimestamp2, pstRangeAction));
        assertEquals(10.2, globalRaoResult.getOptimizedSetPointOnState(stateTimestamp2, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 10.2), globalRaoResult.getOptimizedSetPointsOnState(stateTimestamp2));

        assertTrue(globalRaoResult.isActivatedDuringState(stateTimestamp3, pstRangeAction));
        assertEquals(0, globalRaoResult.getPreOptimizationTapOnState(stateTimestamp3, pstRangeAction));
        assertEquals(16, globalRaoResult.getOptimizedTapOnState(stateTimestamp3, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 16), globalRaoResult.getOptimizedTapsOnState(stateTimestamp3));
        assertEquals(0., globalRaoResult.getPreOptimizationSetPointOnState(stateTimestamp3, pstRangeAction));
        assertEquals(35.32, globalRaoResult.getOptimizedSetPointOnState(stateTimestamp3, pstRangeAction));
        assertEquals(Map.of(pstRangeAction, 35.32), globalRaoResult.getOptimizedSetPointsOnState(stateTimestamp3));
    }

    @Test
    void testNetworkActionActivation() {
        assertTrue(globalRaoResult.isActivatedDuringState(stateTimestamp1, networkAction));
        assertEquals(Set.of(networkAction), globalRaoResult.getActivatedNetworkActionsDuringState(stateTimestamp1));

        assertFalse(globalRaoResult.isActivatedDuringState(stateTimestamp2, networkAction));
        assertTrue(globalRaoResult.getActivatedNetworkActionsDuringState(stateTimestamp2).isEmpty());

        assertTrue(globalRaoResult.isActivatedDuringState(stateTimestamp3, networkAction));
        assertEquals(Set.of(networkAction), globalRaoResult.getActivatedNetworkActionsDuringState(stateTimestamp3));
    }

    @Test
    void testGetIndividualRaoResult() {
        assertEquals(raoResultTimestamp1, globalRaoResult.getIndividualRaoResult(TestsUtils.TIMESTAMP_1));
        assertEquals(raoResultTimestamp2, globalRaoResult.getIndividualRaoResult(TestsUtils.TIMESTAMP_2));
        assertEquals(raoResultTimestamp3, globalRaoResult.getIndividualRaoResult(TestsUtils.TIMESTAMP_3));
    }
}
