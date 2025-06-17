/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FastRaoResultImplTest {

    private PrePerimeterResult initialResult;
    private PrePerimeterResult afterPraResult;
    private PrePerimeterResult afterAraResult;
    private PrePerimeterResult finalResult;
    private RaoResult filteredRaoResult;
    private Crac crac;
    private FastRaoResultImpl result;

    @BeforeEach
    void setUp() {
        crac = ExhaustiveCracCreation.create();
        initialResult = Mockito.mock(PrePerimeterResult.class);
        afterPraResult = Mockito.mock(PrePerimeterResult.class);
        afterAraResult = Mockito.mock(PrePerimeterResult.class);
        finalResult = Mockito.mock(PrePerimeterResult.class);
        for (State state : crac.getStates()) {
            when(initialResult.getComputationStatus(state)).thenReturn(DEFAULT);
            when(afterPraResult.getComputationStatus(state)).thenReturn(DEFAULT);
            when(afterAraResult.getComputationStatus(state)).thenReturn(DEFAULT);
            when(finalResult.getComputationStatus(state)).thenReturn(DEFAULT);
        }
        filteredRaoResult = Mockito.mock(RaoResult.class);
        when(filteredRaoResult.getExecutionDetails()).thenReturn(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY);
        result = new FastRaoResultImpl(
            initialResult, afterPraResult, afterAraResult, finalResult, filteredRaoResult, crac
        );
    }

    @Test
    void testGetComputationStatus() {
        when(initialResult.getSensitivityStatus()).thenReturn(DEFAULT);
        when(afterPraResult.getSensitivityStatus()).thenReturn(PARTIAL_FAILURE);
        when(afterAraResult.getSensitivityStatus()).thenReturn(DEFAULT);
        when(finalResult.getSensitivityStatus()).thenReturn(DEFAULT);

        ComputationStatus status = result.getComputationStatus();
        assertTrue(status == PARTIAL_FAILURE);

        when(initialResult.getSensitivityStatus()).thenReturn(FAILURE);
        when(afterPraResult.getSensitivityStatus()).thenReturn(DEFAULT);
        when(afterAraResult.getSensitivityStatus()).thenReturn(PARTIAL_FAILURE);
        when(finalResult.getSensitivityStatus()).thenReturn(DEFAULT);
        result = new FastRaoResultImpl(
            initialResult, afterPraResult, afterAraResult, finalResult, filteredRaoResult, crac
        );
        status = result.getComputationStatus();
        assertTrue(status == FAILURE);
        assertFalse(result.isSecure(PhysicalParameter.FLOW));
    }

    @Test
    void testGetAppropriateResult() {
        assertEquals(initialResult, result.getAppropriateResult(null));
        assertEquals(afterPraResult, result.getAppropriateResult(crac.getInstant("preventive")));
        assertEquals(afterPraResult, result.getAppropriateResult(crac.getInstant("outage")));
        assertEquals(afterAraResult, result.getAppropriateResult(crac.getInstant("auto")));
        assertEquals(finalResult, result.getAppropriateResult(crac.getInstant("curative")));
        assertThrows(OpenRaoException.class, () -> result.getAppropriateResult(crac.getInstant("blabla")));
    }

    @Test
    void testGetAppropriateResultFlowCnec() {
        FlowCnec flowCnec = crac.getFlowCnec("cnec3autoId");
        assertEquals(afterPraResult, result.getAppropriateResult(crac.getInstant("preventive"), flowCnec));
        assertEquals(afterAraResult, result.getAppropriateResult(crac.getInstant("auto"), flowCnec));
        assertEquals(afterPraResult, result.getAppropriateResult(crac.getInstant("outage"), flowCnec));
        assertEquals(afterAraResult, result.getAppropriateResult(crac.getInstant("curative"), flowCnec));
        assertEquals(initialResult, result.getAppropriateResult(null, flowCnec));

    }

    @Test
    void testGetVirtualCostNames() {
        when(initialResult.getVirtualCostNames()).thenReturn(Set.of("costA", "costB"));
        when(finalResult.getVirtualCostNames()).thenReturn(Set.of("costB", "costC"));
        Set<String> allNames = result.getVirtualCostNames();
        assertEquals(3, allNames.size());
        assertTrue(allNames.contains("costA"));
        assertTrue(allNames.contains("costB"));
        assertTrue(allNames.contains("costC"));
    }

    @Test
    void testGetVirtualCostNamesBothNull() {
        when(initialResult.getVirtualCostNames()).thenReturn(null);
        when(finalResult.getVirtualCostNames()).thenReturn(null);
        Set<String> allNames = result.getVirtualCostNames();
        assertTrue(allNames.isEmpty());
    }

    @Test
    void testGetterAndSetter() {
        FlowCnec flowCnec = crac.getFlowCnec("cnec3autoId");
        Instant preventive = crac.getInstant("preventive");

        // Setup mocks for afterPraResult
        when(afterPraResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(12.2);
        when(afterPraResult.getRelativeMargin(flowCnec, Unit.MEGAWATT)).thenReturn(2.2);
        when(afterPraResult.getFlow(flowCnec, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(89.3);
        when(afterPraResult.getCommercialFlow(flowCnec, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(9.3);
        when(afterPraResult.getLoopFlow(flowCnec, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(856.3);
        when(afterPraResult.getPtdfZonalSum(flowCnec, TwoSides.TWO)).thenReturn(85.3);
        when(afterPraResult.getFunctionalCost()).thenReturn(185.3);
        when(afterPraResult.getMostLimitingElements(2)).thenReturn(
            List.of(crac.getFlowCnec("cnec2prevId"), crac.getFlowCnec("cnec3autoId")));
        when(afterPraResult.getVirtualCost()).thenReturn(-6.3);
        when(afterPraResult.getVirtualCost("vcost1")).thenReturn(15.2);
        when(afterPraResult.getCostlyElements("vcost1", 1)).thenReturn(
            List.of(crac.getFlowCnec("cnec2prevId")));

        // Margin, flows, functional and virtual cost
        assertEquals(12.2, result.getMargin(preventive, flowCnec, Unit.MEGAWATT));
        assertEquals(2.2, result.getRelativeMargin(preventive, flowCnec, Unit.MEGAWATT));
        assertEquals(89.3, result.getFlow(preventive, flowCnec, TwoSides.TWO, Unit.MEGAWATT));
        assertEquals(9.3, result.getCommercialFlow(preventive, flowCnec, TwoSides.TWO, Unit.MEGAWATT));
        assertEquals(856.3, result.getLoopFlow(preventive, flowCnec, TwoSides.TWO, Unit.MEGAWATT));
        assertEquals(85.3, result.getPtdfZonalSum(preventive, flowCnec, TwoSides.TWO));
        assertEquals(185.3, result.getFunctionalCost(preventive));
        assertEquals(-6.3, result.getVirtualCost(preventive));
        assertEquals(15.2, result.getVirtualCost(preventive, "vcost1"));
        assertEquals(List.of(crac.getFlowCnec("cnec2prevId"), crac.getFlowCnec("cnec3autoId")),
            result.getMostLimitingElements(preventive, 2));
        assertEquals(List.of(crac.getFlowCnec("cnec2prevId")), result.getCostlyElements(preventive, "vcost1", 1));

        // get/setCriticalCnecs
        Set<FlowCnec> cnecSet = Set.of(flowCnec);
        result.setCriticalCnecs(cnecSet);
        assertEquals(cnecSet, result.getCriticalCnecs());
    }

    @Test
    void testActivatedActionsDuringState() {
        // Mocks for activation tests
        NetworkAction networkAction = Mockito.mock(NetworkAction.class);
        RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        RemedialAction remedialAction = Mockito.mock(RemedialAction.class);
        State state = Mockito.mock(State.class);
        when(filteredRaoResult.isActivatedDuringState(state, networkAction)).thenReturn(true);
        when(filteredRaoResult.isActivatedDuringState(state, rangeAction)).thenReturn(false);
        when(filteredRaoResult.isActivatedDuringState(state, pstRangeAction)).thenReturn(true);
        assertEquals(true, result.isActivatedDuringState(state, networkAction));
        assertEquals(false, result.isActivatedDuringState(state, rangeAction));
        assertThrows(OpenRaoException.class, () -> result.isActivatedDuringState(state, remedialAction));
        assertEquals(true, result.isActivatedDuringState(state, pstRangeAction));

        when(filteredRaoResult.wasActivatedBeforeState(state, networkAction)).thenReturn(true);
        assertEquals(true, result.wasActivatedBeforeState(state, networkAction));

        when(filteredRaoResult.getActivatedNetworkActionsDuringState(state)).thenReturn(Set.of(networkAction));
        when(filteredRaoResult.getActivatedRangeActionsDuringState(state)).thenReturn(Set.of(rangeAction));
        assertEquals(Set.of(networkAction), result.getActivatedNetworkActionsDuringState(state));
        assertEquals(Set.of(rangeAction), result.getActivatedRangeActionsDuringState(state));

        when(filteredRaoResult.getPreOptimizationTapOnState(state, pstRangeAction)).thenReturn(11);
        when(filteredRaoResult.getOptimizedTapOnState(state, pstRangeAction)).thenReturn(12);
        when(filteredRaoResult.getPreOptimizationSetPointOnState(state, rangeAction)).thenReturn(5.5);
        when(filteredRaoResult.getOptimizedSetPointOnState(state, rangeAction)).thenReturn(7.7);
        when(filteredRaoResult.getOptimizedTapsOnState(state)).thenReturn(Map.of(pstRangeAction, 13));
        when(filteredRaoResult.getOptimizedSetPointsOnState(state)).thenReturn(Map.of(rangeAction, 8.8));
        assertEquals(11, result.getPreOptimizationTapOnState(state, pstRangeAction));
        assertEquals(12, result.getOptimizedTapOnState(state, pstRangeAction));
        assertEquals(5.5, result.getPreOptimizationSetPointOnState(state, rangeAction));
        assertEquals(7.7, result.getOptimizedSetPointOnState(state, rangeAction));
        assertEquals(Map.of(pstRangeAction, 13), result.getOptimizedTapsOnState(state));
        assertEquals(Map.of(rangeAction, 8.8), result.getOptimizedSetPointsOnState(state));

    }

    @Test
    void testExecutionDetailsAndStatus() {
        result.setExecutionDetails(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY);
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY, result.getExecutionDetails());
        when(afterPraResult.getFunctionalCost()).thenReturn(185.3);
        assertFalse(result.isSecure(PhysicalParameter.FLOW));
        State state = Mockito.mock(State.class);
        when(state.getInstant()).thenReturn(crac.getInstant("preventive"));
        when(afterPraResult.getComputationStatus(state)).thenReturn(FAILURE);
        assertEquals(FAILURE, result.getComputationStatus(state));
    }

}
