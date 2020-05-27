/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.SimpleLinearOptimizer;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputationParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.core.LinearProblemParameters;
import com.powsybl.computation.DefaultComputationManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(IteratingLinearOptimizer.class);

    private IteratingLinearOptimizer() { }

    public static String optimize(RaoData raoData,
                                  RaoParameters raoParameters) {
        return optimize(raoData, raoData.getInitialVariantId(), raoParameters);
    }

    public static String optimize(RaoData raoData,
                                  String initialRaoDataVariantId,
                                  RaoParameters raoParameters) {
        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(
            raoParameters.getExtension(SystematicSensitivityComputationParameters.class),
            DefaultComputationManagerConfig.load().createLongTimeExecutionComputationManager());
        return optimize(raoData, initialRaoDataVariantId, systematicSensitivityComputation, raoParameters);
    }

    public static String optimize(RaoData raoData,
                                  SystematicSensitivityComputation systematicSensitivityComputation,
                                  RaoParameters raoParameters) {
        return optimize(raoData, raoData.getInitialVariantId(), systematicSensitivityComputation, raoParameters);
    }

    public static String optimize(RaoData raoData,
                                  String referenceVariantId,
                                  SystematicSensitivityComputation systematicSensitivityComputation,
                                  RaoParameters raoParameters) {
        return optimize(raoData, referenceVariantId, systematicSensitivityComputation, new SimpleLinearOptimizer(raoParameters), raoParameters);
    }

    public static String optimize(RaoData raoData,
                                  SystematicSensitivityComputation systematicSensitivityComputation,
                                  SimpleLinearOptimizer simpleLinearOptimizer,
                                  RaoParameters raoParameters) {
        return optimize(raoData, raoData.getInitialVariantId(), systematicSensitivityComputation, simpleLinearOptimizer, raoParameters);
    }

    public static String optimize(RaoData raoData,
                                  String referenceVariantId,
                                  SystematicSensitivityComputation systematicSensitivityComputation,
                                  SimpleLinearOptimizer simpleLinearOptimizer,
                                  RaoParameters raoParameters) {
        raoData.setWorkingVariant(referenceVariantId);
        String bestVariantId = referenceVariantId;
        String optimizedVariantId;

        for (int iteration = 1; iteration <= raoParameters.getExtension(IteratingLinearOptimizerParameters.class).getMaxIterations(); iteration++) {
            optimizedVariantId = raoData.cloneWorkingVariant();
            raoData.setWorkingVariant(optimizedVariantId);

            // Look for a new RangeAction combination, optimized with the LinearOptimisationEngine
            // Store found solutions in crac extension working variant
            // Apply remedial actions on the network working variant
            LOGGER.info("Iteration {} - linear optimization [start]", iteration);
            simpleLinearOptimizer.run(raoData, raoParameters.getExtension(LinearProblemParameters.class));
            LOGGER.info("Iteration {} - linear optimization [end]", iteration);

            // if the solution has not changed, stop the search
            if (raoData.sameRemedialActions(bestVariantId, optimizedVariantId)) {
                LOGGER.info("Iteration {} - same results as previous iterations, optimal solution found", iteration);
                break;
            }
            LOGGER.info("Iteration {} - systematic analysis [start]", iteration);
            // evaluate sensitivity coefficients and cost on the newly optimised situation
            systematicSensitivityComputation.run(raoData);
            LOGGER.info("Iteration {} - systematic analysis [end]", iteration);

            if (raoData.getCracResult(optimizedVariantId).getCost() < raoData.getCracResult(bestVariantId).getCost()) { // if the solution has been improved, continue the search
                LOGGER.warn("Iteration {} - Better solution found with a minimum margin of {} MW", iteration, -raoData.getCracResult(optimizedVariantId).getCost());
                if (!bestVariantId.equals(raoData.getInitialVariantId())) {
                    raoData.deleteVariant(bestVariantId, false);
                }
                bestVariantId = optimizedVariantId;
            } else { // unexpected behaviour, stop the search
                LOGGER.warn("Iteration {} - Linear Optimization found a worse result than previous iteration: from {} MW to {} MW",
                    iteration, -raoData.getCracResult(bestVariantId).getCost(), -raoData.getCracResult(optimizedVariantId).getCost());
                break;
            }
        }

        return bestVariantId;
    }
}
