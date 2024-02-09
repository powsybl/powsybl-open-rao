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
        Mockito.when(preventiveInstant.comesBefore(curativeInstant)).thenReturn(true);
    }

    @Test
    void testConstructor() {
        Perimeter preventivePerimeter = new Perimeter(basecaseState, Set.of(otherState1, otherState2));
        assertEquals(basecaseState, preventivePerimeter.getOptimisationState());
        assertEquals(Set.of(otherState1, otherState2), preventivePerimeter.getOtherStates());
        assertEquals(Set.of(basecaseState, otherState1, otherState2), preventivePerimeter.getAllStates());
    }

    @Test
    void testConstructor2() {
        Perimeter preventivePerimeter = new Perimeter(basecaseState, Set.of());
        assertEquals(basecaseState, preventivePerimeter.getOptimisationState());
        assertEquals(Set.of(), preventivePerimeter.getOtherStates());
        assertEquals(Set.of(basecaseState), preventivePerimeter.getAllStates());

        preventivePerimeter = new Perimeter(basecaseState, null);
        assertEquals(basecaseState, preventivePerimeter.getOptimisationState());
        assertEquals(Set.of(), preventivePerimeter.getOtherStates());
        assertEquals(Set.of(basecaseState), preventivePerimeter.getAllStates());
    }

    @Test
    void testWrongBasecaseScenario() {
        Set<State> otherStates = Set.of(otherState2);
        assertThrows(NullPointerException.class, () -> new Perimeter(null, otherStates));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new Perimeter(otherState1, otherStates));
        assertEquals("Other states should occur after the optimisation state.", exception.getMessage());
    }

    @Test
    void testWrongOtherScenario() {
        Set<State> otherStates = Set.of(basecaseState, otherState1);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new Perimeter(basecaseState, otherStates));
        assertEquals("Other states should occur after the optimisation state.", exception.getMessage());
    }

    @Test
    void testAddOtherState() {
        Perimeter preventivePerimeter = new Perimeter(basecaseState, null);
        assertEquals(Set.of(), preventivePerimeter.getOtherStates());
        preventivePerimeter.addOtherState(otherState1);
        assertEquals(Set.of(otherState1), preventivePerimeter.getOtherStates());
        preventivePerimeter.addOtherState(otherState2);
        assertEquals(Set.of(otherState1, otherState2), preventivePerimeter.getOtherStates());
        preventivePerimeter.addOtherState(otherState2);
        assertEquals(Set.of(otherState1, otherState2), preventivePerimeter.getOtherStates());
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> preventivePerimeter.addOtherState(basecaseState));
        assertEquals("Other states should occur after the optimisation state.", exception.getMessage());
    }
}
