/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class SumMaxPerTimestampCostEvaluatorResultTest {

    private FlowCnec flowCnecPreventiveT1;
    private FlowCnec flowCnecPreventiveT2;
    private FlowCnec flowCnecPreventiveT3;
    private FlowCnec flowCnecCurative1T1;
    private FlowCnec flowCnecCurative12T1;
    private FlowCnec flowCnecCurativeT2;
    private FlowCnec flowCnecCurativeT3;

    private State preventiveStateT1;
    private State preventiveStateT2;
    private State preventiveStateT3;
    private State curativeStateT1;
    private State curativeStateT2;
    private State curativeStateT3;
    private OffsetDateTime timestamp1 = OffsetDateTime.parse("2025-02-25T15:11Z");
    private OffsetDateTime timestamp2 = OffsetDateTime.parse("2025-02-25T16:11Z");

    @BeforeEach
    void setUp() {
        preventiveStateT1 = Mockito.mock(State.class);
        Mockito.when(preventiveStateT1.getContingency()).thenReturn(Optional.empty());
        flowCnecPreventiveT1 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecPreventiveT1.getState()).thenReturn(preventiveStateT1);
        Mockito.when(flowCnecPreventiveT1.getId()).thenReturn("cnec-preventive-t1");
        Mockito.when(flowCnecPreventiveT1.isOptimized()).thenReturn(true);

        preventiveStateT2 = Mockito.mock(State.class);
        Mockito.when(preventiveStateT2.getContingency()).thenReturn(Optional.empty());
        flowCnecPreventiveT2 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecPreventiveT2.getState()).thenReturn(preventiveStateT2);
        Mockito.when(flowCnecPreventiveT2.getId()).thenReturn("cnec-preventive-t2");
        Mockito.when(flowCnecPreventiveT2.isOptimized()).thenReturn(true);

        preventiveStateT3 = Mockito.mock(State.class);
        Mockito.when(preventiveStateT3.getContingency()).thenReturn(Optional.empty());
        flowCnecPreventiveT3 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecPreventiveT3.getState()).thenReturn(preventiveStateT3);
        Mockito.when(flowCnecPreventiveT3.getId()).thenReturn("cnec-preventive-no-timestamp");
        Mockito.when(flowCnecPreventiveT3.isOptimized()).thenReturn(true);

        Contingency contingency1 = Mockito.mock(Contingency.class);
        Mockito.when(contingency1.getId()).thenReturn("contingency-1");
        curativeStateT1 = Mockito.mock(State.class);
        Mockito.when(curativeStateT1.getContingency()).thenReturn(Optional.of(contingency1));
        flowCnecCurative1T1 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecCurative1T1.getState()).thenReturn(curativeStateT1);
        Mockito.when(flowCnecCurative1T1.getId()).thenReturn("cnec-curative1");
        Mockito.when(flowCnecCurative1T1.isOptimized()).thenReturn(true);
        flowCnecCurative12T1 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecCurative12T1.getState()).thenReturn(curativeStateT1);
        Mockito.when(flowCnecCurative12T1.getId()).thenReturn("cnec-curative12");
        Mockito.when(flowCnecCurative12T1.isOptimized()).thenReturn(true);

        Contingency contingency2 = Mockito.mock(Contingency.class);
        Mockito.when(contingency2.getId()).thenReturn("contingency-2");
        curativeStateT2 = Mockito.mock(State.class);
        Mockito.when(curativeStateT2.getContingency()).thenReturn(Optional.of(contingency2));
        flowCnecCurativeT2 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecCurativeT2.getState()).thenReturn(curativeStateT2);
        Mockito.when(flowCnecCurativeT2.getId()).thenReturn("cnec-curative2");
        Mockito.when(flowCnecCurativeT2.isOptimized()).thenReturn(true);

        Contingency contingency3 = Mockito.mock(Contingency.class);
        Mockito.when(contingency3.getId()).thenReturn("contingency-3");
        curativeStateT3 = Mockito.mock(State.class);
        Mockito.when(curativeStateT3.getContingency()).thenReturn(Optional.of(contingency3));
        flowCnecCurativeT3 = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnecCurativeT3.getState()).thenReturn(curativeStateT3);
        Mockito.when(flowCnecCurativeT3.getId()).thenReturn("cnec-curative3");
        Mockito.when(flowCnecCurativeT3.isOptimized()).thenReturn(true);
    }

    void addTimestamp() {
        Mockito.when(preventiveStateT1.getTimestamp()).thenReturn(Optional.of(timestamp1));
        Mockito.when(preventiveStateT2.getTimestamp()).thenReturn(Optional.of(timestamp2));
        Mockito.when(preventiveStateT3.getTimestamp()).thenReturn(Optional.empty());
        Mockito.when(curativeStateT1.getTimestamp()).thenReturn(Optional.of(timestamp1));
        Mockito.when(curativeStateT2.getTimestamp()).thenReturn(Optional.of(timestamp2));
        Mockito.when(curativeStateT3.getTimestamp()).thenReturn(Optional.empty());

    }

    @Test
    void testEvaluator() {
        addTimestamp();
        Map<FlowCnec, Double> marginPerCnec = Map.of(flowCnecPreventiveT1, -10.0, flowCnecCurative1T1, -50.0, flowCnecCurative12T1, -120.0, flowCnecPreventiveT2, -34.0, flowCnecCurativeT2, -546.0, flowCnecPreventiveT3, 43.0, flowCnecCurativeT3, -76.0);
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(
            marginPerCnec,
            List.of(),
            Unit.MEGAWATT
        );

        // timestamp 1: -120, -10, -50 -> minMargin = -120 -> cost = 120
        // timestamp 2: -34, -546 -> minMargin = -546 -> cost = 546
        // timestamp 3: 43, -76 -> minMargin = -76 -> cost = 76
        // the expected evaluation in the sum of maxes: 120 + 546 + 76 = 742
        assertEquals(742, evaluatorResult.getCost(Set.of(), Set.of()));
    }

    @Test
    void testEvaluatorWithExclusion() {
        addTimestamp();
        Map<FlowCnec, Double> marginPerCnec = Map.of(flowCnecPreventiveT1, -10.0, flowCnecCurative1T1, -50.0, flowCnecCurative12T1, -120.0, flowCnecPreventiveT2, -34.0, flowCnecCurativeT2, -546.0, flowCnecPreventiveT3, 43.0, flowCnecCurativeT3, -76.0);
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(
            marginPerCnec,
            List.of(),
            Unit.MEGAWATT
        );

        // contingencies 2 and 3 are excluded so results associated to flowCnecCurativeT2 and flowCnecCurativeT3 are ignored
        // Also exclude cnec-curative12
        // timestamp 1: -10, -50 -> minMargin = -50 ->  cost = 50
        // timestamp 2: 34      -> minMargin = -34 ->  cost = 34
        // timestamp 3: -43     -> minMargin = 43 ->  cost = -43
        // the expected evaluation in the sum of maxes: 50 + 34 - 43 = 41
        assertEquals(41, evaluatorResult.getCost(Set.of("contingency-3", "contingency-2"), Set.of("cnec-curative12")));
    }

    @Test
    void testWithoutAnyTimestampWithExclusion() {
        Map<FlowCnec, Double> marginPerCnec = Map.of(flowCnecPreventiveT1, -10.0, flowCnecCurative1T1, -50.0, flowCnecCurative12T1, -40.0, flowCnecCurativeT2, 20.0, flowCnecCurativeT3, -17.0);
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(marginPerCnec, List.of(), Unit.MEGAWATT);
        assertEquals(50.0, evaluatorResult.getCost(Set.of(), Set.of()));
        assertEquals(17.0, evaluatorResult.getCost(Set.of("contingency-1", "contingency-2"), Set.of()));
        assertEquals(40.0, evaluatorResult.getCost(Set.of(), Set.of("cnec-curative1")));
        assertEquals(17.0, evaluatorResult.getCost(Set.of("contingency-1"), Set.of("cnec-curative1")));
    }

    @Test
    void testEmptyResult() {
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(Map.of(), List.of(), Unit.MEGAWATT);
        assertEquals(0, evaluatorResult.getCost(Set.of(), Set.of()));
    }

}
