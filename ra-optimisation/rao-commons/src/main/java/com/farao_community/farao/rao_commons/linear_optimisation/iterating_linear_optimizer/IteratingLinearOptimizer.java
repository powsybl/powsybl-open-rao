/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.SimpleLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.parameters.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import com.farao_community.farao.util.SensitivityComputationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(IteratingLinearOptimizer.class);

    private IteratingLinearOptimizer() { }

    public static String optimize(RaoData raoData, SystematicSensitivityComputation systematicSensitivityComputation, RaoParameters raoParameters) {
        IteratingLinearOptimizerParameters parameters;
        if (!Objects.isNull(raoParameters.getExtension(IteratingLinearOptimizerParameters.class))) {
            parameters = raoParameters.getExtension(IteratingLinearOptimizerParameters.class);
        } else {
            parameters = new IteratingLinearOptimizerParameters();
        }
        return optimize(raoData, systematicSensitivityComputation, new SimpleLinearOptimizer(raoParameters), parameters);
    }

    public static String optimize(RaoData raoData,
                                  SystematicSensitivityComputation systematicSensitivityComputation,
                                  SimpleLinearOptimizer simpleLinearOptimizer, IteratingLinearOptimizerParameters parameters) {
        String bestVariantId = raoData.getWorkingVariantId();
        String optimizedVariantId;

        for (int iteration = 1; iteration <= parameters.getMaxIterations(); iteration++) {
            optimizedVariantId = raoData.cloneWorkingVariant();
            raoData.setWorkingVariant(optimizedVariantId);

            /*
            Three steps are gathered in the simpleLinearOptimizer
             - Look for a new RangeAction combination, optimized with the simpleLinearOptimizer
             - Store found solutions in crac extension working variant
             - Apply remedial actions on the network working variant
            It will throw an error if systematic sensitivity computation have not performed on the initial RaoData working variant
             */
            try {
                LOGGER.info("Iteration {} - linear optimization [start]", iteration);
                simpleLinearOptimizer.optimize(raoData);
                LOGGER.info("Iteration {} - linear optimization [end]", iteration);

                // if the solution has not changed, stop the search
                if (raoData.sameRemedialActions(bestVariantId, optimizedVariantId)) {
                    LOGGER.info("Iteration {} - same results as previous iterations, optimal solution found", iteration);
                    raoData.deleteVariant(optimizedVariantId, false);
                    return bestVariantId;
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
                    raoData.deleteVariant(optimizedVariantId, false);
                    return bestVariantId;
                }
            } catch (SensitivityComputationException e) {
                LOGGER.error(String.format("Sensitivity computation failed at iteration %d on %s mode: %s",
                    iteration, systematicSensitivityComputation.isFallback() ? "Fallback" : "Default", e.getMessage()));
                raoData.setWorkingVariant(bestVariantId);
                raoData.deleteVariant(optimizedVariantId, false);
                return bestVariantId;
            }
        }
        return bestVariantId;
    }
}
