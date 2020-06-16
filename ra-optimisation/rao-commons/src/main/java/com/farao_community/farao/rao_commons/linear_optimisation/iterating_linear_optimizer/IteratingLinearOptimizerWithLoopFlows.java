/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.LoopFlowComputation;
import com.farao_community.farao.rao_commons.linear_optimisation.SimpleLinearOptimizer;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import com.farao_community.farao.util.SensitivityComputationException;

import java.util.Map;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerWithLoopFlows extends IteratingLinearOptimizer {
    public IteratingLinearOptimizerWithLoopFlows(SystematicSensitivityComputation systematicSensitivityComputation, RaoParameters raoParameters) {
        super(systematicSensitivityComputation, raoParameters);
    }

    IteratingLinearOptimizerWithLoopFlows(SystematicSensitivityComputation systematicSensitivityComputation, SimpleLinearOptimizer simpleLinearOptimizer, IteratingLinearOptimizerParameters parameters) {
        super(systematicSensitivityComputation, simpleLinearOptimizer, parameters);
    }

    @Override
    protected boolean evaluateNewCost(String optimizedVariantId, int iteration) {
        // If evaluating the new cost fails iteration can stop
        raoData.setWorkingVariant(optimizedVariantId);
        try {
            LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_START, iteration));
            systematicSensitivityComputation.run(raoData);
            raoData.getRaoDataManager().fillCracResultsWithSensis(simpleLinearOptimizer.getParameters().getObjectiveFunction(), systematicSensitivityComputation);
            Map<String, Double> loopFlows = LoopFlowComputation.calculateLoopFlows(raoData);
            raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopFlows);
            LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_END, iteration));
            return true;
        } catch (SensitivityComputationException e) {
            LOGGER.error(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_ERROR, iteration, systematicSensitivityComputation.isFallback() ? "Fallback" : "Default", e.getMessage()));
            return false;
        }
    }
}
