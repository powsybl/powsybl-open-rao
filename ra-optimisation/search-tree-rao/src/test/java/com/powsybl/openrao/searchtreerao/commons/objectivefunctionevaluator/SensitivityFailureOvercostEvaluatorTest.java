/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SensitivityFailureOvercostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowResult flowResult;
    private SensitivityFailureOvercostEvaluator evaluator;
    private FlowCnec cnec1;
    private FlowCnec cnec2;

    @BeforeEach
    public void setUp() {
        flowResult = Mockito.mock(FlowResult.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);
        Contingency contingency1 = Mockito.mock(Contingency.class);
        Mockito.when(contingency1.getId()).thenReturn("contingency-1");
        Contingency contingency2 = Mockito.mock(Contingency.class);
        Mockito.when(contingency2.getId()).thenReturn("contingency-2");
        State state1 = Mockito.mock(State.class);
        Mockito.when(state1.getId()).thenReturn("state-1");
        Mockito.when(state1.getContingency()).thenReturn(Optional.of(contingency1));
        State state2 = Mockito.mock(State.class);
        Mockito.when(state2.getId()).thenReturn("state-2");
        Mockito.when(state2.getContingency()).thenReturn(Optional.of(contingency2));
        Mockito.when(cnec1.getState()).thenReturn(state1);
        Mockito.when(cnec2.getState()).thenReturn(state2);
        Mockito.when(flowResult.getComputationStatus(state1)).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(flowResult.getComputationStatus(state2)).thenReturn(ComputationStatus.FAILURE);
    }

    @Test
    void testGetName() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1), 10000);
        assertEquals("sensitivity-failure-cost", evaluator.getName());
    }

    @Test
    void testCostWithStateInFailure() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1, cnec2), 10000);
        assertEquals(10000, evaluator.evaluate(flowResult, null).getCost(Set.of()), DOUBLE_TOLERANCE);
    }
}
