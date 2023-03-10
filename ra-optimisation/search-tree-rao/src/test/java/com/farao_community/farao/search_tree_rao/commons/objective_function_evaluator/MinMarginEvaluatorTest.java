/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinMarginEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec pureMnec;
    private FlowResult flowResult;
    private RangeActionActivationResult rangeActionActivationResult;
    private SensitivityResult sensitivityResult;
    private MarginEvaluator marginEvaluator;
    private MinMarginEvaluator minMarginEvaluator;

    @Before
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

        rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);

        marginEvaluator = Mockito.mock(MarginEvaluator.class);
        flowResult = Mockito.mock(FlowResult.class);
        when(marginEvaluator.getMargin(flowResult, cnec1, rangeActionActivationResult, sensitivityResult, MEGAWATT)).thenReturn(-150.);
        when(marginEvaluator.getMargin(flowResult, cnec2, rangeActionActivationResult, sensitivityResult, MEGAWATT)).thenReturn(200.);
        when(marginEvaluator.getMargin(flowResult, cnec3, rangeActionActivationResult, sensitivityResult, MEGAWATT)).thenReturn(-250.);
        when(marginEvaluator.getMargin(flowResult, pureMnec, rangeActionActivationResult, sensitivityResult, MEGAWATT)).thenReturn(50.);

        minMarginEvaluator = new MinMarginEvaluator(Set.of(cnec1, cnec2, cnec3, pureMnec), MEGAWATT, marginEvaluator);
    }

    @Test
    public void getName() {
        assertEquals("min-margin-evaluator", minMarginEvaluator.getName());
    }

    @Test
    public void getUnit() {
        assertEquals(MEGAWATT, minMarginEvaluator.getUnit());
    }

    @Test
    public void getMostLimitingElements() {
        List<FlowCnec> costlyElements = minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getRight();
        assertEquals(3, costlyElements.size());
        assertSame(cnec3, costlyElements.get(0));
        assertSame(cnec1, costlyElements.get(1));
        assertSame(cnec2, costlyElements.get(2));
    }

    @Test
    public void getMostLimitingElementsWithLimitedElements() {
        List<FlowCnec> costlyElements = minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getRight();
        assertEquals(2, costlyElements.size());
        assertSame(cnec3, costlyElements.get(0));
        assertSame(cnec1, costlyElements.get(1));
    }

    @Test
    public void computeCost() {
        assertEquals(250., minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, Mockito.mock(ComputationStatus.class)).getLeft(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithPureMnecs() {
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

        MarginEvaluator marginEvaluator = Mockito.mock(MarginEvaluator.class);
        flowResult = Mockito.mock(FlowResult.class);
        when(marginEvaluator.getMargin(flowResult, mnec1, rangeActionActivationResult, sensitivityResult, MEGAWATT)).thenReturn(-150.);
        when(marginEvaluator.getMargin(flowResult, mnec2, rangeActionActivationResult, sensitivityResult, MEGAWATT)).thenReturn(200.);

        minMarginEvaluator = new MinMarginEvaluator(Set.of(mnec1, mnec2), MEGAWATT, marginEvaluator);
        assertTrue(minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getRight().isEmpty());
        assertNull(minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT, new HashSet<>()).getRight());
        assertEquals(-2000, minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, Mockito.mock(ComputationStatus.class)).getLeft(), DOUBLE_TOLERANCE);
    }

    private void mockCnecThresholds(FlowCnec cnec, double threshold) {
        when(cnec.getUpperBound(any(), any())).thenReturn(Optional.of(threshold));
        when(cnec.getLowerBound(any(), any())).thenReturn(Optional.of(-threshold));
    }

    @Test
    public void testAllCnecsUnoptimized() {
        when(marginEvaluator.getMargin(eq(flowResult), any(), any(), any(), eq(MEGAWATT))).thenReturn(Double.MAX_VALUE);
        mockCnecThresholds(cnec1, 1000);
        mockCnecThresholds(cnec2, 2000);
        mockCnecThresholds(cnec3, 3000);
        mockCnecThresholds(pureMnec, 4000);
        assertEquals(-4000., minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, Mockito.mock(ComputationStatus.class)).getLeft(), DOUBLE_TOLERANCE);
    }
}
