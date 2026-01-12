/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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

import java.time.OffsetDateTime;
import java.util.HashSet;
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
    private FlowCnec flowCnecPreventiveNoTimestamp;
    private FlowCnec flowCnecCurative1T1;
    private FlowCnec flowCnecCurative1T2;
    private FlowCnec flowCnecCurative1NoTimestamp;
    private FlowCnec flowCnecCurative2T1;
    private FlowCnec flowCnecCurative2T2;
    private FlowCnec flowCnecCurative2NoTimestamp;
    private FlowCnec flowCnecCurative3T1;
    private FlowCnec flowCnecCurative3T2;
    private FlowCnec flowCnecCurative3NoTimestamp;

    private final OffsetDateTime timestamp1 = OffsetDateTime.parse("2025-02-25T15:11Z");
    private final OffsetDateTime timestamp2 = OffsetDateTime.parse("2025-02-25T16:11Z");

    @BeforeEach
    void setUp() {
        Contingency contingency1 = mockContingency("contingency-1");
        Contingency contingency2 = mockContingency("contingency-2");
        Contingency contingency3 = mockContingency("contingency-3");

        State preventiveStateT1 = mockState(null, timestamp1);
        State preventiveStateT2 = mockState(null, timestamp2);
        State preventiveStateNoTimestamp = mockState(null, null);

        State curativeState1T1 = mockState(contingency1, timestamp1);
        State curativeState1T2 = mockState(contingency1, timestamp2);
        State curativeState1NoTimestamp = mockState(contingency1, null);

        State curativeState2T1 = mockState(contingency2, timestamp1);
        State curativeState2T2 = mockState(contingency2, timestamp2);
        State curativeState2NoTimestamp = mockState(contingency2, null);

        State curativeState3T1 = mockState(contingency3, timestamp1);
        State curativeState3T2 = mockState(contingency3, timestamp2);
        State curativeState3NoTimestamp = mockState(contingency3, null);

        flowCnecPreventiveT1 = mockFlowCnec("cnec-preventive-t1", preventiveStateT1);
        flowCnecPreventiveT2 = mockFlowCnec("cnec-preventive-t2", preventiveStateT2);
        flowCnecPreventiveNoTimestamp = mockFlowCnec("cnec-preventive", preventiveStateNoTimestamp);

        flowCnecCurative1T1 = mockFlowCnec("cnec-curative-1-t1", curativeState1T1);
        flowCnecCurative1T2 = mockFlowCnec("cnec-curative-1-t2", curativeState1T2);
        flowCnecCurative1NoTimestamp = mockFlowCnec("cnec-curative-1", curativeState1NoTimestamp);

        flowCnecCurative2T1 = mockFlowCnec("cnec-curative-2-t1", curativeState2T1);
        flowCnecCurative2T2 = mockFlowCnec("cnec-curative-2-t2", curativeState2T2);
        flowCnecCurative2NoTimestamp = mockFlowCnec("cnec-curative-2", curativeState2NoTimestamp);

        flowCnecCurative3T1 = mockFlowCnec("cnec-curative-3-t1", curativeState3T1);
        flowCnecCurative3T2 = mockFlowCnec("cnec-curative-3-t2", curativeState3T2);
        flowCnecCurative3NoTimestamp = mockFlowCnec("cnec-curative-3", curativeState3NoTimestamp);
    }

    private static Contingency mockContingency(String contingencyId) {
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn(contingencyId);
        return contingency;
    }

    private static State mockState(Contingency contingency, OffsetDateTime timestamp) {
        State state = Mockito.mock(State.class);
        // mock contingency
        if (contingency == null) {
            Mockito.when(state.getContingency()).thenReturn(Optional.empty());
        } else {
            Mockito.when(state.getContingency()).thenReturn(Optional.of(contingency));
        }
        // mock timestamp
        if (timestamp == null) {
            Mockito.when(state.getTimestamp()).thenReturn(Optional.empty());
        } else {
            Mockito.when(state.getTimestamp()).thenReturn(Optional.of(timestamp));
        }
        return state;
    }

    private static FlowCnec mockFlowCnec(String flowCnecId, State state) {
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnec.getId()).thenReturn(flowCnecId);
        Mockito.when(flowCnec.getState()).thenReturn(state);
        Mockito.when(flowCnec.isOptimized()).thenReturn(true);
        return flowCnec;
    }

    @Test
    void testEvaluator() {
        Map<FlowCnec, Double> marginPerCnec = Map.of(flowCnecPreventiveT1, -10.0, flowCnecCurative1T1, -50.0, flowCnecCurative2T1, -120.0, flowCnecPreventiveT2, -34.0, flowCnecCurative1T2, -546.0, flowCnecPreventiveNoTimestamp, 43.0, flowCnecCurative1NoTimestamp, -76.0);
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(
            marginPerCnec,
            List.of(),
            false
        );

        // timestamp 1: -120, -10, -50 -> minMargin = -120 -> cost = 120
        // timestamp 2: -34, -546 -> minMargin = -546 -> cost = 546
        // no timestamp: 43, -76 -> minMargin = -76 -> cost = 76
        // the expected evaluation in the sum of maxes: 120 + 546 + 76 = 742
        assertEquals(742, evaluatorResult.getCost(Set.of(), Set.of()));
    }

    @Test
    void testEvaluatorWithExclusion() {
        Map<FlowCnec, Double> marginPerCnec = Map.of(
            flowCnecPreventiveT1, -10.0,
            flowCnecCurative1T1, -50.0,
            flowCnecCurative2T1, -120.0,
            flowCnecCurative3T1, -200.0,
            flowCnecPreventiveT2, -34.0,
            flowCnecCurative2T2, -546.0,
            flowCnecCurative3T2, -211.0,
            flowCnecPreventiveNoTimestamp, 43.0,
            flowCnecCurative2NoTimestamp, -76.0
        );

        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(
            marginPerCnec,
            List.of(),
            false
        );

        // contingencies 2 and 3 are excluded so results associated to flowCnecCurativeT2 and flowCnecCurativeT3 are ignored
        // Also exclude cnec-curative12
        // timestamp 1: -10, -50 -> minMargin = -50 ->  cost = 50
        // timestamp 2: 34      -> minMargin = -34 ->  cost = 34
        // no timestamp: -43     -> minMargin = 43 ->  cost = -43
        // the expected evaluation in the sum of maxes: 50 + 34 - 43 = 41
        assertEquals(41, evaluatorResult.getCost(Set.of("contingency-3", "contingency-2"), Set.of("cnec-curative12")));
    }

    @Test
    void testWithoutAnyTimestampWithExclusion() {
        Map<FlowCnec, Double> marginPerCnec = Map.of(
            flowCnecPreventiveNoTimestamp, -10.0,
            flowCnecCurative1NoTimestamp, -50.0,
            flowCnecCurative2NoTimestamp, -40.0,
            flowCnecCurative3NoTimestamp, -17.0
        );
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(marginPerCnec, List.of(), false);
        assertEquals(50.0, evaluatorResult.getCost(new HashSet<>(), new HashSet<>()));
        assertEquals(17.0, evaluatorResult.getCost(Set.of("contingency-1", "contingency-2"), new HashSet<>()));
        assertEquals(40.0, evaluatorResult.getCost(new HashSet<>(), Set.of("cnec-curative-1")));
        assertEquals(10.0, evaluatorResult.getCost(Set.of("contingency-3"), Set.of("cnec-curative-1", "cnec-curative-2")));
    }

    @Test
    void testEmptyResult() {
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(Map.of(), List.of(), false);
        assertEquals(-1e9, evaluatorResult.getCost(Set.of(), Set.of()));
    }

}
