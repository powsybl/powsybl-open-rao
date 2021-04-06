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
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerOutput;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.ortools.linearsolver.MPSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(IteratingLinearOptimizer.class);

    protected RaoData raoData;
    protected SystematicSensitivityInterface systematicSensitivityInterface;
    protected ObjectiveFunctionEvaluator objectiveFunctionEvaluator;
    protected LinearOptimizer linearOptimizer;
    protected IteratingLinearOptimizerParameters parameters;

    public IteratingLinearOptimizer(List<ProblemFiller> fillers,
                                    SystematicSensitivityInterface systematicSensitivityInterface,
                                    ObjectiveFunctionEvaluator objectiveFunctionEvaluator,
                                    IteratingLinearOptimizerParameters parameters) {
        this(systematicSensitivityInterface, objectiveFunctionEvaluator, new LinearOptimizer(fillers), parameters);
        // TODO : build LinearOptimizerInput & parameters
        // this.linearOptimizer = new LinearOptimizer(linearOptimizerInput, linearOptimizerParameters)
        createLinearOptimizerInput(IteratingLinearOptimizerInput iteratingLinearOptimizerInput)
    }

    private LinearOptimizerInput createLinearOptimizerInput(IteratingLinearOptimizerInput iteratingLinearOptimizerInput) {
        return LinearOptimizerInput.create()
                .withCnecs(iteratingLinearOptimizerInput.getCnecs())
                .withLoopflowCnecs(iteratingLinearOptimizerInput.getLoopflowCnecs())
                .withInitialCnecResults(iteratingLinearOptimizerInput.getInitialCnecResults())
                .withNetwork(iteratingLinearOptimizerInput.getNetwork())
                .withPrePerimeterCnecMarginsInAbsoluteMW(iteratingLinearOptimizerInput.getPrePerimeterCnecMarginsInAbsoluteMW())
                .withPreperimeterSetpoints(iteratingLinearOptimizerInput.getPreperimeterSetpoints())
                .withRangeActions(iteratingLinearOptimizerInput.getRangeActions())
                .withMostLimitingElements(objectiveFunctionEvaluator.getMostLimitingElements(sensitivityAndLoopflowResults, 10));
    }

    // TODO : remove this
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

    private IteratingLinearOptimizerOutput createOutputFromInitialSituation(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        // TODO : create default output
        return null;
    }

    public IteratingLinearOptimizerOutput optimize(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        Objects.requireNonNull(sensitivityAndLoopflowResults);
        IteratingLinearOptimizerOutput bestIteratingLinearOptimizerOutput = createOutputFromInitialSituation(sensitivityAndLoopflowResults);
        SensitivityAndLoopflowResults updatedSensitivityAndLoopflowResults = sensitivityAndLoopflowResults;

        for (int iteration = 1; iteration <= parameters.getMaxIterations(); iteration++) {
            LinearOptimizerOutput linearOptimizerOutput = null;
            try {
                LOGGER.debug("Iteration {} - linear optimization [start]", iteration);
                linearOptimizerOutput = linearOptimizer.optimize(sensitivityAndLoopflowResults);
                LOGGER.debug("Iteration {} - linear optimization [end]", iteration);
            } catch (LinearOptimisationException e) {
                LOGGER.error("Linear optimization failed at iteration {}: {}", iteration, e.getMessage());
                bestIteratingLinearOptimizerOutput.setStatus(IteratingLinearOptimizerOutput.SolveStatus.ABNORMAL);
                return bestIteratingLinearOptimizerOutput;
            }

            if (!linearOptimizerOutput.getSolveStatus().equals(LinearOptimizerOutput.SolveStatus.OPTIMAL))            {
                LOGGER.warn("Iteration {} - linear optimization cannot find OPTIMAL solution", iteration);
                if (iteration > 1) {
                    bestIteratingLinearOptimizerOutput.setStatus(IteratingLinearOptimizerOutput.SolveStatus.FEASIBLE);
                } else {
                    bestIteratingLinearOptimizerOutput.setStatus(getFirstIterationSolveStatusFromLinear(linearOptimizerOutput.getSolveStatus()));
                }
                return bestIteratingLinearOptimizerOutput;
            } else if (!hasRemedialActionsChanged(bestIteratingLinearOptimizerOutput, linearOptimizerOutput, iteration)) {
                bestIteratingLinearOptimizerOutput.setStatus(IteratingLinearOptimizerOutput.SolveStatus.OPTIMAL);
                return bestIteratingLinearOptimizerOutput;
            } else {
                updatedSensitivityAndLoopflowResults = applyRangeActionsAndRunSensitivityComputation(linearOptimizerOutput, iteration, updatedSensitivityAndLoopflowResults);
                double functionalCost = objectiveFunctionEvaluator.getFunctionalCost(updatedSensitivityAndLoopflowResults);
                double virtualCost = objectiveFunctionEvaluator.getVirtualCost(updatedSensitivityAndLoopflowResults);
                if (functionalCost + virtualCost < bestIteratingLinearOptimizerOutput.getCost()) {
                    LOGGER.info("Iteration {} - Better solution found with a minimum margin of {} {} (optimisation criterion : {})",
                            iteration, -functionalCost, objectiveFunctionEvaluator.getUnit(), functionalCost + virtualCost);
                    bestIteratingLinearOptimizerOutput = new IteratingLinearOptimizerOutput(functionalCost, virtualCost, updatedSensitivityAndLoopflowResults,
                            IteratingLinearOptimizerOutput.SolveStatus.OPTIMAL, linearOptimizerOutput);
                } else {
                    LOGGER.warn("Iteration {} - Linear Optimization found a worse result than previous iteration, with a minimum margin from {} to {} {} (optimisation criterion : from {} to {})",
                            iteration, -bestIteratingLinearOptimizerOutput.getFunctionalCost(), -functionalCost, objectiveFunctionEvaluator.getUnit(), bestIteratingLinearOptimizerOutput.getCost(), functionalCost + virtualCost);
                    bestIteratingLinearOptimizerOutput.setStatus(IteratingLinearOptimizerOutput.SolveStatus.OPTIMAL);
                    return bestIteratingLinearOptimizerOutput;
                }
            }
        }
        return bestIteratingLinearOptimizerOutput;
    }

    private IteratingLinearOptimizerOutput.SolveStatus getFirstIterationSolveStatusFromLinear(LinearOptimizerOutput.SolveStatus solveStatus) {
            switch (solveStatus) {
                case OPTIMAL:
                    return IteratingLinearOptimizerOutput.SolveStatus.OPTIMAL;
                case FEASIBLE:
                    return IteratingLinearOptimizerOutput.SolveStatus.FEASIBLE;
                case INFEASIBLE:
                    return IteratingLinearOptimizerOutput.SolveStatus.INFEASIBLE;
                case UNBOUNDED:
                    return IteratingLinearOptimizerOutput.SolveStatus.UNBOUNDED;
                case NOT_SOLVED:
                    return IteratingLinearOptimizerOutput.SolveStatus.NOT_SOLVED;
                case ABNORMAL:
                default:
                    return IteratingLinearOptimizerOutput.SolveStatus.ABNORMAL;
            }
    }

    private boolean hasRemedialActionsChanged(IteratingLinearOptimizerOutput bestIteratingLinearOptimizerOutput, LinearOptimizerOutput linearOptimizerOutput, int iteration) {
        Map<RangeAction, Double> newSetPoints = linearOptimizerOutput.getRangeActionSetpoints();
        Map<RangeAction, Double> bestSetPoints = bestIteratingLinearOptimizerOutput.getRangeActionSetpoints();

        // TODO : verify if this is necessary
        /* if(!bestSetPoints.keySet().equals(newSetPoints.keySet())) {
            return true;
        }*/

        for (Map.Entry<RangeAction, Double> newSetPointEntry : newSetPoints.entrySet()) {
            if (Math.abs(newSetPointEntry.getValue() - bestSetPoints.get(newSetPointEntry.getKey())) > Math.max(1e-2 * Math.abs(bestSetPoints.get(newSetPointEntry.getKey())), 1e-3)) {
                return true;
            }
        }

        // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
        LOGGER.info("Iteration {} - same results as previous iterations, optimal solution found", iteration);
        return false;
    }

    protected SensitivityAndLoopflowResults applyRangeActionsAndRunSensitivityComputation(LinearOptimizerOutput linearOptimizerOutput, int iteration, SensitivityAndLoopflowResults sensitivityAndLoopflowResults) throws SensitivityAnalysisException{
        Map<RangeAction, Double> rangeActionSetPoints = linearOptimizerOutput.getRangeActionSetpoints();
        rangeActionSetPoints.keySet().forEach(rangeAction -> rangeAction.apply(raoData.getNetwork(), rangeActionSetPoints.get(rangeAction)));

        LOGGER.debug("Iteration {} - systematic analysis [start]", iteration);
        try {
            SystematicSensitivityResult sensiResult = systematicSensitivityInterface.run(raoData.getNetwork());
            LOGGER.debug("Iteration {} - systematic analysis [end]", iteration);
            return updateSensitivityAndLoopflowResults(sensitivityAndLoopflowResults, sensiResult);
        } catch (SensitivityAnalysisException e) {
            LOGGER.error("Sensitivity computation failed at iteration {} on {} mode: {}", iteration, systematicSensitivityInterface.isFallback() ? "Fallback" : "Default", e.getMessage());
            return sensitivityAndLoopflowResults;
        }
    }

    SensitivityAndLoopflowResults updateSensitivityAndLoopflowResults(SensitivityAndLoopflowResults sensitivityAndLoopflowResults, SystematicSensitivityResult updatedSensiResult) {
        return new SensitivityAndLoopflowResults(updatedSensiResult, sensitivityAndLoopflowResults.getCommercialFlows());
    }
}
