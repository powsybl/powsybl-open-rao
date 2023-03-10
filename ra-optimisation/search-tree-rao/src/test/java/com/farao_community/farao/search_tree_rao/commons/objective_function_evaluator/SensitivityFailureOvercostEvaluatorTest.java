/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SensitivityFailureOvercostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowResult flowResult;
    private SensitivityFailureOvercostEvaluator evaluator;
    private RangeActionActivationResult rangeActionActivationResult;
    private SensitivityResult sensitivityResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;

    @Before
    public void setUp() {
        flowResult = Mockito.mock(FlowResult.class);
        rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);
        cnec3 = Mockito.mock(FlowCnec.class);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        State state3 = Mockito.mock(State.class);
        Mockito.when(cnec1.getState()).thenReturn(state1);
        Mockito.when(cnec2.getState()).thenReturn(state2);
        Mockito.when(cnec3.getState()).thenReturn(state3);
        Mockito.when(sensitivityResult.getSensitivityStatus(state1)).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(sensitivityResult.getSensitivityStatus(state2)).thenReturn(ComputationStatus.FALLBACK);
        Mockito.when(sensitivityResult.getSensitivityStatus(state3)).thenReturn(ComputationStatus.FAILURE);
    }

    @Test
    public void testGetName() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1));
        assertEquals("sensitivity-failure-cost", evaluator.getName());
    }

    @Test
    public void testGetUnit() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1));
        assertEquals(Unit.MEGAWATT, evaluator.getUnit());
    }

    @Test
    public void testCostWithNoStateInFailure() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1, cnec2));
        assertEquals(100000, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FAILURE).getLeft(), DOUBLE_TOLERANCE);
        assertTrue(evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FAILURE).getRight().isEmpty());
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getLeft(), DOUBLE_TOLERANCE);
        assertTrue(evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getRight().isEmpty());
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FALLBACK).getLeft(), DOUBLE_TOLERANCE);
        assertTrue(evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FALLBACK).getRight().isEmpty());
    }

    @Test
    public void testCostWithStateInFailure() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1, cnec2, cnec3));
        assertEquals(100000, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FAILURE).getLeft(), DOUBLE_TOLERANCE);
        assertTrue(evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FAILURE).getRight().isEmpty());
        assertEquals(100000, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getLeft(), DOUBLE_TOLERANCE);
        assertTrue(evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getRight().isEmpty());
        assertEquals(100000, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FALLBACK).getLeft(), DOUBLE_TOLERANCE);
        assertTrue(evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FALLBACK).getRight().isEmpty());
    }

    @Test
    public void testGetFlowCnecs() {
        evaluator = new SensitivityFailureOvercostEvaluator(Set.of(cnec1, cnec2));
        assertEquals(0, evaluator.getFlowCnecs().size());
    }
}
