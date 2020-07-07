/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.farao_community.farao.rao_commons.CostEvaluator;
import com.farao_community.farao.rao_commons.MinMarginEvaluator;
import com.farao_community.farao.rao_commons.SystematicSensitivityComputation;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import org.junit.Test;
import org.mockito.Mockito;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoUtilTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    @Test
    public void createLinearOptimizerFromRaoParameters() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        SystematicSensitivityComputation systematicSensitivityComputation = Mockito.mock(SystematicSensitivityComputation.class);
        IteratingLinearOptimizer optimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityComputation);

        assertTrue(optimizer.getFunctionalCostEvaluator() instanceof MinMarginEvaluator);
        assertEquals(AMPERE, optimizer.getFunctionalCostEvaluator().getUnit());
        assertEquals(0, optimizer.getParameters().getFallbackOverCost(), DOUBLE_TOLERANCE);
        assertEquals(10, optimizer.getParameters().getMaxIterations());
    }

    @Test
    public void createLinearOptimizerFromRaoParametersWithLoopFlows() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setRaoWithLoopFlowLimitation(true);
        SystematicSensitivityComputation systematicSensitivityComputation = Mockito.mock(SystematicSensitivityComputation.class);
        IteratingLinearOptimizer optimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityComputation);

        assertTrue(optimizer instanceof IteratingLinearOptimizerWithLoopFlows);
        assertTrue(optimizer.getFunctionalCostEvaluator() instanceof MinMarginEvaluator);
        assertEquals(MEGAWATT, optimizer.getFunctionalCostEvaluator().getUnit());
        assertEquals(0, optimizer.getParameters().getFallbackOverCost(), DOUBLE_TOLERANCE);
        assertEquals(10, optimizer.getParameters().getMaxIterations());
    }

    @Test
    public void createCostEvaluatorFromRaoParameters() {
        RaoParameters raoParameters = new RaoParameters();
        CostEvaluator costEvaluator = RaoUtil.createCostEvaluator(raoParameters);
        assertTrue(costEvaluator instanceof MinMarginEvaluator);
        assertEquals(MEGAWATT, costEvaluator.getUnit());
    }
}
