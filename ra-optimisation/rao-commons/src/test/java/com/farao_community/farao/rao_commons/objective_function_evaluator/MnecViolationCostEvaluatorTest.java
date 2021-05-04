/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import com.farao_community.farao.rao_api.parameters.MnecParameters;
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

    private BranchCnec mnec1;
    private BranchCnec mnec2;
    private BranchCnec pureCnec;
    private BranchResult initialBranchResult;
    private BranchResult currentBranchResult;
    private MnecViolationCostEvaluator evaluator1;
    private MnecViolationCostEvaluator evaluator2;

    @Before
    public void setUp() {
        mnec1 = Mockito.mock(BranchCnec.class);
        when(mnec1.isOptimized()).thenReturn(true);
        when(mnec1.isMonitored()).thenReturn(true);
        mnec2 = Mockito.mock(BranchCnec.class);
        when(mnec2.isOptimized()).thenReturn(false);
        when(mnec2.isMonitored()).thenReturn(true);
        pureCnec = Mockito.mock(BranchCnec.class);
        when(pureCnec.isOptimized()).thenReturn(true);
        when(pureCnec.isMonitored()).thenReturn(false);

        initialBranchResult = Mockito.mock(BranchResult.class);
        currentBranchResult = Mockito.mock(BranchResult.class);

        evaluator1 = new MnecViolationCostEvaluator(
                Set.of(mnec1, pureCnec),
                initialBranchResult,
                new MnecParameters(50, 10, 1)
        );
        evaluator2 = new MnecViolationCostEvaluator(
                Set.of(mnec1, pureCnec),
                initialBranchResult,
                new MnecParameters(20, 2, 1)
        );
    }

    private MnecViolationCostEvaluator createEvaluatorWithCosts(double violationCost) {
        when(initialBranchResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(-200.);
        when(currentBranchResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(-270.);
        when(initialBranchResult.getMargin(mnec2, Unit.MEGAWATT)).thenReturn(-200.);
        when(currentBranchResult.getMargin(mnec2, Unit.MEGAWATT)).thenReturn(-400.);

        return new MnecViolationCostEvaluator(
                Set.of(mnec1, mnec2, pureCnec),
                initialBranchResult,
                new MnecParameters(50, violationCost, 1)
        );
    }

    private MnecViolationCostEvaluator createEvaluatorWithNoCosts() {
        when(initialBranchResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(200.);
        when(currentBranchResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(300.);
        when(initialBranchResult.getMargin(mnec2, Unit.MEGAWATT)).thenReturn(200.);
        when(currentBranchResult.getMargin(mnec2, Unit.MEGAWATT)).thenReturn(150.);

        return new MnecViolationCostEvaluator(
                Set.of(mnec1, mnec2, pureCnec),
                initialBranchResult,
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

        List<BranchCnec> costlyElements = evaluator.getCostlyElements(currentBranchResult, 5);
        assertEquals(2, costlyElements.size());
        assertSame(mnec2, costlyElements.get(0));
        assertSame(mnec1, costlyElements.get(1));
    }

    @Test
    public void getCostlyElementsWithLimitedElements() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithCosts(10);

        List<BranchCnec> costlyElements = evaluator.getCostlyElements(currentBranchResult, 1);
        assertEquals(1, costlyElements.size());
        assertSame(mnec2, costlyElements.get(0));
    }

    @Test
    public void getCostlyElementsWithNoCostlyElements() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithNoCosts();

        List<BranchCnec> costlyElements = evaluator.getCostlyElements(currentBranchResult, 5);
        assertEquals(0, costlyElements.size());
    }

    @Test
    public void computeCostWithTooLowCost() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithCosts(0.5e-10);

        assertEquals(0, evaluator.computeCost(currentBranchResult, Mockito.mock(SensitivityStatus.class)), 1e-12);
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
        when(initialBranchResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(initMargin);
        when(currentBranchResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(newMargin);

        assertEquals(
                expectedCostWithEval1,
                evaluator1.computeCost(currentBranchResult, Mockito.mock(SensitivityStatus.class)),
                DOUBLE_TOLERANCE
        );

        assertEquals(
                expectedCostWithEval2,
                evaluator2.computeCost(currentBranchResult, Mockito.mock(SensitivityStatus.class)),
                DOUBLE_TOLERANCE
        );
    }
}
