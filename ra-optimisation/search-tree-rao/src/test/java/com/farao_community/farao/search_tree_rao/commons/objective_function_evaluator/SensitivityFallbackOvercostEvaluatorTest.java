/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SensitivityFallbackOvercostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowResult flowResult;
    private SensitivityFallbackOvercostEvaluator evaluator;
    private RangeActionActivationResult rangeActionActivationResult;
    private SensitivityResult sensitivityResult;

    @Before
    public void setUp() {
        flowResult = Mockito.mock(FlowResult.class);
        rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);
    }

    @Test
    public void testGetName() {
        evaluator = new SensitivityFallbackOvercostEvaluator(10);
        assertEquals("sensitivity-fallback-cost", evaluator.getName());
    }

    @Test
    public void testGetUnit() {
        evaluator = new SensitivityFallbackOvercostEvaluator(10);
        assertEquals(Unit.MEGAWATT, evaluator.getUnit());
    }

    @Test
    public void testWithNullCost() {
        evaluator = new SensitivityFallbackOvercostEvaluator(0);
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FALLBACK).getLeft(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithCost() {
        evaluator = new SensitivityFallbackOvercostEvaluator(10);
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getLeft(), DOUBLE_TOLERANCE);
        assertEquals(10, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FALLBACK).getLeft(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFailure() {
        evaluator = new SensitivityFallbackOvercostEvaluator(10);
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.FAILURE).getLeft(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCostlyElements() {
        evaluator = new SensitivityFallbackOvercostEvaluator(10);
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT).getRight().size());
        assertEquals(0, evaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, ComputationStatus.DEFAULT, Set.of("")).getRight().size());
    }
}
