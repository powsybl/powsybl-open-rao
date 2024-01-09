/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.castor.algorithm;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.Contingency;
import com.powsybl.open_rao.data.crac_api.State;
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
        assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, curativeState));

        Mockito.when(automatonState.getContingency()).thenReturn(Optional.of(Mockito.mock(Contingency.class)));
        assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, curativeState));
    }

    @Test
    void testWrongCurativeContingency() {
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.empty());
        assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, curativeState));

        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(Mockito.mock(Contingency.class)));
        assertThrows(OpenRaoException.class, () -> new ContingencyScenario(contingency, automatonState, curativeState));
    }

}
