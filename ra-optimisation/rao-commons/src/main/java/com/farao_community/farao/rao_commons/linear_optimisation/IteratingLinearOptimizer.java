/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.SensitivityComputer;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.DiscretePstGroupFiller;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.DiscretePstTapFiller;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.rao_commons.result_api.*;
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

    public LinearOptimizationResult optimize(LinearProblem linearProblem,
                                             Network network,
                                             FlowResult preOptimFlowResult,
                                             SensitivityResult preOptimSensitivityResult,
                                             RangeActionResult preOptimRangeActionResult,
                                             SensitivityComputer sensitivityComputer) {
        IteratingLinearOptimizerResult bestResult = createResult(preOptimFlowResult, preOptimSensitivityResult, preOptimRangeActionResult, 0);

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            solveLinearProblem(linearProblem, iteration);
            if (linearProblem.getStatus() == LinearProblemStatus.FEASIBLE) {
                TECHNICAL_LOGS.warn("The solver was interrupted. A feasible solution has been produced.");
            } else if (linearProblem.getStatus() != LinearProblemStatus.OPTIMAL) {
                BUSINESS_LOGS.error("Linear optimization failed at iteration {}", iteration);
                if (iteration == 1) {
                    return new FailedLinearOptimizationResult();
                }
                bestResult.setStatus(LinearProblemStatus.FEASIBLE);
                return bestResult;
            }

            RangeActionResult currentRangeActionResult = roundResult(linearProblem.getResults(), network, bestResult);

            if (pstOptimizationApproximation.equals(RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS)) {

                // if the PST approximation is APPROXIMATED_INTEGERS, we re-solve the optimization problem
                // but first, we update it, with an adjustment of the PSTs angleToTap conversion factors, to
                // be more accurate in the neighboring of the previous solution

                // (idea: if too long, we could relax the first MIP, but no so straightforward to do with or-tools)

                for (ProblemFiller filler : linearProblem.getFillers()) {
                    // a bit dirty, but computationally more efficient than updating all fillers
                    // (cleaning idea: create two update methods in API)
                    if (filler instanceof DiscretePstTapFiller || filler instanceof DiscretePstGroupFiller) {
                        filler.update(linearProblem, preOptimFlowResult, preOptimSensitivityResult, currentRangeActionResult);
                    }
                }

                solveLinearProblem(linearProblem, iteration);
                if (linearProblem.getStatus() == LinearProblemStatus.OPTIMAL || linearProblem.getStatus() == LinearProblemStatus.FEASIBLE) {
                    currentRangeActionResult = roundResult(linearProblem.getResults(), network, bestResult);
                }
            }

            if (!hasRemedialActionsChanged(currentRangeActionResult, bestResult)) {
                // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
                TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration);
                return bestResult;
            }

            try {
                applyRangeActionsAndRunSensitivityAnalysis(sensitivityComputer, linearProblem.getRangeActions(), currentRangeActionResult, network, iteration);
            } catch (SensitivityAnalysisException e) {
                bestResult.setStatus(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED);
                return bestResult;
            }

            IteratingLinearOptimizerResult currentResult = createResult(
                sensitivityComputer.getBranchResult(),
                sensitivityComputer.getSensitivityResult(),
                currentRangeActionResult,
                iteration
            );

            if (currentResult.getCost() >= bestResult.getCost()) {
                logWorseResult(iteration, bestResult, currentResult);
                applyRangeActions(currentResult.getRangeActions(), bestResult, network);
                return bestResult;
            }

            logBetterResult(iteration, currentResult);
            bestResult = currentResult;
            linearProblem.update(bestResult.getBranchResult(), bestResult.getSensitivityResult(), bestResult.getRangeActionResult());
        }
        bestResult.setStatus(LinearProblemStatus.MAX_ITERATION_REACHED);
        return bestResult;
    }

    private static void solveLinearProblem(LinearProblem linearProblem, int iteration) {
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [start]", iteration);
        linearProblem.solve();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [end]", iteration);
    }

    static boolean hasRemedialActionsChanged(RangeActionResult newRangeActionResult, RangeActionResult oldRangeActionResult) {
        if (!(newRangeActionResult.getRangeActions().equals(oldRangeActionResult.getRangeActions()))) {
            return true;
        }
        for (RangeAction<?> rangeAction : newRangeActionResult.getRangeActions()) {
            if (Math.abs(newRangeActionResult.getOptimizedSetPoint(rangeAction) - oldRangeActionResult.getOptimizedSetPoint(rangeAction)) >= 1e-6) {
                return true;
            }
        }
        return false;
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

    private void applyRangeActions(Set<RangeAction<?>> rangeActions,
                                   RangeActionResult rangeActionResult,
                                   Network network) {
        rangeActions.forEach(rangeAction -> rangeAction.apply(network, rangeActionResult.getOptimizedSetPoint(rangeAction)));
    }

    private void applyRangeActionsAndRunSensitivityAnalysis(SensitivityComputer sensitivityComputer,
                                                            Set<RangeAction<?>> rangeActions,
                                                            RangeActionResult rangeActionResult,
                                                            Network network,
                                                            int iteration) {
        applyRangeActions(rangeActions, rangeActionResult, network);
        try {
            sensitivityComputer.compute(network);
        } catch (SensitivityAnalysisException e) {
            BUSINESS_WARNS.warn("Systematic sensitivity computation failed at iteration {}", iteration);
            throw e;
        }
    }

    private IteratingLinearOptimizerResult createResult(FlowResult flowResult,
                                                        SensitivityResult sensitivityResult,
                                                        RangeActionResult rangeActionResult,
                                                        int nbOfIterations) {
        return new IteratingLinearOptimizerResult(LinearProblemStatus.OPTIMAL, nbOfIterations, rangeActionResult, flowResult,
            objectiveFunction.evaluate(flowResult, sensitivityResult.getSensitivityStatus()), sensitivityResult);
    }

    private RangeActionResult roundResult(RangeActionResult rangeActionResult, Network network, IteratingLinearOptimizerResult previousResult) {
        Map<RangeAction<?>, Double> roundedSetPoints = new HashMap<>();
        rangeActionResult.getOptimizedSetPoints().keySet().stream().filter(PstRangeAction.class::isInstance).forEach(
            rangeAction -> roundedSetPoints.put(rangeAction, rangeActionResult.getOptimizedSetPoint(rangeAction))
        );
        rangeActionResult.getOptimizedSetPoints().keySet().stream().filter(rangeAction -> !(rangeAction instanceof PstRangeAction)).forEach(
            rangeAction -> roundedSetPoints.put(rangeAction, (double) Math.round(rangeActionResult.getOptimizedSetPoint(rangeAction)))
        );

        return BestTapFinder.find(
            roundedSetPoints,
            network,
            previousResult.getObjectiveFunctionResult().getMostLimitingElements(10),
            previousResult.getBranchResult(),
            previousResult.getSensitivityResult()
        );
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ENGLISH, "%.2f", value);
    }
}
