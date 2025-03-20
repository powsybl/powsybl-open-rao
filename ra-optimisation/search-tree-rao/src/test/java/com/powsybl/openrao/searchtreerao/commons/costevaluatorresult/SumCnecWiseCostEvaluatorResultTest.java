/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
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
class SumCnecWiseCostEvaluatorResultTest {
    private State preventiveState;
    private State curativeState1;
    private State curativeState2;

    private FlowCnec flowCnecPreventive;
    private FlowCnec flowCnecCurative1;
    private FlowCnec flowCnecCurative12;
    private FlowCnec flowCnecCurative2;

    @BeforeEach
    void setUp() {
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getContingency()).thenReturn(Optional.empty());
        flowCnecPreventive = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecPreventive.getState()).thenReturn(preventiveState);
        Mockito.when(flowCnecPreventive.getId()).thenReturn("cnec-preventive");
        Mockito.when(flowCnecPreventive.isOptimized()).thenReturn(true);

        Contingency contingency1 = Mockito.mock(Contingency.class);
        Mockito.when(contingency1.getId()).thenReturn("contingency-1");
        curativeState1 = Mockito.mock(State.class);
        Mockito.when(curativeState1.getContingency()).thenReturn(Optional.of(contingency1));
        flowCnecCurative1 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecCurative1.getState()).thenReturn(curativeState1);
        Mockito.when(flowCnecCurative1.getId()).thenReturn("cnec-curative1");
        Mockito.when(flowCnecCurative1.isOptimized()).thenReturn(true);
        flowCnecCurative12 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecCurative12.getState()).thenReturn(curativeState1);
        Mockito.when(flowCnecCurative12.getId()).thenReturn("cnec-curative12");
        Mockito.when(flowCnecCurative12.isOptimized()).thenReturn(true);

        Contingency contingency2 = Mockito.mock(Contingency.class);
        Mockito.when(contingency2.getId()).thenReturn("contingency-2");
        curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getContingency()).thenReturn(Optional.of(contingency2));
        flowCnecCurative2 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecCurative2.getState()).thenReturn(curativeState2);
        Mockito.when(flowCnecCurative2.getId()).thenReturn("cnec-curative2");
        Mockito.when(flowCnecCurative2.isOptimized()).thenReturn(true);
    }

    @Test
    void testWithPreventiveAndCurative() {
        Map<FlowCnec, Double> costPerCnec = Map.of(flowCnecPreventive, 10.0, flowCnecCurative1, 40.0, flowCnecCurative12, 17.0, flowCnecCurative2, -20.0);
        SumCnecWiseCostEvaluatorResult evaluatorResult = new SumCnecWiseCostEvaluatorResult(costPerCnec, List.of());
        assertEquals(47.0, evaluatorResult.getCost(Set.of(), Set.of()));
        assertEquals(10.0, evaluatorResult.getCost(Set.of("contingency-1", "contingency-2"), Set.of()));
        assertEquals(57.0, evaluatorResult.getCost(Set.of("contingency-2"), Set.of("cnec-preventive")));
        assertEquals(67.0, evaluatorResult.getCost(Set.of(), Set.of("cnec-curative2")));
    }

    @Test
    void testWithPreventiveOnly() {
        Map<FlowCnec, Double> costPerCnec = Map.of(flowCnecPreventive, 10.0);
        SumCnecWiseCostEvaluatorResult evaluatorResult = new SumCnecWiseCostEvaluatorResult(costPerCnec, List.of());
        assertEquals(10.0, evaluatorResult.getCost(Set.of(), Set.of()));
        assertEquals(0.0, evaluatorResult.getCost(Set.of(), Set.of("cnec-preventive")));
    }

    @Test
    void testWithCurativeOnly() {
        Map<FlowCnec, Double> costPerCnec = Map.of(flowCnecCurative1, 40.0, flowCnecCurative12, 17.0, flowCnecCurative2, -20.0);
        SumCnecWiseCostEvaluatorResult evaluatorResult = new SumCnecWiseCostEvaluatorResult(costPerCnec, List.of());
        assertEquals(37.0, evaluatorResult.getCost(Set.of(), Set.of()));
        assertEquals(-20.0, evaluatorResult.getCost(Set.of("contingency-1"), Set.of()));
        assertEquals(-3.0, evaluatorResult.getCost(Set.of(), Set.of("cnec-curative1")));
    }

    @Test
    void testEmptyResult() {
        SumCnecWiseCostEvaluatorResult evaluatorResult = new SumCnecWiseCostEvaluatorResult(Map.of(), List.of());
        assertEquals(0.0, evaluatorResult.getCost(Set.of(), Set.of()));
    }
}
