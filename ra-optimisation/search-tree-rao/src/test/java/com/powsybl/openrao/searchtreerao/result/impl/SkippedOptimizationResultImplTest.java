/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SkippedOptimizationResultImplTest {
    private final double sensitivityFailureOverCost = 10000; // DEFAULT_SENSITIVITY_FAILURE_OVERCOST value

    @Test
    void testBasicReturns() {
        FlowCnec flowCnec = mock(FlowCnec.class);
        TwoSides side = mock(TwoSides.class);
        Unit unit = mock(Unit.class);
        SensitivityVariableSet sensitivityVariableSet = mock(SensitivityVariableSet.class);
        State state = mock(State.class);
        PstRangeAction pstRangeAction = mock(PstRangeAction.class);
        RangeAction<?> rangeAction = mock(RangeAction.class);

        SkippedOptimizationResultImpl skippedOptimizationResult = new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE, sensitivityFailureOverCost);

        assertEquals(ComputationStatus.FAILURE, skippedOptimizationResult.getSensitivityStatus());
        assertEquals(ComputationStatus.FAILURE, skippedOptimizationResult.getSensitivityStatus(state));
        assertTrue(skippedOptimizationResult.getContingencies().isEmpty());
        assertTrue(skippedOptimizationResult.getMostLimitingElements(0).isEmpty());
        assertTrue(skippedOptimizationResult.getMostLimitingElements(10).isEmpty());
        assertEquals(sensitivityFailureOverCost, skippedOptimizationResult.getVirtualCost(), 1e-6);
        assertEquals(0, skippedOptimizationResult.getVirtualCost("emptyString"), 1e-6);
        assertEquals(10000, skippedOptimizationResult.getVirtualCost("sensitivity-failure-cost"), 1e-6);
        assertEquals(1, skippedOptimizationResult.getVirtualCostNames().size());
        assertEquals("sensitivity-failure-cost", skippedOptimizationResult.getVirtualCostNames().iterator().next());
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getSensitivityValue(flowCnec, side, rangeAction, unit));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getSensitivityValue(flowCnec, side, sensitivityVariableSet, unit));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getFlow(flowCnec, side, unit));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getCommercialFlow(flowCnec, side, unit));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getPtdfZonalSum(flowCnec, side));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getPtdfZonalSums());
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getCostlyElements("emptyString", 10));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getOptimizedSetpoint(rangeAction, state));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getOptimizedSetpointsOnState(state));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getOptimizedTap(pstRangeAction, state));
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getOptimizedTapsOnState(state));
        assertThrows(OpenRaoException.class, skippedOptimizationResult::getObjectiveFunction);
    }

    @Test
    void testDefaultStatus() {
        State state = mock(State.class);
        Optional<Contingency> optContingency = mock(Optional.class);
        Contingency contingency = mock(Contingency.class);
        Mockito.when(state.getContingency()).thenReturn(optContingency);
        Mockito.when(optContingency.isPresent()).thenReturn(true);
        Mockito.when(optContingency.get()).thenReturn(contingency);
        Mockito.when(contingency.getId()).thenReturn("contingencyId");

        SkippedOptimizationResultImpl skippedOptimizationResult = new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), ComputationStatus.DEFAULT, sensitivityFailureOverCost);
        assertEquals(ComputationStatus.DEFAULT, skippedOptimizationResult.getSensitivityStatus());
        assertEquals(Set.of("contingencyId"), skippedOptimizationResult.getContingencies());
    }

    @Test
    void testActivation() {
        State state = mock(State.class);
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        NetworkAction na3 = Mockito.mock(NetworkAction.class);
        RangeAction<?> ra1 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra2 = Mockito.mock(RangeAction.class);
        Set<NetworkAction> networkActions = Set.of(na1, na2);
        Set<RangeAction<?>> rangeActions = Set.of(ra1, ra2);
        SkippedOptimizationResultImpl skippedOptimizationResult = new SkippedOptimizationResultImpl(state, networkActions, rangeActions, ComputationStatus.DEFAULT, sensitivityFailureOverCost);
        assertEquals(networkActions, skippedOptimizationResult.getActivatedNetworkActions());
        assertTrue(skippedOptimizationResult.isActivated(na1));
        assertTrue(skippedOptimizationResult.isActivated(na2));
        assertFalse(skippedOptimizationResult.isActivated(na3));
        assertEquals(rangeActions, skippedOptimizationResult.getRangeActions());
        assertEquals(rangeActions, skippedOptimizationResult.getActivatedRangeActions(state));
    }
}
