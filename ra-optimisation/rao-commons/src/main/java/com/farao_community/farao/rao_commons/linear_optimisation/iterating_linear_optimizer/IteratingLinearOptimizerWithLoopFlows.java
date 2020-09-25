/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_commons.LoopFlowComputationService;
import com.farao_community.farao.rao_commons.ObjectiveFunctionEvaluator;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_computation.SensitivityComputationException;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerWithLoopFlows extends IteratingLinearOptimizer {

    private boolean loopFlowApproximation;
    private double loopFlowViolationCost;

    public IteratingLinearOptimizerWithLoopFlows(List<ProblemFiller> fillers,
                                                 SystematicSensitivityInterface systematicSensitivityInterface,
                                                 ObjectiveFunctionEvaluator objectiveFunctionEvaluator,
                                                 IteratingLinearOptimizerWithLoopFLowsParameters parameters) {
        super(fillers, systematicSensitivityInterface, objectiveFunctionEvaluator, parameters);
        loopFlowApproximation = parameters.isLoopflowApproximation();
        loopFlowViolationCost = parameters.getLoopFlowViolationCost();
        linearOptimizer = new LinearOptimizer(fillers);
    }

    @Override
    protected boolean evaluateNewCost(String optimizedVariantId, int iteration) {
        // If evaluating the new cost fails iteration can stop
        raoData.setWorkingVariant(optimizedVariantId);
        try {
            LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_START, iteration));
            runSensitivityAndUpdateResults();
            Map<String, Double> loopFlows = LoopFlowComputationService.calculateLoopFlows(raoData, loopFlowApproximation);
            raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopFlows, loopFlowViolationCost);
            LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_END, iteration));
            return true;
        } catch (SensitivityComputationException e) {
            LOGGER.error(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_ERROR, iteration,
                systematicSensitivityInterface.isFallback() ? "Fallback" : "Default", e.getMessage()));
            return false;
        }
    }
}
