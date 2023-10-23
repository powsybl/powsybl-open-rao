/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class ContingencyScenarioTest {
    private Contingency contingency;
    private State automatonState;
    private State curativeState;

    @BeforeEach
    public void setUp() {
        contingency = Mockito.mock(Contingency.class);
        automatonState = Mockito.mock(State.class);
        Mockito.when(automatonState.getContingency()).thenReturn(Optional.of(contingency));
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(contingency));
    }

    @Test
    void test3ArgumentConstructor() {
        ContingencyScenario contingencyScenario = new ContingencyScenario(contingency, automatonState, curativeState);
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.of(automatonState), contingencyScenario.getAutomatonState());
        assertEquals(curativeState, contingencyScenario.getCurativeState());
    }

    @Test
    void test2ArgumentConstructor() {
        ContingencyScenario contingencyScenario = new ContingencyScenario(automatonState, curativeState);
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.of(automatonState), contingencyScenario.getAutomatonState());
        assertEquals(curativeState, contingencyScenario.getCurativeState());

        contingencyScenario = new ContingencyScenario(null, curativeState);
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.empty(), contingencyScenario.getAutomatonState());
        assertEquals(curativeState, contingencyScenario.getCurativeState());
    }

    @Test
    void testNoContingency() {
        assertThrows(NullPointerException.class, () -> new ContingencyScenario(null, automatonState, curativeState));
    }

    @Test
    void testNoCurative() {
        assertThrows(NullPointerException.class, () -> new ContingencyScenario(contingency, automatonState, null));
        assertThrows(NullPointerException.class, () -> new ContingencyScenario(automatonState, null));
    }

    @Test
    void testWrongAutoContingency() {
        Mockito.when(automatonState.getContingency()).thenReturn(Optional.empty());
        FaraoException exception = assertThrows(FaraoException.class, () -> new ContingencyScenario(contingency, automatonState, curativeState));
        assertEquals("Automaton state Mock for State, hashCode: 309135464 do not refer to the contingency Mock for Contingency, hashCode: 1144702392", exception.getMessage());

        Mockito.when(automatonState.getContingency()).thenReturn(Optional.of(Mockito.mock(Contingency.class)));
        exception = assertThrows(FaraoException.class, () -> new ContingencyScenario(contingency, automatonState, curativeState));
        assertEquals("", exception.getMessage());
    }

    @Test
    void testWrongCurativeContingency() {
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.empty());
        FaraoException exception = assertThrows(FaraoException.class, () -> new ContingencyScenario(contingency, automatonState, curativeState));
        assertEquals("Curative state Mock for State, hashCode: 2138645808 do not refer to the contingency Mock for Contingency, hashCode: 550764532", exception.getMessage());

        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(Mockito.mock(Contingency.class)));
        exception = assertThrows(FaraoException.class, () -> new ContingencyScenario(contingency, automatonState, curativeState));
        assertEquals("", exception.getMessage());
    }

}
