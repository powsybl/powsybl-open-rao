/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

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
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.ProblemFillerHelper;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.PowerGradientConstraintFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.LinearProblemResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getPstModel;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class InterTemporalIteratingLinearOptimizer {

    private InterTemporalIteratingLinearOptimizer() {
    }

    public static InterTemporalIteratingLinearOptimizationResult optimize(InterTemporalIteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {

        // 1. Initialize best result using input data

        // TODO: keep using temporal data
        ObjectiveFunction objectiveFunction = input.iteratingLinearOptimizerInputs().getDataPerTimestamp().values().iterator().next().objectiveFunction();

        InterTemporalIteratingLinearOptimizationResult bestResult = createInitialResult(
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterFlowResult),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::preOptimizationSensitivityResult),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterSetpoints).map(RangeActionActivationResultImpl::new),
            objectiveFunction
        );
        InterTemporalIteratingLinearOptimizationResult previousResult = bestResult;

        TemporalData<SensitivityComputer> sensitivityComputers = new TemporalDataImpl<>();

        // 2. Initialize linear problem using input data

        TemporalData<List<ProblemFiller>> problemFillers = getProblemFillersPerTimestamp(input, parameters);
        List<ProblemFiller> interTemporalProblemFillers = getInterTemporalProblemFillers(input);
        LinearProblem linearProblem = buildLinearProblem(problemFillers, interTemporalProblemFillers, parameters);
        fillLinearProblem(
            linearProblem,
            problemFillers,
            interTemporalProblemFillers,
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::initialFlowResult),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::preOptimizationSensitivityResult),
            input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterSetpoints));

        // 3. Iterate

        for (int iteration = 1; iteration <= parameters.getMaxNumberOfIterations(); iteration++) {

            // a. Solve linear problem

            LinearProblemStatus solveStatus = solveLinearProblem(linearProblem, iteration);
            bestResult.setNbOfIteration(iteration);

            // b. Check linear problem status and return best result if not FEASIBLE not OPTIMAL

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

            // c. [PARALLEL] Get and round range action activation results from solver results

            TemporalData<RangeActionActivationResult> rangeActionActivationPerTimestamp = retrieveRangeActionActivationResults(linearProblem, input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterSetpoints), input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::optimizationPerimeter));
            Map<OffsetDateTime, RangeActionActivationResult> roundedResults = new HashMap<>();

            for (OffsetDateTime timestamp : rangeActionActivationPerTimestamp.getTimestamps()) {
                roundedResults.put(timestamp, roundResult(rangeActionActivationPerTimestamp.getData(timestamp).orElseThrow(), bestResult.getResultPerTimestamp().getData(timestamp).orElseThrow(), input.iteratingLinearOptimizerInputs().getData(timestamp).orElseThrow(), parameters));
            }

            rangeActionActivationPerTimestamp = new TemporalDataImpl<>(roundedResults);
            // TODO: do we still want to do this in this MIP?
            rangeActionActivationPerTimestamp = resolveIfApproximatedPstTaps(bestResult, linearProblem, iteration, rangeActionActivationPerTimestamp, input, parameters, problemFillers);

            // d. [PARALLEL?] Check if set-points have changed; if no, return the best result

            if (!hasAnyRangeActionChanged(
                input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::optimizationPerimeter),
                previousResult.getResultPerTimestamp().map(LinearOptimizationResult::getRangeActionActivationResult),
                rangeActionActivationPerTimestamp)) {
                TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration);
                return bestResult;
            }

            // e. [PARALLEL] Run sensitivity analyses with new set-points

            Map<OffsetDateTime, SensitivityComputer> newSensitivityComputers = new HashMap<>();
            for (OffsetDateTime timestamp : rangeActionActivationPerTimestamp.getTimestamps()) {
                newSensitivityComputers.put(timestamp, runSensitivityAnalysis(sensitivityComputers.getData(timestamp).orElse(null), iteration, rangeActionActivationPerTimestamp.getData(timestamp).orElseThrow(), input.iteratingLinearOptimizerInputs().getData(timestamp).orElseThrow(), parameters));
            }

            if (newSensitivityComputers.values().stream().anyMatch(sensitivityComputer -> sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE)) {
                bestResult.setStatus(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED);
                return bestResult;
            }

            sensitivityComputers = new TemporalDataImpl<>(newSensitivityComputers);

            InterTemporalIteratingLinearOptimizationResult newResult = createResultFromData(
                sensitivityComputers,
                input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::network),
                rangeActionActivationPerTimestamp,
                iteration,
                objectiveFunction
            );
            previousResult = newResult;

            // f. [PARALLEL] Update problem fillers with flows, sensitivity coefficients and set-points

            Pair<InterTemporalIteratingLinearOptimizationResult, Boolean> mipShouldStop = updateBestResultAndCheckStopCondition(parameters.getRaRangeShrinking(), linearProblem, input, iteration, newResult, bestResult, problemFillers);
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
        // TODO: parallel
        Map<OffsetDateTime, List<ProblemFiller>> problemFillers = new HashMap<>();
        input.iteratingLinearOptimizerInputs().getDataPerTimestamp().forEach((timestamp, linearOptimizerInput) -> problemFillers.put(timestamp, ProblemFillerHelper.getProblemFillers(linearOptimizerInput, parameters, timestamp)));
        return new TemporalDataImpl<>(problemFillers);
    }

    private static List<ProblemFiller> getInterTemporalProblemFillers(InterTemporalIteratingLinearOptimizerInput input) {
        // TODO: add inter-temporal margin filler (min of all min margins)
        TemporalData<State> preventiveStates = input.iteratingLinearOptimizerInputs().map(linearOptimizerInput -> linearOptimizerInput.optimizationPerimeter().getMainOptimizationState());
        TemporalData<Network> networks = input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::network);
        TemporalData<Set<InjectionRangeAction>> preventiveInjectionRangeActions = input.iteratingLinearOptimizerInputs().map(linearOptimizerInput -> filterPreventiveInjectionRangeAction(linearOptimizerInput.optimizationPerimeter().getRangeActions()));
        return List.of(new PowerGradientConstraintFiller(preventiveStates, networks, preventiveInjectionRangeActions, input.powerGradients()));
    }

    private static Set<InjectionRangeAction> filterPreventiveInjectionRangeAction(Set<RangeAction<?>> rangeActions) {
        return rangeActions.stream().filter(InjectionRangeAction.class::isInstance).map(InjectionRangeAction.class::cast).collect(Collectors.toSet());
    }

    private static LinearProblem buildLinearProblem(TemporalData<List<ProblemFiller>> problemFillers, List<ProblemFiller> interTemporalProblemFillers, IteratingLinearOptimizerParameters parameters) {
        LinearProblemBuilder linearProblemBuilder = LinearProblem.create()
            .withSolver(parameters.getSolverParameters().getSolver())
            .withRelativeMipGap(parameters.getSolverParameters().getRelativeMipGap())
            .withSolverSpecificParameters(parameters.getSolverParameters().getSolverSpecificParameters())
            // TODO: check if we can simply ignore this line
            .withInitialRangeActionActivationResult(new RangeActionActivationResultImpl(new RangeActionSetpointResultImpl(Map.of())));

        // add problem fillers for each timestamp and inter-temporal timestamps
        problemFillers.getDataPerTimestamp().values().forEach(problemFillerOfTimestamp -> problemFillerOfTimestamp.forEach(linearProblemBuilder::withProblemFiller));
        interTemporalProblemFillers.forEach(linearProblemBuilder::withProblemFiller);

        return linearProblemBuilder.build();
    }

    private static void fillLinearProblem(LinearProblem linearProblem, TemporalData<List<ProblemFiller>> problemFillers, List<ProblemFiller> interTemporalProblemFillers, TemporalData<FlowResult> initialFlowResults, TemporalData<SensitivityResult> initialSensitivityResults, TemporalData<RangeActionSetpointResult> initialSetPoints) {
        List<OffsetDateTime> timestamps = problemFillers.getTimestamps();
        timestamps.forEach(timestamp -> {
            List<ProblemFiller> problemFillersForTimestamp = problemFillers.getData(timestamp).orElseThrow();
            problemFillersForTimestamp.forEach(problemFiller -> problemFiller.fill(linearProblem, initialFlowResults.getData(timestamp).orElseThrow(), initialSensitivityResults.getData(timestamp).orElseThrow(), new RangeActionActivationResultImpl(initialSetPoints.getData(timestamp).orElseThrow())));
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

    private static void updateLinearProblemBetweenSensiComputations(LinearProblem linearProblem, TemporalData<List<ProblemFiller>> problemFillers, InterTemporalIteratingLinearOptimizationResult optimizationResult) {
        List<OffsetDateTime> timestamps = problemFillers.getTimestamps();
        timestamps.forEach(timestamp -> {
            List<ProblemFiller> problemFillersForTimestamp = problemFillers.getData(timestamp).orElseThrow();
            LinearOptimizationResult linearOptimizationResult = optimizationResult.getResultPerTimestamp().getData(timestamp).orElseThrow();
            problemFillersForTimestamp.forEach(problemFiller -> problemFiller.fill(linearProblem, linearOptimizationResult, linearOptimizationResult, linearOptimizationResult));
        });
    }

    private static LinearProblemStatus solveLinearProblem(LinearProblem linearProblem, int iteration) {
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [start]", iteration);
        LinearProblemStatus status = linearProblem.solve();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [end]", iteration);
        return status;
    }

    // Sensitivity analysis

    private static SensitivityComputer runSensitivityAnalysis(SensitivityComputer sensitivityComputer, int iteration, RangeActionActivationResult currentRangeActionActivationResult, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        SensitivityComputer tmpSensitivityComputer = sensitivityComputer;
        // TODO: should always be global
        if (input.optimizationPerimeter() instanceof GlobalOptimizationPerimeter) {
            AppliedRemedialActions appliedRemedialActionsInSecondaryStates = applyRangeActions(currentRangeActionActivationResult, input);
            tmpSensitivityComputer = createSensitivityComputer(appliedRemedialActionsInSecondaryStates, input, parameters);
        } else {
            applyRangeActions(currentRangeActionActivationResult, input);
            if (tmpSensitivityComputer == null) { // first iteration, do not need to be updated afterwards
                tmpSensitivityComputer = createSensitivityComputer(input.preOptimizationAppliedRemedialActions(), input, parameters);
            }
        }
        runSensitivityAnalysis(tmpSensitivityComputer, input.network(), iteration);
        return tmpSensitivityComputer;
    }

    private static SensitivityComputer createSensitivityComputer(AppliedRemedialActions appliedRemedialActions, IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {

        SensitivityComputer.SensitivityComputerBuilder builder = SensitivityComputer.create()
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

    private static void runSensitivityAnalysis(SensitivityComputer sensitivityComputer, Network network, int iteration) {
        sensitivityComputer.compute(network);
        if (sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_WARNS.warn("Systematic sensitivity computation failed at iteration {}", iteration);
        }
    }

    // Result management

    private static InterTemporalIteratingLinearOptimizationResult createInitialResult(TemporalData<FlowResult> flowResults, TemporalData<SensitivityResult> sensitivityResults, TemporalData<RangeActionActivationResult> rangeActionActivations, ObjectiveFunction objectiveFunction) {
        return new InterTemporalIteratingLinearOptimizationResult(LinearProblemStatus.OPTIMAL, 0, flowResults, sensitivityResults, rangeActionActivations, objectiveFunction);
    }

    private static InterTemporalIteratingLinearOptimizationResult createResultFromData(TemporalData<SensitivityComputer> sensitivityComputers, TemporalData<Network> networks, TemporalData<RangeActionActivationResult> rangeActionActivation, int nbOfIterations, ObjectiveFunction objectiveFunction) {
        Map<OffsetDateTime, FlowResult> flowResults = new HashMap<>();
        for (OffsetDateTime timestamp : sensitivityComputers.getTimestamps()) {
            FlowResult flowResult = sensitivityComputers.getData(timestamp).orElseThrow().getBranchResult(networks.getData(timestamp).orElseThrow());
            flowResults.put(timestamp, flowResult);
        }
        return new InterTemporalIteratingLinearOptimizationResult(LinearProblemStatus.OPTIMAL, nbOfIterations, new TemporalDataImpl<>(flowResults), sensitivityComputers.map(SensitivityComputer::getSensitivityResult), rangeActionActivation, objectiveFunction);
    }

    private static double computeTotalCost(InterTemporalIteratingLinearOptimizationResult result) {
        return result.getResultPerTimestamp().map(LinearOptimizationResult::getCost).getDataPerTimestamp().values().stream().mapToDouble(v -> v).sum();
    }

    private static double computeTotalFunctionalCost(InterTemporalIteratingLinearOptimizationResult result) {
        return result.getResultPerTimestamp().map(LinearOptimizationResult::getFunctionalCost).getDataPerTimestamp().values().stream().mapToDouble(v -> v).sum();
    }

    // Set-point rounding

    private static TemporalData<RangeActionActivationResult> resolveIfApproximatedPstTaps(InterTemporalIteratingLinearOptimizationResult bestResult, LinearProblem linearProblem, int iteration, TemporalData<RangeActionActivationResult> currentRangeActionActivationResults, InterTemporalIteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters, TemporalData<List<ProblemFiller>> problemFillers) {
        LinearProblemStatus solveStatus;
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = currentRangeActionActivationResults;
        if (getPstModel(parameters.getRangeActionParametersExtension()).equals(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS)) {

            // if the PST approximation is APPROXIMATED_INTEGERS, we re-solve the optimization problem
            // but first, we update it, with an adjustment of the PSTs angleToTap conversion factors, to
            // be more accurate in the neighboring of the previous solution

            // (idea: if too long, we could relax the first MIP, but no so straightforward to do with or-tools)
            updateLinearProblemBetweenMipIterations(linearProblem, problemFillers, rangeActionActivationResults);

            solveStatus = solveLinearProblem(linearProblem, iteration);
            if (solveStatus == LinearProblemStatus.OPTIMAL || solveStatus == LinearProblemStatus.FEASIBLE) {
                TemporalData<RangeActionActivationResult> updatedLinearProblemResults = retrieveRangeActionActivationResults(linearProblem, input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::prePerimeterSetpoints), input.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::optimizationPerimeter));
                Map<OffsetDateTime, RangeActionActivationResult> roundedResults = new HashMap<>();
                updatedLinearProblemResults.getDataPerTimestamp().forEach((timestamp, rangeActionActivationResult) -> roundedResults.put(timestamp, roundResult(rangeActionActivationResult, bestResult.getResultPerTimestamp().getData(timestamp).orElseThrow(), input.iteratingLinearOptimizerInputs().getData(timestamp).orElseThrow(), parameters)));
                rangeActionActivationResults = new TemporalDataImpl<>(roundedResults);
            }
        }
        return rangeActionActivationResults;
    }

    // Logging

    private static void logBetterResult(int iteration, InterTemporalIteratingLinearOptimizationResult result) {
        TECHNICAL_LOGS.info(
            "Iteration {}: better solution found with a cost of {} (functional: {})",
            iteration,
            formatDouble(computeTotalCost(result)),
            formatDouble(computeTotalFunctionalCost(result)));
    }

    private static void logWorseResult(int iteration, InterTemporalIteratingLinearOptimizationResult bestResult, InterTemporalIteratingLinearOptimizationResult currentResult) {
        TECHNICAL_LOGS.info(
            "Iteration {}: linear optimization found a worse result than best iteration, with a cost increasing from {} to {} (functional: from {} to {})",
            iteration,
            formatDouble(computeTotalCost(bestResult)),
            formatDouble(computeTotalCost(currentResult)),
            formatDouble(computeTotalFunctionalCost(bestResult)),
            formatDouble(computeTotalFunctionalCost(currentResult)));
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ENGLISH, "%.2f", value);
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

    // Range action activation

    private static TemporalData<RangeActionActivationResult> retrieveRangeActionActivationResults(LinearProblem linearProblem, TemporalData<RangeActionSetpointResult> prePerimeterSetPoints, TemporalData<OptimizationPerimeter> optimizationPerimeters) {
        Map<OffsetDateTime, RangeActionActivationResult> linearOptimizationResults = new HashMap<>();
        List<OffsetDateTime> timestamps = optimizationPerimeters.getTimestamps();
        timestamps.forEach(timestamp -> linearOptimizationResults.put(timestamp, new LinearProblemResult(linearProblem, prePerimeterSetPoints.getData(timestamp).orElseThrow(), optimizationPerimeters.getData(timestamp).orElseThrow())));
        return new TemporalDataImpl<>(linearOptimizationResults);
    }

    private static AppliedRemedialActions applyRangeActions(RangeActionActivationResult rangeActionActivationResult, IteratingLinearOptimizerInput input) {

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

    // Stop criterion

    private static boolean hasAnyRangeActionChanged(TemporalData<OptimizationPerimeter> optimizationPerimeters, TemporalData<RangeActionActivationResult> previousSetPoints, TemporalData<RangeActionActivationResult> newSetPoints) {
        for (OffsetDateTime timestamp : optimizationPerimeters.getTimestamps()) {
            OptimizationPerimeter optimizationPerimeter = optimizationPerimeters.getData(timestamp).orElseThrow();
            RangeActionActivationResult previousSetPointsAtTimestamp = previousSetPoints.getData(timestamp).orElseThrow();
            RangeActionActivationResult newSetPointsAtTimestamp = newSetPoints.getData(timestamp).orElseThrow();
            for (Map.Entry<State, Set<RangeAction<?>>> activatedRangeActionAtState : optimizationPerimeter.getRangeActionsPerState().entrySet()) {
                State state = activatedRangeActionAtState.getKey();
                for (RangeAction<?> rangeAction : activatedRangeActionAtState.getValue()) {
                    if (Math.abs(newSetPointsAtTimestamp.getOptimizedSetpoint(rangeAction, state) - previousSetPointsAtTimestamp.getOptimizedSetpoint(rangeAction, state)) >= 1e-6) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Pair<InterTemporalIteratingLinearOptimizationResult, Boolean> updateBestResultAndCheckStopCondition(boolean raRangeShrinking, LinearProblem linearProblem, InterTemporalIteratingLinearOptimizerInput input, int iteration, InterTemporalIteratingLinearOptimizationResult currentResult, InterTemporalIteratingLinearOptimizationResult bestResult, TemporalData<List<ProblemFiller>> problemFillers) {
        if (computeTotalCost(currentResult) < computeTotalCost(bestResult)) {
            logBetterResult(iteration, currentResult);
            updateLinearProblemBetweenSensiComputations(linearProblem, problemFillers, currentResult);
            return Pair.of(currentResult, false);
        }
        logWorseResult(iteration, bestResult, currentResult);
        for (OffsetDateTime timestamp : bestResult.getResultPerTimestamp().getTimestamps()) {
            applyRangeActions(bestResult.getResultPerTimestamp().getData(timestamp).orElseThrow(), input.iteratingLinearOptimizerInputs().getData(timestamp).orElseThrow());
        }
        if (raRangeShrinking) {
            updateLinearProblemBetweenSensiComputations(linearProblem, problemFillers, currentResult);
        }
        return Pair.of(bestResult, !raRangeShrinking);
    }
}
