/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.rao_commons.CracVariantManager;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerOutput;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(IteratingLinearOptimizer.class);
    protected static final String UNEXPECTED_BEHAVIOR = "Iteration %d - Linear Optimization found a worse result than previous iteration, with a minimum margin from %.2f to %.2f %s (optimisation criterion : from %.2f to %.2f)";
    protected static final String IMPROVEMENT = "Iteration %d - Better solution found with a minimum margin of %.2f %s (optimisation criterion : %.2f)";
    protected static final String SAME_RESULTS = "Iteration %d - same results as previous iterations, optimal solution found";
    protected static final String SYSTEMATIC_SENSITIVITY_COMPUTATION_START = "Iteration %d - systematic analysis [start]";
    protected static final String SYSTEMATIC_SENSITIVITY_COMPUTATION_END = "Iteration %d - systematic analysis [end]";
    protected static final String SYSTEMATIC_SENSITIVITY_COMPUTATION_ERROR = "Sensitivity computation failed at iteration %d on %s mode: %s";
    protected static final String LINEAR_OPTIMIZATION_START = "Iteration %d - linear optimization [start]";
    protected static final String LINEAR_OPTIMIZATION_END = "Iteration %d - linear optimization [end]";
    protected static final String LINEAR_OPTIMIZATION_INFEASIBLE = "Iteration %d - linear optimization cannot find OPTIMAL solution";
    protected static final String LINEAR_OPTIMIZATION_ERROR = "Linear optimization failed at iteration %d: %s";

    protected RaoData raoData;
    protected CracVariantManager cracVariantManager;
    protected LinearOptimizerOutput bestLinearOptimizerOutput;
    protected SystematicSensitivityInterface systematicSensitivityInterface;
    protected ObjectiveFunctionEvaluator objectiveFunctionEvaluator;
    protected LinearOptimizer linearOptimizer;
    protected IteratingLinearOptimizerParameters parameters;

    public IteratingLinearOptimizer(List<ProblemFiller> fillers,
                                    SystematicSensitivityInterface systematicSensitivityInterface,
                                    ObjectiveFunctionEvaluator objectiveFunctionEvaluator,
                                    IteratingLinearOptimizerParameters parameters) {
        this(systematicSensitivityInterface, objectiveFunctionEvaluator, new LinearOptimizer(fillers), parameters);
    }

    IteratingLinearOptimizer(SystematicSensitivityInterface systematicSensitivityInterface,
                             ObjectiveFunctionEvaluator objectiveFunctionEvaluator,
                             LinearOptimizer linearOptimizer,
                             IteratingLinearOptimizerParameters parameters) {
        this.systematicSensitivityInterface = systematicSensitivityInterface;
        this.objectiveFunctionEvaluator = objectiveFunctionEvaluator;
        this.linearOptimizer = linearOptimizer;
        this.parameters = parameters;
    }

    public ObjectiveFunctionEvaluator getObjectiveFunctionEvaluator() {
        return objectiveFunctionEvaluator;
    }

    public IteratingLinearOptimizerParameters getParameters() {
        return parameters;
    }

    public String optimize(RaoData raoData) {
        this.raoData = raoData;
        cracVariantManager = raoData.getCracVariantManager();
        if (!raoData.hasSensitivityValues()) {
            runSensitivityAndUpdateResults();
        }
        String optimizedVariantId;
        for (int iteration = 1; iteration <= parameters.getMaxIterations(); iteration++) {
            optimizedVariantId = cracVariantManager.cloneWorkingVariant();
            raoData.getCracResultManager().copyCommercialFlowsBetweenVariants(cracVariantManager.getWorkingVariantId(), optimizedVariantId);
            //TODO : copy crac results from one variant to the next in CracVariantManager.cloneWorkingVariant() ?
            cracVariantManager.setWorkingVariant(optimizedVariantId);
            LinearOptimizerOutput linearOptimizerOutput = optimize(iteration, sensitivityAndLoopflowResults);
            if(iteration == 1) {
                bestLinearOptimizerOutput = linearOptimizerOutput;
            }
            if (hasNotOptimized() // If optimization fails iteration can stop
                    || (iteration != 1 && !hasRemedialActionsChanged(linearOptimizerOutput, iteration))
                    || !hasCostImproved(linearOptimizerOutput, iteration)) {
                return getBestVariantWithSafeDelete(optimizedVariantId);
            }
            updateBestVariantId(optimizedVariantId);
        }
        return bestVariantId;
    }

    private void optimize(int iteration, SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        try {
            LOGGER.info(format(LINEAR_OPTIMIZATION_START, iteration));
            linearOptimizer.optimize(sensitivityAndLoopflowResults);
            if (hasNotOptimized()) {
                LOGGER.info(format(LINEAR_OPTIMIZATION_INFEASIBLE, iteration)); //handle INFEASIBLE solver status
            }
            LOGGER.info(format(LINEAR_OPTIMIZATION_END, iteration));
        } catch (LinearOptimisationException e) {
            LOGGER.error(format(LINEAR_OPTIMIZATION_ERROR, iteration, e.getMessage()));
        }
    }

    private boolean hasNotOptimized() {
        return !linearOptimizer.getSolverResultStatusString().equals("OPTIMAL");
    }

    private boolean hasRemedialActionsChanged(LinearOptimizerOutput linearOptimizerOutput, int iteration) {
        Map<RangeAction, Double> newSetPoints = linearOptimizerOutput.getOptimalRangeActionSetpoints();
        Map<RangeAction, Double> bestSetPoints = bestLinearOptimizerOutput.getOptimalRangeActionSetpoints();

        if(!bestSetPoints.keySet().equals(newSetPoints.keySet())) {
            return true;
        }

        for (RangeAction rangeAction : newSetPoints.keySet()) {
            if (!newSetPoints.get(rangeAction).equals(bestSetPoints.get(rangeAction))) {
                return true;
            }
        }

        // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
        LOGGER.info(format(SAME_RESULTS, iteration));
        return false;
    }

    protected void applyRangeActionsAndEvaluateNewCost(LinearOptimizerOutput linearOptimizerOutput, int iteration) throws SensitivityAnalysisException{
        Map<RangeAction, Double> rangeActionSetPoints = linearOptimizerOutput.getOptimalRangeActionSetpoints();
        rangeActionSetPoints.keySet().forEach(rangeAction -> rangeAction.apply(raoData.getNetwork(), rangeActionSetPoints.get(rangeAction)));

        LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_START, iteration));
        runSensitivityAndUpdateResults();
        LOGGER.info(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_END, iteration));
    }

    private boolean hasCostImproved(LinearOptimizerOutput linearOptimizerOutput, int iteration) {
        try {
            applyRangeActionsAndEvaluateNewCost(linearOptimizerOutput, iteration);
        } catch (SensitivityAnalysisException e) {
            LOGGER.error(format(SYSTEMATIC_SENSITIVITY_COMPUTATION_ERROR, iteration,
                    systematicSensitivityInterface.isFallback() ? "Fallback" : "Default", e.getMessage()));
        }
        // If the cost has not improved iteration can stop
        CracResult bestVariantResult = raoData.getCracResult(bestVariantId);
        CracResult optimizedVariantResult = raoData.getCracResult(optimizedVariantId);
        if (optimizedVariantResult.getCost() < bestVariantResult.getCost()) {
            LOGGER.warn(format(IMPROVEMENT, iteration, -optimizedVariantResult.getFunctionalCost(),
                    objectiveFunctionEvaluator.getUnit(), optimizedVariantResult.getCost()));
            return true;
        } else { // unexpected behaviour, stop the search
            LOGGER.warn(format(UNEXPECTED_BEHAVIOR, iteration, -bestVariantResult.getFunctionalCost(),
                    -optimizedVariantResult.getFunctionalCost(), objectiveFunctionEvaluator.getUnit(), bestVariantResult.getCost(),
                    optimizedVariantResult.getCost()));
            return false;
        }
    }

    private String getBestVariantWithSafeDelete(String variantToDelete) {
        cracVariantManager.setWorkingVariant(bestVariantId);
        if (!variantToDelete.equals(raoData.getPreOptimVariantId())) {
            cracVariantManager.deleteVariant(variantToDelete, false);
        }
        return bestVariantId;
    }

    private void updateBestVariantId(String optimizedVariantId) {
        cracVariantManager.setWorkingVariant(optimizedVariantId);
        if (!bestVariantId.equals(raoData.getPreOptimVariantId())) {
            cracVariantManager.deleteVariant(bestVariantId, false);
        }
        bestVariantId = optimizedVariantId;
    }

    void runSensitivityAndUpdateResults() {
        raoData.setSystematicSensitivityResult(
            systematicSensitivityInterface.run(raoData.getNetwork()));

        raoData.getCracResultManager().fillCnecResultWithFlows();
        raoData.getCracResultManager().fillCracResultWithCosts(objectiveFunctionEvaluator.getFunctionalCost(raoData), objectiveFunctionEvaluator.getVirtualCost(raoData));
    }
}
