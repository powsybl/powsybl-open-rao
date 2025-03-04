/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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
public class SumMaxPerTimestampCostEvaluatorResultTest {

    private State preventiveStateT1;
    private State preventiveStateT2;
    private State preventiveStateT3;
    private State curativeState1T1;
    private State curativeState1T2;
    private State curativeState1T3;
    private OffsetDateTime timestamp1 = OffsetDateTime.parse("2025-02-25T15:11Z");
    private OffsetDateTime timestamp2 = OffsetDateTime.parse("2025-02-25T16:11Z");

    @BeforeEach
    void setUp() {
        preventiveStateT1 = Mockito.mock(State.class);
        Mockito.when(preventiveStateT1.getContingency()).thenReturn(Optional.empty());
        Mockito.when(preventiveStateT1.getTimestamp()).thenReturn(Optional.of(timestamp1));

        preventiveStateT2 = Mockito.mock(State.class);
        Mockito.when(preventiveStateT2.getContingency()).thenReturn(Optional.empty());
        Mockito.when(preventiveStateT2.getTimestamp()).thenReturn(Optional.of(timestamp2));

        preventiveStateT3 = Mockito.mock(State.class);
        Mockito.when(preventiveStateT3.getContingency()).thenReturn(Optional.empty());
        Mockito.when(preventiveStateT3.getTimestamp()).thenReturn(Optional.empty());

        Contingency contingency1 = Mockito.mock(Contingency.class);
        Mockito.when(contingency1.getId()).thenReturn("contingency-1");
        curativeState1T1 = Mockito.mock(State.class);
        Mockito.when(curativeState1T1.getContingency()).thenReturn(Optional.of(contingency1));
        Mockito.when(curativeState1T1.getTimestamp()).thenReturn(Optional.of(timestamp1));

        Contingency contingency2 = Mockito.mock(Contingency.class);
        Mockito.when(contingency2.getId()).thenReturn("contingency-2");
        curativeState1T2 = Mockito.mock(State.class);
        Mockito.when(curativeState1T2.getContingency()).thenReturn(Optional.of(contingency2));
        Mockito.when(curativeState1T2.getTimestamp()).thenReturn(Optional.of(timestamp2));

        Contingency contingency3 = Mockito.mock(Contingency.class);
        Mockito.when(contingency3.getId()).thenReturn("contingency-3");
        curativeState1T3 = Mockito.mock(State.class);
        Mockito.when(curativeState1T3.getContingency()).thenReturn(Optional.of(contingency3));
        Mockito.when(curativeState1T3.getTimestamp()).thenReturn(Optional.empty());
    }

    @Test
    public void testEvaluator() {
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(
            Map.of(preventiveStateT1, 120.0, preventiveStateT2, 34.0, preventiveStateT3, -43.0, curativeState1T1, 10.0, curativeState1T2, 546.0, curativeState1T3, 76.0),
            List.of()
        );

        assertEquals(742, evaluatorResult.getCost(Set.of()));
    }

    @Test
    public void testEvaluatorWithExclusion() {
        SumMaxPerTimestampCostEvaluatorResult evaluatorResult = new SumMaxPerTimestampCostEvaluatorResult(
            Map.of(preventiveStateT1, 120.0, preventiveStateT2, 34.0, preventiveStateT3, -43.0, curativeState1T1, 10.0, curativeState1T2, 546.0, curativeState1T3, 76.0),
            List.of()
        );

        assertEquals(111, evaluatorResult.getCost(Set.of("contingency-3", "contingency-2")));
    }

}
