/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.marmot.TestsUtils;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GlobalRangeActionActivationResultTest {
    private State stateTimestamp1;
    private State stateTimestamp2;
    private State stateTimestamp3;
    private PstRangeAction pstRangeActionTimestamp1;
    private PstRangeAction pstRangeActionTimestamp2;
    private PstRangeAction pstRangeActionTimestamp3;
    private RangeActionActivationResult globalRangeActionActivationResult;

    @BeforeEach
    void setUp() {
        stateTimestamp1 = TestsUtils.mockState(TestsUtils.TIMESTAMP_1);
        stateTimestamp2 = TestsUtils.mockState(TestsUtils.TIMESTAMP_2);
        stateTimestamp3 = TestsUtils.mockState(TestsUtils.TIMESTAMP_3);

        pstRangeActionTimestamp1 = Mockito.mock(PstRangeAction.class);
        pstRangeActionTimestamp2 = Mockito.mock(PstRangeAction.class);
        pstRangeActionTimestamp3 = Mockito.mock(PstRangeAction.class);

        RangeActionActivationResult rangeActionActivationResultTimestamp1 = TestsUtils.mockRangeActionActivationResult(stateTimestamp1, pstRangeActionTimestamp1, 5, 6.22);
        RangeActionActivationResult rangeActionActivationResultTimestamp2 = TestsUtils.mockRangeActionActivationResult(stateTimestamp2, pstRangeActionTimestamp2, 8, 12.11);
        RangeActionActivationResult rangeActionActivationResultTimestamp3 = TestsUtils.mockRangeActionActivationResult(stateTimestamp3, pstRangeActionTimestamp3, 1, 0.55);

        globalRangeActionActivationResult = new GlobalRangeActionActivationResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, rangeActionActivationResultTimestamp1, TestsUtils.TIMESTAMP_2, rangeActionActivationResultTimestamp2, TestsUtils.TIMESTAMP_3, rangeActionActivationResultTimestamp3)));
    }

    @Test
    void testRangeActions() {
        assertEquals(Set.of(pstRangeActionTimestamp1, pstRangeActionTimestamp2, pstRangeActionTimestamp3), globalRangeActionActivationResult.getRangeActions());
    }

    @Test
    void testActivatedRangeActions() {
        assertEquals(Set.of(pstRangeActionTimestamp1), globalRangeActionActivationResult.getActivatedRangeActions(stateTimestamp1));
        assertEquals(Set.of(pstRangeActionTimestamp2), globalRangeActionActivationResult.getActivatedRangeActions(stateTimestamp2));
        assertEquals(Set.of(pstRangeActionTimestamp3), globalRangeActionActivationResult.getActivatedRangeActions(stateTimestamp3));
    }

    @Test
    void testOptimalTap() {
        assertEquals(5, globalRangeActionActivationResult.getOptimizedTap(pstRangeActionTimestamp1, stateTimestamp1));
        assertEquals(8, globalRangeActionActivationResult.getOptimizedTap(pstRangeActionTimestamp2, stateTimestamp2));
        assertEquals(1, globalRangeActionActivationResult.getOptimizedTap(pstRangeActionTimestamp3, stateTimestamp3));
    }

    @Test
    void testTapVariation() {
        assertEquals(5, globalRangeActionActivationResult.getTapVariation(pstRangeActionTimestamp1, stateTimestamp1));
        assertEquals(8, globalRangeActionActivationResult.getTapVariation(pstRangeActionTimestamp2, stateTimestamp2));
        assertEquals(1, globalRangeActionActivationResult.getTapVariation(pstRangeActionTimestamp3, stateTimestamp3));
    }

    @Test
    void testOptimizedTapsOnState() {
        assertEquals(Map.of(pstRangeActionTimestamp1, 5), globalRangeActionActivationResult.getOptimizedTapsOnState(stateTimestamp1));
        assertEquals(Map.of(pstRangeActionTimestamp2, 8), globalRangeActionActivationResult.getOptimizedTapsOnState(stateTimestamp2));
        assertEquals(Map.of(pstRangeActionTimestamp3, 1), globalRangeActionActivationResult.getOptimizedTapsOnState(stateTimestamp3));
    }

    @Test
    void testOptimalSetPoint() {
        assertEquals(6.22, globalRangeActionActivationResult.getOptimizedSetpoint(pstRangeActionTimestamp1, stateTimestamp1));
        assertEquals(12.11, globalRangeActionActivationResult.getOptimizedSetpoint(pstRangeActionTimestamp2, stateTimestamp2));
        assertEquals(0.55, globalRangeActionActivationResult.getOptimizedSetpoint(pstRangeActionTimestamp3, stateTimestamp3));
    }

    @Test
    void testSetPointVariation() {
        assertEquals(6.22, globalRangeActionActivationResult.getSetPointVariation(pstRangeActionTimestamp1, stateTimestamp1));
        assertEquals(12.11, globalRangeActionActivationResult.getSetPointVariation(pstRangeActionTimestamp2, stateTimestamp2));
        assertEquals(0.55, globalRangeActionActivationResult.getSetPointVariation(pstRangeActionTimestamp3, stateTimestamp3));
    }

    @Test
    void testOptimizedSetPointsOnState() {
        assertEquals(Map.of(pstRangeActionTimestamp1, 6.22), globalRangeActionActivationResult.getOptimizedSetpointsOnState(stateTimestamp1));
        assertEquals(Map.of(pstRangeActionTimestamp2, 12.11), globalRangeActionActivationResult.getOptimizedSetpointsOnState(stateTimestamp2));
        assertEquals(Map.of(pstRangeActionTimestamp3, 0.55), globalRangeActionActivationResult.getOptimizedSetpointsOnState(stateTimestamp3));
    }

    @Test
    void testActivatedRangeActionsPerState() {
        Map<State, Set<RangeAction<?>>> activatedRangeActionsPerState = globalRangeActionActivationResult.getActivatedRangeActionsPerState();
        assertEquals(Set.of(stateTimestamp1, stateTimestamp2, stateTimestamp3), activatedRangeActionsPerState.keySet());
        assertEquals(Set.of(pstRangeActionTimestamp1), activatedRangeActionsPerState.get(stateTimestamp1));
        assertEquals(Set.of(pstRangeActionTimestamp2), activatedRangeActionsPerState.get(stateTimestamp2));
        assertEquals(Set.of(pstRangeActionTimestamp3), activatedRangeActionsPerState.get(stateTimestamp3));
    }
}
