/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot;

import com.google.common.annotations.Beta;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorFullOptimization;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PostPerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import com.powsybl.openrao.searchtreerao.castor.algorithm.TimeCoupledCastorContingencyScenarios;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.TimeCoupledRaoResultImpl;
import com.powsybl.openrao.searchtreerao.reports.CastorReports;
import com.powsybl.openrao.searchtreerao.reports.MarmotReports;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PreventiveAndCurativesRaoResultImpl;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.searchtreerao.reports.MarmotReports.CURATIVE_SYNCHRONIZATION_PREFIX;

/**
 * Runs a time-coupled RAO where the curative remedial actions are synchronized across the timestamps, meaning that the same
 * decision is applied on every timestamp at once, i.e. the same set of activated network actions and the same
 * range action setpoints.
 * <p>
 * Remedial actions are matched across the timestamps by their CRAC ID only, therefore the IDs must be consistent between
 * all the cracs for this optimization to work.
 * <p>
 * Note:
 * <li> The UsageRules of the remedial actions are not checked across the timestamps.
 * <li> The RaUsageLimits are assumed identical between the timestamps and are not checked.
 * <li> Multi-curative optimization is not supported.
 * <li> No second preventive optimization is run for now
 */
@Beta
public class TimeCoupledCurativeSynchronization {
    public CompletableFuture<TimeCoupledRaoResult> run(TimeCoupledRaoInput timeCoupledRaoInput, RaoParameters raoParameters, ReportNode reportNode) {
        if (!raoParameters.hasExtension(MarmotParameters.class)) {
            MarmotReports.reportMissingMarmotParametersExtension(reportNode);
            raoParameters.addExtension(MarmotParameters.class, new MarmotParameters());
        }
        final MarmotParameters marmotParameters = raoParameters.getExtension(MarmotParameters.class);

        // TODO: add multi-curative implementation in the TimeCoupledCastorContingencyScenarios
        timeCoupledRaoInput.getRaoInputs().getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            if (raoInput.getCrac().getInstants(InstantKind.CURATIVE).size() > 1) {
                throw new OpenRaoException("Time-coupled curative synchronization does not support multi-curative optimization.");
            }
        });

        // initiate lazy networks
        TemporalData<Crac> cracs = timeCoupledRaoInput.getRaoInputs().map(RaoInput::getCrac);
        TemporalData<LazyNetwork> initialNetworks = MarmotUtils.cloneNetworks(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getNetwork));
        MarmotUtils.closeAll(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getNetwork));

        TemporalData<RaoInput> initialInputs = MarmotUtils.merge(initialNetworks, cracs);
        TemporalData<StateTree> stateTrees = initialInputs.map(raoInput -> new StateTree(raoInput.getCrac(), reportNode));

        TemporalData<RaoParameters> raoParametersDuplicates = new TemporalDataImpl<>();
        timeCoupledRaoInput.getTimestampsToRun().forEach(timestamp -> raoParametersDuplicates.put(timestamp, MarmotUtils.cloneParameters(raoParameters, reportNode)));

        int parallelism = Math.min(marmotParameters.getNumberOfThreads(), timeCoupledRaoInput.getTimestampsToRun().size());
        if (parallelism > 1) {
            MarmotReports.reportMarmotOptimizerSetToWorkOnNThreads(reportNode, parallelism, CURATIVE_SYNCHRONIZATION_PREFIX);
        }

        // 1. Compute the initial results for every timestamp
        final ReportNode initialSensiReportNode = MarmotReports.reportMarmotRunningInitialSensiAnalyses(reportNode, CURATIVE_SYNCHRONIZATION_PREFIX);
        TemporalData<PrePerimeterResult> initialResults = runAllSensitivityAnalyses(initialInputs, raoParametersDuplicates, parallelism, initialSensiReportNode);
        MarmotReports.reportMarmotRunningInitialSensiAnalysesEnd(CURATIVE_SYNCHRONIZATION_PREFIX);

        // 2. Evaluate the initial value of the global objective function
        final ReportNode globalObjFuncInitialValueEvalReportNode = MarmotReports.reportMarmotEvaluatingInitialValueOfGlobalObjFunction(reportNode, CURATIVE_SYNCHRONIZATION_PREFIX);
        ObjectiveFunction fullObjectiveFunction = MarmotUtils.buildGlobalObjectiveFunction(cracs, new GlobalFlowResult(initialResults), raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = MarmotUtils.getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction, globalObjFuncInitialValueEvalReportNode);
        MarmotReports.reportMarmotEvaluatingInitialValueOfGlobalObjFunctionEnd(CURATIVE_SYNCHRONIZATION_PREFIX);

        // 3. Run independent preventive optimizations
        CastorReports.reportPreventivePerimeterOptimization(reportNode, CURATIVE_SYNCHRONIZATION_PREFIX + " ");
        TemporalData<OptimizationResult> preventiveOptimizationResults = runIndependentPreventiveOptimizations(
            initialInputs,
            stateTrees,
            initialResults,
            raoParametersDuplicates,
            parallelism,
            reportNode
        );
        CastorReports.reportPreventivePerimeterOptimizationEnd(CURATIVE_SYNCHRONIZATION_PREFIX + " ");

        // 4. Post-PRA sensitivity analyses per timestamp
        final ReportNode postPraSensiReportNode = CastorReports.reportPostPraSensiAnalysis(reportNode);
        TemporalData<PostPerimeterResult> postPreventiveResults = runAllPostPraSensitivityAnalyses(
            initialInputs,
            preventiveOptimizationResults,
            initialResults,
            raoParametersDuplicates,
            parallelism,
            postPraSensiReportNode
        );
        TemporalData<PrePerimeterResult> postPraResults = postPreventiveResults.map(PostPerimeterResult::prePerimeterResultForAllFollowingStates);

        // 5. time coupled curative optimization : all the common curative remedial actions between the timestamps are optimized at once,
        // the activated range actions must have the same setpoint and the same set of network actions must be applied on all the timestamps.
        final ReportNode postContingencyReportNode = CastorReports.reportPostContingencyPerimeterOptimization(reportNode, CURATIVE_SYNCHRONIZATION_PREFIX + " ");
        TemporalData<Map<State, PostPerimeterResult>> postCurativeOptimizationResults = optimizeContingencyScenarios(
            initialInputs,
            stateTrees,
            initialResults,
            preventiveOptimizationResults,
            postPraResults,
            raoParameters,
            postContingencyReportNode
        );
        CastorReports.reportPostContingencyPerimeterOptimizationEnd(CURATIVE_SYNCHRONIZATION_PREFIX);

        // 6. merge the results
        CastorReports.reportMergingPreventiveAndPostContingencyRaoResults(reportNode, CURATIVE_SYNCHRONIZATION_PREFIX);
        LinearOptimizationResult finalObjectiveFunctionResult = MarmotUtils.getInitialObjectiveFunctionResult(
            getPostCurativeOptimizationResults(postCurativeOptimizationResults, postPraResults),
            fullObjectiveFunction,
            reportNode
        );
        TemporalData<RaoResult> raoResultsPerTimestamp = buildRaoResultsPerTimestamp(
            initialInputs,
            stateTrees,
            initialResults,
            postPreventiveResults,
            postCurativeOptimizationResults,
            raoParameters,
            reportNode
        );
        TimeCoupledRaoResult timeCoupledRaoResult = new TimeCoupledRaoResultImpl(initialObjectiveFunctionResult, finalObjectiveFunctionResult, raoResultsPerTimestamp);

        // log final results
        MarmotReports.reportCurativeSynchronizationFinalResults(reportNode, finalObjectiveFunctionResult, raoParameters, 10);
        Instant lastInstant = cracs.getData(cracs.getTimestamps().getFirst()).orElseThrow().getLastInstant();
        MarmotReports.reportCurativeSynchronizationGlobalCost(
            reportNode,
            timeCoupledRaoResult.getGlobalCost(lastInstant),
            timeCoupledRaoResult.getGlobalFunctionalCost(lastInstant),
            timeCoupledRaoResult.getGlobalVirtualCost(lastInstant)
        );
        cracs.getTimestamps().forEach(timestamp -> MarmotReports.reportCurativeSynchronizationCostForTimestamp(
            reportNode,
            timestamp,
            timeCoupledRaoResult.getFunctionalCost(lastInstant, timestamp),
            timeCoupledRaoResult.getVirtualCost(lastInstant, timestamp)
        ));
        MarmotUtils.closeAll(initialNetworks);
        return CompletableFuture.completedFuture(timeCoupledRaoResult);
    }

    /** Builds one RaoResult per timestamp from its own initial, post-preventive and post-contingency results. */
    private static TemporalData<RaoResult> buildRaoResultsPerTimestamp(TemporalData<RaoInput> raoInputs,
                                                                       TemporalData<StateTree> stateTrees,
                                                                       TemporalData<PrePerimeterResult> initialResults,
                                                                       TemporalData<PostPerimeterResult> postPreventiveResults,
                                                                       TemporalData<Map<State, PostPerimeterResult>> postContingencyResults,
                                                                       RaoParameters raoParameters,
                                                                       ReportNode reportNode) {
        Map<OffsetDateTime, RaoResult> raoResultPerTimestamp = new HashMap<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> raoResultPerTimestamp.put(timestamp, new PreventiveAndCurativesRaoResultImpl(
                stateTrees.getData(timestamp).orElseThrow(),
                initialResults.getData(timestamp).orElseThrow(),
                postPreventiveResults.getData(timestamp).orElseThrow(),
                postContingencyResults.getData(timestamp).orElseThrow(),
                raoInput.getCrac(),
                raoParameters,
                reportNode)));
        return new TemporalDataImpl<>(raoResultPerTimestamp);
    }

    /**
     * extracts, for every timestamp, the sensitivity results describing the final situation, it is used to evaluate the global cost.
     * it's either the ones computed after the curtive optimization or the post-PRA ones when the timestamp has no curative state at all.
     */
    private static TemporalData<PrePerimeterResult> getPostCurativeOptimizationResults(TemporalData<Map<State, PostPerimeterResult>> postCurativeOptimizationResults,
                                                                                       TemporalData<PrePerimeterResult> postPraResults) {
        TemporalData<PrePerimeterResult> postCurativeResults = new TemporalDataImpl<>();
        postCurativeOptimizationResults.getDataPerTimestamp().forEach((timestamp, resultPerState) -> postCurativeResults.put(timestamp,
            resultPerState.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().isCurative())
                .map(entry -> entry.getValue().prePerimeterResultForAllFollowingStates())
                .findFirst()
                .orElse(postPraResults.getData(timestamp).orElseThrow())));
        return postCurativeResults;
    }

    /** Runs every timestamp's preventive optimization in parallel. */
    private static TemporalData<OptimizationResult> runIndependentPreventiveOptimizations(TemporalData<RaoInput> raoInputs,
                                                                                          TemporalData<StateTree> stateTrees,
                                                                                          TemporalData<PrePerimeterResult> initialResults,
                                                                                          TemporalData<RaoParameters> raoParameters,
                                                                                          int parallelism,
                                                                                          ReportNode reportNode) {
        return MarmotUtils.smartMap(
            raoInputs,
            raoInput -> {
                OptimizationResult preventiveOptimizationResult = runPreventiveOptimization(
                    raoInput,
                    stateTrees.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow(),
                    initialResults.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow(),
                    raoParameters.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow(),
                    reportNode
                );
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                return preventiveOptimizationResult;
            },
            parallelism
        );
    }

    private static OptimizationResult runPreventiveOptimization(RaoInput raoInput,
                                                                StateTree stateTree,
                                                                PrePerimeterResult initialResult,
                                                                RaoParameters raoParameters,
                                                                ReportNode reportNode) {
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
        CastorFullOptimization castorFullOptimization = new CastorFullOptimization(raoInput, raoParameters, java.time.Instant.now(), reportNode);
        return castorFullOptimization.optimizePreventivePerimeter(stateTree, toolProvider, initialResult, reportNode);
    }

    /** Runs the time-coupled curative optimization */
    private static TemporalData<Map<State, PostPerimeterResult>> optimizeContingencyScenarios(TemporalData<RaoInput> raoInputs,
                                                                                              TemporalData<StateTree> stateTrees,
                                                                                              TemporalData<PrePerimeterResult> initialResults,
                                                                                              TemporalData<OptimizationResult> preventiveOptimizationResults,
                                                                                              TemporalData<PrePerimeterResult> postPraResults,
                                                                                              RaoParameters raoParameters,
                                                                                              ReportNode reportNode) {
        TemporalData<Crac> cracs = new TemporalDataImpl<>();
        TemporalData<ToolProvider> toolProviders = new TemporalDataImpl<>();
        TemporalData<Network> networks = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach(
                (timestamp, raoInput) -> {
                    cracs.put(timestamp, raoInput.getCrac());
                    toolProviders.put(timestamp, ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters));
                    networks.put(timestamp, raoInput.getNetwork());
                });
        double globalPreventiveCost = preventiveOptimizationResults.getDataPerTimestamp().values().stream().mapToDouble(OptimizationResult::getCost).sum();
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, globalPreventiveCost);
        TimeCoupledCastorContingencyScenarios castorContingencyScenarios = new TimeCoupledCastorContingencyScenarios(
            cracs,
            raoParameters,
            toolProviders,
            stateTrees,
            curativeTreeParameters,
            initialResults
        );
        return castorContingencyScenarios.optimizeContingencyScenarios(networks, postPraResults, false, reportNode);
    }

    /** Runs every timestamp's sensitivity analysis after its preventive remedial actions have been applied */
    private static TemporalData<PostPerimeterResult> runAllPostPraSensitivityAnalyses(TemporalData<RaoInput> raoInputs,
                                                                                      TemporalData<OptimizationResult> preventiveResults,
                                                                                      TemporalData<PrePerimeterResult> initialResults,
                                                                                      TemporalData<RaoParameters> raoParametersPerTimestamp,
                                                                                      int parallelism,
                                                                                      ReportNode reportNode) {
        return MarmotUtils.smartMap(
            raoInputs,
            raoInput -> {
                OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
                Crac crac = raoInput.getCrac();
                Network network = raoInput.getNetwork();
                PrePerimeterResult initialResult = initialResults.getData(timestamp).orElseThrow();
                OptimizationResult preventiveOptimizationResult = preventiveResults.getData(timestamp).orElseThrow();
                RaoParameters timestampParameters = raoParametersPerTimestamp.getData(timestamp).orElseThrow();
                ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, timestampParameters);
                RaoUtil.applyRemedialActions(network, preventiveOptimizationResult, crac.getPreventiveState());
                PostPerimeterResult postPreventiveResult = new PostPerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), timestampParameters, toolProvider, true)
                        .runBasedOnInitialPreviousAndOptimizationResults(network, initialResult, initialResult, Collections.emptySet(), preventiveOptimizationResult, null, reportNode);
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                return postPreventiveResult;
            },
            parallelism);
    }

    /** Runs every timestamp's initial sensitivity analysis, on the network as it is given, without any remedial action applied. */
    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalyses(TemporalData<RaoInput> raoInputs,
                                                                              TemporalData<RaoParameters> raoParameters,
                                                                              int parallelism,
                                                                              ReportNode reportNode) {
        return MarmotUtils.smartMap(
            raoInputs,
            raoInput -> {
                Crac crac = raoInput.getCrac();
                RaoParameters timestampParameters = raoParameters.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow();
                ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, timestampParameters);
                // range actions are part of the sensitivity analysis
                PrePerimeterResult sensitivityAnalysisResult = new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(),
                        timestampParameters, toolProvider, false).runInitialSensitivityAnalysis(raoInput.getNetwork(), reportNode);
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                return sensitivityAnalysisResult;
            },
            parallelism);
    }
}
