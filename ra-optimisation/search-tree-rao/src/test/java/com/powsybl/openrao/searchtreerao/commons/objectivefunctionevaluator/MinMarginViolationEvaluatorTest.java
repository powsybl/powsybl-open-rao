/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.BasicMarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MinMarginViolationEvaluatorTest {
    @Test
    void testWithOverload() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        State state = Mockito.mock(State.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());
        Mockito.when(flowCnec.getState()).thenReturn(state);
        Mockito.when(flowCnec.isOptimized()).thenReturn(true);
        Mockito.when(flowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(-1d);

        MinMarginViolationEvaluator evaluator = new MinMarginViolationEvaluator(Set.of(flowCnec), Unit.MEGAWATT, new BasicMarginEvaluator());
        CostEvaluatorResult result = evaluator.evaluate(flowResult, null);
        assertEquals(1000.0, result.getCost(Set.of()));
        assertEquals(List.of(flowCnec), result.getCostlyElements(Set.of()));
    }

    @Test
    void testWithoutOverload() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        State state = Mockito.mock(State.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());
        Mockito.when(flowCnec.getState()).thenReturn(state);
        Mockito.when(flowCnec.isOptimized()).thenReturn(true);
        Mockito.when(flowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(500d);

        MinMarginViolationEvaluator evaluator = new MinMarginViolationEvaluator(Set.of(flowCnec), Unit.MEGAWATT, new BasicMarginEvaluator());
        CostEvaluatorResult result = evaluator.evaluate(flowResult, null);
        assertEquals(0.0, result.getCost(Set.of()));
        assertEquals(Collections.emptyList(), result.getCostlyElements(Set.of()));
    }
}
