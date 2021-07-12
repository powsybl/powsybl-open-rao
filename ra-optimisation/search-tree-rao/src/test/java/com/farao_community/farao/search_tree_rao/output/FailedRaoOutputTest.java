/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.output;

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
public class FailedRaoOutputTest {
    @Test
    public void testBasicReturns() {
        OptimizationState optimizationState = mock(OptimizationState.class);
        State state = mock(State.class);
        PstRangeAction pstRangeAction = mock(PstRangeAction.class);
        RangeAction rangeAction = mock(RangeAction.class);
        NetworkAction networkAction = mock(NetworkAction.class);

        FailedRaoOutput failedRaoOutput = new FailedRaoOutput();

        assertEquals(ComputationStatus.FAILURE, failedRaoOutput.getComputationStatus());
        assertThrows(FaraoException.class, () -> failedRaoOutput.getPerimeterResult(optimizationState, state));
        assertThrows(FaraoException.class, failedRaoOutput::getPostPreventivePerimeterResult);
        assertThrows(FaraoException.class, failedRaoOutput::getInitialResult);

        assertThrows(FaraoException.class, () -> failedRaoOutput.getFunctionalCost(optimizationState));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getMostLimitingElements(optimizationState, 10));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getVirtualCost(optimizationState));
        assertThrows(FaraoException.class, failedRaoOutput::getVirtualCostNames);
        assertThrows(FaraoException.class, () -> failedRaoOutput.getVirtualCost(optimizationState, "sensitivity-fallback-cost"));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getCostlyElements(optimizationState, "sensitivity-fallback-cost", 10));

        assertThrows(FaraoException.class, () -> failedRaoOutput.wasActivatedBeforeState(state, networkAction));
        assertThrows(FaraoException.class, () -> failedRaoOutput.isActivatedDuringState(state, networkAction));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getActivatedNetworkActionsDuringState(state));

        assertThrows(FaraoException.class, () -> failedRaoOutput.isActivatedDuringState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getPreOptimizationTapOnState(state, pstRangeAction));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getOptimizedTapOnState(state, pstRangeAction));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getPreOptimizationSetPointOnState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getOptimizedSetPointOnState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getActivatedRangeActionsDuringState(state));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getOptimizedTapsOnState(state));
        assertThrows(FaraoException.class, () -> failedRaoOutput.getOptimizedSetPointsOnState(state));
    }
}
