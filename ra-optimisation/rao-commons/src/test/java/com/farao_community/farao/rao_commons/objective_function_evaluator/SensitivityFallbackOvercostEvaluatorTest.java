/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SensitivityFallbackOvercostEvaluatorTest {

    private static final double DOUBLE_TOLERANCE = 0.01;
    private SystematicSensitivityResult systematicSensitivityResult;
    private RaoData raoData;

    @Before
    public void setUp() {
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        raoData = Mockito.mock(RaoData.class);
        Mockito.when(raoData.getSystematicSensitivityResult()).thenReturn(systematicSensitivityResult);
    }

    @Test
    public void testSuccess() {
        Mockito.when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        assertEquals(0., new SensitivityFallbackOvercostEvaluator(0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(0., new SensitivityFallbackOvercostEvaluator(10.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(0., new SensitivityFallbackOvercostEvaluator(100.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFallBack() {
        Mockito.when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.FALLBACK);
        assertEquals(0., new SensitivityFallbackOvercostEvaluator(0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(10., new SensitivityFallbackOvercostEvaluator(10.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(100., new SensitivityFallbackOvercostEvaluator(100.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test (expected = FaraoException.class)
    public void testFailure() {
        Mockito.when(systematicSensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        new SensitivityFallbackOvercostEvaluator(100.).getCost(raoData);
    }
}
