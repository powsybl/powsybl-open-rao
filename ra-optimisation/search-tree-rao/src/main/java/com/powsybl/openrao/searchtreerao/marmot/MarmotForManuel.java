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
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
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
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPostOptimizationResults;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@Beta
@AutoService(TimeCoupledRaoProvider.class)
public class MarmotForManuel implements TimeCoupledRaoProvider {

    private static final String TIME_COUPLED_RAO = "TimeCoupledRaoForManuel";
    private static final String MIN_MARGIN_VIOLATION_EVALUATOR = "min-margin-violation-evaluator";
    // TODO the RAO implementation to use should be a parameter
    private static final String SINGLE_TS_RAO_IMPLEMENTATION = "FastRao";

    static void applyForcedActions(TemporalData<RaoInput> raoInputs, RodaParameters rodaParameters) {
        if (rodaParameters == null || rodaParameters.getForcedPreventiveActions().isEmpty()) {
            return;
        }
        OpenRaoLoggerProvider.BUSINESS_LOGS.info(String.format("Applying %d forced preventive actions before running RAO.", rodaParameters.getForcedPreventiveActions().size()));
        raoInputs.getDataPerTimestamp().values().stream().map(RaoInput::getNetwork).forEach(network -> {
            rodaParameters.getForcedPreventiveActions().stream().filter(action -> !action.toModification().apply(network, true))
                .forEach(action -> BUSINESS_WARNS.warn(String.format("Action '%s' could not be applied.", action.getId())));
            rodaParameters.getForcedPreventiveActions().forEach(action -> action.toModification().apply(network, false));
        });
    }

    private CompletableFuture<TimeCoupledRaoResult> runSingleTsRao(TimeCoupledRaoInput raoInputs, RaoParameters raoParameters) {
        OffsetDateTime ts = raoInputs.getRaoInputs().getTimestamps().getFirst();
        RaoInput raoInput = raoInputs.getRaoInputs().getData(ts).orElseThrow();
        RaoResult result = Rao.find(SINGLE_TS_RAO_IMPLEMENTATION).run(raoInput, raoParameters);
        // TODO two first arguments may be replaced by automatic detection on result by ts
        return CompletableFuture.completedFuture(new TimeCoupledRaoResultImpl(null, null, new TemporalDataImpl<>(Map.of(ts, result))));
    }

    @Override
    public CompletableFuture<TimeCoupledRaoResult> run(final TimeCoupledRaoInput timeCoupledRaoInput,
                                                       final RaoParameters raoParameters,
                                                       final ReportNode reportNode) {
        applyForcedActions(timeCoupledRaoInput.getRaoInputs(), raoParameters.getExtension(RodaParameters.class));

        if (timeCoupledRaoInput.getRaoInputs().getTimestamps().size() == 1) {
            TECHNICAL_LOGS.info("[MARMOT] Only one time-step in inputs. Calling single time-step RAO directly: {}", SINGLE_TS_RAO_IMPLEMENTATION);
            return runSingleTsRao(timeCoupledRaoInput, raoParameters);
        }

        if (!raoParameters.hasExtension(MarmotParameters.class)) {
            MarmotReports.reportMissingMarmotParametersExtension(reportNode);
            raoParameters.addExtension(MarmotParameters.class, new MarmotParameters());
        }
        final MarmotParameters marmotParameters = raoParameters.getExtension(MarmotParameters.class);

        // Initiate lazy networks
        TemporalData<Crac> cracs = timeCoupledRaoInput.getRaoInputs().map(RaoInput::getCrac);
        TemporalData<Network> initialNetworks = timeCoupledRaoInput.getRaoInputs().map(RaoInput::getNetwork);
        MarmotUtils.releaseAllWithOverwrite(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getNetwork));

        TemporalData<RaoInput> initialInputs = timeCoupledRaoInput.getRaoInputs();

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
        TemporalData<PrePerimeterResult> initialResults = runAllInitialSensitivityAnalyses(initialInputs, raoParameters, parallelism, initialSensiReportNode);
        MarmotReports.reportMarmotRunningInitialSensiAnalysesEnd();

