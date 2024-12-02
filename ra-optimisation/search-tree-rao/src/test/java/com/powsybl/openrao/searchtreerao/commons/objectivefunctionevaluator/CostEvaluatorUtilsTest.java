/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluatorUtils.groupFlowCnecsPerState;
import static com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluatorUtils.sortFlowCnecsByDecreasingCost;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CostEvaluatorUtilsTest {
    private FlowCnec flowCnec1;
    private FlowCnec flowCnec2;
    private FlowCnec flowCnec3;
    private FlowCnec flowCnec4;
    private FlowCnec flowCnec5;
    private State state1;
    private State state2;
    private State state3;

    @BeforeEach
    void setUp() {
        flowCnec1 = Mockito.mock(FlowCnec.class);
        flowCnec2 = Mockito.mock(FlowCnec.class);
        flowCnec3 = Mockito.mock(FlowCnec.class);
        flowCnec4 = Mockito.mock(FlowCnec.class);
        flowCnec5 = Mockito.mock(FlowCnec.class);

        state1 = Mockito.mock(State.class);
        state2 = Mockito.mock(State.class);
        state3 = Mockito.mock(State.class);

        Mockito.when(flowCnec1.getState()).thenReturn(state1);
        Mockito.when(flowCnec2.getState()).thenReturn(state1);
        Mockito.when(flowCnec3.getState()).thenReturn(state2);
        Mockito.when(flowCnec4.getState()).thenReturn(state2);
        Mockito.when(flowCnec5.getState()).thenReturn(state3);
    }

    @Test
    void testGroupFlowCnecsPerState() {
        assertEquals(Map.of(state1, Set.of(flowCnec1, flowCnec2), state2, Set.of(flowCnec3, flowCnec4), state3, Set.of(flowCnec5)), groupFlowCnecsPerState(Set.of(flowCnec1, flowCnec2, flowCnec3, flowCnec4, flowCnec5)));
    }

    @Test
    void testSortFlowCnecsByDecreasingCost() {
        Map<FlowCnec, Double> costPerCnec = Map.of(flowCnec1, 100.0, flowCnec2, 0.0, flowCnec3, 10.0, flowCnec4, 200.5, flowCnec5, 50.0);
        assertEquals(List.of(flowCnec4, flowCnec1, flowCnec5, flowCnec3, flowCnec2), sortFlowCnecsByDecreasingCost(costPerCnec));
    }
}
