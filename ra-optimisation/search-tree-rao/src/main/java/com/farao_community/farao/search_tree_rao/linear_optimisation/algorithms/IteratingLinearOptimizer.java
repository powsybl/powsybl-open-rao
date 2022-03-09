/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.GlobalOptimizationContext;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.OptimizationContext;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers.DiscretePstGroupFiller;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers.DiscretePstTapFiller;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers.ProblemFiller;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.inputs.IteratingLinearOptimizerInput;
import com.farao_community.farao.search_tree_rao.linear_optimisation.parameters.IteratingLinearOptimizerParameters;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.FailedLinearOptimizationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.IteratingLinearOptimizationResultImpl;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.result.impl.LinearProblemResult;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizer {
    private final ObjectiveFunction objectiveFunction;
    private final int maxIterations;
    private final RaoParameters.PstOptimizationApproximation pstOptimizationApproximation;

    public IteratingLinearOptimizer(ObjectiveFunction objectiveFunction, int maxIterations, RaoParameters.PstOptimizationApproximation pstOptimizationApproximation) {
        this.objectiveFunction = objectiveFunction;
        this.maxIterations = maxIterations;
        this.pstOptimizationApproximation = pstOptimizationApproximation;
    }

    public LinearOptimizationResult optimize(IteratingLinearOptimizerInput input,
                                             IteratingLinearOptimizerParameters parameters) {

        IteratingLinearOptimizationResultImpl bestResult = createResult(
            input.getPreOptimizationFlowResult(),
            input.getPreOptimizationSensitivityResult(),
            input.getRaActivationFromParentLeaf(),
            0);

        SensitivityComputer sensitivityComputer = null;

        LinearProblem linearProblem = new LinearProblemSmartBuilder()
            .withInputs(input)
            .withParameters(parameters)
            .build();

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            LinearProblemStatus solveStatus = solveLinearProblem(linearProblem, iteration);
            if (solveStatus == LinearProblemStatus.FEASIBLE) {
                TECHNICAL_LOGS.warn("The solver was interrupted. A feasible solution has been produced.");
            } else if (solveStatus != LinearProblemStatus.OPTIMAL) {
                BUSINESS_LOGS.error("Linear optimization failed at iteration {}", iteration);
                if (iteration == 1) {
                    return new FailedLinearOptimizationResultImpl();
                }
                bestResult.setStatus(LinearProblemStatus.FEASIBLE);
                return bestResult;
            }

            RangeActionActivationResult linearProblemResult = new LinearProblemResult(linearProblem, input.getPrePerimeterSetpoints(), input.getOptimizationContext());
            RangeActionActivationResult currentRangeActionActivationResult = roundResult(
                linearProblemResult,
                input.getNetwork(),
                bestResult,
                input.getOptimizationContext().getFirstOptimizedState(),
                input.getPrePerimeterSetpoints());

            if (pstOptimizationApproximation.equals(RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS)) {

                // if the PST approximation is APPROXIMATED_INTEGERS, we re-solve the optimization problem
                // but first, we update it, with an adjustment of the PSTs angleToTap conversion factors, to
                // be more accurate in the neighboring of the previous solution

                // (idea: if too long, we could relax the first MIP, but no so straightforward to do with or-tools)
                linearProblem.updateBetweenMipIteration(currentRangeActionActivationResult);

                solveStatus = solveLinearProblem(linearProblem, iteration);
                if (solveStatus == LinearProblemStatus.OPTIMAL || solveStatus == LinearProblemStatus.FEASIBLE) {
                    currentRangeActionActivationResult = roundResult(
                        new LinearProblemResult(linearProblem, input.getPrePerimeterSetpoints(), input.getOptimizationContext()),                        input.getNetwork(),
                        bestResult,
                        input.getOptimizationContext().getFirstOptimizedState(),
                        input.getPrePerimeterSetpoints());
                }
            }

            if (!hasRemedialActionsChanged(currentRangeActionActivationResult, bestResult, input.getOptimizationContext())) {
                // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
                TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration);
                return bestResult;
            }

            try {
                if (input.getOptimizationContext() instanceof GlobalOptimizationContext) {
                    AppliedRemedialActions appliedRemedialActionsInSecondaryStates = applyRangeActions(input, currentRangeActionActivationResult);
                    sensitivityComputer = createSensitivityComputer(input, appliedRemedialActionsInSecondaryStates);
                    runSensitivityAnalysis(sensitivityComputer, input.getNetwork(), iteration);
                } else {
                    applyRangeActions(input, currentRangeActionActivationResult);
                    if (sensitivityComputer == null) { // first iteration, do not need to be updated afterwards
                        sensitivityComputer = createSensitivityComputer(input, input.getPreOptimizationAppliedRemedialActions());
                    }
                    runSensitivityAnalysis(sensitivityComputer, input.getNetwork(), iteration);
                }

            } catch (SensitivityAnalysisException e) {
                bestResult.setStatus(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED);
                return bestResult;
            }

            IteratingLinearOptimizationResultImpl currentResult = createResult(
                sensitivityComputer.getBranchResult(),
                sensitivityComputer.getSensitivityResult(),
                currentRangeActionActivationResult,
                iteration
            );

            if (currentResult.getCost() >= bestResult.getCost()) {
                logWorseResult(iteration, bestResult, currentResult);
                applyRangeActions(input, bestResult);
                return bestResult;
            }

            logBetterResult(iteration, currentResult);
            bestResult = currentResult;
            linearProblem.updateBetweenSensiIteration(bestResult.getBranchResult(), bestResult.getSensitivityResult(), bestResult.getRangeActionResult());
        }
        bestResult.setStatus(LinearProblemStatus.MAX_ITERATION_REACHED);
        return bestResult;
    }

    private static LinearProblemStatus solveLinearProblem(LinearProblem linearProblem, int iteration) {
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [start]", iteration);
        LinearProblemStatus status = linearProblem.solve();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [end]", iteration);
        return status;
    }

    private static boolean hasRemedialActionsChanged(RangeActionActivationResult newRangeActionActivationResult, RangeActionActivationResult oldRangeActionActivationResult, OptimizationContext optimizationContext) {
        return optimizationContext.getAvailableRangeActions().entrySet().stream()
            .anyMatch(e -> e.getValue().stream()
                .anyMatch(ra -> Math.abs(newRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()) - oldRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey())) >= 1e-6));
    }

    private AppliedRemedialActions applyRangeActions(IteratingLinearOptimizerInput input, RangeActionActivationResult rangeActionActivationResult) {

        OptimizationContext optimizationContext = input.getOptimizationContext();

        // apply RangeAction from first optimization state
        optimizationContext.getAvailableRangeActions().get(optimizationContext.getFirstOptimizedState())
            .forEach(ra -> ra.apply(input.getNetwork(), rangeActionActivationResult.getOptimizedSetpoint(ra, optimizationContext.getFirstOptimizedState())));

        // add RangeAction activated in the following states
        if (optimizationContext instanceof GlobalOptimizationContext) {
            AppliedRemedialActions appliedRemedialActions = input.getPreOptimizationAppliedRemedialActions().copy();
            optimizationContext.getAvailableRangeActions().entrySet().stream()
                .filter(e -> !e.getKey().equals(optimizationContext.getFirstOptimizedState())) // remove preventive state
                .forEach(e -> e.getValue().forEach(ra -> appliedRemedialActions.addAppliedRangeAction(e.getKey(), ra, rangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()))));
            return appliedRemedialActions;
        } else {
            return null;
        }
    }

    private static SensitivityComputer createSensitivityComputer(IteratingLinearOptimizerInput input, AppliedRemedialActions appliedRemedialActions) {
        return SensitivityComputer.create()
            .withCnecs(input.getFlowCnecs())
            .withCommercialFlowsResults(input.getInitialFlowResult())
            .withPtdfsResults(input.getInitialFlowResult())
            .withAppliedRemedialActions(appliedRemedialActions)
            .withToolProvider(input.getToolProvider())
            .build();
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
            objectiveFunction.evaluate(flowResult, sensitivityResult.getSensitivityStatus()), sensitivityResult);
    }

    private RangeActionActivationResult roundResult(RangeActionActivationResult rangeActionActivationResult, Network network, IteratingLinearOptimizationResultImpl previousResult, State optimizedState, RangeActionSetpointResult prePerimeterSetpoints) {
        Map<RangeAction<?>, Double> roundedSetPoints = new HashMap<>();
        rangeActionActivationResult.getRangeActions().stream().filter(PstRangeAction.class::isInstance).forEach(
            rangeAction -> roundedSetPoints.put(rangeAction, rangeActionActivationResult.getOptimizedSetpoint(rangeAction, optimizedState))
        );
        rangeActionActivationResult.getRangeActions().stream().filter(rangeAction -> !(rangeAction instanceof PstRangeAction)).forEach(
            rangeAction -> roundedSetPoints.put(rangeAction, (double) Math.round(rangeActionActivationResult.getOptimizedSetpoint(rangeAction, optimizedState)))
        );

        return BestTapFinder.find(
            roundedSetPoints,
            network,
            optimizedState,
            prePerimeterSetpoints,
            previousResult.getObjectiveFunctionResult().getMostLimitingElements(10),
            previousResult.getBranchResult(),
            previousResult.getSensitivityResult()
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