        // 2. Evaluate the initial value of the global objective function
        final ReportNode globalObjFuncInitialValueEvalReportNode = MarmotReports.reportMarmotEvaluatingInitialValueOfGlobalObjFunction(reportNode);
        ObjectiveFunction fullObjectiveFunction = buildGlobalObjectiveFunction(cracs, new GlobalFlowResult(initialResults), raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction, globalObjFuncInitialValueEvalReportNode);
        MarmotReports.reportMarmotEvaluatingInitialValueOfGlobalObjFunctionEnd();

        // 3. Run independent RAOs to compute and apply the optimal preventive remedial actions
        // skipped

        // TODO : Add time-coupled constraint check if none violated then return
        // boolean noTimeCoupledConstraints = timeCoupledRaoInput.getTimeCoupledConstraints().getGeneratorConstraints().isEmpty();

        // 4. Retrieve post-topological optimization results
        TemporalData<AppliedRemedialActions> curativeTopologicalActions = MarmotUtils.smartMap(
            cracs, crac -> new AppliedRemedialActions(), parallelism
        );
        TemporalData<NetworkActionsResult> preventiveTopologicalActions = MarmotUtils.smartMap(
            cracs, crac -> new NetworkActionsResultImpl(Collections.emptyMap()), parallelism
        );

        final ReportNode globalRangeActionsOptimizationReportNode = MarmotReports.reportMarmotGlobalRangeActionsOptimization(reportNode);
        TemporalData<Set<FlowCnec>> consideredCnecs = MarmotUtils.smartMap(
            cracs, Crac::getFlowCnecs, parallelism
        );

        // Create and iteratively solve MIP to find optimal range actions' set-points
        final ReportNode globalRaOptimForIterationReportNode = MarmotReports.reportMarmotGlobalRangeActionsOptimizationForIteration(globalRangeActionsOptimizationReportNode, 1);
        GlobalLinearOptimizationResult linearOptimizationResults = optimizeLinearRemedialActions(
            timeCoupledRaoInput,
            initialResults,
            raoParameters,
            preventiveTopologicalActions,
            curativeTopologicalActions,
            consideredCnecs,
            fullObjectiveFunction,
            parallelism,
            globalRaOptimForIterationReportNode
        );
        MarmotUtils.releaseAllWithoutOverwrite(initialInputs.map(RaoInput::getNetwork));
        MarmotReports.reportMarmotGlobalRangeActionsOptimizationForIterationEnd(1);
        MarmotReports.reportMarmotNextIterationOfMip(globalRangeActionsOptimizationReportNode, linearOptimizationResults, raoParameters, 10);
        MarmotReports.reportMarmotGlobalRangeActionsOptimizationEnd();

        // 7. Merge topological and linear result
        if (linearOptimizationResults.getStatus() == LinearProblemStatus.INFEASIBLE) {
            MarmotReports.reportMarmotInfeasibleGlobalMip(reportNode);
            MarmotReports.reportMarmotUnoptimizedRaoResult(reportNode, initialObjectiveFunctionResult, raoParameters, 10);
            TimeCoupledRaoResultImpl timeCoupledRaoResult = mergeTopologicalAndLinearOptimizationResults(
                initialInputs,
                initialResults,
                initialObjectiveFunctionResult,
                linearOptimizationResults,
                initialInputs.map(r -> Set.of()),
                initialInputs.map(r -> new AppliedRemedialActions()),
                initialInputs.map(r -> Set.of()),
                raoParameters,
                reportNode
            );
            MarmotUtils.releaseAllWithoutOverwrite(initialNetworks);
            return CompletableFuture.completedFuture(timeCoupledRaoResult);
        }

        final ReportNode mergingTopoAndLinearRaReportNode = MarmotReports.reportMarmotMergingTopoAndLinearRemedialActionResults(reportNode);
        TimeCoupledRaoResultImpl timeCoupledRaoResult = mergeTopologicalAndLinearOptimizationResults(
            initialInputs,
            initialResults,
            initialObjectiveFunctionResult,
            linearOptimizationResults,
            preventiveTopologicalActions.map(NetworkActionsResult::getActivatedNetworkActions),
            curativeTopologicalActions,
            consideredCnecs,
            raoParameters,
            mergingTopoAndLinearRaReportNode
        );

