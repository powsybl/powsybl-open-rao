/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class MinMarginEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec pureMnec;
    private FlowResult flowResult;
    private MarginEvaluator marginEvaluator;
    private MinMarginEvaluator minMarginEvaluator;

    @BeforeEach
    public void setUp() {
        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());
        cnec1 = Mockito.mock(FlowCnec.class); // Only optimized
        when(cnec1.isMonitored()).thenReturn(false);
        when(cnec1.isOptimized()).thenReturn(true);
        when(cnec1.getState()).thenReturn(state);
        cnec2 = Mockito.mock(FlowCnec.class); // Only optimized
        when(cnec2.isMonitored()).thenReturn(false);
        when(cnec2.isOptimized()).thenReturn(true);
        when(cnec2.getState()).thenReturn(state);
        cnec3 = Mockito.mock(FlowCnec.class); // Optimized and monitored
        when(cnec3.isMonitored()).thenReturn(true);
        when(cnec3.isOptimized()).thenReturn(true);
        when(cnec3.getState()).thenReturn(state);
        pureMnec = Mockito.mock(FlowCnec.class); // Only monitored
        when(pureMnec.isMonitored()).thenReturn(true);
        when(pureMnec.isOptimized()).thenReturn(false);
        when(pureMnec.getState()).thenReturn(state);

        marginEvaluator = Mockito.mock(MarginEvaluator.class);
        flowResult = Mockito.mock(FlowResult.class);
        when(marginEvaluator.getMargin(flowResult, cnec1, MEGAWATT)).thenReturn(-150.);
        when(marginEvaluator.getMargin(flowResult, cnec2, MEGAWATT)).thenReturn(200.);
        when(marginEvaluator.getMargin(flowResult, cnec3, MEGAWATT)).thenReturn(-250.);
        when(marginEvaluator.getMargin(flowResult, pureMnec, MEGAWATT)).thenReturn(50.);

        minMarginEvaluator = new MinMarginEvaluator(Set.of(cnec1, cnec2, cnec3, pureMnec), MEGAWATT, marginEvaluator);
    }

    @Test
    void getName() {
        assertEquals("min-margin-evaluator", minMarginEvaluator.getName());
    }

    @Test
    void getUnit() {
        assertEquals(MEGAWATT, minMarginEvaluator.getUnit());
    }

    @Test
    void getMostLimitingElements() {
        List<FlowCnec> costlyElements = minMarginEvaluator.computeCostAndLimitingElements(flowResult).getRight();
        assertEquals(3, costlyElements.size());
        assertSame(cnec3, costlyElements.get(0));
        assertSame(cnec1, costlyElements.get(1));
        assertSame(cnec2, costlyElements.get(2));
    }

    @Test
    void computeCost() {
        assertEquals(250., minMarginEvaluator.computeCostAndLimitingElements(flowResult).getLeft(), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithPureMnecs() {
        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());
        FlowCnec mnec1 = Mockito.mock(FlowCnec.class);
        when(mnec1.isMonitored()).thenReturn(true);
        when(mnec1.isOptimized()).thenReturn(false);
        when(mnec1.getState()).thenReturn(state);
        FlowCnec mnec2 = Mockito.mock(FlowCnec.class);
        when(mnec2.isMonitored()).thenReturn(true);
        when(mnec2.isOptimized()).thenReturn(false);
        when(mnec2.getState()).thenReturn(state);
        mockCnecThresholds(mnec1, 1000);
        mockCnecThresholds(mnec2, 2000);

        marginEvaluator = Mockito.mock(MarginEvaluator.class);
        flowResult = Mockito.mock(FlowResult.class);
        when(marginEvaluator.getMargin(flowResult, mnec1, MEGAWATT)).thenReturn(-150.);
        when(marginEvaluator.getMargin(flowResult, mnec2, MEGAWATT)).thenReturn(200.);

        minMarginEvaluator = new MinMarginEvaluator(Set.of(mnec1, mnec2), MEGAWATT, marginEvaluator);
        assertTrue(minMarginEvaluator.computeCostAndLimitingElements(flowResult).getRight().isEmpty());
        assertEquals(-2000, minMarginEvaluator.computeCostAndLimitingElements(flowResult).getLeft(), DOUBLE_TOLERANCE);
    }

    private void mockCnecThresholds(FlowCnec cnec, double threshold) {
        when(cnec.getUpperBound(any(), any())).thenReturn(Optional.of(threshold));
        when(cnec.getLowerBound(any(), any())).thenReturn(Optional.of(-threshold));
    }

    @Test
    void testAllCnecsUnoptimized() {
        when(marginEvaluator.getMargin(eq(flowResult), any(), eq(MEGAWATT))).thenReturn(Double.MAX_VALUE);
        mockCnecThresholds(cnec1, 1000);
        mockCnecThresholds(cnec2, 2000);
        mockCnecThresholds(cnec3, 3000);
        mockCnecThresholds(pureMnec, 4000);
        assertEquals(-4000., minMarginEvaluator.computeCostAndLimitingElements(flowResult).getLeft(), DOUBLE_TOLERANCE);
    }
}
