/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
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
class ObjectiveFunctionTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private MinMarginEvaluator minMarginEvaluator;
    private MnecViolationCostEvaluator mnecViolationCostEvaluator;
    private LoopFlowViolationCostEvaluator loopFlowViolationCostEvaluator;
    private FlowResult flowResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;

    @BeforeEach
    public void setUp() {
        flowResult = Mockito.mock(FlowResult.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);

        minMarginEvaluator = Mockito.mock(MinMarginEvaluator.class);
        when(minMarginEvaluator.computeCostAndLimitingElements(flowResult)).thenReturn(Pair.of(-300., List.of(cnec1, cnec2)));
        when(minMarginEvaluator.computeCostAndLimitingElements(flowResult, new HashSet<>())).thenReturn(Pair.of(-300., List.of(cnec1, cnec2)));

        mnecViolationCostEvaluator = Mockito.mock(MnecViolationCostEvaluator.class);
        when(mnecViolationCostEvaluator.getName()).thenReturn("mnec-cost");
        when(mnecViolationCostEvaluator.computeCostAndLimitingElements(flowResult)).thenReturn(Pair.of(1000., List.of(cnec1)));
        when(mnecViolationCostEvaluator.computeCostAndLimitingElements(flowResult, new HashSet<>())).thenReturn(Pair.of(1000., List.of(cnec1)));

        loopFlowViolationCostEvaluator = Mockito.mock(LoopFlowViolationCostEvaluator.class);
        when(loopFlowViolationCostEvaluator.getName()).thenReturn("loop-flow-cost");
        when(loopFlowViolationCostEvaluator.computeCostAndLimitingElements(flowResult)).thenReturn(Pair.of(100., List.of(cnec2)));
        when(loopFlowViolationCostEvaluator.computeCostAndLimitingElements(flowResult, new HashSet<>())).thenReturn(Pair.of(100., List.of(cnec2)));
    }

    @Test
    void testWithFunctionalCostOnly() {
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create()
                .withFunctionalCostEvaluator(minMarginEvaluator)
                .build();

        // functional cost
        assertEquals(-300., objectiveFunction.getFunctionalCostAndLimitingElements(flowResult).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), objectiveFunction.getFunctionalCostAndLimitingElements(flowResult).getRight());

        // virtual cost
        assertTrue(objectiveFunction.getVirtualCostNames().isEmpty());
        assertTrue(Double.isNaN(objectiveFunction.getVirtualCostAndCostlyElements(flowResult, "mnec-cost", new HashSet<>()).getLeft()));
        assertTrue(objectiveFunction.getVirtualCostAndCostlyElements(flowResult, "mnec-cost", new HashSet<>()).getRight().isEmpty());

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult);
        assertEquals(-300., result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0., result.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(-300., result.getCost(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), result.getMostLimitingElements(10));
        assertTrue(result.getVirtualCostNames().isEmpty());
    }

    @Test
    void testWithVirtualCostOnly() {
        ObjectiveFunction.ObjectiveFunctionBuilder objectiveFunctionBuilder = ObjectiveFunction.create()
                .withVirtualCostEvaluator(mnecViolationCostEvaluator);
        assertThrows(NullPointerException.class, objectiveFunctionBuilder::build);
    }

    @Test
    void testWithFunctionalAndVirtualCost() {
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create()
                .withFunctionalCostEvaluator(minMarginEvaluator)
                .withVirtualCostEvaluator(mnecViolationCostEvaluator)
                .withVirtualCostEvaluator(loopFlowViolationCostEvaluator)
                .build();

        // functional cost
        assertEquals(-300., objectiveFunction.getFunctionalCostAndLimitingElements(flowResult).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), objectiveFunction.getFunctionalCostAndLimitingElements(flowResult).getRight());

        // virtual cost sum
        assertEquals(2, objectiveFunction.getVirtualCostNames().size());
        assertTrue(objectiveFunction.getVirtualCostNames().containsAll(Set.of("mnec-cost", "loop-flow-cost")));

        // mnec virtual cost
        assertEquals(1000., objectiveFunction.getVirtualCostAndCostlyElements(flowResult, "mnec-cost", new HashSet<>()).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1), objectiveFunction.getVirtualCostAndCostlyElements(flowResult, "mnec-cost", new HashSet<>()).getRight());

        // loopflow virtual cost
        assertEquals(100., objectiveFunction.getVirtualCostAndCostlyElements(flowResult, "loop-flow-cost", new HashSet<>()).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec2), objectiveFunction.getVirtualCostAndCostlyElements(flowResult, "loop-flow-cost", new HashSet<>()).getRight());

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult);
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

    @Test
    void testBuildForInitialSensitivityComputation() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);

        searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(0.);
        ObjectiveFunction objectiveFunction = new ObjectiveFunction.ObjectiveFunctionBuilder().buildForInitialSensitivityComputation(
            Set.of(cnec1, cnec2), raoParameters
        );
        assertTrue(objectiveFunction.getVirtualCostNames().isEmpty());

        searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(1.);
        objectiveFunction = new ObjectiveFunction.ObjectiveFunctionBuilder().buildForInitialSensitivityComputation(
            Set.of(cnec1, cnec2), raoParameters
        );
        assertEquals(Set.of("sensitivity-failure-cost"), objectiveFunction.getVirtualCostNames());
    }
}
