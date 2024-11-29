/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class LoopFlowViolationCostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowResult initialLoopFlows;
    private FlowResult currentLoopFlows;
    private LoopFlowParameters parameters;
    private com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParameters parametersExtension;

    private LoopFlowViolationCostEvaluator evaluator;

    @BeforeEach
    public void setUp() {
        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());
        LoopFlowThreshold cnec1Extension = Mockito.mock(LoopFlowThreshold.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        when(cnec1.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE));
        when(cnec1.getExtension(LoopFlowThreshold.class)).thenReturn(cnec1Extension);
        when(cnec1.getState()).thenReturn(state);

        LoopFlowThreshold cnec2Extension = Mockito.mock(LoopFlowThreshold.class);
        cnec2 = Mockito.mock(FlowCnec.class);
        when(cnec2.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE));
        when(cnec2.getExtension(LoopFlowThreshold.class)).thenReturn(cnec2Extension);
        when(cnec2.getState()).thenReturn(state);

        initialLoopFlows = Mockito.mock(FlowResult.class);
        currentLoopFlows = Mockito.mock(FlowResult.class);
        parameters = Mockito.mock(LoopFlowParameters.class);
        parametersExtension = Mockito.mock(com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParameters.class);
    }

    private void setInputThresholdWithReliabilityMargin(FlowCnec branchCnec, double inputThresholdWIthReliabilityMargin) {
        LoopFlowThreshold cnecLoopFlowExtension = branchCnec.getExtension(LoopFlowThreshold.class);
        when(cnecLoopFlowExtension.getThresholdWithReliabilityMargin(Unit.MEGAWATT)).thenReturn(inputThresholdWIthReliabilityMargin);
    }

    private void setInitialLoopFLow(FlowCnec branchCnec, double initialLoopFLow) {
        when(initialLoopFlows.getLoopFlow(branchCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(initialLoopFLow);
    }

    private void setCurrentLoopFLow(FlowCnec branchCnec, double currentLoopFlow) {
        when(currentLoopFlows.getLoopFlow(branchCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(currentLoopFlow);
    }

    private void setAcceptableAugmentationInMW(double acceptableAugmentationInMW) {
        when(parameters.getAcceptableIncrease()).thenReturn(acceptableAugmentationInMW);
    }

    private void setViolationCost(double violationCost) {
        when(parametersExtension.getViolationCost()).thenReturn(violationCost);
    }

    private void buildLoopFlowViolationCostEvaluator() {
        evaluator = new LoopFlowViolationCostEvaluator(Set.of(cnec1, cnec2), initialLoopFlows, parametersExtension.getViolationCost(), parameters.getAcceptableIncrease());
    }

    @Test
    void testGetName() {
        buildLoopFlowViolationCostEvaluator();
        assertEquals("loop-flow-cost", evaluator.getName());
    }

    @Test
    void testGetUnit() {
        buildLoopFlowViolationCostEvaluator();
        assertEquals(Unit.MEGAWATT, evaluator.getUnit());
    }

    @Test
    void testLoopFlowExcessWithInitialAndCurrentLoopFlowBelowInputThreshold() {
        // When initial loop-flow + acceptable augmentation is below input threshold, it is the limiting element
        setAcceptableAugmentationInMW(0);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 10);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(0, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    void testLoopFlowExcessWithCurrentLoopFLowAboveInputThresholdAndNoAcceptableDiminution() {
        // When initial loop-flow + acceptable augmentation is below input threshold, it is the limiting element
        setAcceptableAugmentationInMW(0);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 190);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(90, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    void testLoopFlowExcessWithCurrentLoopFLowAboveInputThresholdAndWithAcceptableAugmentation() {
        // Acceptable augmentation should have no effect when the loop-flow is limited by input threshold
        setAcceptableAugmentationInMW(20);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, 190);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(90, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    void testLoopFlowExcessWithNegativeLoopFlow() {
        // Loop-flow excess must be computed toward absolute value of loop-flow
        setAcceptableAugmentationInMW(0);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 0);
        setCurrentLoopFLow(cnec1, -190);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(90, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    void testLoopFlowExcessInitialLoopFlowAboveThreshold() {
        // When initial loop-flow + acceptable augmentation is above input threshold, they are the limiting elements
        setAcceptableAugmentationInMW(0);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 150);
        setCurrentLoopFLow(cnec1, 200);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(50, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    void testLoopFlowExcessInitialLoopFlowAboveThresholdAndAcceptableAugmentation() {
        // When initial loop-flow + acceptable augmentation is above input threshold, they are the limiting elements
        setAcceptableAugmentationInMW(20);
        setInputThresholdWithReliabilityMargin(cnec1, 100);
        setInitialLoopFLow(cnec1, 150);
        setCurrentLoopFLow(cnec1, 200);

        buildLoopFlowViolationCostEvaluator();

        assertEquals(30, evaluator.getLoopFlowExcess(currentLoopFlows, cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    void testCostWithTwoCnecs() {
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

        assertEquals(150, evaluator.computeCostAndLimitingElements(currentLoopFlows).getLeft(), DOUBLE_TOLERANCE);
    }

    @Test
    void testCostWithTwoCnecsWithDifferentCost() {
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

        assertEquals(300, evaluator.computeCostAndLimitingElements(currentLoopFlows).getLeft(), DOUBLE_TOLERANCE);
    }

    @Test
    void testCostlyElements() {
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

        List<FlowCnec> costlyElements = evaluator.computeCostAndLimitingElements(currentLoopFlows).getRight();
        assertEquals(2, costlyElements.size());
        assertSame(cnec1, costlyElements.get(0));
        assertSame(cnec2, costlyElements.get(1));
    }

    @Test
    void testCostlyElementsWithNonCostlyElements() {
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

        List<FlowCnec> costlyElements = evaluator.computeCostAndLimitingElements(currentLoopFlows).getRight();
        assertEquals(1, costlyElements.size());
        assertSame(cnec2, costlyElements.get(0));
    }
}
