/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class AutomatonBatchTest {
    private NetworkAction topologicalAutomaton;
    private RangeAction<?> rangeAutomaton;
    private PstRangeAction pstAutomaton;

    @BeforeEach
    void setUp() {
        topologicalAutomaton = Mockito.mock(NetworkAction.class);
        Mockito.when(topologicalAutomaton.getSpeed()).thenReturn(Optional.of(5));
        rangeAutomaton = Mockito.mock(RangeAction.class);
        Mockito.when(rangeAutomaton.getSpeed()).thenReturn(Optional.of(5));
        pstAutomaton = Mockito.mock(PstRangeAction.class);
        Mockito.when(pstAutomaton.getSpeed()).thenReturn(Optional.of(2));
        Mockito.when(pstAutomaton.getId()).thenReturn("pst-automaton");
    }

    @Test
    void testCreateEmptyBatch() {
        AutomatonBatch automatonBatch = new AutomatonBatch(2);
        assertEquals(2, automatonBatch.getTimeAfterOutage());
        assertTrue(automatonBatch.getTopologicalAutomatons().isEmpty());
        assertTrue(automatonBatch.getRangeAutomatons().isEmpty());
    }

    @Test
    void testAddAutomatonsToBatch() {
        AutomatonBatch automatonBatch = new AutomatonBatch(5);
        automatonBatch.add(topologicalAutomaton);
        automatonBatch.add(rangeAutomaton);
        assertEquals(Set.of(topologicalAutomaton), automatonBatch.getTopologicalAutomatons());
        assertEquals(Set.of(rangeAutomaton), automatonBatch.getRangeAutomatons());
    }

    @Test
    void testAddAutomatonWithWrongSpeed() {
        AutomatonBatch automatonBatch = new AutomatonBatch(5);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> automatonBatch.add(pstAutomaton));
        assertEquals("The speed of automaton pst-automaton is inconsistent with the automaton batch speed (5).", exception.getMessage());
    }

    @Test
    void testCompareTo() {
        AutomatonBatch fastBatch = new AutomatonBatch(2);
        AutomatonBatch slowBatch = new AutomatonBatch(200);
        assertEquals(-1, fastBatch.compareTo(slowBatch));
    }
}
