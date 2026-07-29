/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
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
        Mockito.when(automatonInstant.getKind()).thenReturn(InstantKind.AUTO);
        automatonState = Mockito.mock(State.class);
        Mockito.when(automatonState.getInstant()).thenReturn(automatonInstant);
        Mockito.when(automatonState.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(automatonState.getId()).thenReturn("automatonState");

        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.getKind()).thenReturn(InstantKind.CURATIVE);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(curativeState.getId()).thenReturn("curativeState");
    }

    @Test
    void testWithAutoAndCurative() {
        Perimeter curativePerimeter = new Perimeter(curativeState, null);
        ContingencyScenario contingencyScenario = ContingencyScenario.create()
            .withContingency(contingency)
            .withAutomatonState(automatonState)
            .withCurativePerimeter(curativePerimeter)
            .build();
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.of(automatonState), contingencyScenario.getAutomatonState());
        assertEquals(List.of(curativePerimeter), contingencyScenario.getCurativePerimeters());
    }

    @Test
    void testCurativeOnly() {
        Perimeter curativePerimeter = new Perimeter(curativeState, null);
        ContingencyScenario contingencyScenario = ContingencyScenario.create()
            .withContingency(contingency)
            .withCurativePerimeter(curativePerimeter)
            .build();
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.empty(), contingencyScenario.getAutomatonState());
        assertEquals(List.of(curativePerimeter), contingencyScenario.getCurativePerimeters());
    }

    @Test
    void testAutoOnly() {
        ContingencyScenario contingencyScenario = ContingencyScenario.create()
            .withContingency(contingency)
            .withAutomatonState(automatonState)
            .build();
        assertEquals(contingency, contingencyScenario.getContingency());
        assertEquals(Optional.of(automatonState), contingencyScenario.getAutomatonState());
        assertEquals(List.of(), contingencyScenario.getCurativePerimeters());
    }

    @Test
    void testNoContingency() {
        Perimeter curativePerimeter = new Perimeter(curativeState, null);
        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create()
            .withAutomatonState(automatonState)
            .withCurativePerimeter(curativePerimeter);
        assertThrows(NullPointerException.class, contingencyScenarioBuilder::build);
    }

    @Test
    void testContingencyOnly() {
        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create()
            .withContingency(contingency);
        OpenRaoException openRaoException = assertThrows(OpenRaoException.class, contingencyScenarioBuilder::build);
        assertEquals("Contingency contingency scenario should have at least an auto or curative state.",
            openRaoException.getMessage());
    }

    @Test
    void testWrongAutoContingency() {
        Mockito.when(automatonState.getContingency()).thenReturn(Optional.empty());
        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create()
            .withContingency(contingency)
            .withAutomatonState(automatonState);
        OpenRaoException openRaoException = assertThrows(OpenRaoException.class, contingencyScenarioBuilder::build);
        assertEquals("State automatonState does not refer to the contingency contingency.",
            openRaoException.getMessage());
    }

    @Test
    void testWrongCurativeContingency() {
        Perimeter curativePerimeter = new Perimeter(curativeState, null);
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.empty());
        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create()
            .withContingency(contingency)
            .withCurativePerimeter(curativePerimeter);
        OpenRaoException openRaoException = assertThrows(OpenRaoException.class, contingencyScenarioBuilder::build);
        assertEquals("State curativeState does not refer to the contingency contingency.",
            openRaoException.getMessage());
    }

    @Test
    void testWrongAutoInstantKind() {
        Mockito.when(automatonState.getInstant().getKind()).thenReturn(InstantKind.PREVENTIVE);
        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create()
            .withContingency(contingency)
            .withAutomatonState(automatonState);
        OpenRaoException openRaoException = assertThrows(OpenRaoException.class, contingencyScenarioBuilder::build);
        assertEquals("Instant of state automatonState is not of kind AUTO.",
            openRaoException.getMessage());
    }

    @Test
    void testWrongCurativeInstantKind() {
        Perimeter curativePerimeter = new Perimeter(curativeState, null);
        Mockito.when(curativeState.getInstant().getKind()).thenReturn(InstantKind.PREVENTIVE);
        ContingencyScenario.ContingencyScenarioBuilder contingencyScenarioBuilder = ContingencyScenario.create()
            .withContingency(contingency)
            .withCurativePerimeter(curativePerimeter);
        OpenRaoException openRaoException = assertThrows(OpenRaoException.class, contingencyScenarioBuilder::build);
        assertEquals("Instant of state curativeState is not of kind CURATIVE.",
            openRaoException.getMessage());
    }
}
