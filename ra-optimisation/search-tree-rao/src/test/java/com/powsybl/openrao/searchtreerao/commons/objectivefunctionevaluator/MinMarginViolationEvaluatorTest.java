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
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MinMarginViolationEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    void testWithOverload() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        State state = Mockito.mock(State.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());
        Mockito.when(flowCnec.getState()).thenReturn(state);
        Mockito.when(flowCnec.isOptimized()).thenReturn(true);
        Mockito.when(flowCnec.getId()).thenReturn("cnec1");
        Mockito.when(flowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(-1d);
        MinMarginViolationEvaluator evaluator = new MinMarginViolationEvaluator(Set.of(flowCnec), Unit.MEGAWATT, new BasicMarginEvaluator(), 1000.0);
        CostEvaluatorResult result = evaluator.evaluate(flowResult, null);
        assertEquals(1000.0, result.getCost(Set.of(), Set.of()), DOUBLE_TOLERANCE);
        assertEquals(List.of(flowCnec), result.getCostlyElements(Set.of(), Set.of()));
    }

    @Test
    void testWithoutOverload() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        State state = Mockito.mock(State.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());
        Mockito.when(flowCnec.getState()).thenReturn(state);
        Mockito.when(flowCnec.isOptimized()).thenReturn(true);
        Mockito.when(flowCnec.getId()).thenReturn("cnec1");
        Mockito.when(flowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(500d);

        MinMarginViolationEvaluator evaluator = new MinMarginViolationEvaluator(Set.of(flowCnec), Unit.MEGAWATT, new BasicMarginEvaluator(), 10000);
        CostEvaluatorResult result = evaluator.evaluate(flowResult, null);
        assertEquals(0.0, result.getCost(Set.of(), Set.of()), DOUBLE_TOLERANCE);
        assertTrue(result.getCostlyElements(Set.of(), Set.of()).isEmpty());
    }

    //same test as in MinMarginEvaluatorTest but capAtZero is true
    @Test
    void testNoCnecs() {
        MinMarginViolationEvaluator emptyEvaluator = new MinMarginViolationEvaluator(Collections.emptySet(), MEGAWATT, Mockito.mock(MarginEvaluator.class), 10000.);
        assertEquals(0., emptyEvaluator.evaluate(Mockito.mock(FlowResult.class), null).getCost(Set.of(), Set.of()), DOUBLE_TOLERANCE);
    }
}
