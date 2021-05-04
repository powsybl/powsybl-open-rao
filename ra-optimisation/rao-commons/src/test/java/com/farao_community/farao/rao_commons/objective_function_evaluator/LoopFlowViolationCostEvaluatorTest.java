/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import com.farao_community.farao.rao_api.parameters.LoopFlowParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private BranchCnec cnec1;
    private BranchCnec cnec2;
    private BranchResult initialLoopFlows;
    private BranchResult currentLoopFlows;
    private SensitivityStatus sensitivityStatus;
    private LoopFlowParameters parameters;
    private LoopFlowViolationCostEvaluator evaluator;

    @Before
    public void setUp() {
        CnecLoopFlowExtension cnec1Extension = Mockito.mock(CnecLoopFlowExtension.class);
        cnec1 = Mockito.mock(BranchCnec.class);
        when(cnec1.getExtension(CnecLoopFlowExtension.class)).thenReturn(cnec1Extension);

        CnecLoopFlowExtension cnec2Extension = Mockito.mock(CnecLoopFlowExtension.class);
        cnec2 = Mockito.mock(BranchCnec.class);
        when(cnec2.getExtension(CnecLoopFlowExtension.class)).thenReturn(cnec2Extension);

        initialLoopFlows = Mockito.mock(BranchResult.class);
        currentLoopFlows = Mockito.mock(BranchResult.class);
        sensitivityStatus = Mockito.mock(SensitivityStatus.class);
        parameters = Mockito.mock(LoopFlowParameters.class);
    }

    private void setInputThresholdWithReliabilityMargin(BranchCnec branchCnec, double inputThresholdWIthReliabilityMargin) {
        CnecLoopFlowExtension cnecLoopFlowExtension = branchCnec.getExtension(CnecLoopFlowExtension.class);
        when(cnecLoopFlowExtension.getThresholdWithReliabilityMargin(Unit.MEGAWATT)).thenReturn(inputThresholdWIthReliabilityMargin);
    }

    private void setInitialLoopFLow(BranchCnec branchCnec, double initialLoopFLow) {
        when(initialLoopFlows.getLoopFlow(branchCnec, Unit.MEGAWATT)).thenReturn(initialLoopFLow);
    }

    private void setCurrentLoopFLow(BranchCnec branchCnec, double currentLoopFlow) {
        when(currentLoopFlows.getLoopFlow(branchCnec, Unit.MEGAWATT)).thenReturn(currentLoopFlow);
    }

    private void setAcceptableAugmentationInMW(double acceptableAugmentationInMW) {
        when(parameters.getLoopFlowAcceptableAugmentation()).thenReturn(acceptableAugmentationInMW);
    }

    private void setViolationCost(double violationCost) {
        when(parameters.getLoopFlowViolationCost()).thenReturn(violationCost);
    }

    private void buildLoopFlowViolationCostEvaluator() {
        evaluator = new LoopFlowViolationCostEvaluator(Set.of(cnec1, cnec2), initialLoopFlows, parameters);
    }

    @Test
    public void testGetName() {
        buildLoopFlowViolationCostEvaluator();
        assertEquals("loop-flow-cost", evaluator.getName());
    }

    @Test
    public void testGetUnit() {
        buildLoopFlowViolationCostEvaluator();
        assertEquals(Unit.MEGAWATT, evaluator.getUnit());
    }

    @Test
    public void testLoopFlowExcessWithInitialAndCurrentLoopFlowBelowInputThreshold() {
        // When initial loop-flow + acceptable augmentation is below input threshold, it is the limiting element
        setAcceptableAugmentationInMW(0);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 10);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(0, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowExcessWithCurrentLoopFLowAboveInputThresholdAndNoAcceptableDiminution() {
        // When initial loop-flow + acceptable augmentation is below input threshold, it is the limiting element
        setAcceptableAugmentationInMW(0);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 190);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(90, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowExcessWithCurrentLoopFLowAboveInputThresholdAndWithAcceptableAugmentation() {
        // Acceptable augmentation should have no effect when the loop-flow is limited by input threshold
        setAcceptableAugmentationInMW(20);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 190);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(90, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowExcessWithNegativeLoopFlow() {
        // Loop-flow excess must be computed toward absolute value of loop-flow
        setAcceptableAugmentationInMW(0);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, -190);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(90, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowExcessInitialLoopFlowAboveThreshold() {
        // When initial loop-flow + acceptable augmentation is above input threshold, they are the limiting elements
        setAcceptableAugmentationInMW(0);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 150);
        setCurrentLoopFLow(cnec1, 200);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(50, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowExcessInitialLoopFlowAboveThresholdAndAcceptableAugmentation() {
        // When initial loop-flow + acceptable augmentation is above input threshold, they are the limiting elements
        setAcceptableAugmentationInMW(20);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 150);
        setCurrentLoopFLow(cnec1, 200);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(30, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCostWithTwoCnecs() {
        setViolationCost(1);
        setAcceptableAugmentationInMW(0);

        // Loop-flow excess is 100MW
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 200);

        // Loop-flow excess is 50MW
        setInputThresholdWithReliabilityMargin(cnec2, 100);
        setInitialLoopFLow(cnec2, 150);
        setCurrentLoopFLow(cnec2, 200);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(150, evaluator.computeCost(currentLoopFlows, sensitivityStatus), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCostWithTwoCnecsWithDifferentCost() {
        setViolationCost(2);
        setAcceptableAugmentationInMW(0);

        // Loop-flow excess is 100MW
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 200);

        // Loop-flow excess is 50MW
        setInputThresholdWithReliabilityMargin(cnec2, 100);
        setInitialLoopFLow(cnec2, 150);
        setCurrentLoopFLow(cnec2, 200);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(300, evaluator.computeCost(currentLoopFlows, sensitivityStatus), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCostlyElements() {
        setViolationCost(1);
        setAcceptableAugmentationInMW(0);

        // Loop-flow excess is 100MW
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 200);

        // Loop-flow excess is 50MW
        setInputThresholdWithReliabilityMargin(cnec2, 100);
        setInitialLoopFLow(cnec2, 150);
        setCurrentLoopFLow(cnec2, 200);

        buildLoopFlowViolationCostEvaluator();

        List<BranchCnec> costlyElements = evaluator.getCostlyElements(currentLoopFlows, 5);
        assertEquals(2, costlyElements.size());
        assertSame(cnec1, costlyElements.get(0));
        assertSame(cnec2, costlyElements.get(1));
    }

    @Test
    public void testCostlyElementsWithLimitedElements() {
        setViolationCost(1);
        setAcceptableAugmentationInMW(0);

        // Loop-flow excess is 100MW
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 200);

        // Loop-flow excess is 50MW
        setInputThresholdWithReliabilityMargin(cnec2, 100);
        setInitialLoopFLow(cnec2, 150);
        setCurrentLoopFLow(cnec2, 200);

        buildLoopFlowViolationCostEvaluator();

        List<BranchCnec> costlyElements = evaluator.getCostlyElements(currentLoopFlows, 1);
        assertEquals(1, costlyElements.size());
        assertSame(cnec1, costlyElements.get(0));
    }

    @Test
    public void testCostlyElementsWithNonCostlyElements() {
        setViolationCost(1);
        setAcceptableAugmentationInMW(0);

        // Loop-flow excess is null
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 70);

        // Loop-flow excess is 50MW
        setInputThresholdWithReliabilityMargin(cnec2, 100);
        setInitialLoopFLow(cnec2, 150);
        setCurrentLoopFLow(cnec2, 200);

        buildLoopFlowViolationCostEvaluator();

        List<BranchCnec> costlyElements = evaluator.getCostlyElements(currentLoopFlows, 5);
        assertEquals(1, costlyElements.size());
        assertSame(cnec2, costlyElements.get(0));
    }
}
