/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.google.common.annotations.Beta;
import com.powsybl.commons.report.ReportNode;
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
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.marmot.results.TimeCoupledRaoResultImpl;
import com.powsybl.openrao.searchtreerao.reports.MarmotReports;
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

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO;
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
    // TODO the RAO implementation to use should be a parameter
    private static final String SINGLE_TS_RAO_IMPLEMENTATION = "FastRao";

    @Override
    public CompletableFuture<TimeCoupledRaoResult> run(final TimeCoupledRaoInput timeCoupledRaoInput,
                                                       final RaoParameters raoParameters,
                                                       final ReportNode reportNode) {
        if (!raoParameters.hasExtension(MarmotParameters.class)) {
            MarmotReports.reportMissingMarmotParametersExtension(reportNode);
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
        timeCoupledRaoInput.getTimestampsToRun().forEach(timestamp -> raoParametersDuplicates.put(timestamp, MarmotUtils.cloneParameters(raoParameters, reportNode)));

        // Configure parallelism for multi-threading computation
        int parallelism = Math.min(marmotParameters.getNumberOfThreads(), timeCoupledRaoInput.getTimestampsToRun().size());
        if (parallelism > 1) {
            MarmotReports.reportMarmotOptimizerSetToWorkOnNThreads(reportNode, parallelism);
        }

        // 1. Compute the initial results for each timestamp as a baseline
        final ReportNode initialSensiReportNode = MarmotReports.reportMarmotRunningInitialSensiAnalyses(reportNode);
        // WARNING: initial results do not contain range actions set-points nor sensitivity results
        //  -> faster sensitivity computations
        //  -> initial set-points of MIP are computed after the independent RAOs of step 3
        TemporalData<PrePerimeterResult> initialResults = runAllInitialSensitivityAnalyses(initialInputs, raoParametersDuplicates, parallelism, initialSensiReportNode);
        MarmotReports.reportMarmotRunningInitialSensiAnalysesEnd();

        // 2. Evaluate the initial value of the global objective function
        final ReportNode globalObjFuncInitialValueEvalReportNode = MarmotReports.reportMarmotEvaluatingInitialValueOfGlobalObjFunction(reportNode);
        ObjectiveFunction fullObjectiveFunction = buildGlobalObjectiveFunction(cracs, new GlobalFlowResult(initialResults), raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction, globalObjFuncInitialValueEvalReportNode);
        MarmotReports.reportMarmotEvaluatingInitialValueOfGlobalObjFunctionEnd();

        // 3. Run independent RAOs to compute and apply the optimal preventive remedial actions
        final ReportNode topologicalOptimizationReportNode = MarmotReports.reportMarmotTopologicalOptimization(reportNode);
        TemporalData<Set<FlowCnec>> consideredCnecs = new TemporalDataImpl<>(); // used for FastRAO
        final TemporalData<RaoResult> topologicalOptimizationResults = timeCoupledRaoInput.getPreComputedRaoResults() == null
            ? runTopologicalOptimization(initialInputs, consideredCnecs, raoParametersDuplicates, parallelism, topologicalOptimizationReportNode)
            : applyPreventiveToposFromRaoResults(initialInputs, timeCoupledRaoInput.getPreComputedRaoResults(), consideredCnecs, parallelism, topologicalOptimizationReportNode);
        MarmotReports.reportMarmotTopologicalOptimizationEnd();

        // TODO : Add time-coupled constraint check if none violated then return
        // boolean noTimeCoupledConstraints = timeCoupledRaoInput.getTimeCoupledConstraints().getGeneratorConstraints().isEmpty();

        // 4. Retrieve post-topological optimization results
        TemporalData<NetworkActionsResult> preventiveTopologicalActions = getPreventiveTopologicalActions(cracs, topologicalOptimizationResults, parallelism);
        // Get the curative actions applied in the individual results to be able to apply them during sensitivity computations
        TemporalData<AppliedRemedialActions> curativeRemedialActions = MarmotUtils.getAppliedRemedialActionsInCurative(cracs, topologicalOptimizationResults);
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
        //       MarmotReports.reportMarmotNoTimeCoupledConstraintProvided(reportNode);
        //       <!-- Log limiting elements and costs -->
        //       return CompletableFuture.completedFuture(new TimeCoupledRaoResultImpl(initialObjectiveFunctionResult, postTopologicalOptimizationResult, topologicalOptimizationResults));
        //  } else if (areTimeCoupledConstraintsRespected(timeCoupledRaoInput, topologicalOptimizationResults)) {
        //       TECHNICAL_LOGS.info("[MARMOT] All time-coupled constraint are respected; no need to re-optimize range actions");
        //       <!-- Log limiting elements and costs -->
        //       return CompletableFuture.completedFuture(new TimeCoupledRaoResultImpl(initialObjectiveFunctionResult, postTopologicalOptimizationResult, topologicalOptimizationResults));
        //  }
        //  TECHNICAL_LOGS.info("[MARMOT] Some time-coupled constraint are not respected; range actions will be re-optimized");

        // 5. Create and iteratively solve MIP to find optimal range actions' set-points

        final ReportNode globalRangeActionsOptimizationReportNode = MarmotReports.reportMarmotGlobalRangeActionsOptimization(reportNode);

        TemporalData<PrePerimeterResult> sensiResults;
        GlobalLinearOptimizationResult linearOptimizationResults;
        GlobalLinearOptimizationResult fullResults;
        FlowResult initialFlowResult = new GlobalFlowResult(initialResults);
        int counter = 1;
        do {
            // Run post topo sensitivity analysis on all timestamps ON CONSIDERED CNECS ONLY (which is why we do it every loop)
            final ReportNode systematicTimeCoupledSensiAnalysisReportNode = MarmotReports.reportMarmotSystematicTimeCoupledSensitivityAnalysis(globalRangeActionsOptimizationReportNode);
            TemporalData<PrePerimeterResult> postTopoResults = runAllSensitivityAnalysesBasedOnInitialResult(
                initialInputs,
                curativeRemedialActions,
                initialResults,
                raoParametersDuplicates,
                consideredCnecs,
                parallelism,
                systematicTimeCoupledSensiAnalysisReportNode
            );
            MarmotReports.reportMarmotSystematicTimeCoupledSensitivityAnalysisEnd();

            // Build objective function with ONLY THE CONSIDERED CNECS
            ObjectiveFunction filteredObjectiveFunction = buildFilteredObjectiveFunction(
                cracs,
                initialFlowResult,
                raoParameters,
                consideredCnecs
            );

            // Create and iteratively solve MIP to find optimal range actions' set-points FOR THE CONSIDERED CNECS
            final ReportNode globalRaOptimForIterationReportNode = MarmotReports.reportMarmotGlobalRangeActionsOptimizationForIteration(globalRangeActionsOptimizationReportNode, counter);
            linearOptimizationResults = optimizeLinearRemedialActions(
                new TimeCoupledRaoInput(initialInputs, timeCoupledRaoInput.getTimestampsToRun(), timeCoupledRaoInput.getTimeCoupledConstraints()),
                initialResults,
                initialSetpointResults,
                postTopoResults,
                raoParameters,
                preventiveTopologicalActions,
                curativeRemedialActions,
                consideredCnecs,
                filteredObjectiveFunction,
                parallelism,
                globalRaOptimForIterationReportNode
            );
            MarmotUtils.releaseAllWithoutOverwrite(initialInputs.map(RaoInput::getNetwork));
            MarmotReports.reportMarmotGlobalRangeActionsOptimizationForIterationEnd(counter);

            // Compute the flows on ALL the cnecs to check if the worst cnecs have changed and were considered in the MIP or not
            sensiResults = applyActionsAndRunFullSensitivityAnalysis(initialInputs, curativeRemedialActions, linearOptimizationResults, initialResults, raoParametersDuplicates, parallelism, reportNode);

            // Create a global result with the flows on ALL cnecs and the actions applied during MIP
            // TODO: does this contain curative setpoints?
            TemporalData<RangeActionActivationResult> rangeActionActivationResultTemporalData = linearOptimizationResults.getRangeActionActivationResultTemporalData();
            fullResults = new GlobalLinearOptimizationResult(
                sensiResults,
                sensiResults.map(PrePerimeterResult::getSensitivityResult),
                rangeActionActivationResultTemporalData,
                preventiveTopologicalActions,
                fullObjectiveFunction,
                linearOptimizationResults.getStatus(),
                globalRangeActionsOptimizationReportNode
            );

            MarmotReports.reportMarmotNextIterationOfMip(globalRangeActionsOptimizationReportNode, fullResults, raoParameters, 10);
            counter++;
        } while (
            shouldContinueAndAddCnecs(sensiResults, consideredCnecs, getFlowUnit(raoParameters), marmotParameters, globalRangeActionsOptimizationReportNode)
                && counter < marmotParameters.getMaxMipIterations()); // Stop if the worst element of each TS has been considered during MIP
        MarmotReports.reportMarmotGlobalRangeActionsOptimizationEnd();

        // 7. Merge topological and linear result
        if (fullResults.getStatus() == LinearProblemStatus.INFEASIBLE) {
            MarmotReports.reportMarmotInfeasibleGlobalMip(reportNode);
            MarmotReports.reportMarmotUnoptimizedRaoResult(reportNode, initialObjectiveFunctionResult, raoParameters, 10);
            TimeCoupledRaoResultImpl timeCoupledRaoResult = mergeTopologicalAndLinearOptimizationResults(
                initialInputs,
                initialResults,
                initialObjectiveFunctionResult,
                fullResults,
                initialInputs.map(r -> Set.of()),
                initialInputs.map(r -> new AppliedRemedialActions()),
                initialInputs.map(r -> Set.of()),
                raoParameters,
                reportNode
            );
            MarmotUtils.closeAll(initialNetworks);
            return CompletableFuture.completedFuture(timeCoupledRaoResult);
        }

        final ReportNode mergingTopoAndLinearRaReportNode = MarmotReports.reportMarmotMergingTopoAndLinearRemedialActionResults(reportNode);
        TimeCoupledRaoResultImpl timeCoupledRaoResult = mergeTopologicalAndLinearOptimizationResults(
            initialInputs,
            initialResults,
            initialObjectiveFunctionResult,
            fullResults,
            preventiveTopologicalActions.map(NetworkActionsResult::getActivatedNetworkActions),
            curativeRemedialActions,
            consideredCnecs,
            raoParameters,
            mergingTopoAndLinearRaReportNode
        );

        // 8. Log initial and final results
        MarmotReports.reportMarmotInitialResults(reportNode, initialObjectiveFunctionResult, raoParameters, 10);
        MarmotReports.reportMarmotResultAfterGlobalLinearOptimization(reportNode, fullResults, raoParameters, 10);

        MarmotUtils.closeAll(initialNetworks);
        return CompletableFuture.completedFuture(timeCoupledRaoResult);
    }

    private TemporalData<RangeActionSetpointResult> getInitialSetpointResults(TemporalData<Crac> cracs, int parallelism) {
        return MarmotUtils.smartMap(
            cracs,
            crac -> {
                Map<RangeAction<?>, Double> setPointMap = new HashMap<>();
                crac.getRangeActions().forEach(rangeAction ->
                    setPointMap.put(rangeAction, MarmotUtils.getInitialSetPoint(rangeAction))
                );
                return new RangeActionSetpointResultImpl(setPointMap);
            },
            parallelism
        );
    }

    private boolean shouldContinueAndAddCnecs(final TemporalData<PrePerimeterResult> loadFlowResults,
                                              final TemporalData<Set<FlowCnec>> consideredCnecs,
                                              final Unit flowUnit,
                                              final MarmotParameters marmotParameters,
                                              final ReportNode reportNode) {
        final int cnecsToAddPerVirtualCostName = marmotParameters.getNumberOfCnecsToAddPerVirtualCostName();
        final double minRelativeImprovementOnMargin = marmotParameters.getMinRelativeImprovementOnMargin();
        final double marginWindowToConsider = marmotParameters.getMarginWindowToConsider();

        final AtomicBoolean shouldContinue = new AtomicBoolean(false);
        updateShouldContinue(loadFlowResults, consideredCnecs, minRelativeImprovementOnMargin, shouldContinue, flowUnit);

        if (shouldContinue.get()) {
            updateConsideredCnecs(loadFlowResults, consideredCnecs, marginWindowToConsider, cnecsToAddPerVirtualCostName, flowUnit, reportNode);
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

    private static void updateConsideredCnecs(final TemporalData<PrePerimeterResult> loadFlowResults,
                                              final TemporalData<Set<FlowCnec>> consideredCnecs,
                                              final double marginWindowToConsider,
                                              final int cnecsToAddPerVirtualCostName,
                                              final Unit flowUnit,
                                              final ReportNode reportNode) {
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
        MarmotReports.reportMarmotCnecs(reportNode, addedCnecsForLogging);
    }

    public record LoggingAddedCnecs(OffsetDateTime offsetDateTime,
                                    String vcName,
                                    List<String> addedCnecs,
                                    Map<String, Double> margins) {
        private void addCnec(String cnec) {
            addedCnecs.add(cnec);
        }

        private void addCnec(String cnec, double margin) {
            addedCnecs.add(cnec);
            margins.put(cnec, margin);
        }
    }

    private static TemporalData<PrePerimeterResult> applyActionsAndRunFullSensitivityAnalysis(final TemporalData<RaoInput> postTopoInputs,
                                                                                              final TemporalData<AppliedRemedialActions> curativeRemedialActions,
                                                                                              final LinearOptimizationResult filteredResult,
                                                                                              final TemporalData<PrePerimeterResult> initialResults,
                                                                                              final TemporalData<RaoParameters> raoParameters,
                                                                                              final int parallelism,
                                                                                              final ReportNode reportNode) {
        return MarmotUtils.smartMap(
            postTopoInputs,
            raoInput -> {
                OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
                State preventiveState = raoInput.getCrac().getPreventiveState();
                raoInput.getCrac().getRangeActions(preventiveState).forEach(rangeAction ->
                    rangeAction.apply(raoInput.getNetwork(), filteredResult.getOptimizedSetpoint(rangeAction, preventiveState))
                );
                PrePerimeterResult sensitivityAnalysisResults = runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(
                    postTopoInputs.getData(timestamp).orElseThrow(),
                    curativeRemedialActions.getData(timestamp).orElseThrow(),
                    initialResults.getData(timestamp).orElseThrow(),
                    raoParameters.getData(timestamp).orElseThrow(),
                    reportNode);
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                return sensitivityAnalysisResults;
            },
            parallelism
        );
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(final TemporalData<RaoInput> raoInputs,
                                                                      final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                      final TemporalData<RaoParameters> raoParameters,
                                                                      final int parallelism,
                                                                      final ReportNode reportNode) {
        return MarmotUtils.smartMap(raoInputs, raoInput -> runSingleTopologicalOptimization(raoInput, consideredCnecs, raoParameters.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow(), reportNode), parallelism);
    }

    /**
     * Runs a CASTOR optimization for a given RAO input and applies the optimal preventive network actions on the input network.
     *
     * @param raoInput        The RAO input for the optimization.
     * @param consideredCnecs The temporal data of flow CNECs to be considered by FastRAO.
     * @param raoParameters   The RAO parameters for the optimization.
     * @return The resulting RaoResult after the optimization.
     */
    private static RaoResult runSingleTopologicalOptimization(final RaoInput raoInput,
                                                              final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                              final RaoParameters raoParameters,
                                                              final ReportNode reportNode) {
        OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
        final ReportNode raoRunReportNode = MarmotReports.reportMarmotRunningRaoForTimestamp(reportNode, timestamp);
        RaoResult raoResult = Rao.find(SINGLE_TS_RAO_IMPLEMENTATION).run(raoInput, raoParameters, null, raoRunReportNode);
        CriticalCnecsResult criticalCnecsResult = raoResult.getExtension(CriticalCnecsResult.class);
        Set<FlowCnec> critcalCnecs = (criticalCnecsResult != null) ?
            criticalCnecsResult.getCriticalCnecIds().stream().map(id -> raoInput.getCrac().getFlowCnec(id)).collect(Collectors.toSet()) :
            raoInput.getCrac().getFlowCnecs();
        MarmotReports.reportMarmotRunningRaoForTimestampEnd(timestamp);
        consideredCnecs.put(timestamp, critcalCnecs);
        State preventiveState = raoInput.getCrac().getPreventiveState();
        NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of(preventiveState, raoResult.getActivatedNetworkActionsDuringState(preventiveState)));
        MarmotReports.reportMarmotApplyingPraAfterOptimForTimestamp(reportNode, timestamp);
        MarmotUtils.applyPreventiveRemedialActions(raoInput, networkActionsResult, reportNode);
        return raoResult;
    }

    private static TemporalData<RaoResult> applyPreventiveToposFromRaoResults(final TemporalData<RaoInput> raoInputs,
                                                                              final TemporalData<RaoResult> raoResults,
                                                                              final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                              final int parallelism,
                                                                              final ReportNode reportNode) {
        return MarmotUtils.smartMap(raoInputs, raoInput -> applyPreventiveTopologicalActions(raoInput, raoResults.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow(), consideredCnecs, reportNode), parallelism);
    }

    private static RaoResult applyPreventiveTopologicalActions(final RaoInput raoInput,
                                                               final RaoResult raoResult,
                                                               final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                               final ReportNode reportNode) {
        OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
        State preventiveState = raoInput.getCrac().getPreventiveState();
        NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of(preventiveState, raoResult.getActivatedNetworkActionsDuringState(preventiveState)));
        MarmotReports.reportMarmotApplyingPraAfterOptimForTimestamp(reportNode, timestamp);
        MarmotUtils.applyPreventiveRemedialActions(raoInput, networkActionsResult, reportNode);
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

    private static TemporalData<PrePerimeterResult> runAllInitialSensitivityAnalyses(final TemporalData<RaoInput> raoInputs,
                                                                                     final TemporalData<RaoParameters> raoParameters,
                                                                                     final int parallelism,
                                                                                     final ReportNode reportNode) {
        return MarmotUtils.smartMap(
            raoInputs,
            raoInput -> {
                OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
                PrePerimeterResult sensitivityAnalysisResult = MarmotUtils.runInitialSensitivityAnalysis(
                    raoInput,
                    raoParameters.getData(timestamp).orElseThrow(),
                    reportNode
                );
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                return sensitivityAnalysisResult;
            },
            parallelism
        );
    }

    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalysesBasedOnInitialResult(final TemporalData<RaoInput> raoInputs,
                                                                                                  final TemporalData<AppliedRemedialActions> curativeRemedialActions,
                                                                                                  final TemporalData<? extends FlowResult> initialFlowResults,
                                                                                                  final TemporalData<RaoParameters> raoParameters,
                                                                                                  final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                                                  final int parallelism,
                                                                                                  final ReportNode reportNode) {
        return MarmotUtils.smartMap(
            raoInputs,
            raoInput -> {
                OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
                PrePerimeterResult sensitivityAnalysisResult = runSensitivityAnalysisBasedOnInitialResult(
                    raoInput,
                    curativeRemedialActions.getData(timestamp).orElseThrow(),
                    initialFlowResults.getData(timestamp).orElseThrow(),
                    raoParameters.getData(timestamp).orElseThrow(),
                    consideredCnecs.getData(timestamp).orElseThrow(),
                    reportNode
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

    private static GlobalLinearOptimizationResult optimizeLinearRemedialActions(final TimeCoupledRaoInput raoInput,
                                                                                final TemporalData<PrePerimeterResult> initialResults,
                                                                                final TemporalData<RangeActionSetpointResult> initialSetpoints,
                                                                                final TemporalData<PrePerimeterResult> postTopologicalActionsResults,
                                                                                final RaoParameters parameters,
                                                                                final TemporalData<NetworkActionsResult> preventiveTopologicalActions,
                                                                                final TemporalData<AppliedRemedialActions> curativeRemedialActions,
                                                                                final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                                final ObjectiveFunction objectiveFunction,
                                                                                final int parallelism,
                                                                                final ReportNode reportNode) {

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
                    .withPreOptimizationAppliedRemedialActions(curativeRemedialActions.getData(timestamp).orElseThrow())
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

        return TimeCoupledIteratingLinearOptimizer.optimize(timeCoupledLinearOptimizerInput, linearOptimizerParameters, parallelism, reportNode);
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs, TemporalData<Set<FlowCnec>> consideredCnecs, int parallelism) {
        return MarmotUtils.smartMap(
            cracs,
            crac -> {
                OffsetDateTime timestamp = crac.getTimestamp().orElseThrow();
                return new PreventiveOptimizationPerimeter(
                    crac.getPreventiveState(),
                    consideredCnecs.getData(timestamp).orElseThrow(),
                    new HashSet<>(), // no loopflows for now
                    new HashSet<>(), // don't re-optimize topological actions in Marmot
                    crac.getRangeActions(crac.getPreventiveState())
                );
            },
            parallelism
        );
    }

    private static TimeCoupledRaoResultImpl mergeTopologicalAndLinearOptimizationResults(final TemporalData<RaoInput> raoInputs,
                                                                                         final TemporalData<PrePerimeterResult> initialResults,
                                                                                         final ObjectiveFunctionResult initialLinearOptimizationResult,
                                                                                         final GlobalLinearOptimizationResult globalLinearOptimizationResult,
                                                                                         final TemporalData<Set<NetworkAction>> preventiveNetworkActions,
                                                                                         final TemporalData<AppliedRemedialActions> curativeRemedialActions,
                                                                                         final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                                         final RaoParameters raoParameters,
                                                                                         final ReportNode reportNode) {
        return new TimeCoupledRaoResultImpl(
            initialLinearOptimizationResult,
            globalLinearOptimizationResult,
            getPostOptimizationResults(
                raoInputs,
                initialResults,
                globalLinearOptimizationResult,
                preventiveNetworkActions,
                curativeRemedialActions,
                consideredCnecs,
                raoParameters,
                reportNode
            )
        );
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

        Set<State> allOptimizedStates = new HashSet<>();
        cracs.map(Crac::getStates).getDataPerTimestamp().values().forEach(allOptimizedStates::addAll);

        return ObjectiveFunction.build(
            flatConsideredCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates
        );
    }

    private LinearOptimizationResult getInitialObjectiveFunctionResult(final TemporalData<PrePerimeterResult> prePerimeterResults,
                                                                       final ObjectiveFunction objectiveFunction,
                                                                       final ReportNode reportNode) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = prePerimeterResults.map(RangeActionActivationResultImpl::new);
        TemporalData<NetworkActionsResult> networkActionsResults = new TemporalDataImpl<>();
        return new GlobalLinearOptimizationResult(
            prePerimeterResults.map(PrePerimeterResult::getFlowResult),
            prePerimeterResults.map(PrePerimeterResult::getSensitivityResult),
            rangeActionActivationResults,
            networkActionsResults,
            objectiveFunction,
            LinearProblemStatus.OPTIMAL,
            reportNode
        );
    }

    @Override
    public String getName() {
        return TIME_COUPLED_RAO;
    }
}
