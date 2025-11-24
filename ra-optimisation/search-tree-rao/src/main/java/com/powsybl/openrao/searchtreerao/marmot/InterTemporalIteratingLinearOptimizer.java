/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.BestTapFinder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.ProblemFillerHelper;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.PowerGradientConstraintFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.reports.LinearOptimizerReports;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.LinearProblemResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getPstModel;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class InterTemporalIteratingLinearOptimizer {

    private InterTemporalIteratingLinearOptimizer() {
    }

    public static GlobalLinearOptimizationResult optimize(final InterTemporalIteratingLinearOptimizerInput input,
                                                          final IteratingLinearOptimizerParameters parameters,
                                                          final ReportNode reportNode) {
        // 1. Initialize best result using input data
        GlobalLinearOptimizationResult bestResult = createInitialResult(
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::preOptimizationFlowResult),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::preOptimizationSensitivityResult),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterSetpoints).map(RangeActionActivationResultImpl::new),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::appliedNetworkActionsInPrimaryState),
            input.objectiveFunction(),
            reportNode
        );
        GlobalLinearOptimizationResult previousResult = bestResult;

        TemporalData<SensitivityComputer> sensitivityComputers = new TemporalDataImpl<>();

        // 2. Initialize linear problem using input data
        TemporalData<List<ProblemFiller>> problemFillers = getProblemFillersPerTimestamp(input, parameters);
        List<ProblemFiller> interTemporalProblemFillers = getInterTemporalProblemFillers(input);
        LinearProblem linearProblem = buildLinearProblem(problemFillers, interTemporalProblemFillers, parameters);
        fillLinearProblem(
            linearProblem,
            problemFillers,
            interTemporalProblemFillers,
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::preOptimizationFlowResult),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::preOptimizationSensitivityResult),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterSetpoints));

        // 3. Iterate
        for (int iteration = 1; iteration <= parameters.getMaxNumberOfIterations(); iteration++) {
            // a. Solve linear problem
            LinearProblemStatus solveStatus = solveLinearProblem(linearProblem, iteration, reportNode);
            // b. Check linear problem status and return best result if not FEASIBLE not OPTIMAL
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

            // c. Get and round range action activation results from solver results
            // TODO: we could use a GlobalRangeActionActivationResult rather than a TemporalData<RangeActionActivationResult>
            TemporalData<RangeActionActivationResult> rangeActionActivationPerTimestamp = retrieveRangeActionActivationResults(linearProblem, input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterSetpoints), input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::optimizationPerimeter));
            Map<OffsetDateTime, RangeActionActivationResult> roundedResults = new HashMap<>();

            for (OffsetDateTime timestamp : rangeActionActivationPerTimestamp.getTimestamps()) {
                roundedResults.put(timestamp, roundResult(rangeActionActivationPerTimestamp.getData(timestamp).orElseThrow(), bestResult, input.iteratingLinearOptimizerInputs().getData(timestamp).orElseThrow(), parameters));
            }

            rangeActionActivationPerTimestamp = new TemporalDataImpl<>(roundedResults);
            rangeActionActivationPerTimestamp = resolveIfApproximatedPstTaps(bestResult, linearProblem, iteration, rangeActionActivationPerTimestamp, input, parameters, problemFillers, reportNode);

            // d. Check if set-points have changed; if no, return the best result
            if (!hasAnyRangeActionChanged(
                input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::optimizationPerimeter),
                previousResult,
                rangeActionActivationPerTimestamp)) {
                LinearOptimizerReports.reportSameResultAsPreviousIterations(reportNode, iteration);
                return bestResult;
            }

            // e.  Run sensitivity analyses with new set-points
            Map<OffsetDateTime, SensitivityComputer> newSensitivityComputers = new HashMap<>();
            for (OffsetDateTime timestamp : rangeActionActivationPerTimestamp.getTimestamps()) {
                newSensitivityComputers.put(timestamp, runSensitivityAnalysis(sensitivityComputers.getData(timestamp).orElse(null), iteration, rangeActionActivationPerTimestamp.getData(timestamp).orElseThrow(), input.iteratingLinearOptimizerInputs().getData(timestamp).orElseThrow(), parameters, reportNode));
            }

            if (newSensitivityComputers.values().stream().anyMatch(sensitivityComputer -> sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE)) {
                bestResult.setStatus(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED);
                return bestResult;
            }

            sensitivityComputers = new TemporalDataImpl<>(newSensitivityComputers);

            GlobalLinearOptimizationResult newResult = createResultFromData(
                sensitivityComputers,
                input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::network),
                rangeActionActivationPerTimestamp,
                input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::appliedNetworkActionsInPrimaryState),
                input.objectiveFunction(),
                reportNode
            );
            previousResult = newResult;

            // f. Update problem fillers with flows, sensitivity coefficients and set-points
            Pair<GlobalLinearOptimizationResult, Boolean> mipShouldStop = updateBestResultAndCheckStopCondition(parameters.getRaRangeShrinking(), linearProblem, input, iteration, newResult, bestResult, problemFillers, interTemporalProblemFillers, reportNode);
            if (Boolean.TRUE.equals(mipShouldStop.getRight())) {
                return bestResult;
            } else {
                bestResult = mipShouldStop.getLeft();
            }
        }

        bestResult.setStatus(LinearProblemStatus.MAX_ITERATION_REACHED);
        return bestResult;
    }

    /* Helper methods */
    // Linear problem management
    private static TemporalData<List<ProblemFiller>> getProblemFillersPerTimestamp(InterTemporalIteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        Map<OffsetDateTime, List<ProblemFiller>> problemFillers = new HashMap<>();
        input.iteratingLinearOptimizerInputs().getDataPerTimestamp().forEach((timestamp, linearOptimizerInput) -> problemFillers.put(timestamp, ProblemFillerHelper.getProblemFillers(linearOptimizerInput, parameters, timestamp)));
        return new TemporalDataImpl<>(problemFillers);
    }

    private static List<ProblemFiller> getInterTemporalProblemFillers(InterTemporalIteratingLinearOptimizerInput input) {
        // TODO: add inter-temporal margin filler (min of all min margins)
        TemporalData<State> preventiveStates = input.iteratingLinearOptimizerInputs().map(linearOptimizerInput -> linearOptimizerInput.optimizationPerimeter().getMainOptimizationState());
        TemporalData<Set<InjectionRangeAction>> preventiveInjectionRangeActions = input.iteratingLinearOptimizerInputs().map(linearOptimizerInput -> filterPreventiveInjectionRangeAction(linearOptimizerInput.optimizationPerimeter().getRangeActions()));
        return List.of(new PowerGradientConstraintFiller(preventiveStates, preventiveInjectionRangeActions, input.intertemporalConstraints().getGeneratorConstraints()));
    }

    private static Set<InjectionRangeAction> filterPreventiveInjectionRangeAction(Set<RangeAction<?>> rangeActions) {
        return rangeActions.stream().filter(InjectionRangeAction.class::isInstance).map(InjectionRangeAction.class::cast).collect(Collectors.toSet());
    }

    private static LinearProblem buildLinearProblem(TemporalData<List<ProblemFiller>> problemFillers, List<ProblemFiller> interTemporalProblemFillers, IteratingLinearOptimizerParameters parameters) {
        LinearProblemBuilder linearProblemBuilder = LinearProblem.create()
            .withSolver(parameters.getSolverParameters().getSolver())
            .withRelativeMipGap(parameters.getSolverParameters().getRelativeMipGap())
            .withSolverSpecificParameters(parameters.getSolverParameters().getSolverSpecificParameters());

        // add problem fillers for each timestamp and inter-temporal timestamps
        problemFillers.getDataPerTimestamp().values().forEach(problemFillerOfTimestamp -> problemFillerOfTimestamp.forEach(linearProblemBuilder::withProblemFiller));
        interTemporalProblemFillers.forEach(linearProblemBuilder::withProblemFiller);

        return linearProblemBuilder.build();
    }

    private static void fillLinearProblem(LinearProblem linearProblem, TemporalData<List<ProblemFiller>> problemFillers, List<ProblemFiller> interTemporalProblemFillers, TemporalData<FlowResult> flowResults, TemporalData<SensitivityResult> sensitivityResults, TemporalData<RangeActionSetpointResult> setPoints) {
        List<OffsetDateTime> timestamps = problemFillers.getTimestamps();
        timestamps.forEach(timestamp -> {
            List<ProblemFiller> problemFillersForTimestamp = problemFillers.getData(timestamp).orElseThrow();
            problemFillersForTimestamp.forEach(problemFiller -> problemFiller.fill(linearProblem, flowResults.getData(timestamp).orElseThrow(), sensitivityResults.getData(timestamp).orElseThrow(), new RangeActionActivationResultImpl(setPoints.getData(timestamp).orElseThrow())));
        });
        // For now, the Power Gradient Constraint filler is the only inter-temporal filler and does not use any input but the linear problem
        // A global inter-temporal flow/sensitivity/set-point result does not exist anyway
        interTemporalProblemFillers.forEach(problemFiller -> problemFiller.fill(linearProblem, null, null, null));
    }

    private static void updateLinearProblemBetweenMipIterations(LinearProblem linearProblem, TemporalData<List<ProblemFiller>> problemFillers, TemporalData<RangeActionActivationResult> rangeActionActivationResults) {
        List<OffsetDateTime> timestamps = problemFillers.getTimestamps();
        timestamps.forEach(timestamp -> {
            List<ProblemFiller> problemFillersForTimestamp = problemFillers.getData(timestamp).orElseThrow();
            problemFillersForTimestamp.forEach(problemFiller -> problemFiller.updateBetweenMipIteration(linearProblem, rangeActionActivationResults.getData(timestamp).orElseThrow()));
        });
    }

    private static void updateLinearProblemBetweenSensiComputations(LinearProblem linearProblem, TemporalData<List<ProblemFiller>> problemFillers, List<ProblemFiller> interTemporalProblemFillers, LinearOptimizationResult optimizationResult) {
        linearProblem.reset();
        List<OffsetDateTime> timestamps = problemFillers.getTimestamps();
        timestamps.forEach(timestamp -> {
            List<ProblemFiller> problemFillersForTimestamp = problemFillers.getData(timestamp).orElseThrow();
            problemFillersForTimestamp.forEach(problemFiller -> problemFiller.fill(linearProblem, optimizationResult, optimizationResult, optimizationResult));
        });
        interTemporalProblemFillers.forEach(problemFiller -> problemFiller.fill(linearProblem, null, null, null));
    }

    private static LinearProblemStatus solveLinearProblem(final LinearProblem linearProblem,
                                                          final int iteration,
                                                          final ReportNode reportNode) {
        LinearOptimizerReports.reportLinearOptimizationAtIterationStart(reportNode, iteration);
        LinearProblemStatus status = linearProblem.solve();
        LinearOptimizerReports.reportLinearOptimizationAtIterationEnd(reportNode, iteration);
        return status;
    }

    // Sensitivity analysis

    private static SensitivityComputer runSensitivityAnalysis(final SensitivityComputer sensitivityComputer,
                                                              final int iteration,
                                                              final RangeActionActivationResult currentRangeActionActivationResult,
                                                              final IteratingLinearOptimizerInput input,
                                                              final IteratingLinearOptimizerParameters parameters,
                                                              final ReportNode reportNode) {
        SensitivityComputer tmpSensitivityComputer = sensitivityComputer;
        // TODO: if we want to force 2P, shoud always be global
        if (input.optimizationPerimeter() instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActionsInSecondaryStates = IteratingLinearOptimizer.applyRangeActions(currentRangeActionActivationResult, input);
            tmpSensitivityComputer = createSensitivityComputer(appliedRemedialActionsInSecondaryStates, input, parameters, reportNode);
        } else {
            IteratingLinearOptimizer.applyRangeActions(currentRangeActionActivationResult, input);
            if (tmpSensitivityComputer == null) { // first iteration, do not need to be updated afterwards
                tmpSensitivityComputer = createSensitivityComputer(input.preOptimizationAppliedRemedialActions(), input, parameters, reportNode);
            }
        }
        runSensitivityAnalysis(tmpSensitivityComputer, input.network(), iteration, reportNode);
        return tmpSensitivityComputer;
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

    // Result management
    private static GlobalLinearOptimizationResult createInitialResult(final TemporalData<FlowResult> flowResults,
                                                                      final TemporalData<SensitivityResult> sensitivityResults,
                                                                      final TemporalData<RangeActionActivationResult> rangeActionActivations,
                                                                      final TemporalData<NetworkActionsResult> preventiveTopologicalActions,
                                                                      final ObjectiveFunction objectiveFunction,
                                                                      final ReportNode reportNode) {
        return new GlobalLinearOptimizationResult(flowResults, sensitivityResults, rangeActionActivations, preventiveTopologicalActions, objectiveFunction, LinearProblemStatus.OPTIMAL, reportNode);
    }

    private static GlobalLinearOptimizationResult createResultFromData(final TemporalData<SensitivityComputer> sensitivityComputers,
                                                                       final TemporalData<Network> networks,
                                                                       final TemporalData<RangeActionActivationResult> rangeActionActivation,
                                                                       final TemporalData<NetworkActionsResult> preventiveTopologicalActions,
                                                                       final ObjectiveFunction objectiveFunction,
                                                                       final ReportNode reportNode) {
        Map<OffsetDateTime, FlowResult> flowResults = new HashMap<>();
        for (OffsetDateTime timestamp : sensitivityComputers.getTimestamps()) {
            FlowResult flowResult = sensitivityComputers.getData(timestamp).orElseThrow().getBranchResult(networks.getData(timestamp).orElseThrow());
            flowResults.put(timestamp, flowResult);
        }
        return new GlobalLinearOptimizationResult(new TemporalDataImpl<>(flowResults), sensitivityComputers.map(SensitivityComputer::getSensitivityResult), rangeActionActivation, preventiveTopologicalActions, objectiveFunction, LinearProblemStatus.OPTIMAL, reportNode);
    }

    // Set-point rounding
    private static TemporalData<RangeActionActivationResult> resolveIfApproximatedPstTaps(final GlobalLinearOptimizationResult bestResult,
                                                                                          final LinearProblem linearProblem,
                                                                                          final int iteration,
                                                                                          final TemporalData<RangeActionActivationResult> currentRangeActionActivationResults,
                                                                                          final InterTemporalIteratingLinearOptimizerInput input,
                                                                                          final IteratingLinearOptimizerParameters parameters,
                                                                                          final TemporalData<List<ProblemFiller>> problemFillers,
                                                                                          final ReportNode reportNode) {
        LinearProblemStatus solveStatus;
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = currentRangeActionActivationResults;
        if (getPstModel(parameters.getRangeActionParametersExtension()).equals(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS)) {

            // if the PST approximation is APPROXIMATED_INTEGERS, we re-solve the optimization problem
            // but first, we update it, with an adjustment of the PSTs angleToTap conversion factors, to
            // be more accurate in the neighboring of the previous solution

            // (idea: if too long, we could relax the first MIP, but no so straightforward to do with or-tools)
            updateLinearProblemBetweenMipIterations(linearProblem, problemFillers, rangeActionActivationResults);

            solveStatus = solveLinearProblem(linearProblem, iteration, reportNode);
            if (solveStatus == LinearProblemStatus.OPTIMAL || solveStatus == LinearProblemStatus.FEASIBLE) {
                TemporalData<RangeActionActivationResult> updatedLinearProblemResults = retrieveRangeActionActivationResults(linearProblem, input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterSetpoints), input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::optimizationPerimeter));
                Map<OffsetDateTime, RangeActionActivationResult> roundedResults = new HashMap<>();
                updatedLinearProblemResults.getDataPerTimestamp().forEach((timestamp, rangeActionActivationResult) -> roundedResults.put(timestamp, roundResult(rangeActionActivationResult, bestResult, input.iteratingLinearOptimizerInputs().getData(timestamp).orElseThrow(), parameters)));
                rangeActionActivationResults = new TemporalDataImpl<>(roundedResults);
            }
        }
        return rangeActionActivationResults;
    }

    // Logging
    private static void logBetterResult(final int iteration, final LinearOptimizationResult result, final ReportNode reportNode) {
        LinearOptimizerReports.reportLinearOptimFoundBetterSolution(reportNode, iteration, result.getCost(), result.getFunctionalCost());

        result.getVirtualCostNames().forEach(vc -> {
            double cost = result.getVirtualCost(vc);
            if (cost > 1e-6) {
                LinearOptimizerReports.reportCostOf(reportNode, vc, cost);
            }
        });
    }

    private static void logWorseResult(final int iteration,
                                       final LinearOptimizationResult bestResult,
                                       final LinearOptimizationResult currentResult,
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

    private static RangeActionActivationResult roundResult(RangeActionActivationResult linearProblemResult, LinearOptimizationResult previousResult, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        RangeActionActivationResultImpl roundedResult = roundPsts(linearProblemResult, previousResult, input, parameters);
        roundOtherRas(linearProblemResult, input.optimizationPerimeter(), roundedResult);
        return roundedResult;
    }

    private static RangeActionActivationResultImpl roundPsts(RangeActionActivationResult linearProblemResult, LinearOptimizationResult previousResult, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        if (getPstModel(parameters.getRangeActionParametersExtension()).equals(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS)) {
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

    // TODO: check that this does not violate gradient constraints
    static void roundOtherRas(RangeActionActivationResult linearProblemResult,
                              OptimizationPerimeter optimizationContext,
                              RangeActionActivationResultImpl roundedResult) {
        optimizationContext.getRangeActionsPerState().keySet().forEach(state -> linearProblemResult.getActivatedRangeActions(state).stream()
            .filter(ra -> !(ra instanceof PstRangeAction))
            .forEach(ra -> roundedResult.putResult(ra, state, Math.round(linearProblemResult.getOptimizedSetpoint(ra, state)))));
    }

    private static TemporalData<RangeActionActivationResult> retrieveRangeActionActivationResults(LinearProblem linearProblem, TemporalData<RangeActionSetpointResult> prePerimeterSetPoints, TemporalData<OptimizationPerimeter> optimizationPerimeters) {
        Map<OffsetDateTime, RangeActionActivationResult> linearOptimizationResults = new HashMap<>();
        List<OffsetDateTime> timestamps = optimizationPerimeters.getTimestamps();
        timestamps.forEach(timestamp -> linearOptimizationResults.put(timestamp, new LinearProblemResult(linearProblem, prePerimeterSetPoints.getData(timestamp).orElseThrow(), optimizationPerimeters.getData(timestamp).orElseThrow())));
        return new TemporalDataImpl<>(linearOptimizationResults);
    }

    // Stop criterion

    private static boolean hasAnyRangeActionChanged(TemporalData<OptimizationPerimeter> optimizationPerimeters, RangeActionActivationResult previousSetPoints, TemporalData<RangeActionActivationResult> newSetPoints) {
        for (OffsetDateTime timestamp : optimizationPerimeters.getTimestamps()) {
            OptimizationPerimeter optimizationPerimeter = optimizationPerimeters.getData(timestamp).orElseThrow();
            RangeActionActivationResult newSetPointsAtTimestamp = newSetPoints.getData(timestamp).orElseThrow();
            for (Map.Entry<State, Set<RangeAction<?>>> activatedRangeActionAtState : optimizationPerimeter.getRangeActionsPerState().entrySet()) {
                State state = activatedRangeActionAtState.getKey();
                for (RangeAction<?> rangeAction : activatedRangeActionAtState.getValue()) {
                    if (Math.abs(newSetPointsAtTimestamp.getOptimizedSetpoint(rangeAction, state) - previousSetPoints.getOptimizedSetpoint(rangeAction, state)) >= 1e-6) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Pair<GlobalLinearOptimizationResult, Boolean> updateBestResultAndCheckStopCondition(final boolean raRangeShrinking,
                                                                                                       final LinearProblem linearProblem,
                                                                                                       final InterTemporalIteratingLinearOptimizerInput input,
                                                                                                       final int iteration,
                                                                                                       final GlobalLinearOptimizationResult currentResult,
                                                                                                       final GlobalLinearOptimizationResult bestResult,
                                                                                                       final TemporalData<List<ProblemFiller>> problemFillers,
                                                                                                       final List<ProblemFiller> interTemporalProblemFillers,
                                                                                                       final ReportNode reportNode) {
        if (currentResult.getCost() < bestResult.getCost()) {
            logBetterResult(iteration, currentResult, reportNode);
            updateLinearProblemBetweenSensiComputations(linearProblem, problemFillers, interTemporalProblemFillers, currentResult);
            return Pair.of(currentResult, false);
        }
        logWorseResult(iteration, bestResult, currentResult, reportNode);
        for (OffsetDateTime timestamp : input.iteratingLinearOptimizerInputs().getTimestamps()) {
            IteratingLinearOptimizer.applyRangeActions(bestResult, input.iteratingLinearOptimizerInputs().getData(timestamp).orElseThrow());
        }
        if (raRangeShrinking) {
            updateLinearProblemBetweenSensiComputations(linearProblem, problemFillers, interTemporalProblemFillers, currentResult);
        }
        return Pair.of(bestResult, !raRangeShrinking);
    }
}
