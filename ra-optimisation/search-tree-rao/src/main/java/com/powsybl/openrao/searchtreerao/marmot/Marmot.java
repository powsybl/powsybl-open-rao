/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.reports.MarmotReports;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.marmot.results.InterTemporalRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Marmot implements InterTemporalRaoProvider {

    private static final String INTER_TEMPORAL_RAO = "InterTemporalRao";
    private static final String INITIAL_SCENARIO = "InitialScenario";
    private static final String POST_TOPO_SCENARIO = "PostTopoScenario";
    private static final String MIP_SCENARIO = "MipScenario";
    private static final String MIN_MARGIN_VIOLATION_EVALUATOR = "min-margin-violation-evaluator";

    @Override
    public CompletableFuture<InterTemporalRaoResult> run(final InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths,
                                                         final RaoParameters raoParameters,
                                                         final ReportNode reportNode) {
        // 1. Run independent RAOs to compute optimal preventive topological remedial actions
        final ReportNode topologicalOptimizationReportNode = MarmotReports.reportMarmotTopologicalOptimization(reportNode);
        TemporalData<Set<FlowCnec>> consideredCnecs = new TemporalDataImpl<>();
        TemporalData<RaoResult> topologicalOptimizationResults = runTopologicalOptimization(interTemporalRaoInputWithNetworkPaths.getRaoInputs(), consideredCnecs, raoParameters, topologicalOptimizationReportNode);
        MarmotReports.reportMarmotTopologicalOptimizationEnd();

        // 2. Get the initial results from the various independent results to avoid recomputing them
        TemporalData<PrePerimeterResult> initialResults = buildInitialResults(topologicalOptimizationResults);

        // TODO : Add intertemporal constraint check if none violated then return
        boolean noInterTemporalConstraint = interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints().getGeneratorConstraints().isEmpty();

        // 3. Apply independent topological remedial actions (and preventive range actions if there are no inter-temporal constraints)
        InterTemporalRaoInput interTemporalRaoInput = importNetworksFromInterTemporalRaoInputWithNetworkPaths(interTemporalRaoInputWithNetworkPaths);
        MarmotReports.reportMarmotApplyingOptimalTopologicalActionsOnNetworks(reportNode);
        ObjectiveFunction fullObjectiveFunction = buildGlobalObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction, reportNode);

        // 4. Evaluate objective function after independent optimizations
        MarmotReports.reportMarmotEvaluatingGlobalResultAfterIndependentOptimizations(reportNode);
        TemporalData<PrePerimeterResult> postTopologicalActionsResults = topologicalOptimizationResults.map(
            raoResult -> ((FastRaoResultImpl) raoResult).getFinalResult()
        );
        TemporalData<RangeActionSetpointResult> initialSetpointResults = getInitialSetpointResults(topologicalOptimizationResults, interTemporalRaoInput.getRaoInputs());
        LinearOptimizationResult postTopologicalOptimizationResult = getPostTopologicalOptimizationResult(
            initialSetpointResults,
            postTopologicalActionsResults,
            fullObjectiveFunction,
            topologicalOptimizationResults,
            interTemporalRaoInput.getRaoInputs().map(individualRaoInput -> individualRaoInput.getCrac().getPreventiveState()),
            reportNode);

        // if no inter-temporal constraints are defined, the results can be returned
        if (noInterTemporalConstraint) {
            MarmotReports.reportMarmotNoInterTemporalConstraintProvided(reportNode);
            return CompletableFuture.completedFuture(new InterTemporalRaoResultImpl(initialObjectiveFunctionResult, postTopologicalOptimizationResult, topologicalOptimizationResults));
        }

        // 5. Get and apply topological actions applied in independent optimizations
        TemporalData<NetworkActionsResult> preventiveTopologicalActions = getPreventiveTopologicalActions(interTemporalRaoInputWithNetworkPaths.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), topologicalOptimizationResults);
        applyPreventiveTopologicalActionsOnNetworks(interTemporalRaoInput.getRaoInputs(), preventiveTopologicalActions, reportNode);

        // 6. Create and iteratively solve MIP to find optimal range actions' set-points
        // Get the curative ations applied in the individual results to be able to apply them during sensitivity computations
        TemporalData<AppliedRemedialActions> curativeRemedialActions = MarmotUtils.getAppliedRemedialActionsInCurative(interTemporalRaoInput.getRaoInputs(), topologicalOptimizationResults);

        final ReportNode globalRangeActionsOptimizationReportNode = MarmotReports.reportMarmotGlobalRangeActionsOptimization(reportNode);
        // make fast rao result lighter by keeping only initial flow result and filtered rao result for actions
        replaceFastRaoResultsWithLightVersions(topologicalOptimizationResults);

        //TODO: loop
        TemporalData<PrePerimeterResult> loadFlowResults;
        GlobalLinearOptimizationResult linearOptimizationResults;
        GlobalLinearOptimizationResult fullResults;
        int counter = 1;
        do {
            // Clone the PostTopoScenario variant to make sure we work on a clean variant every time
            interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().values().forEach(raoInput -> {
                raoInput.getNetwork().getVariantManager().cloneVariant(POST_TOPO_SCENARIO, MIP_SCENARIO, true);
                raoInput.getNetwork().getVariantManager().setWorkingVariant(MIP_SCENARIO);
            });

            // Run post topo sensitivity analysis on all timestamps ON CONSIDERED CNECS ONLY (which is why we do it every loop)
            final ReportNode systematicInterTemporalSensiAnalysisReportNode = MarmotReports.reportMarmotSystematicInterTemporalSensitivityAnalysis(reportNode);
            TemporalData<PrePerimeterResult> postTopoResults = runAllSensitivityAnalysesBasedOnInitialResult(interTemporalRaoInput.getRaoInputs(), curativeRemedialActions, initialResults, raoParameters, consideredCnecs, systematicInterTemporalSensiAnalysisReportNode);
            MarmotReports.reportMarmotSystematicInterTemporalSensitivityAnalysisEnd();

            // Build objective function with ONLY THE CONSIDERED CNECS
            ObjectiveFunction filteredObjectiveFunction = buildFilteredObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), raoParameters, consideredCnecs);

            // Create and iteratively solve MIP to find optimal range actions' set-points FOR THE CONSIDERED CNECS
            final ReportNode globalRaOptimForIterationReportNode = MarmotReports.reportMarmotGlobalRangeActionsOptimizationForIteration(reportNode, counter);
            linearOptimizationResults = optimizeLinearRemedialActions(interTemporalRaoInput, initialResults, initialSetpointResults, postTopoResults, raoParameters, preventiveTopologicalActions, curativeRemedialActions, consideredCnecs, filteredObjectiveFunction, globalRaOptimForIterationReportNode);
            MarmotReports.reportMarmotGlobalRangeActionsOptimizationForIterationEnd(counter);

            // Compute the flows on ALL the cnecs to check if the worst cnecs have changed and were considered in the MIP or not
            loadFlowResults = applyActionsAndRunFullLoadflow(interTemporalRaoInput.getRaoInputs(), curativeRemedialActions, linearOptimizationResults, initialResults, raoParameters, reportNode);

            // Create a global result with the flows on ALL cnecs and the actions applied during MIP
            TemporalData<RangeActionActivationResult> rangeActionActivationResultTemporalData = linearOptimizationResults.getRangeActionActivationResultTemporalData();
            fullResults = new GlobalLinearOptimizationResult(loadFlowResults, loadFlowResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResultTemporalData, preventiveTopologicalActions, fullObjectiveFunction, LinearProblemStatus.OPTIMAL, globalRangeActionsOptimizationReportNode);

            MarmotReports.reportMarmotNextIterationOfMip(reportNode, fullResults, raoParameters, 10);
            counter++;
        } while (shouldContinueAndAddCnecs(loadFlowResults, consideredCnecs, globalRangeActionsOptimizationReportNode) && counter < 10); // Stop if the worst element of each TS has been considered during MIP
        MarmotReports.reportMarmotGlobalRangeActionsOptimizationEnd();

        // 7. Merge topological and linear result
        final ReportNode mergingTopoAndLinearRaReportNode = MarmotReports.reportMarmotMergingTopoAndLinearRemedialActionResults(reportNode);
        InterTemporalRaoResultImpl interTemporalRaoResult = mergeTopologicalAndLinearOptimizationResults(interTemporalRaoInput.getRaoInputs(), initialResults, initialObjectiveFunctionResult, fullResults, topologicalOptimizationResults, raoParameters, mergingTopoAndLinearRaReportNode);

        // 8. Log initial and final results
        MarmotReports.reportMarmotResultBeforeTopologicalOptimization(reportNode, initialObjectiveFunctionResult, raoParameters, 10);
        MarmotReports.reportMarmotResultBeforeGlobalLinearOptimization(reportNode, postTopologicalOptimizationResult, raoParameters, 10);
        MarmotReports.reportMarmotResultAfterGlobalLinearOptimization(reportNode, fullResults, raoParameters, 10);

        return CompletableFuture.completedFuture(interTemporalRaoResult);
    }

    private TemporalData<RangeActionSetpointResult> getInitialSetpointResults(TemporalData<RaoResult> postTopologicalActionsResults, TemporalData<RaoInput> raoInputs) {
        TemporalData<RangeActionSetpointResult> initialSetpointResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            Map<RangeAction<?>, Double> setPointMap = new HashMap<>();
            raoInput.getCrac().getRangeActions().forEach(rangeAction ->
                setPointMap.put(rangeAction, postTopologicalActionsResults.getData(timestamp).orElseThrow()
                    .getPreOptimizationSetPointOnState(raoInput.getCrac().getPreventiveState(), rangeAction))
            );
            RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(
                setPointMap
            );
            initialSetpointResults.put(timestamp, rangeActionSetpointResult);
        });
        return initialSetpointResults;
    }

    private boolean shouldContinueAndAddCnecs(final TemporalData<PrePerimeterResult> loadFlowResults,
                                              final TemporalData<Set<FlowCnec>> consideredCnecs,
                                              final ReportNode reportNode) {
        int cnecsToAddPerVirtualCostName = 20;
        double minRelativeImprovementOnMargin = 0.1;
        double marginWindowToConsider = 5.0;

        AtomicBoolean shouldContinue = new AtomicBoolean(false);
        updateShouldContinue(loadFlowResults, consideredCnecs, minRelativeImprovementOnMargin, shouldContinue);

        if (shouldContinue.get()) {
            updateConsideredCnecs(loadFlowResults, consideredCnecs, marginWindowToConsider, cnecsToAddPerVirtualCostName, reportNode);
        }
        return shouldContinue.get();
    }

    private static void updateShouldContinue(TemporalData<PrePerimeterResult> loadFlowResults, TemporalData<Set<FlowCnec>> consideredCnecs, double minRelativeImprovementOnMargin, AtomicBoolean shouldContinue) {
        loadFlowResults.getTimestamps().forEach(timestamp -> {
            PrePerimeterResult loadFlowResult = loadFlowResults.getData(timestamp).orElseThrow();
            Set<FlowCnec> previousCnecs = consideredCnecs.getData(timestamp).orElseThrow();

            // for margin violation - need to compare to min improvement on margin
            // ordered list of cnecs with an overload
            List<FlowCnec> worstCnecsForMarginViolation = loadFlowResult.getCostlyElements(MIN_MARGIN_VIOLATION_EVALUATOR, Integer.MAX_VALUE);
            double worstConsideredMargin = worstCnecsForMarginViolation.stream()
                .filter(previousCnecs::contains)
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, Unit.MEGAWATT))
                .orElse(0.);
            double worstMarginOfAll = worstCnecsForMarginViolation.stream()
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, Unit.MEGAWATT))
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
        });
    }

    private static void updateConsideredCnecs(final TemporalData<PrePerimeterResult> loadFlowResults,
                                              final TemporalData<Set<FlowCnec>> consideredCnecs,
                                              final double marginWindowToConsider,
                                              final int cnecsToAddPerVirtualCostName,
                                              final ReportNode reportNode) {
        List<LoggingAddedCnecs> addedCnecsForLogging = new ArrayList<>();
        loadFlowResults.getTimestamps().forEach(timestamp -> {
            PrePerimeterResult loadFlowResult = loadFlowResults.getData(timestamp).orElseThrow();
            Set<FlowCnec> previousIterationCnecs = consideredCnecs.getData(timestamp).orElseThrow();
            Set<FlowCnec> nextIterationCnecs = new HashSet<>(previousIterationCnecs);

            double worstConsideredMargin = loadFlowResult.getCostlyElements(MIN_MARGIN_VIOLATION_EVALUATOR, Integer.MAX_VALUE)
                .stream()
                .filter(previousIterationCnecs::contains)
                .findFirst()
                .map(cnec -> loadFlowResult.getMargin(cnec, Unit.MEGAWATT))
                .orElse(0.);

            loadFlowResult.getVirtualCostNames().forEach(vcName -> {
                LoggingAddedCnecs currentLoggingAddedCnecs = new LoggingAddedCnecs(timestamp, vcName, new ArrayList<>(), new HashMap<>());
                int addedCnecsForVcName = 0;

                // for min margin violation take all cnecs
                if (vcName.equals(MIN_MARGIN_VIOLATION_EVALUATOR)) {
                    for (FlowCnec cnec : loadFlowResult.getCostlyElements(vcName, Integer.MAX_VALUE)) {
                        if (loadFlowResult.getMargin(cnec, Unit.MEGAWATT) > worstConsideredMargin + marginWindowToConsider && addedCnecsForVcName > cnecsToAddPerVirtualCostName) {
                            // stop if out of window and already added enough
                            break;
                        } else if (!previousIterationCnecs.contains(cnec)) {
                            // if in window or not added enough yet, add
                            nextIterationCnecs.add(cnec);
                            addedCnecsForVcName++;
                            currentLoggingAddedCnecs.addCnec(cnec.getId(), loadFlowResult.getMargin(cnec, Unit.MEGAWATT));
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
            });
            consideredCnecs.put(timestamp, nextIterationCnecs);
        });
        MarmotReports.reportMarmotCnecs(reportNode, addedCnecsForLogging);
    }

    public record LoggingAddedCnecs(OffsetDateTime offsetDateTime, String vcName, List<String> addedCnecs, Map<String, Double> margins) {
        private void addCnec(String cnec) {
            addedCnecs.add(cnec);
        }

        private void addCnec(String cnec, double margin) {
            addedCnecs.add(cnec);
            margins.put(cnec, margin);
        }
    }

    private static TemporalData<PrePerimeterResult> applyActionsAndRunFullLoadflow(final TemporalData<RaoInput> raoInputs,
                                                                                   final TemporalData<AppliedRemedialActions> curativeRemedialActions,
                                                                                   final LinearOptimizationResult filteredResult,
                                                                                   final TemporalData<PrePerimeterResult> initialResults,
                                                                                   final RaoParameters raoParameters,
                                                                                   final ReportNode reportNode) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            // duplicate the postTopoScenario variant and switch to the new clone
            raoInput.getNetwork().getVariantManager().cloneVariant(POST_TOPO_SCENARIO, "PostPreventiveScenario", true);
            raoInput.getNetwork().getVariantManager().setWorkingVariant("PostPreventiveScenario");
            State preventiveState = raoInput.getCrac().getPreventiveState();
            raoInput.getCrac().getRangeActions(preventiveState).forEach(rangeAction ->
                rangeAction.apply(raoInput.getNetwork(), filteredResult.getOptimizedSetpoint(rangeAction, preventiveState))
            );
            prePerimeterResults.put(timestamp, runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(
                raoInputs.getData(timestamp).orElseThrow(),
                curativeRemedialActions.getData(timestamp).orElseThrow(),
                initialResults.getData(timestamp).orElseThrow(),
                raoParameters,
                reportNode));
            // switch back to the postTopoScenario to avoid keeping applied range actions when entering the MIP
            raoInput.getNetwork().getVariantManager().setWorkingVariant(POST_TOPO_SCENARIO);
        });
        return prePerimeterResults;
    }

    private void replaceFastRaoResultsWithLightVersions(TemporalData<RaoResult> topologicalOptimizationResults) {
        topologicalOptimizationResults.getDataPerTimestamp().forEach((timestamp, raoResult) -> topologicalOptimizationResults.put(timestamp, new LightFastRaoResultImpl((FastRaoResultImpl) raoResult)));
    }

    private InterTemporalRaoInput importNetworksFromInterTemporalRaoInputWithNetworkPaths(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths) {
        return new InterTemporalRaoInput(
            interTemporalRaoInputWithNetworkPaths.getRaoInputs().map(raoInputWithNetworksPath -> {
                RaoInput raoInput = raoInputWithNetworksPath.toRaoInputWithPostIcsImportNetworkPath();
                raoInput.getNetwork().getVariantManager().cloneVariant(raoInput.getNetworkVariantId(), INITIAL_SCENARIO);
                return raoInput;
            }),
            interTemporalRaoInputWithNetworkPaths.getTimestampsToRun(),
            interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints()
        );
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(final TemporalData<RaoInputWithNetworkPaths> raoInputs,
                                                                      final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                      final RaoParameters raoParameters,
                                                                      final ReportNode reportNode) {
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolverSpecificParameters("MAXTIME 15");

        TemporalData<RaoResult> individualResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((datetime, individualRaoInputWithNetworkPath) -> {
            RaoInput individualRaoInput = RaoInput
                .build(Network.read(individualRaoInputWithNetworkPath.getPostIcsImportNetworkPath()), individualRaoInputWithNetworkPath.getCrac())
                .build();
            Set<FlowCnec> cnecs = new HashSet<>();
            final OffsetDateTime timestamp = individualRaoInput.getCrac().getTimestamp().orElseThrow();
            final ReportNode runRaoReportNode = MarmotReports.reportMarmotRunRaoForTimestamp(reportNode, timestamp);
            final RaoResult raoResult = FastRao.launchFastRaoOptimization(individualRaoInput, raoParameters, null, cnecs, runRaoReportNode);
            MarmotReports.reportMarmotRunRaoForTimestampEnd(timestamp);
            consideredCnecs.put(datetime, cnecs);
            individualResults.put(datetime, raoResult);
        });
        return individualResults;
    }

    private static void applyPreventiveTopologicalActionsOnNetworks(final TemporalData<RaoInput> raoInputs,
                                                                    final TemporalData<NetworkActionsResult> preventiveTopologicalActionsResults,
                                                                    final ReportNode reportNode) {
        raoInputs.getTimestamps().forEach(timestamp -> {
            RaoInput raoInput = raoInputs.getData(timestamp).orElseThrow();
            NetworkActionsResult networkActionsResult = preventiveTopologicalActionsResults.getData(timestamp).orElseThrow();
            MarmotUtils.applyPreventiveRemedialActions(raoInput, networkActionsResult, INITIAL_SCENARIO, POST_TOPO_SCENARIO, reportNode);
        });
    }

    private TemporalData<PrePerimeterResult> buildInitialResults(TemporalData<RaoResult> topologicalOptimizationResults) {
        TemporalData<PrePerimeterResult> initialResults = new TemporalDataImpl<>();
        topologicalOptimizationResults.getDataPerTimestamp().forEach((timestamp, raoResult) ->
            initialResults.put(timestamp, ((FastRaoResultImpl) raoResult).getInitialResult()));
        return initialResults;
    }

    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalysesBasedOnInitialResult(final TemporalData<RaoInput> raoInputs,
                                                                                                  final TemporalData<AppliedRemedialActions> curativeRemedialActions,
                                                                                                  final TemporalData<? extends FlowResult> initialFlowResults,
                                                                                                  final RaoParameters raoParameters,
                                                                                                  final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                                                  final ReportNode reportNode) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getTimestamps().forEach(timestamp ->
            prePerimeterResults.put(timestamp, runSensitivityAnalysisBasedOnInitialResult(
                raoInputs.getData(timestamp).orElseThrow(),
                curativeRemedialActions.getData(timestamp).orElseThrow(),
                initialFlowResults.getData(timestamp).orElseThrow(),
                raoParameters,
                consideredCnecs.getData(timestamp).orElseThrow(),
                reportNode
        )));
        return prePerimeterResults;
    }

    private static TemporalData<NetworkActionsResult> getPreventiveTopologicalActions(TemporalData<Crac> cracs, TemporalData<RaoResult> raoResults) {
        Map<OffsetDateTime, NetworkActionsResult> preventiveTopologicalActions = new HashMap<>();
        cracs.getTimestamps().forEach(timestamp -> {
            State preventiveState = cracs.getData(timestamp).orElseThrow().getPreventiveState();
            preventiveTopologicalActions.put(timestamp, new NetworkActionsResultImpl(Map.of(preventiveState, raoResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(preventiveState))));
        });
        return new TemporalDataImpl<>(preventiveTopologicalActions);
    }

    private static GlobalLinearOptimizationResult optimizeLinearRemedialActions(final InterTemporalRaoInput raoInput,
                                                                                final TemporalData<PrePerimeterResult> initialResults,
                                                                                final TemporalData<RangeActionSetpointResult> initialSetpoints,
                                                                                final TemporalData<PrePerimeterResult> postTopologicalActionsResults,
                                                                                final RaoParameters parameters,
                                                                                final TemporalData<NetworkActionsResult> preventiveTopologicalActions,
                                                                                final TemporalData<AppliedRemedialActions> curativeRemedialActions,
                                                                                final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                                final ObjectiveFunction objectiveFunction,
                                                                                final ReportNode reportNode) {

        // -- Build IteratingLinearOptimizerInterTemporalInput
        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInput::getCrac), consideredCnecs);
        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global

        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
        raoInput.getRaoInputs().getTimestamps().forEach(timestamp -> linearOptimizerInputPerTimestamp.put(timestamp,
            IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getNetwork())
            .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow().copyWithFilteredAvailableHvdcRangeAction(raoInput.getRaoInputs().getData(timestamp).get().getNetwork()))
            .withInitialFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPrePerimeterFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPreOptimizationFlowResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
            .withPrePerimeterSetpoints(initialSetpoints.getData(timestamp).orElseThrow())
            .withPreOptimizationSensitivityResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
            .withPreOptimizationAppliedRemedialActions(curativeRemedialActions.getData(timestamp).orElseThrow())
            .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
            .withOutageInstant(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getOutageInstant())
            .withAppliedNetworkActionsInPrimaryState(preventiveTopologicalActions.getData(timestamp).orElseThrow())
            .build()));
        InterTemporalIteratingLinearOptimizerInput interTemporalLinearOptimizerInput = new InterTemporalIteratingLinearOptimizerInput(new TemporalDataImpl<>(linearOptimizerInputPerTimestamp), objectiveFunction, raoInput.getIntertemporalConstraints());

        // Build parameters
        // Unoptimized cnec parameters ignored because only PRAs
        // TODO: define static method to define Ra Limitation Parameters from crac and topos (mutualize with search tree) : SearchTreeParameters::decreaseRemedialActionsUsageLimits
        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder linearOptimizerParametersBuilder = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(parameters.getObjectiveFunctionParameters().getType())
            .withObjectiveFunctionUnit(parameters.getObjectiveFunctionParameters().getUnit())
            .withRangeActionParameters(parameters.getRangeActionsOptimizationParameters())
            .withRangeActionParametersExtension(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters())
            .withMaxNumberOfIterations(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()) || SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()))
            .withSolverParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxMinRelativeMarginParameters(parameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class))
            .withRaLimitationParameters(new RangeActionLimitationParameters())
            .withMinMarginParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().orElse(new SearchTreeRaoCostlyMinMarginParameters()));
        parameters.getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParametersExtension);
        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParametersExtension);
        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();

        return InterTemporalIteratingLinearOptimizer.optimize(interTemporalLinearOptimizerInput, linearOptimizerParameters, reportNode);
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs, TemporalData<Set<FlowCnec>> consideredCnecs) {
        TemporalData<OptimizationPerimeter> optimizationPerimeters = new TemporalDataImpl<>();
        cracs.getTimestamps().forEach(timestamp -> {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            optimizationPerimeters.put(timestamp, new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                consideredCnecs.getData(timestamp).orElseThrow(),
                new HashSet<>(), // no loopflows for now
                new HashSet<>(), // don't re-optimize topological actions in Marmot
                crac.getRangeActions(crac.getPreventiveState())
            ));
        });
        return optimizationPerimeters;
    }

    private static InterTemporalRaoResultImpl mergeTopologicalAndLinearOptimizationResults(final TemporalData<RaoInput> raoInputs,
                                                                                           final TemporalData<PrePerimeterResult> initialResults,
                                                                                           final ObjectiveFunctionResult initialLinearOptimizationResult,
                                                                                           final GlobalLinearOptimizationResult globalLinearOptimizationResult,
                                                                                           final TemporalData<RaoResult> topologicalOptimizationResults,
                                                                                           final RaoParameters raoParameters,
                                                                                           final ReportNode reportNode) {
        return new InterTemporalRaoResultImpl(
            initialLinearOptimizationResult,
            globalLinearOptimizationResult,
            getPostOptimizationResults(
                raoInputs,
                initialResults,
                globalLinearOptimizationResult,
                topologicalOptimizationResults,
                raoParameters,
                reportNode));
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(MarmotUtils::getPreventivePerimeterCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private static ObjectiveFunction buildFilteredObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, RaoParameters raoParameters, TemporalData<Set<FlowCnec>> consideredCnecs) {
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

    private LinearOptimizationResult getInitialObjectiveFunctionResult(final TemporalData<PrePerimeterResult> prePerimeterResults,
                                                                       final ObjectiveFunction objectiveFunction,
                                                                       final ReportNode reportNode) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = prePerimeterResults.map(RangeActionActivationResultImpl::new);
        TemporalData<NetworkActionsResult> networkActionsResults = new TemporalDataImpl<>();
        return new GlobalLinearOptimizationResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult), prePerimeterResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL, reportNode);
    }

    private LinearOptimizationResult getPostTopologicalOptimizationResult(final TemporalData<RangeActionSetpointResult> allInitialSetPoints,
                                                                          final TemporalData<PrePerimeterResult> prePerimeterResults,
                                                                          final ObjectiveFunction objectiveFunction,
                                                                          final TemporalData<RaoResult> topologicalOptimizationResults,
                                                                          final TemporalData<State> preventiveStates,
                                                                          final ReportNode reportNode) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = getRangeActionActivationResults(allInitialSetPoints, topologicalOptimizationResults, preventiveStates);
        TemporalData<NetworkActionsResult> networkActionsResults = getNetworkActionActivationResults(topologicalOptimizationResults, preventiveStates);
        return new GlobalLinearOptimizationResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult), prePerimeterResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL, reportNode);
    }

    private static TemporalData<RangeActionActivationResult> getRangeActionActivationResults(TemporalData<RangeActionSetpointResult> allInitialSetPoints, TemporalData<RaoResult> topologicalOptimizationResults, TemporalData<State> preventiveStates) {
        Map<OffsetDateTime, RangeActionActivationResult> rangeActionsResults = new HashMap<>();
        topologicalOptimizationResults.getTimestamps().forEach(
            timestamp -> {
                State preventiveState = preventiveStates.getData(timestamp).orElseThrow();
                RangeActionSetpointResult initialSetPoints = allInitialSetPoints.getData(timestamp).orElseThrow();
                RangeActionSetpointResult optimizedSetPoints = new RangeActionSetpointResultImpl(topologicalOptimizationResults.getData(timestamp).orElseThrow().getOptimizedSetPointsOnState(preventiveState));
                RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(initialSetPoints);
                optimizedSetPoints.getRangeActions().forEach(rangeAction -> rangeActionActivationResult.putResult(rangeAction, preventiveState, optimizedSetPoints.getSetpoint(rangeAction)));
                rangeActionsResults.put(timestamp, rangeActionActivationResult);
            }
        );
        return new TemporalDataImpl<>(rangeActionsResults);
    }

    private static TemporalData<NetworkActionsResult> getNetworkActionActivationResults(TemporalData<RaoResult> topologicalOptimizationResults, TemporalData<State> preventiveStates) {
        Map<OffsetDateTime, NetworkActionsResult> networkActionsResults = new HashMap<>();
        topologicalOptimizationResults.getTimestamps().forEach(
            timestamp -> {
                State preventiveState = preventiveStates.getData(timestamp).orElseThrow();
                Set<NetworkAction> activatedNetworkActions = topologicalOptimizationResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(preventiveState);
                networkActionsResults.put(timestamp, new NetworkActionsResultImpl(Map.of(preventiveState, activatedNetworkActions)));
            }
        );
        return new TemporalDataImpl<>(networkActionsResults);
    }

    @Override
    public String getName() {
        return INTER_TEMPORAL_RAO;
    }
}
