/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.google.common.annotations.Beta;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.marmot.results.TimeCoupledRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.logCost;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPostOptimizationResults;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runSensitivityAnalysisBasedOnInitialResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@Beta
@AutoService(TimeCoupledRaoProvider.class)
public class Marmot implements TimeCoupledRaoProvider {

    private static final String TIME_COUPLED_RAO = "TimeCoupledRao";
    private static final String MIN_MARGIN_VIOLATION_EVALUATOR = "min-margin-violation-evaluator";

    @Override
    public CompletableFuture<TimeCoupledRaoResult> run(TimeCoupledRaoInput timeCoupledRaoInput, RaoParameters raoParameters) {
        if (!raoParameters.hasExtension(MarmotParameters.class)) {
            BUSINESS_WARNS.warn("Parameters are missing MarmotParameters extension. Default MarmotParameters will be used");
            raoParameters.addExtension(MarmotParameters.class, new MarmotParameters());
        }
        final MarmotParameters marmotParameters = raoParameters.getExtension(MarmotParameters.class);

        // Initiate lazy networks
        TemporalData<Crac> cracs = timeCoupledRaoInput.getRaoInputs().map(RaoInput::getCrac);
        TemporalData<LazyNetwork> initialNetworks = MarmotUtils.cloneNetworks(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getNetwork));
        MarmotUtils.closeAll(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getNetwork));

        TemporalData<RaoInput> initialInputs = MarmotUtils.merge(initialNetworks, cracs);

        // RaoParametes are stored in a TemporalData. They're the same for every timestamp, but this prevents concurrent access when multi-threading is activated
        TemporalData<RaoParameters> raoParametersDuplicates = new TemporalDataImpl<>();
        timeCoupledRaoInput.getTimestampsToRun().forEach(timestamp -> raoParametersDuplicates.put(timestamp, MarmotUtils.cloneParameters(raoParameters)));

        // Configure parallelism for multi-threading computation
        int parallelism = Math.min(marmotParameters.getNumberOfThreads(), timeCoupledRaoInput.getTimestampsToRun().size());
        if (parallelism > 1) {
            TECHNICAL_LOGS.info("[MARMOT] Optimizer set to work on {} threads", parallelism);
        }

        // 1. Compute the initial results for each timestamp as a baseline
        TECHNICAL_LOGS.info("[MARMOT] ----- Running initial sensitivity analyses [start]");
        // WARNING: initial results do not contain range actions set-points nor sensitivity results
        //  -> faster sensitivity computations
        //  -> initial set-points of MIP are computed after the independent RAOs of step 3
        TemporalData<PrePerimeterResult> initialResults = runAllInitialSensitivityAnalyses(initialInputs, raoParametersDuplicates, parallelism);
        TECHNICAL_LOGS.info("[MARMOT] ----- Running initial sensitivity analyses [end]");

        // 2. Evaluate the initial value of the global objective function
        TECHNICAL_LOGS.info("[MARMOT] ----- Evaluating the initial value of the global objective function [start]");
        ObjectiveFunction fullObjectiveFunction = buildGlobalObjectiveFunction(cracs, new GlobalFlowResult(initialResults), raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction);
        TECHNICAL_LOGS.info("[MARMOT] ----- Evaluating the initial value of the global objective function [end]");

        // 3. Run independent RAOs to compute and apply the optimal preventive remedial actions
        TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [start]");
        TemporalData<Set<FlowCnec>> consideredCnecs = new TemporalDataImpl<>(); // used for FastRAO
        final TemporalData<RaoResult> topologicalOptimizationResults = timeCoupledRaoInput.getPreComputedRaoResults() == null
            ? runTopologicalOptimization(initialInputs, consideredCnecs, raoParametersDuplicates, parallelism)
            : applyPreventiveToposFromRaoResults(initialInputs, timeCoupledRaoInput.getPreComputedRaoResults(), consideredCnecs, parallelism);
        TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [end]");

        // TODO : Add time-coupled constraint check if none violated then return
        // boolean noTimeCoupledConstraints = timeCoupledRaoInput.getTimeCoupledConstraints().getGeneratorConstraints().isEmpty();

        // 4. Retrieve post-topological optimization results
        TemporalData<NetworkActionsResult> preventiveTopologicalActions = getPreventiveTopologicalActions(cracs, topologicalOptimizationResults, parallelism);
        // Get the curative actions applied in the individual results to be able to apply them during sensitivity computations
        TemporalData<AppliedRemedialActions> curativeTopologicalActions = MarmotUtils.getAppliedRemedialActionsInCurative(cracs, topologicalOptimizationResults);
        TemporalData<RangeActionSetpointResult> initialSetpointResults = getInitialSetpointResults(cracs, parallelism);
        topologicalOptimizationResults.clear(); // delete RAO results

        // Update initialResults to add RangeActionSetpointResult -> make sure that the initial setpoint field appear
        // for all range actions in the final raoResult
        initialResults = new TemporalDataImpl<>(
            initialResults.getDataPerTimestamp().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> new PrePerimeterSensitivityResultImpl(
                        entry.getValue().getFlowResult(),
                        entry.getValue().getSensitivityResult(),
                        initialSetpointResults.getData(entry.getKey()).orElseThrow(),
                        entry.getValue().getObjectiveFunctionResult()
                    )
                ))
        );

        // TODO: check time-coupled constraints. If one of the following requirements is met, exit:
        //  - no time-coupled constraints provided
        //  - all time-coupled constraints respected by the individual RAO results (this covers the case when the post-topological optimization cost is 0)
        //  if (noTimeCoupledConstraints) {
        //       TECHNICAL_LOGS.info("[MARMOT] No time-coupled constraint provided; no need to re-optimize range actions");
        //       <!-- Log limiting elements and costs -->
        //       return CompletableFuture.completedFuture(new TimeCoupledRaoResultImpl(initialObjectiveFunctionResult, postTopologicalOptimizationResult, topologicalOptimizationResults));
        //  } else if (areTimeCoupledConstraintsRespected(timeCoupledRaoInput, topologicalOptimizationResults)) {
        //       TECHNICAL_LOGS.info("[MARMOT] All time-coupled constraint are respected; no need to re-optimize range actions");
        //       <!-- Log limiting elements and costs -->
        //       return CompletableFuture.completedFuture(new TimeCoupledRaoResultImpl(initialObjectiveFunctionResult, postTopologicalOptimizationResult, topologicalOptimizationResults));
        //  }
        //  TECHNICAL_LOGS.info("[MARMOT] Some time-coupled constraint are not respected; range actions will be re-optimized");

        // 5. Create and iteratively solve MIP to find optimal range actions' set-points

        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start]");

        TemporalData<PrePerimeterResult> sensiResults;
        GlobalLinearOptimizationResult linearOptimizationResults;
        GlobalLinearOptimizationResult fullResults;
        FlowResult initialFlowResult = new GlobalFlowResult(initialResults);
        int counter = 1;
        do {
            // Run post topo sensitivity analysis on all timestamps ON CONSIDERED CNECS ONLY (which is why we do it every loop)
            TECHNICAL_LOGS.info("[MARMOT] Systematic time-coupled sensitivity analysis [start]");
            TemporalData<PrePerimeterResult> postTopoResults = runAllSensitivityAnalysesBasedOnInitialResult(
                initialInputs,
                curativeTopologicalActions,
                initialResults,
                raoParametersDuplicates,
                consideredCnecs,
                parallelism
            );
            TECHNICAL_LOGS.info("[MARMOT] Systematic time-coupled sensitivity analysis [end]");

            // Build objective function with ONLY THE CONSIDERED CNECS
            ObjectiveFunction filteredObjectiveFunction = buildFilteredObjectiveFunction(
                cracs,
                initialFlowResult,
                raoParameters,
                consideredCnecs
            );

            // Create and iteratively solve MIP to find optimal range actions' set-points FOR THE CONSIDERED CNECS
            TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start] for iteration {}", counter);
            linearOptimizationResults = optimizeLinearRemedialActions(
                new TimeCoupledRaoInput(initialInputs, timeCoupledRaoInput.getTimestampsToRun(), timeCoupledRaoInput.getTimeCoupledConstraints()),
                initialResults,
                initialSetpointResults,
                postTopoResults,
                raoParameters,
                preventiveTopologicalActions,
                curativeTopologicalActions,
                consideredCnecs,
                filteredObjectiveFunction,
                parallelism
            );
            MarmotUtils.releaseAllWithoutOverwrite(initialInputs.map(RaoInput::getNetwork));
            TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end] for iteration {}", counter);

            // Compute the flows on ALL the cnecs to check if the worst cnecs have changed and were considered in the MIP or not
            sensiResults = applyActionsAndRunFullSensitivityAnalysis(initialInputs, curativeTopologicalActions, linearOptimizationResults, initialResults, raoParametersDuplicates, parallelism);

            // Create a global result with the flows on ALL cnecs and the actions applied during MIP
            // TODO: does this contain curative setpoints?
            TemporalData<RangeActionActivationResult> rangeActionActivationResultTemporalData = linearOptimizationResults.getRangeActionActivationResultTemporalData();
            fullResults = new GlobalLinearOptimizationResult(
                sensiResults,
                sensiResults.map(PrePerimeterResult::getSensitivityResult),
                rangeActionActivationResultTemporalData,
                preventiveTopologicalActions,
                fullObjectiveFunction,
                linearOptimizationResults.getStatus()
            );

            logCost("[MARMOT] next iteration of MIP: ", fullResults, raoParameters, 10);
            counter++;
        } while (
            shouldContinueAndAddCnecs(sensiResults, consideredCnecs, getFlowUnit(raoParameters), marmotParameters)
                && counter < marmotParameters.getMaxMipIterations()); // Stop if the worst element of each TS has been considered during MIP
        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end]");

        // 7. Merge topological and linear result
        if (fullResults.getStatus() == LinearProblemStatus.INFEASIBLE) {
            TECHNICAL_LOGS.warn("[MARMOT] The global MIP was infeasible, possibly due to time-coupled constraints that are incoherent/inconsistent or that cannot be met. Rolling back to initial situation.");
            logCost("[MARMOT] Unoptimized RAO results: ", initialObjectiveFunctionResult, raoParameters, 10);
            TimeCoupledRaoResultImpl timeCoupledRaoResult = mergeTopologicalAndLinearOptimizationResults(
                initialInputs,
                initialResults,
                initialObjectiveFunctionResult,
                fullResults,
                initialInputs.map(r -> Set.of()),
                initialInputs.map(r -> new AppliedRemedialActions()),
                initialInputs.map(r -> Set.of()),
                raoParameters
            );
            MarmotUtils.closeAll(initialNetworks);
            return CompletableFuture.completedFuture(timeCoupledRaoResult);
        }

        TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");
        TimeCoupledRaoResultImpl timeCoupledRaoResult = mergeTopologicalAndLinearOptimizationResults(
            initialInputs,
            initialResults,
            initialObjectiveFunctionResult,
            fullResults,
            preventiveTopologicalActions.map(NetworkActionsResult::getActivatedNetworkActions),
            curativeTopologicalActions,
            consideredCnecs,
            raoParameters
        );

        // 8. Log initial and final results
        logCost("[MARMOT] Initial results: ", initialObjectiveFunctionResult, raoParameters, 10);
        logCost("[MARMOT] After global linear optimization: ", fullResults, raoParameters, 10);

        MarmotUtils.releaseAllWithoutOverwrite(initialNetworks);
        return CompletableFuture.completedFuture(timeCoupledRaoResult);
    }

    private TemporalData<RangeActionSetpointResult> getInitialSetpointResults(TemporalData<Crac> cracs, int parallelism) {
        return MarmotUtils.smartMap(
            cracs,
            crac -> {
                Map<RangeAction<?>, Double> setPointMap = new HashMap<>();
                // get the initial setpoints of the preventive and curative range actions :
                crac.getRangeActions(crac.getPreventiveState()).forEach(rangeAction ->
                    setPointMap.put(rangeAction, MarmotUtils.getInitialSetPoint(rangeAction))
                );
                crac.getStates().stream().filter(s -> s.getInstant().isCurative())
                        .forEach(state -> crac.getRangeActions(state).forEach(
                                rangeAction -> setPointMap.put(rangeAction,
                                        MarmotUtils.getInitialSetPoint(rangeAction))
                        ));
                return new RangeActionSetpointResultImpl(setPointMap);
            },
            parallelism
        );
    }

    private boolean shouldContinueAndAddCnecs(final TemporalData<PrePerimeterResult> loadFlowResults,
                                              final TemporalData<Set<FlowCnec>> consideredCnecs,
                                              final Unit flowUnit,
                                              final MarmotParameters marmotParameters) {
        final int cnecsToAddPerVirtualCostName = marmotParameters.getNumberOfCnecsToAddPerVirtualCostName();
        final double minRelativeImprovementOnMargin = marmotParameters.getMinRelativeImprovementOnMargin();
        final double marginWindowToConsider = marmotParameters.getMarginWindowToConsider();

        final AtomicBoolean shouldContinue = new AtomicBoolean(false);
        updateShouldContinue(loadFlowResults, consideredCnecs, minRelativeImprovementOnMargin, shouldContinue, flowUnit);

        if (shouldContinue.get()) {
            updateConsideredCnecs(loadFlowResults, consideredCnecs, marginWindowToConsider, cnecsToAddPerVirtualCostName, flowUnit);
        }
        return shouldContinue.get();
    }

    private static void updateShouldContinue(TemporalData<PrePerimeterResult> loadFlowResults,
                                             TemporalData<Set<FlowCnec>> consideredCnecs,
                                             double minRelativeImprovementOnMargin,
                                             AtomicBoolean shouldContinue,
                                             Unit flowUnit) {
        for (OffsetDateTime timestamp : loadFlowResults.getTimestamps()) {
            PrePerimeterResult loadFlowResult = loadFlowResults.getData(timestamp).orElseThrow();
            Set<FlowCnec> previousCnecs = consideredCnecs.getData(timestamp).orElseThrow();

            // for margin violation - need to compare to min improvement on margin
            // ordered list of cnecs with an overload
            List<FlowCnec> worstCnecsForMarginViolation = loadFlowResult.getCostlyElements(
                MIN_MARGIN_VIOLATION_EVALUATOR,
                Integer.MAX_VALUE
            );
            double worstConsideredMargin = worstCnecsForMarginViolation.stream()
                .filter(previousCnecs::contains)
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, flowUnit))
                .orElse(0.);
            double worstMarginOfAll = worstCnecsForMarginViolation.stream()
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, flowUnit))
                .orElse(0.);
            // if worst overload > worst considered overload *( 1 + minImprovementOnLoad)
            if (worstMarginOfAll < worstConsideredMargin * (1 + minRelativeImprovementOnMargin) - 1e-6) {
                shouldContinue.set(true);
            }

            // for other violations - just check if cnec was considered
            loadFlowResult.getVirtualCostNames().stream()
                .filter(vcName -> !vcName.equals(MIN_MARGIN_VIOLATION_EVALUATOR))
                .forEach(vcName -> {
                    Optional<FlowCnec> worstCnec = loadFlowResult.getCostlyElements(vcName, 1).stream().findFirst();
                    if (worstCnec.isPresent() && !previousCnecs.contains(worstCnec.get())) {
                        shouldContinue.set(true);
                    }
                });
        }
    }

    private static void updateConsideredCnecs(TemporalData<PrePerimeterResult> loadFlowResults,
                                              TemporalData<Set<FlowCnec>> consideredCnecs,
                                              double marginWindowToConsider,
                                              int cnecsToAddPerVirtualCostName,
                                              Unit flowUnit) {
        List<LoggingAddedCnecs> addedCnecsForLogging = new ArrayList<>();
        for (OffsetDateTime timestamp : loadFlowResults.getTimestamps()) {
            PrePerimeterResult loadFlowResult = loadFlowResults.getData(timestamp).orElseThrow();
            Set<FlowCnec> previousIterationCnecs = consideredCnecs.getData(timestamp).orElseThrow();
            Set<FlowCnec> nextIterationCnecs = new HashSet<>(previousIterationCnecs);

            double worstConsideredMargin = loadFlowResult.getCostlyElements(MIN_MARGIN_VIOLATION_EVALUATOR, Integer.MAX_VALUE)
                .stream()
                .filter(previousIterationCnecs::contains)
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, flowUnit))
                .orElse(0.);

            for (String vcName : loadFlowResult.getVirtualCostNames()) {
                LoggingAddedCnecs currentLoggingAddedCnecs = new LoggingAddedCnecs(timestamp, vcName, new ArrayList<>(), new HashMap<>());
                int addedCnecsForVcName = 0;

                // for min margin violation take all cnecs
                if (vcName.equals(MIN_MARGIN_VIOLATION_EVALUATOR)) {
                    for (FlowCnec cnec : loadFlowResult.getCostlyElements(vcName, Integer.MAX_VALUE)) {
                        if (loadFlowResult.getMargin(
                            cnec,
                            flowUnit
                        ) > worstConsideredMargin + marginWindowToConsider && addedCnecsForVcName > cnecsToAddPerVirtualCostName) {
                            // stop if out of window and already added enough
                            break;
                        } else if (!previousIterationCnecs.contains(cnec)) {
                            // if in window or not added enough yet, add
                            nextIterationCnecs.add(cnec);
                            addedCnecsForVcName++;
                            currentLoggingAddedCnecs.addCnec(cnec.getId(), loadFlowResult.getMargin(cnec, flowUnit));
                        }
                    }
                } else if (loadFlowResult.getVirtualCost(vcName) > 1e-6) {
                    for (FlowCnec cnec : loadFlowResult.getCostlyElements(vcName, Integer.MAX_VALUE)) {
                        if (!previousIterationCnecs.contains(cnec)) {
                            nextIterationCnecs.add(cnec);
                            currentLoggingAddedCnecs.addCnec(cnec.getId());
                        }
                    }
                }
                addedCnecsForLogging.add(currentLoggingAddedCnecs);
            }
            consideredCnecs.put(timestamp, nextIterationCnecs);
        }
        logCnecs(addedCnecsForLogging);
    }

    private static void logCnecs(List<LoggingAddedCnecs> addedCnecsForLogging) {
        StringBuilder logMessage = new StringBuilder("[MARMOT] Proceeding to next iteration by adding:");
        for (LoggingAddedCnecs loggingAddedCnecs : addedCnecsForLogging) {
            if (!loggingAddedCnecs.addedCnecs().isEmpty()) {
                logMessage.append(" for timestamp ").append(loggingAddedCnecs.offsetDateTime().toString()).append(" and virtual cost ").append(loggingAddedCnecs.vcName()).append(" ");
                for (String cnec : loggingAddedCnecs.addedCnecs()) {
                    String cnecString = loggingAddedCnecs.vcName().equals(MIN_MARGIN_VIOLATION_EVALUATOR) ?
                        cnec + "(" + loggingAddedCnecs.margins().get(cnec) + ")" + "," :
                        cnec + ",";
                    logMessage.append(cnecString);
                }
            }
        }
        TECHNICAL_LOGS.info(logMessage.toString());
    }

    record LoggingAddedCnecs(OffsetDateTime offsetDateTime, String vcName, List<String> addedCnecs,
                             Map<String, Double> margins) {
        private void addCnec(String cnec) {
            addedCnecs.add(cnec);
        }

        private void addCnec(String cnec, double margin) {
            addedCnecs.add(cnec);
            margins.put(cnec, margin);
        }
    }

    private static TemporalData<PrePerimeterResult> applyActionsAndRunFullSensitivityAnalysis(TemporalData<RaoInput> postTopoInputs,
                                                                                              TemporalData<AppliedRemedialActions> curativeTopologicalActions,
                                                                                              LinearOptimizationResult filteredResult,
                                                                                              TemporalData<PrePerimeterResult> initialResults,
                                                                                              TemporalData<RaoParameters> raoParameters, int parallelism) {
        return MarmotUtils.smartMap(
            postTopoInputs,
            raoInput -> {
                OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
                State preventiveState = raoInput.getCrac().getPreventiveState();
                raoInput.getCrac().getRangeActions(preventiveState).forEach(rangeAction ->
                    rangeAction.apply(raoInput.getNetwork(), filteredResult.getOptimizedSetpoint(rangeAction, preventiveState))
                );

                AppliedRemedialActions allCurativeActions = new AppliedRemedialActions();
                AppliedRemedialActions topoCurativeActions = curativeTopologicalActions.getData(timestamp).orElseThrow();
                raoInput.getCrac().getStates().stream()
                        .filter(state -> state.getInstant().isCurative())
                        .forEach(state -> {
                            topoCurativeActions.getAppliedNetworkActions(state)
                                    .forEach(networkAction ->
                                            allCurativeActions.addAppliedNetworkAction(state, networkAction));
                            filteredResult.getActivatedRangeActions(state).forEach(rangeAction ->
                                    allCurativeActions.addAppliedRangeAction(state, rangeAction, filteredResult.getOptimizedSetpoint(rangeAction, state)));
                        });

                PrePerimeterResult sensitivityAnalysisResults = runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(
                        postTopoInputs.getData(timestamp).orElseThrow(),
                        allCurativeActions,
                        initialResults.getData(timestamp).orElseThrow(),
                        raoParameters.getData(timestamp).orElseThrow());
                MarmotUtils.releaseNetwork(raoInput.getNetwork());
                return sensitivityAnalysisResults;
            },
            parallelism
        );
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(TemporalData<RaoInput> raoInputs, TemporalData<Set<FlowCnec>> consideredCnecs, TemporalData<RaoParameters> raoParameters, int parallelism) {
        return MarmotUtils.smartMap(raoInputs, raoInput -> runSingleTopologicalOptimization(raoInput, consideredCnecs, raoParameters.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow()), parallelism);
    }

    /**
     * Runs a CASTOR optimization for a given RAO input and applies the optimal preventive network actions on the input network.
     *
     * @param raoInput        The RAO input for the optimization.
     * @param consideredCnecs The temporal data of flow CNECs to be considered by FastRAO.
     * @param raoParameters   The RAO parameters for the optimization.
     * @return The resulting RaoResult after the optimization.
     */
    private static RaoResult runSingleTopologicalOptimization(RaoInput raoInput, TemporalData<Set<FlowCnec>> consideredCnecs, RaoParameters raoParameters) {
        Set<FlowCnec> cnecs = new HashSet<>();
        OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
        String logMessage = "[MARMOT] >>> Running RAO for timestamp %s [{}]".formatted(timestamp);
        TECHNICAL_LOGS.info(logMessage, "start");
        RaoResult raoResult = FastRao.launchFastRaoOptimization(raoInput, raoParameters, null, cnecs);
        TECHNICAL_LOGS.info(logMessage, "end");
        consideredCnecs.put(timestamp, cnecs);
        State preventiveState = raoInput.getCrac().getPreventiveState();
        NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of(preventiveState, raoResult.getActivatedNetworkActionsDuringState(preventiveState)));
        TECHNICAL_LOGS.info("[MARMOT] >>> Applying preventive remedial actions after optimization for timestamp %s".formatted(timestamp));
        MarmotUtils.applyPreventiveRemedialActions(raoInput, networkActionsResult);
        CriticalCnecsResult criticalCnecsResult = new CriticalCnecsResult();
        Set<String> criticalCnecsIds = cnecs.stream().map(FlowCnec::getId).collect(Collectors.toSet());
        criticalCnecsResult.setCriticalCnecIds(criticalCnecsIds);
        raoResult.addExtension(CriticalCnecsResult.class, criticalCnecsResult);
        return raoResult;
    }

    private static TemporalData<RaoResult> applyPreventiveToposFromRaoResults(TemporalData<RaoInput> raoInputs, TemporalData<RaoResult> raoResults, TemporalData<Set<FlowCnec>> consideredCnecs, int parallelism) {
        return MarmotUtils.smartMap(raoInputs, raoInput -> applyPreventiveTopologicalActions(raoInput, raoResults.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow(), consideredCnecs), parallelism);
    }

    private static RaoResult applyPreventiveTopologicalActions(RaoInput raoInput, RaoResult raoResult, TemporalData<Set<FlowCnec>> consideredCnecs) {
        OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
        State preventiveState = raoInput.getCrac().getPreventiveState();
        NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of(preventiveState, raoResult.getActivatedNetworkActionsDuringState(preventiveState)));
        TECHNICAL_LOGS.info("[MARMOT] >>> Applying preventive remedial actions after optimization for timestamp %s".formatted(timestamp));
        MarmotUtils.applyPreventiveRemedialActions(raoInput, networkActionsResult);
        Set<FlowCnec> cnecs = new HashSet<>();
        CriticalCnecsResult criticalCnecsResult = raoResult.getExtension(CriticalCnecsResult.class);
        if (criticalCnecsResult != null) {
            criticalCnecsResult.getCriticalCnecIds().forEach(
                id -> cnecs.add(raoInput.getCrac().getFlowCnec(id))
            );
        } else {
            cnecs.addAll(raoInput.getCrac().getFlowCnecs());
        }
        consideredCnecs.put(timestamp, cnecs);
        return raoResult;
    }

    private static TemporalData<PrePerimeterResult> runAllInitialSensitivityAnalyses(TemporalData<RaoInput> raoInputs,
                                                                                     TemporalData<RaoParameters> raoParameters,
                                                                                     int parallelism) {
        return MarmotUtils.smartMap(
            raoInputs,
            raoInput -> {
                OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
                PrePerimeterResult sensitivityAnalysisResult = MarmotUtils.runInitialSensitivityAnalysis(
                    raoInput,
                    raoParameters.getData(timestamp).orElseThrow()
                );
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                return sensitivityAnalysisResult;
            },
            parallelism
        );
    }

    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalysesBasedOnInitialResult(TemporalData<RaoInput> raoInputs,
                                                                                                  TemporalData<AppliedRemedialActions> curativeTopologicalActions,
                                                                                                  TemporalData<? extends FlowResult> initialFlowResults,
                                                                                                  TemporalData<RaoParameters> raoParameters,
                                                                                                  TemporalData<Set<FlowCnec>> consideredCnecs, int parallelism) {
        return MarmotUtils.smartMap(
            raoInputs,
            raoInput -> {
                OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
                PrePerimeterResult sensitivityAnalysisResult = runSensitivityAnalysisBasedOnInitialResult(
                    raoInput,
                    curativeTopologicalActions.getData(timestamp).orElseThrow(),
                    initialFlowResults.getData(timestamp).orElseThrow(),
                    raoParameters.getData(timestamp).orElseThrow(),
                    consideredCnecs.getData(timestamp).orElseThrow()
                );
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                return sensitivityAnalysisResult;
            },
            parallelism
        );
    }

    private static TemporalData<NetworkActionsResult> getPreventiveTopologicalActions(TemporalData<Crac> cracs, TemporalData<RaoResult> raoResults, int parallelism) {
        return MarmotUtils.smartMap(
            cracs,
            crac -> {
                OffsetDateTime timestamp = crac.getTimestamp().orElseThrow();
                return new NetworkActionsResultImpl(
                    Map.of(
                        crac.getPreventiveState(),
                        raoResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(crac.getPreventiveState())
                    ));
            },
            parallelism
        );
    }

    private static GlobalLinearOptimizationResult optimizeLinearRemedialActions(TimeCoupledRaoInput raoInput,
                                                                                TemporalData<PrePerimeterResult> initialResults,
                                                                                TemporalData<RangeActionSetpointResult> initialSetpoints,
                                                                                TemporalData<PrePerimeterResult> postTopologicalActionsResults,
                                                                                RaoParameters parameters,
                                                                                TemporalData<NetworkActionsResult> preventiveTopologicalActions,
                                                                                TemporalData<AppliedRemedialActions> curativeTopologicalActions,
                                                                                TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                                ObjectiveFunction objectiveFunction,
                                                                                int parallelism) {

        // -- Build IteratingLinearOptimizertimeCoupledInput
        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInput::getCrac), consideredCnecs, parallelism);
        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global

        TemporalData<IteratingLinearOptimizerInput> linearOptimizerInputs = MarmotUtils.smartMap(
            raoInput.getRaoInputs(),
            individualRaoInput -> {
                OffsetDateTime timestamp = MarmotUtils.getTimestamp(individualRaoInput);
                IteratingLinearOptimizerInput iteratingLinearOptimizerInput = IteratingLinearOptimizerInput.create()
                    .withNetwork(new LazyNetwork(individualRaoInput.getNetwork()))
                    .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow()
                        .copyWithFilteredAvailableHvdcRangeAction(individualRaoInput.getNetwork()))
                    .withInitialFlowResult(initialResults.getData(timestamp).orElseThrow())
                    .withPrePerimeterFlowResult(initialResults.getData(timestamp).orElseThrow())
                    .withPreOptimizationFlowResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
                    .withPrePerimeterSetpoints(initialSetpoints.getData(timestamp).orElseThrow())
                    .withPreOptimizationSensitivityResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
                    .withPreOptimizationAppliedRemedialActions(curativeTopologicalActions.getData(timestamp).orElseThrow())
                    .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
                    .withOutageInstant(individualRaoInput.getCrac().getOutageInstant())
                    .withAppliedNetworkActionsInPrimaryState(preventiveTopologicalActions.getData(timestamp).orElseThrow())
                    .build();
                MarmotUtils.releaseNetworkWithoutOverwrite(individualRaoInput.getNetwork());
                MarmotUtils.releaseNetworkWithoutOverwrite(iteratingLinearOptimizerInput.network());
                return iteratingLinearOptimizerInput;
            },
            parallelism
        );

        TimeCoupledIteratingLinearOptimizerInput timeCoupledLinearOptimizerInput = new TimeCoupledIteratingLinearOptimizerInput(
            linearOptimizerInputs, objectiveFunction, raoInput.getTimeCoupledConstraints());

        // TODO : a priori ce release all ne devrait pas être utile MAIS il semblerait qu'il y ait des réseaux pas fermés en arrivant ici,
        // à investiguer
        MarmotUtils.releaseAllWithoutOverwrite(raoInput.getRaoInputs().map(RaoInput::getNetwork));
        MarmotUtils.releaseAllWithoutOverwrite(timeCoupledLinearOptimizerInput.iteratingLinearOptimizerInputs().map(IteratingLinearOptimizerInput::network));

        // Build parameters
        // Unoptimized cnec parameters ignored because only PRAs
        // TODO: define static method to define Ra Limitation Parameters from crac and topos (mutualize with search tree) : SearchTreeParameters::decreaseRemedialActionsUsageLimits
        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder linearOptimizerParametersBuilder = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(parameters.getObjectiveFunctionParameters().getType())
            .withFlowUnit(getFlowUnit(parameters))
            .withRangeActionParameters(parameters.getRangeActionsOptimizationParameters())
            .withRangeActionParametersExtension(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters())
            .withMaxNumberOfIterations(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(ENABLED.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking())
                || ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()))
            .withSolverParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxMinRelativeMarginParameters(parameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class))
            .withRaLimitationParameters(new RangeActionLimitationParameters())
            .withMinMarginParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().orElse(new SearchTreeRaoCostlyMinMarginParameters()));
        parameters.getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParametersExtension);
        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParametersExtension);
        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();

        return TimeCoupledIteratingLinearOptimizer.optimize(timeCoupledLinearOptimizerInput, linearOptimizerParameters, parallelism);
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs, TemporalData<Set<FlowCnec>> consideredCnecs, int parallelism) {
        return MarmotUtils.smartMap(
            cracs,
            crac -> {
                OffsetDateTime timestamp = crac.getTimestamp().orElseThrow();
                Map<State, Set<RangeAction<?>>> availableRangeActions = new HashMap<>();
                // get the preventive range actions
                State preventiveState = crac.getPreventiveState();
                Set<RangeAction<?>> preventiveRangeActions = new HashSet<>(crac.getRangeActions(preventiveState));
                if (!preventiveRangeActions.isEmpty()) {
                    availableRangeActions.put(preventiveState, preventiveRangeActions);
                }
                // get the curative range actions for all the post contingency states
                crac.getStates().stream()
                        .filter(state -> state.getInstant().isCurative())
                        .forEach(state -> {
                            Set<RangeAction<?>> curativeRangeActions = new HashSet<>(crac.getRangeActions(state));
                            if (!curativeRangeActions.isEmpty()) {
                                availableRangeActions.put(state, curativeRangeActions);
                            }
                        });

                return new GlobalOptimizationPerimeter(
                        preventiveState,
                        consideredCnecs.getData(timestamp).orElseThrow(),
                        new HashSet<>(), // no loopflows for now
                        new HashSet<>(), // don't re-optimize topological actions in Marmot
                        availableRangeActions
                );
            },
            parallelism
        );
    }

    private static TimeCoupledRaoResultImpl mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs,
                                                                                         TemporalData<PrePerimeterResult> initialResults,
                                                                                         ObjectiveFunctionResult initialLinearOptimizationResult,
                                                                                         GlobalLinearOptimizationResult globalLinearOptimizationResult,
                                                                                         TemporalData<Set<NetworkAction>> preventiveNetworkActions,
                                                                                         TemporalData<AppliedRemedialActions> curativeTopologicalActions,
                                                                                         TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                                         RaoParameters raoParameters) {
        return new TimeCoupledRaoResultImpl(
            initialLinearOptimizationResult,
            globalLinearOptimizationResult,
            getPostOptimizationResults(
                raoInputs,
                initialResults,
                globalLinearOptimizationResult,
                preventiveNetworkActions,
                curativeTopologicalActions,
                consideredCnecs,
                raoParameters));
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(Crac::getFlowCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>();
        cracs.map(Crac::getStates).getDataPerTimestamp().values().forEach(allOptimizedStates::addAll);
        return ObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private static ObjectiveFunction buildFilteredObjectiveFunction(TemporalData<Crac> cracs,
                                                                    FlowResult globalInitialFlowResult,
                                                                    RaoParameters raoParameters,
                                                                    TemporalData<Set<FlowCnec>> consideredCnecs) {
        Set<FlowCnec> flatConsideredCnecs = new HashSet<>();
        consideredCnecs.getDataPerTimestamp().values().forEach(flatConsideredCnecs::addAll);

        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(
            flatConsideredCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private LinearOptimizationResult getInitialObjectiveFunctionResult(TemporalData<PrePerimeterResult> prePerimeterResults, ObjectiveFunction objectiveFunction) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = prePerimeterResults.map(RangeActionActivationResultImpl::new);
        TemporalData<NetworkActionsResult> networkActionsResults = new TemporalDataImpl<>();
        return new GlobalLinearOptimizationResult(
            prePerimeterResults.map(PrePerimeterResult::getFlowResult),
            prePerimeterResults.map(PrePerimeterResult::getSensitivityResult),
            rangeActionActivationResults,
            networkActionsResults,
            objectiveFunction,
            LinearProblemStatus.OPTIMAL
        );
    }

    @Override
    public String getName() {
        return TIME_COUPLED_RAO;
    }
}
