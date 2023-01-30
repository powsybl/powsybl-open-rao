/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SkippedOptimizationResultImplTest {
    @Test
    public void testBasicReturns() {
        FlowCnec flowCnec = mock(FlowCnec.class);
        Side side = mock(Side.class);
        Unit unit = mock(Unit.class);
        SensitivityVariableSet sensitivityVariableSet = mock(SensitivityVariableSet.class);
        State state = mock(State.class);
        PstRangeAction pstRangeAction = mock(PstRangeAction.class);
        RangeAction rangeAction = mock(RangeAction.class);

        SkippedOptimizationResultImpl skippedOptimizationResult = new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE);

        assertEquals(ComputationStatus.FAILURE, skippedOptimizationResult.getSensitivityStatus());
        assertEquals(ComputationStatus.FAILURE, skippedOptimizationResult.getSensitivityStatus(state));
        assertTrue(skippedOptimizationResult.getContingencies().isEmpty());
        assertTrue(skippedOptimizationResult.getMostLimitingElements(0).isEmpty());
        assertTrue(skippedOptimizationResult.getMostLimitingElements(10).isEmpty());
        assertEquals(0, skippedOptimizationResult.getVirtualCost(), 1e-6);
        assertEquals(0, skippedOptimizationResult.getVirtualCost("emptyString"), 1e-6);
        assertTrue(skippedOptimizationResult.getVirtualCostNames().isEmpty());
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getSensitivityValue(flowCnec, side, rangeAction, unit));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getSensitivityValue(flowCnec, side, sensitivityVariableSet, unit));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getFlow(flowCnec, side, unit));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getCommercialFlow(flowCnec, side, unit));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getPtdfZonalSum(flowCnec, side));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getCostlyElements("emptyString", 10));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getOptimizedSetpoint(rangeAction, state));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getOptimizedSetpointsOnState(state));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getOptimizedTap(pstRangeAction, state));
        assertThrows(FaraoException.class, () -> skippedOptimizationResult.getOptimizedTapsOnState(state));
        assertThrows(FaraoException.class, skippedOptimizationResult::getFunctionalCost);
        assertThrows(FaraoException.class, skippedOptimizationResult::getObjectiveFunction);
    }
}
