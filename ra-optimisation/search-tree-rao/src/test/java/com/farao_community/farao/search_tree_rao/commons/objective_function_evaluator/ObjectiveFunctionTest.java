/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.ObjectiveFunctionResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ObjectiveFunctionTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private MinMarginEvaluator minMarginEvaluator;
    private MnecViolationCostEvaluator mnecViolationCostEvaluator;
    private LoopFlowViolationCostEvaluator loopFlowViolationCostEvaluator;
    private FlowResult flowResult;
    private ComputationStatus sensitivityStatus;
    private RangeActionActivationResult rangeActionActivationResult;
    private SensitivityResult sensitivityResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;

    @BeforeEach
    public void setUp() {
        flowResult = Mockito.mock(FlowResult.class);
        sensitivityStatus = Mockito.mock(ComputationStatus.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);

        rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);

        minMarginEvaluator = Mockito.mock(MinMarginEvaluator.class);
        when(minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus)).thenReturn(Pair.of(-300., List.of(cnec1, cnec2)));
        when(minMarginEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, new HashSet<>())).thenReturn(Pair.of(-300., List.of(cnec1, cnec2)));

        mnecViolationCostEvaluator = Mockito.mock(MnecViolationCostEvaluator.class);
        when(mnecViolationCostEvaluator.getName()).thenReturn("mnec-cost");
        when(mnecViolationCostEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus)).thenReturn(Pair.of(1000., List.of(cnec1)));
        when(mnecViolationCostEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, new HashSet<>())).thenReturn(Pair.of(1000., List.of(cnec1)));

        loopFlowViolationCostEvaluator = Mockito.mock(LoopFlowViolationCostEvaluator.class);
        when(loopFlowViolationCostEvaluator.getName()).thenReturn("loop-flow-cost");
        when(loopFlowViolationCostEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus)).thenReturn(Pair.of(100., List.of(cnec2)));
        when(loopFlowViolationCostEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, new HashSet<>())).thenReturn(Pair.of(100., List.of(cnec2)));
    }

    @Test
    public void testWithFunctionalCostOnly() {
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create()
                .withFunctionalCostEvaluator(minMarginEvaluator)
                .build();

        // functional cost
        assertEquals(-300., objectiveFunction.getFunctionalCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), objectiveFunction.getFunctionalCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus).getRight());

        // virtual cost
        assertTrue(objectiveFunction.getVirtualCostNames().isEmpty());
        assertTrue(Double.isNaN(objectiveFunction.getVirtualCostAndCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "mnec-cost").getLeft()));
        assertTrue(objectiveFunction.getVirtualCostAndCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "mnec-cost").getRight().isEmpty());

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus);
        assertEquals(-300., result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0., result.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(-300., result.getCost(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), result.getMostLimitingElements(10));
        assertTrue(result.getVirtualCostNames().isEmpty());
    }

    @Test
    public void testWithVirtualCostOnly() {
        assertThrows(NullPointerException.class, () ->
            ObjectiveFunction.create()
                    .withVirtualCostEvaluator(mnecViolationCostEvaluator)
                    .build());
    }

    @Test
    public void testWithFunctionalAndVirtualCost() {
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create()
                .withFunctionalCostEvaluator(minMarginEvaluator)
                .withVirtualCostEvaluator(mnecViolationCostEvaluator)
                .withVirtualCostEvaluator(loopFlowViolationCostEvaluator)
                .build();

        // functional cost
        assertEquals(-300., objectiveFunction.getFunctionalCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), objectiveFunction.getFunctionalCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus).getRight());

        // virtual cost sum
        assertEquals(2, objectiveFunction.getVirtualCostNames().size());
        assertTrue(objectiveFunction.getVirtualCostNames().containsAll(Set.of("mnec-cost", "loop-flow-cost")));

        // mnec virtual cost
        assertEquals(1000., objectiveFunction.getVirtualCostAndCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "mnec-cost").getLeft(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1), objectiveFunction.getVirtualCostAndCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "mnec-cost").getRight());

        // loopflow virtual cost
        assertEquals(100., objectiveFunction.getVirtualCostAndCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "loop-flow-cost").getLeft(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec2), objectiveFunction.getVirtualCostAndCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, "loop-flow-cost").getRight());

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus);
        assertEquals(-300., result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(1100., result.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(800., result.getCost(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), result.getMostLimitingElements(10));
        assertEquals(2, result.getVirtualCostNames().size());
        assertTrue(result.getVirtualCostNames().containsAll(Set.of("mnec-cost", "loop-flow-cost")));
        assertEquals(1000., result.getVirtualCost("mnec-cost"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1), result.getCostlyElements("mnec-cost", 10));
        assertEquals(100., result.getVirtualCost("loop-flow-cost"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec2), result.getCostlyElements("loop-flow-cost", 10));
    }
}
