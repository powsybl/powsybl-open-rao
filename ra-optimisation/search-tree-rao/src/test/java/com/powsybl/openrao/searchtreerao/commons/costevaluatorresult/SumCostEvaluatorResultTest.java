/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class SumCostEvaluatorResultTest {
    private State preventiveState;
    private State curativeState1;
    private State curativeState2;
    private State curativeState3;

    @BeforeEach
    void setUp() {
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getContingency()).thenReturn(Optional.empty());

        Contingency contingency1 = Mockito.mock(Contingency.class);
        Mockito.when(contingency1.getId()).thenReturn("contingency-1");
        curativeState1 = Mockito.mock(State.class);
        Mockito.when(curativeState1.getContingency()).thenReturn(Optional.of(contingency1));

        Contingency contingency2 = Mockito.mock(Contingency.class);
        Mockito.when(contingency2.getId()).thenReturn("contingency-2");
        curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getContingency()).thenReturn(Optional.of(contingency2));

        Contingency contingency3 = Mockito.mock(Contingency.class);
        Mockito.when(contingency3.getId()).thenReturn("contingency-3");
        curativeState3 = Mockito.mock(State.class);
        Mockito.when(curativeState3.getContingency()).thenReturn(Optional.of(contingency3));
    }

    @Test
    void testWithPreventiveAndCurative() {
        Map<State, Double> costPerState = Map.of(preventiveState, 10.0, curativeState1, 50.0, curativeState2, -20.0, curativeState3, 17.0);
        SumCostEvaluatorResult evaluatorResult = new SumCostEvaluatorResult(costPerState, List.of());
        assertEquals(57.0, evaluatorResult.getCost(Set.of()));
        assertEquals(27.0, evaluatorResult.getCost(Set.of("contingency-1", "contingency-2")));
    }

    @Test
    void testWithPreventiveOnly() {
        Map<State, Double> costPerState = Map.of(preventiveState, 10.0);
        SumCostEvaluatorResult evaluatorResult = new SumCostEvaluatorResult(costPerState, List.of());
        assertEquals(10.0, evaluatorResult.getCost(Set.of()));
    }

    @Test
    void testWithCurativeOnly() {
        Map<State, Double> costPerState = Map.of(curativeState1, 50.0, curativeState2, -20.0, curativeState3, 17.0);
        SumCostEvaluatorResult evaluatorResult = new SumCostEvaluatorResult(costPerState, List.of());
        assertEquals(47.0, evaluatorResult.getCost(Set.of()));
        assertEquals(-20.0, evaluatorResult.getCost(Set.of("contingency-1", "contingency-3")));
    }

    @Test
    void testEmptyResult() {
        SumCostEvaluatorResult evaluatorResult = new SumCostEvaluatorResult(Map.of(), List.of());
        assertEquals(0.0, evaluatorResult.getCost(Set.of()));
    }
}
