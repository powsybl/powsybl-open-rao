/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.timecoupledsearchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.RaoUtil;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.ToolProvider;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.timecoupledsearchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.timecoupledsearchtreerao.reports.CastorReports;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.PostPerimeterResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;

public class TimeCoupledCurativeOptimization {

    public CompletableFuture<TimeCoupledRaoResult> run(final TimeCoupledRaoInput timeCoupledRaoInput, final RaoParameters raoParameters, final ReportNode reportNode) {
        TemporalData<RaoInput> raoInputs = timeCoupledRaoInput.getRaoInputs();
        int parallelism = Math.min(getAvailableCPUs(raoParameters), raoInputs.getTimestamps().size());

        // 1. independent initial sensitivity analyses
        TemporalData<PrePerimeterResult> initialResults = runAllSensitivityAnalyses(raoInputs, raoParameters, parallelism, reportNode);

        // 2. independent preventive optimizations
        CastorReports.reportPreventivePerimeterOptimization(reportNode);
        TemporalData<OptimizationResult> preventiveResults = runAllPreventiveOptimization(raoInputs, initialResults, raoParameters, parallelism, reportNode);
        CastorReports.reportPreventivePerimeterOptimizationEnd();

        // 3. post-PRA sensitivity analyses per timestamp
        TemporalData<PrePerimeterResult> postPraResults = runAllPostPraSensitivityAnalyses(raoInputs, preventiveResults, initialResults, raoParameters, parallelism, reportNode);

        // 4. global curative optimization
        TemporalData<Map<State, PostPerimeterResult>> postContingencyResults = optimizeContingencyScenarios(raoInputs, initialResults, preventiveResults, postPraResults, raoParameters, reportNode);

        // returned future carries null, which blocks any assertion on the run's output in the integration tests.
        return CompletableFuture.completedFuture(null);
    }

    // 1. Initial sensitivity analyses
    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalyses(TemporalData<RaoInput> raoInputs,
                                                                              RaoParameters raoParameters,
                                                                              int parallelism,
                                                                              ReportNode reportNode) {
        return MarmotUtils.smartMap(
                raoInputs,
                raoInput -> {
                    PrePerimeterResult sensitivityAnalysisResult = MarmotUtils.runInitialSensitivityAnalysis(
                            raoInput,
                            raoParameters,
                            reportNode
                    );
                    MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                    return sensitivityAnalysisResult;
                },
                parallelism);
    }

    // 2. Independent preventive optimizations
    private static TemporalData<OptimizationResult> runAllPreventiveOptimization(TemporalData<RaoInput> raoInputs,
                                                                                 TemporalData<PrePerimeterResult> initialResults,
                                                                                 RaoParameters raoParameters,
                                                                                 int parallelism,
                                                                                 ReportNode reportNode) {
        return MarmotUtils.smartMap(
                raoInputs,
                raoInput -> runPreventiveOptimization(
                        raoInput,
                        initialResults.getData(MarmotUtils.getTimestamp(raoInput)).orElseThrow(),
                        raoParameters,
                        reportNode),
                parallelism);
    }

    private static OptimizationResult runPreventiveOptimization(RaoInput raoInput,
                                                                PrePerimeterResult initialResult,
                                                                RaoParameters raoParameters,
                                                                ReportNode reportNode) {
        Crac crac = raoInput.getCrac();
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
        StateTree stateTree = new StateTree(crac, reportNode);
        CastorFullOptimization castorFullOptimization = new CastorFullOptimization(raoInput, raoParameters, Instant.now(), reportNode);
        return castorFullOptimization.optimizePreventivePerimeter(stateTree, toolProvider, initialResult, reportNode).getLeft();
    }

    // 3. Post-PRA sensitivity analyses
    private static TemporalData<PrePerimeterResult> runAllPostPraSensitivityAnalyses(TemporalData<RaoInput> raoInputs,
                                                                                     TemporalData<OptimizationResult> preventiveResults,
                                                                                     TemporalData<PrePerimeterResult> initialResults,
                                                                                     RaoParameters raoParameters,
                                                                                     int parallelism,
                                                                                     ReportNode reportNode) {
        return MarmotUtils.smartMap(
                raoInputs,
                raoInput -> {
                    OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
                    PrePerimeterResult sensitivityAnalysisResult = runPostPraSensitivity(
                            raoInput,
                            initialResults.getData(timestamp).orElseThrow(),
                            preventiveResults.getData(timestamp).orElseThrow(),
                            raoParameters,
                            reportNode);
                    MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                    return sensitivityAnalysisResult;
                },
                parallelism);
    }

    private static PrePerimeterResult runPostPraSensitivity(RaoInput raoInput,
                                                            PrePerimeterResult initialResult,
                                                            OptimizationResult preventiveOptimizationResult,
                                                            RaoParameters raoParameters,
                                                            ReportNode reportNode) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
        RaoUtil.applyRemedialActions(network, preventiveOptimizationResult, crac.getPreventiveState());
        boolean anyPreventiveActionActivated = !preventiveOptimizationResult.getActivatedNetworkActions().isEmpty() || !preventiveOptimizationResult.getActivatedRangeActionsPerState().isEmpty();
        if (anyPreventiveActionActivated) {
            return new PostPerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true)
                    .runBasedOnInitialPreviousAndOptimizationResults(network, initialResult, initialResult, Collections.emptySet(), preventiveOptimizationResult, null, reportNode)
                    .prePerimeterResultForAllFollowingStates();
        }
        return new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, false).runBasedOnInitialResults(network, initialResult, Collections.emptySet(), null, reportNode);
    }

    // 4. Global curative optimization
    private static TemporalData<Map<State, PostPerimeterResult>> optimizeContingencyScenarios(TemporalData<RaoInput> raoInputs,
                                                                                              TemporalData<PrePerimeterResult> initialResults,
                                                                                              TemporalData<OptimizationResult> preventiveResults,
                                                                                              TemporalData<PrePerimeterResult> postPraResults,
                                                                                              RaoParameters raoParameters,
                                                                                              ReportNode reportNode) {
        Map<OffsetDateTime, Crac> cracMap = new HashMap<>();
        Map<OffsetDateTime, ToolProvider> toolProviderMap = new HashMap<>();
        Map<OffsetDateTime, StateTree> stateTreeMap = new HashMap<>();
        Map<OffsetDateTime, Network> networkMap = new HashMap<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            cracMap.put(timestamp, raoInput.getCrac());
            toolProviderMap.put(timestamp, ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters));
            stateTreeMap.put(timestamp, new StateTree(raoInput.getCrac(), reportNode));
            networkMap.put(timestamp, raoInput.getNetwork());
        });
        TemporalData<Crac> cracs = new TemporalDataImpl<>(cracMap);
        TemporalData<ToolProvider> toolProviders = new TemporalDataImpl<>(toolProviderMap);
        TemporalData<StateTree> stateTrees = new TemporalDataImpl<>(stateTreeMap);
        TemporalData<Network> networks = new TemporalDataImpl<>(networkMap);

        double globalPreventiveOptimalCost = preventiveResults.getDataPerTimestamp().values().stream().mapToDouble(OptimizationResult::getCost).sum();
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, globalPreventiveOptimalCost);

        TimeCoupledCastorContingencyScenarios castorContingencyScenarios = new TimeCoupledCastorContingencyScenarios(
                cracs,
                raoParameters,
                toolProviders,
                stateTrees,
                curativeTreeParameters,
                initialResults);
        return castorContingencyScenarios.optimizeContingencyScenarios(networks, postPraResults, false, reportNode);
    }
}
