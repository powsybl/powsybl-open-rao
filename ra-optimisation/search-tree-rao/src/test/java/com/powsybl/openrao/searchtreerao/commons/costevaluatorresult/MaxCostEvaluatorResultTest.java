/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.Unit;
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
class MaxCostEvaluatorResultTest {
    private State preventiveState;
    private State curativeState1;
    private State curativeState2;
    private State curativeState3;
    private FlowCnec flowCnecPreventive;
    private FlowCnec flowCnecCurative1;
    private FlowCnec flowCnecCurative12;
    private FlowCnec flowCnecCurative2;
    private FlowCnec flowCnecCurative3;

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

        Contingency contingency3 = Mockito.mock(Contingency.class);
        Mockito.when(contingency3.getId()).thenReturn("contingency-3");
        curativeState3 = Mockito.mock(State.class);
        Mockito.when(curativeState3.getContingency()).thenReturn(Optional.of(contingency3));
        flowCnecCurative3 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecCurative3.getState()).thenReturn(curativeState3);
        Mockito.when(flowCnecCurative3.getId()).thenReturn("cnec-curative3");
        Mockito.when(flowCnecCurative3.isOptimized()).thenReturn(true);
    }

    @Test
    void testWithPreventiveAndCurative() {
        Map<FlowCnec, Double> marginPerCnec = Map.of(flowCnecPreventive, -10.0, flowCnecCurative1, -50.0, flowCnecCurative12, -40.0, flowCnecCurative2, 20.0, flowCnecCurative3, -17.0);
        MaxCostEvaluatorResult evaluatorResult = new MaxCostEvaluatorResult(marginPerCnec, List.of(), Unit.MEGAWATT);
        assertEquals(50.0, evaluatorResult.getCost(Set.of(), Set.of()));
        assertEquals(17.0, evaluatorResult.getCost(Set.of("contingency-1", "contingency-2"), Set.of()));
        assertEquals(40.0, evaluatorResult.getCost(Set.of(), Set.of("cnec-curative1")));
        assertEquals(17.0, evaluatorResult.getCost(Set.of("contingency-1"), Set.of("cnec-curative1")));
    }

    @Test
    void testWithPreventiveOnly() {
        Map<FlowCnec, Double> marginPerCnec = Map.of(flowCnecPreventive, -10.0);
        MaxCostEvaluatorResult evaluatorResult = new MaxCostEvaluatorResult(marginPerCnec, List.of(), Unit.MEGAWATT);
        assertEquals(10.0, evaluatorResult.getCost(Set.of(), Set.of()));
    }

    @Test
    void testWithCurativeOnly() {
        Map<FlowCnec, Double> marginPerCnec = Map.of(flowCnecCurative1, -50.0, flowCnecCurative12, -40.0, flowCnecCurative2, 20.0, flowCnecCurative3, -17.0);
        MaxCostEvaluatorResult evaluatorResult = new MaxCostEvaluatorResult(marginPerCnec, List.of(), Unit.MEGAWATT);
        assertEquals(50.0, evaluatorResult.getCost(Set.of(), Set.of()));
        assertEquals(17.0, evaluatorResult.getCost(Set.of(), Set.of("cnec-curative1", "cnec-curative12")));
        assertEquals(-20.0, evaluatorResult.getCost(Set.of("contingency-1", "contingency-3"), Set.of()));

    }

    @Test
    void testEmptyResult() {
        MaxCostEvaluatorResult evaluatorResult = new MaxCostEvaluatorResult(Map.of(), List.of(), Unit.MEGAWATT);
        assertEquals(-Double.MAX_VALUE, evaluatorResult.getCost(Set.of(), Set.of()));
    }
}
