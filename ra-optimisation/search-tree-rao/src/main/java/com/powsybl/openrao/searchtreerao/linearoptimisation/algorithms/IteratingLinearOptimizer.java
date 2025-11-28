/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.reports.LinearOptimizerReports;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.IteratingLinearOptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.LinearProblemResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.apache.commons.lang3.tuple.Pair;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getPstModel;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizer {

    private IteratingLinearOptimizer() {

    }

    public static LinearOptimizationResult optimize(final IteratingLinearOptimizerInput input,
                                                    final IteratingLinearOptimizerParameters parameters,
                                                    final ReportNode reportNode) {

        IteratingLinearOptimizationResultImpl bestResult = createResult(
                input.preOptimizationFlowResult(),
                input.preOptimizationSensitivityResult(),
                input.raActivationFromParentLeaf(),
                input.appliedNetworkActionsInPrimaryState(),
                0,
                input.objectiveFunction(),
                reportNode);

        IteratingLinearOptimizationResultImpl previousResult = bestResult;

        SensitivityComputer sensitivityComputer = null;

        LinearProblem linearProblem = LinearProblem.create()
                .buildFromInputsAndParameters(input, parameters);

        linearProblem.fill(input.preOptimizationFlowResult(), input.preOptimizationSensitivityResult());

        for (int iteration = 1; iteration <= parameters.getMaxNumberOfIterations(); iteration++) {
            LinearProblemStatus solveStatus = solveLinearProblem(linearProblem, iteration, reportNode);
            bestResult.setNbOfIteration(iteration);
            if (solveStatus == LinearProblemStatus.FEASIBLE) {
                LinearOptimizerReports.reportSolverInterrupted(reportNode);
            } else if (solveStatus != LinearProblemStatus.OPTIMAL) {
                LinearOptimizerReports.reportLinearOptimizationFailedAtIteration(reportNode, iteration);
                if (iteration == 1) {
                    bestResult.setStatus(solveStatus);
                    LinearOptimizerReports.reportLinearProblemFailedWithStatus(reportNode, solveStatus);
                    return bestResult;
                }
                bestResult.setStatus(LinearProblemStatus.FEASIBLE);
                return bestResult;
            }

            RangeActionActivationResult linearProblemResult = new LinearProblemResult(linearProblem, input.prePerimeterSetpoints(), input.optimizationPerimeter());
            RangeActionActivationResult currentRangeActionActivationResult = roundResult(linearProblemResult, bestResult, input, parameters);
            currentRangeActionActivationResult = resolveIfApproximatedPstTaps(bestResult, linearProblem, iteration, currentRangeActionActivationResult, input, parameters, reportNode);

            if (!hasAnyRangeActionChanged(currentRangeActionActivationResult, previousResult, input.optimizationPerimeter())) {
                // If the solution has not changed, no need to run a new sensitivity computation and iteration can stop
                LinearOptimizerReports.reportSameResultAsPreviousIterations(reportNode, iteration);
                return bestResult;
            }

            sensitivityComputer = runSensitivityAnalysis(sensitivityComputer, iteration, currentRangeActionActivationResult, input, parameters, reportNode);
            if (sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE) {
                bestResult.setStatus(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED);
                return bestResult;
            }

            IteratingLinearOptimizationResultImpl currentResult = createResult(
                    sensitivityComputer.getBranchResult(input.network()),
                    sensitivityComputer.getSensitivityResult(),
                    currentRangeActionActivationResult,
                    input.appliedNetworkActionsInPrimaryState(),
                    iteration,
                    input.objectiveFunction(),
                    reportNode
            );
            previousResult = currentResult;

            Pair<IteratingLinearOptimizationResultImpl, Boolean> mipShouldStop = updateBestResultAndCheckStopCondition(parameters.getRaRangeShrinking(), linearProblem, input, iteration, currentResult, bestResult, reportNode);
            if (Boolean.TRUE.equals(mipShouldStop.getRight())) {
                return bestResult;
            } else {
                bestResult = mipShouldStop.getLeft();
            }
        }
        bestResult.setStatus(LinearProblemStatus.MAX_ITERATION_REACHED);
        return bestResult;
    }

    private static SensitivityComputer runSensitivityAnalysis(final SensitivityComputer sensitivityComputer,
                                                              final int iteration,
                                                              final RangeActionActivationResult currentRangeActionActivationResult,
                                                              final IteratingLinearOptimizerInput input,
                                                              final IteratingLinearOptimizerParameters parameters,
                                                              final ReportNode reportNode) {
        SensitivityComputer tmpSensitivityComputer = sensitivityComputer;
        if (input.optimizationPerimeter() instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActionsInSecondaryStates = applyRangeActions(currentRangeActionActivationResult, input);
            tmpSensitivityComputer = createSensitivityComputer(appliedRemedialActionsInSecondaryStates, input, parameters, reportNode);
        } else {
            applyRangeActions(currentRangeActionActivationResult, input);
            if (tmpSensitivityComputer == null) { // first iteration, do not need to be updated afterwards
                tmpSensitivityComputer = createSensitivityComputer(input.preOptimizationAppliedRemedialActions(), input, parameters, reportNode);
            }
        }
        runSensitivityAnalysis(tmpSensitivityComputer, input.network(), iteration, reportNode);
        return tmpSensitivityComputer;
    }

    private static RangeActionActivationResult resolveIfApproximatedPstTaps(final IteratingLinearOptimizationResultImpl bestResult,
                                                                            final LinearProblem linearProblem,
                                                                            final int iteration,
                                                                            final RangeActionActivationResult currentRangeActionActivationResult,
                                                                            final IteratingLinearOptimizerInput input,
                                                                            final IteratingLinearOptimizerParameters parameters,
                                                                            final ReportNode reportNode) {
        LinearProblemStatus solveStatus;
        RangeActionActivationResult rangeActionActivationResult = currentRangeActionActivationResult;
        if (getPstModel(parameters.getRangeActionParametersExtension()).equals(PstModel.APPROXIMATED_INTEGERS)) {

            // if the PST approximation is APPROXIMATED_INTEGERS, we re-solve the optimization problem
            // but first, we update it, with an adjustment of the PSTs angleToTap conversion factors, to
            // be more accurate in the neighboring of the previous solution

            // (idea: if too long, we could relax the first MIP, but no so straightforward to do with or-tools)
            linearProblem.updateBetweenMipIteration(rangeActionActivationResult);

            solveStatus = solveLinearProblem(linearProblem, iteration, reportNode);
            if (solveStatus == LinearProblemStatus.OPTIMAL || solveStatus == LinearProblemStatus.FEASIBLE) {
                RangeActionActivationResult updatedLinearProblemResult = new LinearProblemResult(linearProblem, input.prePerimeterSetpoints(), input.optimizationPerimeter());
                rangeActionActivationResult = roundResult(updatedLinearProblemResult, bestResult, input, parameters);
            }
        }
        return rangeActionActivationResult;
    }

    private static LinearProblemStatus solveLinearProblem(final LinearProblem linearProblem,
                                                          final int iteration,
                                                          final ReportNode reportNode) {
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [start]", iteration);
        LinearProblemStatus status = linearProblem.solve();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [end]", iteration);
        return status;
    }

    private static boolean hasAnyRangeActionChanged(RangeActionActivationResult newRangeActionActivationResult, RangeActionActivationResult oldRangeActionActivationResult, OptimizationPerimeter optimizationContext) {
        return optimizationContext.getRangeActionsPerState().entrySet().stream()
                .anyMatch(e -> e.getValue().stream()
                        .anyMatch(ra -> Math.abs(newRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()) - oldRangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey())) >= 1e-6));
    }

    public static AppliedRemedialActions applyRangeActions(RangeActionActivationResult rangeActionActivationResult, IteratingLinearOptimizerInput input) {

        OptimizationPerimeter optimizationContext = input.optimizationPerimeter();

        // apply RangeAction from first optimization state
        optimizationContext.getRangeActionsPerState().get(optimizationContext.getMainOptimizationState())
                .forEach(ra -> ra.apply(input.network(), rangeActionActivationResult.getOptimizedSetpoint(ra, optimizationContext.getMainOptimizationState())));

        // add RangeAction activated in the following states
        if (optimizationContext instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActions = input.preOptimizationAppliedRemedialActions().copyNetworkActionsAndAutomaticRangeActions();

            optimizationContext.getRangeActionsPerState().entrySet().stream()
                    .filter(e -> !e.getKey().equals(optimizationContext.getMainOptimizationState())) // remove preventive state
                    .forEach(e -> e.getValue().forEach(ra -> appliedRemedialActions.addAppliedRangeAction(e.getKey(), ra, rangeActionActivationResult.getOptimizedSetpoint(ra, e.getKey()))));
            return appliedRemedialActions;
        } else {
            return null;
        }
    }

    private static SensitivityComputer createSensitivityComputer(final AppliedRemedialActions appliedRemedialActions,
                                                                 final IteratingLinearOptimizerInput input,
                                                                 final IteratingLinearOptimizerParameters parameters,
                                                                 final ReportNode reportNode) {

        SensitivityComputer.SensitivityComputerBuilder builder = SensitivityComputer.create(reportNode)
                .withCnecs(input.optimizationPerimeter().getFlowCnecs())
                .withRangeActions(input.optimizationPerimeter().getRangeActions())
                .withAppliedRemedialActions(appliedRemedialActions)
                .withToolProvider(input.toolProvider())
                .withOutageInstant(input.outageInstant());

        if (parameters.isRaoWithLoopFlowLimitation() && parameters.getLoopFlowParametersExtension().getPtdfApproximation().shouldUpdatePtdfWithPstChange()) {
            builder.withCommercialFlowsResults(input.toolProvider().getLoopFlowComputation(), input.optimizationPerimeter().getLoopFlowCnecs());
        } else if (parameters.isRaoWithLoopFlowLimitation()) {
            builder.withCommercialFlowsResults(input.preOptimizationFlowResult());
        }
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            if (parameters.getMaxMinRelativeMarginParameters().getPtdfApproximation().shouldUpdatePtdfWithPstChange()) {
                builder.withPtdfsResults(input.toolProvider().getAbsolutePtdfSumsComputation(), input.optimizationPerimeter().getFlowCnecs());
            } else {
                builder.withPtdfsResults(input.preOptimizationFlowResult());
            }
        }

        return builder.build();
    }

    private static void runSensitivityAnalysis(final SensitivityComputer sensitivityComputer,
                                               final Network network,
                                               final int iteration,
                                               final ReportNode reportNode) {
        sensitivityComputer.compute(network);
        if (sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE) {
            LinearOptimizerReports.reportSystematicSensitivityComputationFailedAtIteration(reportNode, iteration);
        }
    }

    private static IteratingLinearOptimizationResultImpl createResult(FlowResult flowResult,
                                                               SensitivityResult sensitivityResult,
                                                               RangeActionActivationResult rangeActionActivation,
                                                               NetworkActionsResult networkActionsResult,
                                                               int nbOfIterations,
                                                               ObjectiveFunction objectiveFunction,
                                                              final ReportNode reportNode) {
        return new IteratingLinearOptimizationResultImpl(LinearProblemStatus.OPTIMAL, nbOfIterations, rangeActionActivation, flowResult,
                objectiveFunction.evaluate(flowResult, new RemedialActionActivationResultImpl(rangeActionActivation, networkActionsResult), reportNode), sensitivityResult);
    }

    private static Pair<IteratingLinearOptimizationResultImpl, Boolean> updateBestResultAndCheckStopCondition(final boolean raRangeShrinking,
                                                                                                              final LinearProblem linearProblem,
                                                                                                              final IteratingLinearOptimizerInput input,
                                                                                                              final int iteration,
                                                                                                              final IteratingLinearOptimizationResultImpl currentResult,
                                                                                                              final IteratingLinearOptimizationResultImpl bestResult,
                                                                                                              final ReportNode reportNode) {
        if (currentResult.getCost() < bestResult.getCost()) {
            logBetterResult(iteration, currentResult, reportNode);
            linearProblem.updateBetweenSensiIteration(currentResult.getBranchResult(), currentResult.getSensitivityResult(), currentResult.getRangeActionActivationResult());
            return Pair.of(currentResult, false);
        }
        logWorseResult(iteration, bestResult, currentResult, reportNode);
        applyRangeActions(bestResult, input);
        if (raRangeShrinking) {
            linearProblem.updateBetweenSensiIteration(currentResult.getBranchResult(), currentResult.getSensitivityResult(), currentResult.getRangeActionActivationResult());
        }
        return Pair.of(bestResult, !raRangeShrinking);
    }

    private static RangeActionActivationResult roundResult(RangeActionActivationResult linearProblemResult, IteratingLinearOptimizationResultImpl previousResult, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        RangeActionActivationResultImpl roundedResult = roundPsts(linearProblemResult, previousResult, input, parameters);
        roundOtherRas(linearProblemResult, input.optimizationPerimeter(), roundedResult);
        return roundedResult;
    }

    private static RangeActionActivationResultImpl roundPsts(RangeActionActivationResult linearProblemResult, IteratingLinearOptimizationResultImpl previousResult, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        if (getPstModel(parameters.getRangeActionParametersExtension()).equals(PstModel.CONTINUOUS)) {
            return BestTapFinder.round(
                linearProblemResult,
                input.network(),
                input.optimizationPerimeter(),
                input.prePerimeterSetpoints(),
                previousResult,
                parameters.getObjectiveFunctionUnit()
            );
        }
        RangeActionActivationResultImpl roundedResult = new RangeActionActivationResultImpl(input.prePerimeterSetpoints());
        input.optimizationPerimeter().getRangeActionOptimizationStates().forEach(state -> linearProblemResult.getActivatedRangeActions(state)
            .stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast)
            .forEach(pst -> roundedResult.putResult(pst, state, pst.convertTapToAngle(linearProblemResult.getOptimizedTap(pst, state))))
        );
        return roundedResult;
    }

    static void roundOtherRas(RangeActionActivationResult linearProblemResult,
                             OptimizationPerimeter optimizationContext,
                             RangeActionActivationResultImpl roundedResult) {
        optimizationContext.getRangeActionsPerState().keySet().forEach(state -> linearProblemResult.getActivatedRangeActions(state).stream()
            .filter(ra -> !(ra instanceof PstRangeAction))
            .forEach(ra -> roundedResult.putResult(ra, state, Math.round(linearProblemResult.getOptimizedSetpoint(ra, state)))));
    }

    private static void logBetterResult(final int iteration,
                                        final ObjectiveFunctionResult currentObjectiveFunctionResult,
                                        final ReportNode reportNode) {
        LinearOptimizerReports.reportLinearOptimFoundBetterSolution(reportNode, iteration, currentObjectiveFunctionResult.getCost(), currentObjectiveFunctionResult.getFunctionalCost());

        currentObjectiveFunctionResult.getVirtualCostNames().forEach(vc -> {
            double cost = currentObjectiveFunctionResult.getVirtualCost(vc);
            if (cost > 1e-6) {
                LinearOptimizerReports.reportCostOf(reportNode, vc, cost);
            }
        });
    }

    private static void logWorseResult(final int iteration,
                                       final ObjectiveFunctionResult bestResult,
                                       final ObjectiveFunctionResult currentResult,
                                       final ReportNode reportNode) {
        LinearOptimizerReports.reportLinearOptimFoundWorseResult(reportNode,
                iteration,
                bestResult.getCost(),
                currentResult.getCost(),
                bestResult.getFunctionalCost(),
                currentResult.getFunctionalCost());

        currentResult.getVirtualCostNames().forEach(vc -> {
            double cost = currentResult.getVirtualCost(vc);
            if (cost > 1e-6) {
                LinearOptimizerReports.reportCostOf(reportNode, vc, cost);
            }
        });
    }
}
