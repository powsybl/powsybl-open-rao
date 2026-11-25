/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.result.impl;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SkippedOptimizationResultImplTest {

    @Test
    void testBasicReturns() {
        FlowCnec flowCnec = mock(FlowCnec.class);
        TwoSides side = mock(TwoSides.class);
        Unit unit = mock(Unit.class);
        SensitivityVariableSet sensitivityVariableSet = mock(SensitivityVariableSet.class);
        State state = mock(State.class);
        PstRangeAction pstRangeAction = mock(PstRangeAction.class);
        NetworkAction networkAction = mock(NetworkAction.class);
        RangeAction<?> rangeAction = mock(RangeAction.class);

        // DEFAULT_SENSITIVITY_FAILURE_OVERCOST value
        double sensitivityFailureOverCost = 10000;
        SkippedOptimizationResultImpl skippedOptimizationResult = new SkippedOptimizationResultImpl(state, sensitivityFailureOverCost);

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
        assertThrows(OpenRaoException.class, () -> skippedOptimizationResult.getOptimizedTap(pstRangeAction, state));

        assertTrue(skippedOptimizationResult.getOptimizedSetpointsOnState(state).isEmpty());
        assertTrue(skippedOptimizationResult.getActivatedNetworkActions().isEmpty());
        assertTrue(skippedOptimizationResult.getActivatedRangeActions(state).isEmpty());
        assertTrue(skippedOptimizationResult.getRangeActions().isEmpty());
        assertTrue(skippedOptimizationResult.getActivatedNetworkActionsPerState().isEmpty());
        assertTrue(skippedOptimizationResult.getActivatedRangeActionsPerState().isEmpty());
        assertTrue(skippedOptimizationResult.getOptimizedTapsOnState(state).isEmpty());
        assertFalse(skippedOptimizationResult.isActivated(networkAction));
    }
}
