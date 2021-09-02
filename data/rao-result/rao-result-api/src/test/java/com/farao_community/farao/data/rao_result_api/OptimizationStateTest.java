/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_api;

import com.farao_community.farao.data.crac_api.State;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;
import static com.farao_community.farao.data.crac_api.Instant.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OptimizationStateTest {
    @Test
    public void testBuildFromInstant() {
        assertEquals(INITIAL, OptimizationState.beforeOptimizing(PREVENTIVE));
        assertEquals(AFTER_PRA, OptimizationState.afterOptimizing(PREVENTIVE));

        assertEquals(INITIAL, OptimizationState.beforeOptimizing(OUTAGE));
        assertEquals(AFTER_PRA, OptimizationState.afterOptimizing(OUTAGE));

        assertEquals(AFTER_PRA, OptimizationState.beforeOptimizing(AUTO));
        assertEquals(AFTER_ARA, OptimizationState.afterOptimizing(AUTO));

        assertEquals(AFTER_ARA, OptimizationState.beforeOptimizing(CURATIVE));
        assertEquals(AFTER_CRA, OptimizationState.afterOptimizing(CURATIVE));
    }

    @Test
    public void testBuildFromState() {
        State state = Mockito.mock(State.class);

        Mockito.when(state.getInstant()).thenReturn(PREVENTIVE);
        assertEquals(INITIAL, OptimizationState.beforeOptimizing(state));
        assertEquals(AFTER_PRA, OptimizationState.afterOptimizing(state));

        Mockito.when(state.getInstant()).thenReturn(OUTAGE);
        assertEquals(INITIAL, OptimizationState.beforeOptimizing(state));
        assertEquals(AFTER_PRA, OptimizationState.afterOptimizing(state));

        Mockito.when(state.getInstant()).thenReturn(AUTO);
        assertEquals(AFTER_PRA, OptimizationState.beforeOptimizing(state));
        assertEquals(AFTER_ARA, OptimizationState.afterOptimizing(state));

        Mockito.when(state.getInstant()).thenReturn(CURATIVE);
        assertEquals(AFTER_ARA, OptimizationState.beforeOptimizing(state));
        assertEquals(AFTER_CRA, OptimizationState.afterOptimizing(state));
    }
}
