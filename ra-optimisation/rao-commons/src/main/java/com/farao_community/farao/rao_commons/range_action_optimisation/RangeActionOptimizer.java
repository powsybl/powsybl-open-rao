/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.range_action_optimisation;

import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.SystematicAnalysisEngine;
import com.farao_community.farao.rao_commons.range_action_optimisation.optimisation.fillers.FillerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RangeActionOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RangeActionOptimizer.class);

    private RangeActionOptimizer() { }

    public static String optimize(RaoData raoData,
                               String initialRaoDataVariantId,
                               SystematicAnalysisEngine systematicAnalysisEngine,
                               LinearOptimisationEngine linearOptimisationEngine,
                               RangeActionOptimizerParameters rangeActionOptimizerParameters,
                               FillerParameters fillerParameters) {
        raoData.setWorkingVariant(initialRaoDataVariantId);
        String bestVariantId = initialRaoDataVariantId;
        String optimizedVariantId;

        for (int iteration = 1; iteration <= rangeActionOptimizerParameters.getMaxIterations(); iteration++) {
            optimizedVariantId = raoData.cloneWorkingVariant();
            raoData.setWorkingVariant(optimizedVariantId);

            // Look for a new RangeAction combination, optimized with the LinearOptimisationEngine
            // Store found solutions in crac extension working variant
            // Apply remedial actions on the network working variant
            LOGGER.info("Iteration {} - linear optimization [start]", iteration);
            linearOptimisationEngine.run(raoData, fillerParameters);
            LOGGER.info("Iteration {} - linear optimization [end]", iteration);

            // if the solution has not changed, stop the search
            if (raoData.sameRemedialActions(bestVariantId, optimizedVariantId)) {
                LOGGER.info("Iteration {} - same results as previous iterations, optimal solution found", iteration);
                break;
            }
            LOGGER.info("Iteration {} - systematic analysis [start]", iteration);
            // evaluate sensitivity coefficients and cost on the newly optimised situation
            systematicAnalysisEngine.run(raoData);
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
