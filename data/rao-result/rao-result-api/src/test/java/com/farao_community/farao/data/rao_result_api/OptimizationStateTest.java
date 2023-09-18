/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_api;

import com.farao_community.farao.data.crac_api.State;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;
import static com.farao_community.farao.data.crac_api.Instant.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OptimizationStateTest {
    @Test
    void testBuildFromInstant() {
        assertEquals(INITIAL, OptimizationState.beforeOptimizing(PREVENTIVE));
        assertEquals(AFTER_PRA, OptimizationState.afterOptimizing(PREVENTIVE));

        assertEquals(INITIAL, OptimizationState.beforeOptimizing(OUTAGE));
        assertEquals(AFTER_PRA, OptimizationState.afterOptimizing(OUTAGE));

        assertEquals(AFTER_PRA, OptimizationState.beforeOptimizing(AUTO));
        assertEquals(AFTER_ARA, OptimizationState.afterOptimizing(AUTO));

        assertEquals(AFTER_CRA2, OptimizationState.beforeOptimizing(CURATIVE));
        assertEquals(AFTER_CRA, OptimizationState.afterOptimizing(CURATIVE));
    }

    @Test
    void testBuildFromState() {
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
        assertEquals(AFTER_CRA2, OptimizationState.beforeOptimizing(state));
        assertEquals(AFTER_CRA, OptimizationState.afterOptimizing(state));
    }

    @Test
    void testGetFirstInstant() {
        assertEquals(PREVENTIVE, INITIAL.getFirstInstant());
        assertEquals(PREVENTIVE, AFTER_PRA.getFirstInstant());
        assertEquals(AUTO, AFTER_ARA.getFirstInstant());
        assertEquals(CURATIVE, AFTER_CRA.getFirstInstant());
    }

    @Test
    void testToString() {
        assertEquals("initial", INITIAL.toString());
        assertEquals("after PRA", AFTER_PRA.toString());
        assertEquals("after ARA", AFTER_ARA.toString());
        assertEquals("after CRA", AFTER_CRA.toString());
    }

    @Test
    void testMin() {
        assertEquals(INITIAL, OptimizationState.min(INITIAL, INITIAL));
        assertEquals(INITIAL, OptimizationState.min(INITIAL, AFTER_PRA));
        assertEquals(INITIAL, OptimizationState.min(INITIAL, AFTER_ARA));
        assertEquals(INITIAL, OptimizationState.min(INITIAL, AFTER_CRA));

        assertEquals(AFTER_PRA, OptimizationState.min(AFTER_PRA, AFTER_PRA));
        assertEquals(AFTER_PRA, OptimizationState.min(AFTER_PRA, AFTER_ARA));
        assertEquals(AFTER_PRA, OptimizationState.min(AFTER_PRA, AFTER_CRA));

        assertEquals(AFTER_ARA, OptimizationState.min(AFTER_ARA, AFTER_ARA));
        assertEquals(AFTER_ARA, OptimizationState.min(AFTER_ARA, AFTER_CRA));

        assertEquals(AFTER_CRA, OptimizationState.min(AFTER_CRA, AFTER_CRA));
    }
}
