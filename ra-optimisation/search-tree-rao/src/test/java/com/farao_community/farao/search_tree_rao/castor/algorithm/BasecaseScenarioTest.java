/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BasecaseScenarioTest {
    private State basecaseState;
    private State otherState1;
    private State otherState2;

    @Before
    public void setUp() {
        basecaseState = Mockito.mock(State.class);
        Mockito.when(basecaseState.getInstant()).thenReturn(Instant.PREVENTIVE);
        otherState1 = Mockito.mock(State.class);
        Mockito.when(otherState1.getInstant()).thenReturn(Instant.OUTAGE);
        otherState2 = Mockito.mock(State.class);
        Mockito.when(otherState2.getInstant()).thenReturn(Instant.CURATIVE);
    }

    @Test
    public void testConstructor() {
        BasecaseScenario basecaseScenario = new BasecaseScenario(basecaseState, Set.of(otherState1, otherState2));
        assertEquals(basecaseState, basecaseScenario.getBasecaseState());
        assertEquals(Set.of(otherState1, otherState2), basecaseScenario.getOtherStates());
        assertEquals(Set.of(basecaseState, otherState1, otherState2), basecaseScenario.getAllStates());
    }

    @Test
    public void testConstructor2() {
        BasecaseScenario basecaseScenario = new BasecaseScenario(basecaseState, Set.of());
        assertEquals(basecaseState, basecaseScenario.getBasecaseState());
        assertEquals(Set.of(), basecaseScenario.getOtherStates());
        assertEquals(Set.of(basecaseState), basecaseScenario.getAllStates());

        basecaseScenario = new BasecaseScenario(basecaseState, null);
        assertEquals(basecaseState, basecaseScenario.getBasecaseState());
        assertEquals(Set.of(), basecaseScenario.getOtherStates());
        assertEquals(Set.of(basecaseState), basecaseScenario.getAllStates());
    }

    @Test
    public void testWrongBasecaseScenario() {
        Set<State> otherStates = Set.of(otherState2);
        assertThrows(NullPointerException.class, () -> new BasecaseScenario(null, otherStates));
        assertThrows(FaraoException.class, () -> new BasecaseScenario(otherState1, otherStates));
    }

    @Test
    public void testWrongOtherScenario() {
        Set<State> otherStates = Set.of(basecaseState, otherState1);
        assertThrows(FaraoException.class, () -> new BasecaseScenario(basecaseState, otherStates));
    }

    @Test
    public void testAddOtherState() {
        BasecaseScenario basecaseScenario = new BasecaseScenario(basecaseState, null);
        assertEquals(Set.of(), basecaseScenario.getOtherStates());
        basecaseScenario.addOtherState(otherState1);
        assertEquals(Set.of(otherState1), basecaseScenario.getOtherStates());
        basecaseScenario.addOtherState(otherState2);
        assertEquals(Set.of(otherState1, otherState2), basecaseScenario.getOtherStates());
        basecaseScenario.addOtherState(otherState2);
        assertEquals(Set.of(otherState1, otherState2), basecaseScenario.getOtherStates());
        assertThrows(FaraoException.class, () -> basecaseScenario.addOtherState(basecaseState));
    }
}
