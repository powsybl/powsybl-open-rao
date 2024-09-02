/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PerimeterTest {
    private State basecaseState;
    private State otherState1;
    private State otherState2;

    @BeforeEach
    public void setUp() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.isPreventive()).thenReturn(true);
        Instant outageInstant = Mockito.mock(Instant.class);
        Instant curativeInstant = Mockito.mock(Instant.class);

        basecaseState = Mockito.mock(State.class);
        Mockito.when(basecaseState.getInstant()).thenReturn(preventiveInstant);
        otherState1 = Mockito.mock(State.class);
        Mockito.when(otherState1.getInstant()).thenReturn(outageInstant);
        Mockito.when(otherState1.toString()).thenReturn("Other state 1");
        otherState2 = Mockito.mock(State.class);
        Mockito.when(otherState2.getInstant()).thenReturn(curativeInstant);
        Mockito.when(preventiveInstant.comesBefore(outageInstant)).thenReturn(true);
        Mockito.when(outageInstant.comesAfter(preventiveInstant)).thenReturn(true);
        Mockito.when(preventiveInstant.comesBefore(curativeInstant)).thenReturn(true);
        Mockito.when(curativeInstant.comesAfter(preventiveInstant)).thenReturn(true);
        Mockito.when(outageInstant.comesBefore(curativeInstant)).thenReturn(true);
        Mockito.when(curativeInstant.comesAfter(outageInstant)).thenReturn(true);
    }

    @Test
    void testConstructor() {
        Perimeter preventivePerimeter = new Perimeter(basecaseState, Set.of(otherState1, otherState2));
        assertEquals(basecaseState, preventivePerimeter.getRaOptimisationState());
        assertEquals(Set.of(basecaseState, otherState1, otherState2), preventivePerimeter.getAllStates());
    }

    @Test
    void testConstructor2() {
        Perimeter preventivePerimeter = new Perimeter(basecaseState, Set.of());
        assertEquals(basecaseState, preventivePerimeter.getRaOptimisationState());
        assertEquals(Set.of(basecaseState), preventivePerimeter.getAllStates());

        preventivePerimeter = new Perimeter(basecaseState, null);
        assertEquals(basecaseState, preventivePerimeter.getRaOptimisationState());
        assertEquals(Set.of(basecaseState), preventivePerimeter.getAllStates());
    }

    @Test
    void testNullRaOptimizationState() {
        Set<State> stateSet = Set.of(otherState1);
        assertThrows(NullPointerException.class, () -> new Perimeter(null, stateSet));
    }

    @Test
    void testAddCnecStateOccurringAfterRaOptimizationState() {
        Set<State> otherStateSet = Set.of(otherState1);
        OpenRaoException exception1 = assertThrows(OpenRaoException.class, () -> new Perimeter(otherState2, otherStateSet));
        assertEquals("Other states should occur after the optimisation state.", exception1.getMessage());
        Perimeter perimeter = new Perimeter(otherState2, Set.of());
        OpenRaoException exception2 = assertThrows(OpenRaoException.class, () -> perimeter.addOtherState(otherState1));
        assertEquals("Other states should occur after the optimisation state.", exception2.getMessage());
    }

    @Test
    void testAddOtherState() {
        Perimeter preventivePerimeter = new Perimeter(basecaseState, null);
        assertEquals(Set.of(basecaseState), preventivePerimeter.getAllStates());
        preventivePerimeter.addOtherState(otherState1);
        assertEquals(Set.of(basecaseState, otherState1), preventivePerimeter.getAllStates());
        preventivePerimeter.addOtherState(otherState2);
        assertEquals(Set.of(basecaseState, otherState1, otherState2), preventivePerimeter.getAllStates());
        preventivePerimeter.addOtherState(otherState2);
        assertEquals(Set.of(basecaseState, otherState1, otherState2), preventivePerimeter.getAllStates());
    }

    @Test
    void testAddRemedialActionStateToCnecsStates1() {
        Perimeter preventivePerimeter = new Perimeter(basecaseState, new HashSet<>());
        preventivePerimeter.addOtherState(basecaseState);
        assertEquals(Set.of(basecaseState), preventivePerimeter.getAllStates());
    }

    @Test
    void testAddRemedialActionStateToCnecsStates2() {
        Perimeter preventivePerimeter = new Perimeter(basecaseState, Set.of(basecaseState));
        assertEquals(Set.of(basecaseState), preventivePerimeter.getAllStates());
    }
}
