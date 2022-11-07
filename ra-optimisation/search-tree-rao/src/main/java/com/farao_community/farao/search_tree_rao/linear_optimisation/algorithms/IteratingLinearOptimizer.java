/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.GlobalOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.linear_optimisation.inputs.IteratingLinearOptimizerInput;
import com.farao_community.farao.search_tree_rao.linear_optimisation.parameters.IteratingLinearOptimizerParameters;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.IteratingLinearOptimizationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.LinearProblemResult;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.powsybl.iidm.network.Network;

import java.util.Locale;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizer {

    private final IteratingLinearOptimizerInput input;
    private final IteratingLinearOptimizerParameters parameters;

    public IteratingLinearOptimizer(IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        this.input = input;
        this.parameters = parameters;
    }

    public LinearOptimizationResult optimize() {

        IteratingLinearOptimizationResultImpl bestResult = createResult(
            input.getPreOptimizationFlowResult(),
            input.getPreOptimizationSensitivityResult(),
            input.getRaActivationFromParentLeaf(),
            0);

        SensitivityComputer sensitivityComputer = null;

        LinearProblem linearProblem = new LinearProblemBuilder()
            .buildFromInputsAndParameters(input, parameters);

        linearProblem.fill(input.getPreOptimizationFlowResult(), input.getPreOptimizationSensitivityResult());

        for (int iteration = 1; iteration <= parameters.getMaxNumberOfIterations(); iteration++) {
            LinearProblemStatus solveStatus = solveLinearProblem(linearProblem, iteration);
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
            RangeActionActivationResult currentRangeActionActivationResult = roundResult(linearProblemResult, bestResult);

            currentRangeActionActivationResult = resolveIfApproximatedPstTaps(bestResult, linearProblem, iteration, currentRangeActionActivationResult);

            if (!hasRemedialActionsChanged(currentRangeActionActivationResult, bestResult, input.getOptimizationPerimeter())) {
                // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
                TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration);
                return bestResult;
            }

            try {
                sensitivityComputer = runSensitivityAnalysis(sensitivityComputer, iteration, currentRangeActionActivationResult);
            } catch (SensitivityAnalysisException e) {
                bestResult.setStatus(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED);
                return bestResult;
            }

            IteratingLinearOptimizationResultImpl currentResult = createResult(
                sensitivityComputer.getBranchResult(input.getNetwork()),
                sensitivityComputer.getSensitivityResult(),
                currentRangeActionActivationResult,
                iteration
            );

            if (currentResult.getCost() >= bestResult.getCost()) {
                logWorseResult(iteration, bestResult, currentResult);
                applyRangeActions(bestResult);
                return bestResult;
            }

            logBetterResult(iteration, currentResult);
            bestResult = currentResult;
            linearProblem.updateBetweenSensiIteration(bestResult.getBranchResult(), bestResult.getSensitivityResult(), bestResult.getRangeActionActivationResult());
        }
        bestResult.setStatus(LinearProblemStatus.MAX_ITERATION_REACHED);
        return bestResult;
    }

    private SensitivityComputer runSensitivityAnalysis(SensitivityComputer sensitivityComputer, int iteration, RangeActionActivationResult currentRangeActionActivationResult) {
        SensitivityComputer tmpSensitivityComputer = sensitivityComputer;
        if (input.getOptimizationPerimeter() instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActionsInSecondaryStates = applyRangeActions(currentRangeActionActivationResult);
            tmpSensitivityComputer = createSensitivityComputer(appliedRemedialActionsInSecondaryStates);
        } else {
            applyRangeActions(currentRangeActionActivationResult);
            if (tmpSensitivityComputer == null) { // first iteration, do not need to be updated afterwards
                tmpSensitivityComputer = createSensitivityComputer(input.getPreOptimizationAppliedRemedialActions());
            }
        }
        runSensitivityAnalysis(tmpSensitivityComputer, input.getNetwork(), iteration);
        return tmpSensitivityComputer;
    }

    private RangeActionActivationResult resolveIfApproximatedPstTaps(IteratingLinearOptimizationResultImpl bestResult, LinearProblem linearProblem, int iteration, RangeActionActivationResult currentRangeActionActivationResult) {
        LinearProblemStatus solveStatus;
        RangeActionActivationResult rangeActionActivationResult = currentRangeActionActivationResult;
        if (parameters.getRangeActionParameters().getPstOptimizationApproximation().equals(RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS)) {

            // if the PST approximation is APPROXIMATED_INTEGERS, we re-solve the optimization problem
            // but first, we update it, with an adjustment of the PSTs angleToTap conversion factors, to
            // be more accurate in the neighboring of the previous solution

            // (idea: if too long, we could relax the first MIP, but no so straightforward to do with or-tools)
            linearProblem.updateBetweenMipIteration(rangeActionActivationResult);

            solveStatus = solveLinearProblem(linearProblem, iteration);
            if (solveStatus == LinearProblemStatus.OPTIMAL || solveStatus == LinearProblemStatus.FEASIBLE) {
                RangeActionActivationResult updatedLinearProblemResult = new LinearProblemResult(linearProblem, input.getPrePerimeterSetpoints(), input.getOptimizationPerimeter());
                rangeActionActivationResult = roundResult(updatedLinearProblemResult, bestResult);
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

    private boolean hasRemedialActionsChanged(RangeActionActivationResult newRangeActionActivationResult, RangeActionActivationResult oldRangeActionActivationResult, OptimizationPerimeter optimizationContext) {
        return optimizationContext.getRangeActionsPerState().entrySet().stream()
            .anyMatch(e -> e.getValue().stream()
                .anyMatch(ra -> Math.abs(newRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()) - oldRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey())) >= 1e-6));
    }

    private AppliedRemedialActions applyRangeActions(RangeActionActivationResult rangeActionActivationResult) {

        OptimizationPerimeter optimizationContext = input.getOptimizationPerimeter();

        // apply RangeAction from first optimization state
        optimizationContext.getRangeActionsPerState().get(optimizationContext.getMainOptimizationState())
            .forEach(ra -> ra.apply(input.getNetwork(), rangeActionActivationResult.getOptimizedSetpoint(ra, optimizationContext.getMainOptimizationState())));

        // add RangeAction activated in the following states
        if (optimizationContext instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActions = input.getPreOptimizationAppliedRemedialActions().copyNetworkActions();
            optimizationContext.getRangeActionsPerState().entrySet().stream()
                .filter(e -> !e.getKey().equals(optimizationContext.getMainOptimizationState())) // remove preventive state
                .forEach(e -> e.getValue().forEach(ra -> appliedRemedialActions.addAppliedRangeAction(e.getKey(), ra, rangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()))));
            return appliedRemedialActions;
        } else {
            return null;
        }
    }

    private SensitivityComputer createSensitivityComputer(AppliedRemedialActions appliedRemedialActions) {

        SensitivityComputer.SensitivityComputerBuilder builder = SensitivityComputer.create()
            .withCnecs(input.getOptimizationPerimeter().getFlowCnecs())
            .withRangeActions(input.getOptimizationPerimeter().getRangeActions())
            .withAppliedRemedialActions(appliedRemedialActions)
            .withToolProvider(input.getToolProvider());

        if (parameters.isRaoWithLoopFlowLimitation() && parameters.getLoopFlowParameters().getLoopFlowApproximationLevel().shouldUpdatePtdfWithPstChange()) {
            builder.withCommercialFlowsResults(input.getToolProvider().getLoopFlowComputation(), input.getOptimizationPerimeter().getLoopFlowCnecs());
        } else if (parameters.isRaoWithLoopFlowLimitation()) {
            builder.withCommercialFlowsResults(input.getPreOptimizationFlowResult());
        }
        if (parameters.getObjectiveFunction().doesRequirePtdf()) {
            builder.withPtdfsResults(input.getInitialFlowResult());
        }

        return builder.build();
    }

    private void runSensitivityAnalysis(SensitivityComputer sensitivityComputer, Network network, int iteration) {
        try {
            sensitivityComputer.compute(network);
        } catch (SensitivityAnalysisException e) {
            BUSINESS_WARNS.warn("Systematic sensitivity computation failed at iteration {}", iteration);
            throw e;
        }
    }

    private IteratingLinearOptimizationResultImpl createResult(FlowResult flowResult,
                                                               SensitivityResult sensitivityResult,
                                                               RangeActionActivationResult rangeActionActivation,
                                                               int nbOfIterations) {
        return new IteratingLinearOptimizationResultImpl(LinearProblemStatus.OPTIMAL, nbOfIterations, rangeActionActivation, flowResult,
            input.getObjectiveFunction().evaluate(flowResult, rangeActionActivation, sensitivityResult, sensitivityResult.getSensitivityStatus()), sensitivityResult);
    }

    private RangeActionActivationResult roundResult(RangeActionActivationResult linearProblemResult, IteratingLinearOptimizationResultImpl previousResult) {
        return BestTapFinder.round(
            linearProblemResult,
            input.getNetwork(),
            input.getOptimizationPerimeter(),
            input.getPrePerimeterSetpoints(),
            previousResult,
            parameters.getObjectiveFunctionUnit()
        );
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
            "Iteration {}: linear optimization found a worse result than previous iteration, with a cost increasing from {} to {} (functional: from {} to {})",
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
