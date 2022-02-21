/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.MnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    private FlowCnec mnec1;
    private FlowCnec mnec2;
    private FlowCnec pureCnec;
    private FlowResult initialFlowResult;
    private FlowResult currentFlowResult;
    private MnecViolationCostEvaluator evaluator1;
    private MnecViolationCostEvaluator evaluator2;

    @Before
    public void setUp() {
        mnec1 = Mockito.mock(FlowCnec.class);
        when(mnec1.isOptimized()).thenReturn(true);
        when(mnec1.isMonitored()).thenReturn(true);
        mnec2 = Mockito.mock(FlowCnec.class);
        when(mnec2.isOptimized()).thenReturn(false);
        when(mnec2.isMonitored()).thenReturn(true);
        pureCnec = Mockito.mock(FlowCnec.class);
        when(pureCnec.isOptimized()).thenReturn(true);
        when(pureCnec.isMonitored()).thenReturn(false);

        initialFlowResult = Mockito.mock(FlowResult.class);
        currentFlowResult = Mockito.mock(FlowResult.class);

        evaluator1 = new MnecViolationCostEvaluator(
                Set.of(mnec1, pureCnec),
                initialFlowResult,
                new MnecParameters(50, 10, 1)
        );
        evaluator2 = new MnecViolationCostEvaluator(
                Set.of(mnec1, pureCnec),
                initialFlowResult,
                new MnecParameters(20, 2, 1)
        );
    }

    private MnecViolationCostEvaluator createEvaluatorWithCosts(double violationCost) {
        when(initialFlowResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(-200.);
        when(currentFlowResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(-270.);
        when(initialFlowResult.getMargin(mnec2, Unit.MEGAWATT)).thenReturn(-200.);
        when(currentFlowResult.getMargin(mnec2, Unit.MEGAWATT)).thenReturn(-400.);

        return new MnecViolationCostEvaluator(
                Set.of(mnec1, mnec2, pureCnec),
                initialFlowResult,
                new MnecParameters(50, violationCost, 1)
        );
    }

    private MnecViolationCostEvaluator createEvaluatorWithNoCosts() {
        when(initialFlowResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(200.);
        when(currentFlowResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(300.);
        when(initialFlowResult.getMargin(mnec2, Unit.MEGAWATT)).thenReturn(200.);
        when(currentFlowResult.getMargin(mnec2, Unit.MEGAWATT)).thenReturn(150.);

        return new MnecViolationCostEvaluator(
                Set.of(mnec1, mnec2, pureCnec),
                initialFlowResult,
                new MnecParameters(50, 10, 1)
        );
    }

    @Test
    public void getUnit() {
        assertEquals(Unit.MEGAWATT, evaluator1.getUnit());
    }

    @Test
    public void getName() {
        assertEquals("mnec-cost", evaluator1.getName());
    }

    @Test
    public void getCostlyElements() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithCosts(10);

        List<FlowCnec> costlyElements = evaluator.getCostlyElements(currentFlowResult, 5);
        assertEquals(2, costlyElements.size());
        assertSame(mnec2, costlyElements.get(0));
        assertSame(mnec1, costlyElements.get(1));
    }

    @Test
    public void getCostlyElementsWithLimitedElements() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithCosts(10);

        List<FlowCnec> costlyElements = evaluator.getCostlyElements(currentFlowResult, 1);
        assertEquals(1, costlyElements.size());
        assertSame(mnec2, costlyElements.get(0));
    }

    @Test
    public void getCostlyElementsWithNoCostlyElements() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithNoCosts();

        List<FlowCnec> costlyElements = evaluator.getCostlyElements(currentFlowResult, 5);
        assertEquals(0, costlyElements.size());
    }

    @Test
    public void computeCostWithTooLowCost() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithCosts(0.5e-10);

        assertEquals(0, evaluator.computeCost(currentFlowResult, Mockito.mock(ComputationStatus.class)), 1e-12);
    }

    @Test
    public void testVirtualCostComputationInMW() {
        testCost(-100, 0, 0, 0);
        testCost(-100, -50, 0, 0);
        testCost(-100, -150, 0, 60);
        testCost(-100, -200, 500, 160);
        testCost(-100, -250, 1000, 260);
        testCost(30, 0, 0, 0);
        testCost(30, -20, 0, 40);
        testCost(30, -50, 300, 100);
        testCost(200, 200, 0, 0);
        testCost(200, 100, 0, 0);
        testCost(200, 0, 0, 0);
        testCost(200, -10, 100, 20);
    }

    private void testCost(double initMargin, double newMargin, double expectedCostWithEval1, double expectedCostWithEval2) {
        when(initialFlowResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(initMargin);
        when(currentFlowResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(newMargin);

        assertEquals(
                expectedCostWithEval1,
                evaluator1.computeCost(currentFlowResult, Mockito.mock(ComputationStatus.class)),
                DOUBLE_TOLERANCE
        );

        assertEquals(
                expectedCostWithEval2,
                evaluator2.computeCost(currentFlowResult, Mockito.mock(ComputationStatus.class)),
                DOUBLE_TOLERANCE
        );
    }
}