        // 8. Log initial and final results
        MarmotReports.reportMarmotInitialResults(reportNode, initialObjectiveFunctionResult, raoParameters, 10);
        MarmotReports.reportMarmotResultAfterGlobalLinearOptimization(reportNode, linearOptimizationResults, raoParameters, 10);

        MarmotUtils.releaseAllWithoutOverwrite(initialNetworks);
        return CompletableFuture.completedFuture(timeCoupledRaoResult);
    }

    private static TemporalData<PrePerimeterResult> runAllInitialSensitivityAnalyses(final TemporalData<RaoInput> raoInputs,
                                                                                     final RaoParameters raoParameters,
                                                                                     final int parallelism,
                                                                                     final ReportNode reportNode) {
        return MarmotUtils.smartMap(
            raoInputs,
            raoInput -> {
                PrePerimeterResult sensitivityAnalysisResult = MarmotUtils.runInitialSensitivityAnalysis(
                    raoInput,
                    raoParameters,
                    reportNode,
                    true
                );
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
                return sensitivityAnalysisResult;
            },
            parallelism
        );
    }

    private static GlobalLinearOptimizationResult optimizeLinearRemedialActions(final TimeCoupledRaoInput raoInput,
                                                                                final TemporalData<PrePerimeterResult> initialResults,
                                                                                final RaoParameters parameters,
                                                                                final TemporalData<NetworkActionsResult> preventiveTopologicalActions,
                                                                                final TemporalData<AppliedRemedialActions> curativeTopologicalActions,
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
                    .withPreOptimizationFlowResult(initialResults.getData(timestamp).orElseThrow())
                    .withPrePerimeterSetpoints(initialResults.getData(timestamp).orElseThrow())
                    .withPreOptimizationSensitivityResult(initialResults.getData(timestamp).orElseThrow())
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

        return TimeCoupledIteratingLinearOptimizer.optimize(timeCoupledLinearOptimizerInput, linearOptimizerParameters, parallelism, reportNode);
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs, TemporalData<Set<FlowCnec>> consideredCnecs, int parallelism) {
        return MarmotUtils.smartMap(
            cracs,
            crac -> {
                OffsetDateTime timestamp = crac.getTimestamp().orElseThrow();
                Map<State, Set<RangeAction<?>>> availableRangeActions = new HashMap<>();
                State preventiveState = crac.getPreventiveState();
                Set<State> optimizedCurativeStates = consideredCnecs.getData(timestamp).orElseThrow().stream()
                    .map(Cnec::getState)
                    .filter(state -> state.getInstant().isCurative())
                    .collect(Collectors.toSet());
                // set of range actions optimized by the mip
                crac.getStates().stream()
                        .filter(state -> state.isPreventive() || state.getInstant().isCurative() && optimizedCurativeStates.contains(state))
                        .forEach(state -> MarmotUtils.addRangeActionsPerState(availableRangeActions, crac, state));
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

    private static TimeCoupledRaoResultImpl mergeTopologicalAndLinearOptimizationResults(final TemporalData<RaoInput> raoInputs,
                                                                                         final TemporalData<PrePerimeterResult> initialResults,
                                                                                         final ObjectiveFunctionResult initialLinearOptimizationResult,
                                                                                         final GlobalLinearOptimizationResult globalLinearOptimizationResult,
                                                                                         final TemporalData<Set<NetworkAction>> preventiveNetworkActions,
                                                                                         final TemporalData<AppliedRemedialActions> curativeTopologicalActions,
                                                                                         final TemporalData<Set<FlowCnec>> consideredCnecs,
                                                                                         final RaoParameters raoParameters,
                                                                                         final ReportNode reportNode) {
        TimeCoupledRaoResultImpl result = new TimeCoupledRaoResultImpl(
            initialLinearOptimizationResult,
            globalLinearOptimizationResult,
            getPostOptimizationResults(
                raoInputs,
                initialResults,
                globalLinearOptimizationResult,
                preventiveNetworkActions,
                curativeTopologicalActions,
                consideredCnecs,
                raoParameters,
                reportNode
            )
        );
        return result;
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
