/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms;

import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.powsybl.open_rao.search_tree_rao.commons.SensitivityComputer;
import com.powsybl.open_rao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.powsybl.open_rao.search_tree_rao.commons.optimization_perimeters.GlobalOptimizationPerimeter;
import com.powsybl.open_rao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.open_rao.search_tree_rao.result.api.*;
import com.powsybl.open_rao.search_tree_rao.result.impl.IteratingLinearOptimizationResultImpl;
import com.powsybl.open_rao.search_tree_rao.result.impl.LinearProblemResult;
import com.powsybl.open_rao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Locale;

import static com.powsybl.open_rao.commons.logs.FaraoLoggerProvider.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizer {

    private IteratingLinearOptimizer() {

    }

    public static LinearOptimizationResult optimize(IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters, Instant outageInstant) {

        IteratingLinearOptimizationResultImpl bestResult = createResult(
                input.getPreOptimizationFlowResult(),
                input.getPreOptimizationSensitivityResult(),
                input.getRaActivationFromParentLeaf(),
                0,
                input.getObjectiveFunction());

        IteratingLinearOptimizationResultImpl previousResult = bestResult;

        SensitivityComputer sensitivityComputer = null;

        LinearProblem linearProblem = LinearProblem.create()
                .buildFromInputsAndParameters(input, parameters);

        linearProblem.fill(input.getPreOptimizationFlowResult(), input.getPreOptimizationSensitivityResult());

        for (int iteration = 1; iteration <= parameters.getMaxNumberOfIterations(); iteration++) {
            LinearProblemStatus solveStatus = solveLinearProblem(linearProblem, iteration);
            bestResult.setNbOfIteration(iteration);
            if (solveStatus == LinearProblemStatus.FEASIBLE) {
                TECHNICAL_LOGS.warn("The solver was interrupted. A feasible solution has been produced.");
            } else if (solveStatus != LinearProblemStatus.OPTIMAL) {
                BUSINESS_LOGS.error("Linear optimization failed at iteration {}", iteration);
                if (iteration == 1) {
                    bestResult.setStatus(solveStatus);
                    BUSINESS_LOGS.info("Linear problem failed with the following status : {}, initial situation is kept.", solveStatus);
                    return bestResult;
                }
                bestResult.setStatus(LinearProblemStatus.FEASIBLE);
                return bestResult;
            }

            RangeActionActivationResult linearProblemResult = new LinearProblemResult(linearProblem, input.getPrePerimeterSetpoints(), input.getOptimizationPerimeter());
            RangeActionActivationResult currentRangeActionActivationResult = roundResult(linearProblemResult, bestResult, input, parameters);
            currentRangeActionActivationResult = resolveIfApproximatedPstTaps(bestResult, linearProblem, iteration, currentRangeActionActivationResult, input, parameters);

            if (!hasRemedialActionsChanged(currentRangeActionActivationResult, previousResult, input.getOptimizationPerimeter())) {
                // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
                TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration);
                return bestResult;
            }

            sensitivityComputer = runSensitivityAnalysis(sensitivityComputer, iteration, currentRangeActionActivationResult, input, parameters, outageInstant);
            if (sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE) {
                bestResult.setStatus(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED);
                return bestResult;
            }

            IteratingLinearOptimizationResultImpl currentResult = createResult(
                    sensitivityComputer.getBranchResult(input.getNetwork()),
                    sensitivityComputer.getSensitivityResult(),
                    currentRangeActionActivationResult,
                    iteration,
                    input.getObjectiveFunction()
            );
            previousResult = currentResult;

            Pair<IteratingLinearOptimizationResultImpl, Boolean> mipShouldStop = updateBestResultAndCheckStopCondition(parameters.getRaRangeShrinking(), linearProblem, input, iteration, currentResult, bestResult);
            if (mipShouldStop.getRight()) {
                return bestResult;
            } else {
                bestResult = mipShouldStop.getLeft();
            }
        }
        bestResult.setStatus(LinearProblemStatus.MAX_ITERATION_REACHED);
        return bestResult;
    }

    private static SensitivityComputer runSensitivityAnalysis(SensitivityComputer sensitivityComputer, int iteration, RangeActionActivationResult currentRangeActionActivationResult, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters, Instant outageInstant) {
        SensitivityComputer tmpSensitivityComputer = sensitivityComputer;
        if (input.getOptimizationPerimeter() instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActionsInSecondaryStates = applyRangeActions(currentRangeActionActivationResult, input);
            tmpSensitivityComputer = createSensitivityComputer(appliedRemedialActionsInSecondaryStates, input, parameters);
        } else {
            applyRangeActions(currentRangeActionActivationResult, input);
            if (tmpSensitivityComputer == null) { // first iteration, do not need to be updated afterwards
                tmpSensitivityComputer = createSensitivityComputer(input.getPreOptimizationAppliedRemedialActions(), input, parameters);
            }
        }
        runSensitivityAnalysis(tmpSensitivityComputer, input.getNetwork(), iteration);
        return tmpSensitivityComputer;
    }

    private static RangeActionActivationResult resolveIfApproximatedPstTaps(IteratingLinearOptimizationResultImpl bestResult, LinearProblem linearProblem, int iteration, RangeActionActivationResult currentRangeActionActivationResult, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        LinearProblemStatus solveStatus;
        RangeActionActivationResult rangeActionActivationResult = currentRangeActionActivationResult;
        if (parameters.getRangeActionParameters().getPstModel().equals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS)) {

            // if the PST approximation is APPROXIMATED_INTEGERS, we re-solve the optimization problem
            // but first, we update it, with an adjustment of the PSTs angleToTap conversion factors, to
            // be more accurate in the neighboring of the previous solution

            // (idea: if too long, we could relax the first MIP, but no so straightforward to do with or-tools)
            linearProblem.updateBetweenMipIteration(rangeActionActivationResult);

            solveStatus = solveLinearProblem(linearProblem, iteration);
            if (solveStatus == LinearProblemStatus.OPTIMAL || solveStatus == LinearProblemStatus.FEASIBLE) {
                RangeActionActivationResult updatedLinearProblemResult = new LinearProblemResult(linearProblem, input.getPrePerimeterSetpoints(), input.getOptimizationPerimeter());
                rangeActionActivationResult = roundResult(updatedLinearProblemResult, bestResult, input, parameters);
            }
        }
        return rangeActionActivationResult;
    }

    private static LinearProblemStatus solveLinearProblem(LinearProblem linearProblem, int iteration) {
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [start]", iteration);
        LinearProblemStatus status = linearProblem.solve();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [end]", iteration);
        return status;
    }

    private static boolean hasRemedialActionsChanged(RangeActionActivationResult newRangeActionActivationResult, RangeActionActivationResult oldRangeActionActivationResult, OptimizationPerimeter optimizationContext) {
        return optimizationContext.getRangeActionsPerState().entrySet().stream()
                .anyMatch(e -> e.getValue().stream()
                        .anyMatch(ra -> Math.abs(newRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()) - oldRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey())) >= 1e-6));
    }

    private static AppliedRemedialActions applyRangeActions(RangeActionActivationResult rangeActionActivationResult, IteratingLinearOptimizerInput input) {

        OptimizationPerimeter optimizationContext = input.getOptimizationPerimeter();

        // apply RangeAction from first optimization state
        optimizationContext.getRangeActionsPerState().get(optimizationContext.getMainOptimizationState())
                .forEach(ra -> ra.apply(input.getNetwork(), rangeActionActivationResult.getOptimizedSetpoint(ra, optimizationContext.getMainOptimizationState())));

        // add RangeAction activated in the following states
        if (optimizationContext instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActions = input.getPreOptimizationAppliedRemedialActions().copyNetworkActionsAndAutomaticRangeActions();

            optimizationContext.getRangeActionsPerState().entrySet().stream()
                    .filter(e -> !e.getKey().equals(optimizationContext.getMainOptimizationState())) // remove preventive state
                    .forEach(e -> e.getValue().forEach(ra -> appliedRemedialActions.addAppliedRangeAction(e.getKey(), ra, rangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()))));
            return appliedRemedialActions;
        } else {
            return null;
        }
    }

    private static SensitivityComputer createSensitivityComputer(AppliedRemedialActions appliedRemedialActions, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {

        SensitivityComputer.SensitivityComputerBuilder builder = SensitivityComputer.create()
                .withCnecs(input.getOptimizationPerimeter().getFlowCnecs())
                .withRangeActions(input.getOptimizationPerimeter().getRangeActions())
                .withAppliedRemedialActions(appliedRemedialActions)
                .withToolProvider(input.getToolProvider())
                .withOutageInstant(input.getCrac().getOutageInstant());

        if (parameters.isRaoWithLoopFlowLimitation() && parameters.getLoopFlowParameters().getPtdfApproximation().shouldUpdatePtdfWithPstChange()) {
            builder.withCommercialFlowsResults(input.getToolProvider().getLoopFlowComputation(), input.getOptimizationPerimeter().getLoopFlowCnecs());
        } else if (parameters.isRaoWithLoopFlowLimitation()) {
            builder.withCommercialFlowsResults(input.getPreOptimizationFlowResult());
        }
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            if (parameters.getMaxMinRelativeMarginParameters().getPtdfApproximation().shouldUpdatePtdfWithPstChange()) {
                builder.withPtdfsResults(input.getToolProvider().getAbsolutePtdfSumsComputation(), input.getOptimizationPerimeter().getFlowCnecs());
            } else {
                builder.withPtdfsResults(input.getPreOptimizationFlowResult());
            }
        }

        return builder.build();
    }

    private static void runSensitivityAnalysis(SensitivityComputer sensitivityComputer, Network network, int iteration) {
        sensitivityComputer.compute(network);
        if (sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_WARNS.warn("Systematic sensitivity computation failed at iteration {}", iteration);
        }
    }

    private static IteratingLinearOptimizationResultImpl createResult(FlowResult flowResult,
                                                               SensitivityResult sensitivityResult,
                                                               RangeActionActivationResult rangeActionActivation,
                                                               int nbOfIterations,
                                                               ObjectiveFunction objectiveFunction) {
        return new IteratingLinearOptimizationResultImpl(LinearProblemStatus.OPTIMAL, nbOfIterations, rangeActionActivation, flowResult,
                objectiveFunction.evaluate(flowResult, rangeActionActivation, sensitivityResult, sensitivityResult.getSensitivityStatus()), sensitivityResult);
    }

    private static RangeActionActivationResult roundResult(RangeActionActivationResult linearProblemResult, IteratingLinearOptimizationResultImpl previousResult, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        return BestTapFinder.round(
                linearProblemResult,
                input.getNetwork(),
                input.getOptimizationPerimeter(),
                input.getPrePerimeterSetpoints(),
                previousResult,
                parameters.getObjectiveFunctionUnit()
        );
    }

    private static Pair<IteratingLinearOptimizationResultImpl, Boolean> updateBestResultAndCheckStopCondition(boolean raRangeShrinking, LinearProblem linearProblem, IteratingLinearOptimizerInput input, int iteration, IteratingLinearOptimizationResultImpl currentResult, IteratingLinearOptimizationResultImpl bestResult) {
        if (currentResult.getCost() < bestResult.getCost()) {
            logBetterResult(iteration, currentResult);
            linearProblem.updateBetweenSensiIteration(currentResult.getBranchResult(), currentResult.getSensitivityResult(), currentResult.getRangeActionActivationResult());
            return Pair.of(currentResult, false);
        }
        logWorseResult(iteration, bestResult, currentResult);
        applyRangeActions(bestResult, input);
        if (raRangeShrinking) {
            linearProblem.updateBetweenSensiIteration(currentResult.getBranchResult(), currentResult.getSensitivityResult(), currentResult.getRangeActionActivationResult());
        }
        return Pair.of(bestResult, !raRangeShrinking);
    }

    private static void logBetterResult(int iteration, ObjectiveFunctionResult currentObjectiveFunctionResult) {
        TECHNICAL_LOGS.info(
                "Iteration {}: better solution found with a cost of {} (functional: {})",
                iteration,
                formatDouble(currentObjectiveFunctionResult.getCost()),
                formatDouble(currentObjectiveFunctionResult.getFunctionalCost()));
    }

    private static void logWorseResult(int iteration, ObjectiveFunctionResult bestResult, ObjectiveFunctionResult currentResult) {
        TECHNICAL_LOGS.info(
                "Iteration {}: linear optimization found a worse result than best iteration, with a cost increasing from {} to {} (functional: from {} to {})",
                iteration,
                formatDouble(bestResult.getCost()),
                formatDouble(currentResult.getCost()),
                formatDouble(bestResult.getFunctionalCost()),
                formatDouble(currentResult.getFunctionalCost()));
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ENGLISH, "%.2f", value);
    }

}
