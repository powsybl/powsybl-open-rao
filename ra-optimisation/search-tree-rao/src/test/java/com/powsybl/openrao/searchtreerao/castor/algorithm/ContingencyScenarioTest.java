/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
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
        Mockito.when(contingency.getId()).thenReturn("contingency");

        Instant automatonInstant = Mockito.mock(Instant.class);
        Mockito.when(automatonInstant.isAuto()).thenReturn(true);
        Mockito.when(automatonInstant.getOrder()).thenReturn(2);
        automatonState = Mockito.mock(State.class);
        Mockito.when(automatonState.getInstant()).thenReturn(automatonInstant);
        Mockito.when(automatonState.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(automatonState.getId()).thenReturn("automatonState");

        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isCurative()).thenReturn(true);
        Mockito.when(automatonInstant.getOrder()).thenReturn(3);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(curativeState.getId()).thenReturn("curativeState");
    }

    @Test
    void test3ArgumentConstructor() {
        ContingencyScenario contingencyScenario = new ContingencyScenario(contingency, automatonState, List.of(curativeState));
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.of(automatonState), contingencyScenario.getAutomatonState());
        assertEquals(List.of(curativeState), contingencyScenario.getCurativeStates());
    }

    @Test
    void test2ArgumentConstructor() {
        ContingencyScenario contingencyScenario = new ContingencyScenario(automatonState, List.of(curativeState));
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.of(automatonState), contingencyScenario.getAutomatonState());
        assertEquals(List.of(curativeState), contingencyScenario.getCurativeStates());

        contingencyScenario = new ContingencyScenario(List.of(curativeState));
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.empty(), contingencyScenario.getAutomatonState());
        assertEquals(List.of(curativeState), contingencyScenario.getCurativeStates());
    }

    @Test
    void testNoContingency() {
        assertThrows(NullPointerException.class, () -> new ContingencyScenario(null, automatonState, List.of(curativeState)));
    }

    @Test
    void testNoCurative() {
        assertThrows(NullPointerException.class, () -> new ContingencyScenario(contingency, automatonState, null));
        assertThrows(NullPointerException.class, () -> new ContingencyScenario(automatonState, null));

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new ContingencyScenario(automatonState, List.of()));
        assertEquals("There should be at least one contingency state.", exception.getMessage());
    }

    @Test
    void testWrongAutoContingency() {
        Mockito.when(automatonState.getContingency()).thenReturn(Optional.empty());
        OpenRaoException emptyContingencyException = assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, List.of(curativeState)));
        assertEquals("State automatonState has no contingency.", emptyContingencyException.getMessage());

        Mockito.when(automatonState.getContingency()).thenReturn(Optional.of(Mockito.mock(Contingency.class)));
        OpenRaoException wrongContingencyException = assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, List.of(curativeState)));
        assertEquals("State automatonState does not refer to expected contingency contingency.", wrongContingencyException.getMessage());
    }

    @Test
    void testWrongCurativeContingency() {
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.empty());
        OpenRaoException emptyContingencyException = assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, List.of(curativeState)));
        assertEquals("State curativeState has no contingency.", emptyContingencyException.getMessage());

        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(Mockito.mock(Contingency.class)));
        OpenRaoException wrongContingencyException = assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, List.of(curativeState)));
        assertEquals("State curativeState does not refer to expected contingency contingency.", wrongContingencyException.getMessage());
    }

    @Test
    void testWrongInstantKind() {
        OpenRaoException notAutoException = assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, curativeState, List.of(automatonState)));
        assertEquals("State curativeState is not auto.", notAutoException.getMessage());

        OpenRaoException notCurativeException = assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, List.of(automatonState)));
        assertEquals("State automatonState is not curative.", notCurativeException.getMessage());
    }

    @Test
    void testSeveralCurativesStates() {
        Instant curativeInstant2 = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant2.isCurative()).thenReturn(true);
        Mockito.when(curativeInstant2.getOrder()).thenReturn(4);
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(curativeInstant2);
        Mockito.when(curativeState2.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(curativeState2.getId()).thenReturn("curativeState2");

        Instant curativeInstant3 = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant3.isCurative()).thenReturn(true);
        Mockito.when(curativeInstant3.getOrder()).thenReturn(5);
        State curativeState3 = Mockito.mock(State.class);
        Mockito.when(curativeState3.getInstant()).thenReturn(curativeInstant3);
        Mockito.when(curativeState3.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(curativeState3.getId()).thenReturn("curativeState3");

        // Initialise with unordered curative instants and check they are ordered by the constructor
        ContingencyScenario contingencyScenario = new ContingencyScenario(contingency, automatonState, List.of(curativeState3, curativeState, curativeState2));
        assertEquals(3, contingencyScenario.getCurativeStates().size());
        assertEquals("curativeState", contingencyScenario.getCurativeStates().get(0).getId());
        assertEquals("curativeState2", contingencyScenario.getCurativeStates().get(1).getId());
        assertEquals("curativeState3", contingencyScenario.getCurativeStates().get(2).getId());
    }

}
