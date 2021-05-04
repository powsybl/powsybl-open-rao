/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SensitivityFallbackOvercostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private BranchResult branchResult;
    private SensitivityFallbackOvercostEvaluator evaluator;

    @Before
    public void setUp() {
        branchResult = Mockito.mock(BranchResult.class);
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
        assertEquals(0, evaluator.computeCost(branchResult, SensitivityStatus.DEFAULT), DOUBLE_TOLERANCE);
        assertEquals(0, evaluator.computeCost(branchResult, SensitivityStatus.FALLBACK), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithCost() {
        evaluator = new SensitivityFallbackOvercostEvaluator(10);
        assertEquals(0, evaluator.computeCost(branchResult, SensitivityStatus.DEFAULT), DOUBLE_TOLERANCE);
        assertEquals(10, evaluator.computeCost(branchResult, SensitivityStatus.FALLBACK), DOUBLE_TOLERANCE);
    }

    @Test (expected = FaraoException.class)
    public void testFailure() {
        evaluator = new SensitivityFallbackOvercostEvaluator(10);
        evaluator.computeCost(branchResult, SensitivityStatus.FAILURE);
    }

    @Test
    public void testGetCostlyElements() {
        evaluator = new SensitivityFallbackOvercostEvaluator(10);
        assertEquals(0, evaluator.getCostlyElements(branchResult, 5).size());
    }
}
