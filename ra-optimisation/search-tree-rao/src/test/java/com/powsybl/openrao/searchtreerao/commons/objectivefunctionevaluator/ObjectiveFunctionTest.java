/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

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
    private SensitivityResult sensitivityResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;

    @BeforeEach
    public void setUp() {
        State preventiveState = Mockito.mock(State.class);
        Instant preventiveInstant = Mockito.mock(Instant.class);
        when(preventiveState.getInstant()).thenReturn(preventiveInstant);
        when(preventiveInstant.isPreventive()).thenReturn(true);
        flowResult = Mockito.mock(FlowResult.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        when(cnec1.getState()).thenReturn(preventiveState);
        cnec2 = Mockito.mock(FlowCnec.class);
        when(cnec2.getState()).thenReturn(preventiveState);

        sensitivityResult = Mockito.mock(SensitivityResult.class);

        minMarginEvaluator = Mockito.mock(MinMarginEvaluator.class);
        Map<FlowCnec, Double> minMarginMap = new LinkedHashMap<>();
        minMarginMap.put(cnec1, -300.);
        minMarginMap.put(cnec2, -400.);
        when(minMarginEvaluator.computeCostAndLimitingElements(flowResult, sensitivityResult)).thenReturn(Pair.of(-300., minMarginMap));
        when(minMarginEvaluator.computeCostAndLimitingElements(flowResult, sensitivityResult, new HashSet<>())).thenReturn(Pair.of(-300., minMarginMap));

        mnecViolationCostEvaluator = Mockito.mock(MnecViolationCostEvaluator.class);
        when(mnecViolationCostEvaluator.getName()).thenReturn("mnec-cost");
        when(mnecViolationCostEvaluator.computeCostAndLimitingElements(flowResult, sensitivityResult)).thenReturn(Pair.of(1000., Map.of(cnec1, 1000.)));
        when(mnecViolationCostEvaluator.computeCostAndLimitingElements(flowResult, sensitivityResult, new HashSet<>())).thenReturn(Pair.of(1000., Map.of(cnec1, 1000.)));

        loopFlowViolationCostEvaluator = Mockito.mock(LoopFlowViolationCostEvaluator.class);
        when(loopFlowViolationCostEvaluator.getName()).thenReturn("loop-flow-cost");
        when(loopFlowViolationCostEvaluator.computeCostAndLimitingElements(flowResult, sensitivityResult)).thenReturn(Pair.of(100., Map.of(cnec2, 100.)));
        when(loopFlowViolationCostEvaluator.computeCostAndLimitingElements(flowResult, sensitivityResult, new HashSet<>())).thenReturn(Pair.of(100., Map.of(cnec2, 100.)));
    }

    @Test
    void testWithFunctionalCostOnly() {
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create()
                .withFunctionalCostEvaluator(minMarginEvaluator)
                .build();

        Pair<Double, Map<FlowCnec, Double>> costAndMostLimitingElements = objectiveFunction.getFunctionalCostAndLimitingElements(flowResult, sensitivityResult);
        // functional cost
        assertEquals(-300., costAndMostLimitingElements.getLeft(), DOUBLE_TOLERANCE);
        Iterator<FlowCnec> iterator = costAndMostLimitingElements.getRight().keySet().iterator();
        assertEquals(cnec1, iterator.next());
        assertEquals(cnec2, iterator.next());

        // virtual cost
        assertTrue(objectiveFunction.getVirtualCostNames().isEmpty());
        assertTrue(Double.isNaN(objectiveFunction.getVirtualCostAndCostlyElements(flowResult, sensitivityResult, "mnec-cost").getLeft()));
        assertTrue(objectiveFunction.getVirtualCostAndCostlyElements(flowResult, sensitivityResult, "mnec-cost").getRight().isEmpty());

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult, sensitivityResult);
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

        Pair<Double, Map<FlowCnec, Double>> costAndMostLimitingElements = objectiveFunction.getFunctionalCostAndLimitingElements(flowResult, sensitivityResult);
        // functional cost
        assertEquals(-300., costAndMostLimitingElements.getLeft(), DOUBLE_TOLERANCE);
        Iterator<FlowCnec> iterator = costAndMostLimitingElements.getRight().keySet().iterator();
        assertEquals(cnec1, iterator.next());
        assertEquals(cnec2, iterator.next());

        // virtual cost sum
        assertEquals(2, objectiveFunction.getVirtualCostNames().size());
        assertTrue(objectiveFunction.getVirtualCostNames().containsAll(Set.of("mnec-cost", "loop-flow-cost")));

        // mnec virtual cost
        assertEquals(1000., objectiveFunction.getVirtualCostAndCostlyElements(flowResult, sensitivityResult, "mnec-cost").getLeft(), DOUBLE_TOLERANCE);
        assertEquals(Map.of(cnec1, 1000.), objectiveFunction.getVirtualCostAndCostlyElements(flowResult, sensitivityResult, "mnec-cost").getRight());

        // loopflow virtual cost
        assertEquals(100., objectiveFunction.getVirtualCostAndCostlyElements(flowResult, sensitivityResult, "loop-flow-cost").getLeft(), DOUBLE_TOLERANCE);
        assertEquals(Map.of(cnec2, 100.), objectiveFunction.getVirtualCostAndCostlyElements(flowResult, sensitivityResult, "loop-flow-cost").getRight());

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult, sensitivityResult);
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

        raoParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(0.);
        ObjectiveFunction objectiveFunction = new ObjectiveFunction.ObjectiveFunctionBuilder().buildForInitialSensitivityComputation(
            Set.of(cnec1, cnec2), raoParameters
        );
        assertTrue(objectiveFunction.getVirtualCostNames().isEmpty());

        raoParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(1.);
        objectiveFunction = new ObjectiveFunction.ObjectiveFunctionBuilder().buildForInitialSensitivityComputation(
            Set.of(cnec1, cnec2), raoParameters
        );
        assertEquals(Set.of("sensitivity-failure-cost"), objectiveFunction.getVirtualCostNames());
    }
}
