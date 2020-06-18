/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_commons.LoopFlowComputation;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.rao_commons.SystematicSensitivityComputation;
import com.farao_community.farao.util.SensitivityComputationException;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerWithLoopFlows extends IteratingLinearOptimizer {

    private boolean loopFlowApproximation;

    public IteratingLinearOptimizerWithLoopFlows(List<ProblemFiller> fillers,
                                                 SystematicSensitivityComputation systematicSensitivityComputation,
                                                 IteratingLinearOptimizerWithLoopFLowsParameters parameters) {
        super(fillers, systematicSensitivityComputation, parameters);
        loopFlowApproximation = parameters.isLoopflowApproximation();
        linearOptimizer = new LinearOptimizer(fillers);
    }

    @Override
    protected boolean evaluateNewCost(String optimizedVariantId, int iteration) {
        // If evaluating the new cost fails iteration can stop
        raoData.setWorkingVariant(optimizedVariantId);
        try {
            LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_START, iteration));
            systematicSensitivityComputation.run(raoData, parameters.getUnit());
            raoData.getRaoDataManager().fillCracResultsWithSensis(parameters.getUnit(),
                systematicSensitivityComputation.isFallback() ? parameters.getFallbackOverCost() : 0);
            Map<String, Double> loopFlows = LoopFlowComputation.calculateLoopFlows(raoData, loopFlowApproximation);
            raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopFlows);
            LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_END, iteration));
            return true;
        } catch (SensitivityComputationException e) {
            LOGGER.error(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_ERROR, iteration, systematicSensitivityComputation.isFallback() ? "Fallback" : "Default", e.getMessage()));
            return false;
        }
    }
}
