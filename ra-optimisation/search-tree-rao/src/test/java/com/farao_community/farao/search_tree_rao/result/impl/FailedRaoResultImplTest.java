/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class FailedRaoResultImplTest {
    @Test
    public void testBasicReturns() {
        OptimizationState optimizationState = mock(OptimizationState.class);
        State state = mock(State.class);
        PstRangeAction pstRangeAction = mock(PstRangeAction.class);
        RangeAction rangeAction = mock(RangeAction.class);
        NetworkAction networkAction = mock(NetworkAction.class);

        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();

        assertEquals(ComputationStatus.FAILURE, failedRaoResultImpl.getComputationStatus());
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPerimeterResult(optimizationState, state));
        assertThrows(FaraoException.class, failedRaoResultImpl::getPostPreventivePerimeterResult);
        assertThrows(FaraoException.class, failedRaoResultImpl::getInitialResult);

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFunctionalCost(optimizationState));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMostLimitingElements(optimizationState, 10));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVirtualCost(optimizationState));
        assertThrows(FaraoException.class, failedRaoResultImpl::getVirtualCostNames);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVirtualCost(optimizationState, "sensitivity-fallback-cost"));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getCostlyElements(optimizationState, "sensitivity-fallback-cost", 10));

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.wasActivatedBeforeState(state, networkAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, networkAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getActivatedNetworkActionsDuringState(state));

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPreOptimizationTapOnState(state, pstRangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedTapOnState(state, pstRangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPreOptimizationSetPointOnState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedSetPointOnState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getActivatedRangeActionsDuringState(state));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedTapsOnState(state));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedSetPointsOnState(state));
    }
}
