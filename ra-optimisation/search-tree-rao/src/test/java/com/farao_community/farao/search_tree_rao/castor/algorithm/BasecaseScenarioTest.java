/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.CracImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class BasecaseScenarioTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private State basecaseState;
    private State otherState1;
    private State otherState2;

    @BeforeEach
    public void setUp() {
        Crac crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        Instant preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        basecaseState = Mockito.mock(State.class);
        Mockito.when(basecaseState.getInstant()).thenReturn(preventiveInstant);
        otherState1 = Mockito.mock(State.class);
        Mockito.when(otherState1.getInstant()).thenReturn(outageInstant);
        Mockito.when(otherState1.toString()).thenReturn("Other state 1");
        otherState2 = Mockito.mock(State.class);
        Mockito.when(otherState2.getInstant()).thenReturn(curativeInstant);
    }

    @Test
    void testConstructor() {
        BasecaseScenario basecaseScenario = new BasecaseScenario(basecaseState, Set.of(otherState1, otherState2));
        assertEquals(basecaseState, basecaseScenario.getBasecaseState());
        assertEquals(Set.of(otherState1, otherState2), basecaseScenario.getOtherStates());
        assertEquals(Set.of(basecaseState, otherState1, otherState2), basecaseScenario.getAllStates());
    }

    @Test
    void testConstructor2() {
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
    void testWrongBasecaseScenario() {
        Set<State> otherStates = Set.of(otherState2);
        assertThrows(NullPointerException.class, () -> new BasecaseScenario(null, otherStates));
        FaraoException exception = assertThrows(FaraoException.class, () -> new BasecaseScenario(otherState1, otherStates));
        assertEquals("Basecase state `Other state 1` is not preventive", exception.getMessage());
    }

    @Test
    void testWrongOtherScenario() {
        Set<State> otherStates = Set.of(basecaseState, otherState1);
        FaraoException exception = assertThrows(FaraoException.class, () -> new BasecaseScenario(basecaseState, otherStates));
        assertEquals("OtherStates should not be preventive", exception.getMessage());
    }

    @Test
    void testAddOtherState() {
        BasecaseScenario basecaseScenario = new BasecaseScenario(basecaseState, null);
        assertEquals(Set.of(), basecaseScenario.getOtherStates());
        basecaseScenario.addOtherState(otherState1);
        assertEquals(Set.of(otherState1), basecaseScenario.getOtherStates());
        basecaseScenario.addOtherState(otherState2);
        assertEquals(Set.of(otherState1, otherState2), basecaseScenario.getOtherStates());
        basecaseScenario.addOtherState(otherState2);
        assertEquals(Set.of(otherState1, otherState2), basecaseScenario.getOtherStates());
        FaraoException exception = assertThrows(FaraoException.class, () -> basecaseScenario.addOtherState(basecaseState));
        assertEquals("OtherStates should not be preventive", exception.getMessage());
    }
}
