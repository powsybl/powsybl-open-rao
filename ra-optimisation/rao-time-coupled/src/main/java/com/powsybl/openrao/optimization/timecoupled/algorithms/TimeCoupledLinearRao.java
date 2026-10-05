/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.optimization.timecoupled.algorithms;

import com.google.auto.service.AutoService;
import com.google.ortools.Loader;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.optimization.commons.RaoUtil;
import com.powsybl.openrao.optimization.commons.ToolProvider;
import com.powsybl.openrao.optimization.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.optimization.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.optimization.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.optimization.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.optimization.linear.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.optimization.linear.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.optimization.timecoupled.marmot.MarmotUtils;
import com.powsybl.openrao.optimization.timecoupled.marmot.TimeCoupledIteratingLinearOptimizer;
import com.powsybl.openrao.optimization.timecoupled.marmot.TimeCoupledIteratingLinearOptimizerInput;
import com.powsybl.openrao.optimization.timecoupled.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.optimization.timecoupled.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.optimization.timecoupled.marmot.results.TimeCoupledRaoResultImpl;
import com.powsybl.openrao.optimization.timecoupled.reports.TimeCoupledLinearRaoReports;
import com.powsybl.openrao.optimization.commons.result.api.FlowResult;
import com.powsybl.openrao.optimization.commons.result.api.LinearProblemStatus;
import com.powsybl.openrao.optimization.commons.result.api.NetworkActionsResult;
import com.powsybl.openrao.optimization.commons.result.api.PrePerimeterResult;
import com.powsybl.openrao.optimization.commons.result.api.RangeActionActivationResult;
import com.powsybl.openrao.optimization.commons.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.optimization.commons.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.optimization.commons.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.optimization.commons.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.optimization.commons.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO;
import static com.powsybl.openrao.optimization.commons.RaoUtil.getFlowUnit;

/**
 * Time-coupled RAO provider running a single global linear optimization of all the range actions
 * of all the timestamps (with time-coupling constraints), each timestamp being described by a
 * {@link GlobalOptimizationPerimeter} over its full CRAC.
 * It corresponds to the time-coupled optimization of root leaves, without any search tree:
 * network actions are therefore never optimized.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(TimeCoupledRaoProvider.class)
public class TimeCoupledLinearRao implements TimeCoupledRaoProvider {

    static {
        try {
            Loader.loadNativeLibraries();
        } catch (UnsatisfiedLinkError e) {
            TECHNICAL_LOGS.error("Native library jniortools could not be loaded. You can ignore this message if it is not needed.");
        }
    }

    private static final String TIME_COUPLED_LINEAR_RAO = "TimeCoupledLinearRao";
    private static final int PARALLELISM = 1;

    @Override
    public String getName() {
        return TIME_COUPLED_LINEAR_RAO;
    }

    @Override
    public CompletableFuture<TimeCoupledRaoResult> run(final TimeCoupledRaoInput timeCoupledRaoInput, final RaoParameters raoParameters, final ReportNode reportNode) {
        TemporalData<Crac> cracs = timeCoupledRaoInput.getRaoInputs().map(RaoInput::getCrac);
        warnIfNetworkActionsInCracs(cracs, reportNode);

        TemporalData<LazyNetwork> networks = MarmotUtils.cloneNetworks(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getNetwork));
        MarmotUtils.closeAll(timeCoupledRaoInput.getRaoInputs().map(RaoInput::getNetwork));
        TemporalData<RaoInput> raoInputs = MarmotUtils.merge(networks, cracs);
        try {
            raoInputs.getDataPerTimestamp().values().forEach(raoInput -> {
                RaoUtil.initData(raoInput, raoParameters, reportNode);
                MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
            });
            return CompletableFuture.completedFuture(optimize(raoInputs, timeCoupledRaoInput, raoParameters, reportNode));
        } catch (OpenRaoException e) {
            TECHNICAL_LOGS.error(String.format("Time-coupled linear RAO failed: %s", e.getMessage()), e);
            throw e;
        } finally {
            MarmotUtils.closeAll(networks);
        }
    }

    static void warnIfNetworkActionsInCracs(TemporalData<Crac> cracs, ReportNode reportNode) {
        cracs.getDataPerTimestamp().forEach((timestamp, crac) -> {
            if (!crac.getNetworkActions().isEmpty()) {
                TimeCoupledLinearRaoReports.reportTimeCoupledLinearRaoNetworkActionsIgnored(reportNode, timestamp, crac.getNetworkActions().size());
            }
        });
    }

    private static TimeCoupledRaoResult optimize(TemporalData<RaoInput> raoInputs, TimeCoupledRaoInput timeCoupledRaoInput,
                                                 RaoParameters raoParameters, ReportNode reportNode) {
        TemporalData<Crac> cracs = raoInputs.map(RaoInput::getCrac);

        // ----- INITIAL SENSI -----
        ReportNode initialSensiReportNode = TimeCoupledLinearRaoReports.reportTimeCoupledLinearRaoRunningInitialSensiAnalyses(reportNode);
        TemporalData<PrePerimeterResult> rawInitialResults = MarmotUtils.smartMap(raoInputs, raoInput -> {
            PrePerimeterResult result = MarmotUtils.runInitialSensitivityAnalysis(raoInput, raoParameters, initialSensiReportNode);
            MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
            return result;
        }, PARALLELISM);
        TemporalData<RangeActionSetpointResult> initialSetpoints = cracs.map(crac -> {
            Map<RangeAction<?>, Double> setpoints = new HashMap<>();
            crac.getRangeActions().forEach(ra -> setpoints.put(ra, MarmotUtils.getInitialSetPoint(ra)));
            return new RangeActionSetpointResultImpl(setpoints);
        });
        Map<OffsetDateTime, PrePerimeterResult> initialResultsMap = new HashMap<>();
        rawInitialResults.getDataPerTimestamp().forEach((timestamp, result) -> initialResultsMap.put(timestamp,
            new PrePerimeterSensitivityResultImpl(result.getFlowResult(), result.getSensitivityResult(),
                initialSetpoints.getData(timestamp).orElseThrow(), result.getObjectiveFunctionResult())));
        TemporalData<PrePerimeterResult> initialResults = new TemporalDataImpl<>(initialResultsMap);

        FlowResult globalInitialFlowResult = new GlobalFlowResult(initialResults);
        ObjectiveFunction objectiveFunction = buildGlobalObjectiveFunction(cracs, globalInitialFlowResult, raoParameters);
        GlobalLinearOptimizationResult initialObjectiveFunctionResult = new GlobalLinearOptimizationResult(
            initialResults.map(PrePerimeterResult::getFlowResult),
            initialResults.map(PrePerimeterResult::getSensitivityResult),
            initialResults.map(r -> (RangeActionActivationResult) new RangeActionActivationResultImpl(r)),
            new TemporalDataImpl<>(),
            objectiveFunction,
            LinearProblemStatus.OPTIMAL,
            reportNode);

        // ----- GLOBAL TIME-COUPLED LINEAR OPTIMIZATION -----
        ReportNode optimizationReportNode = TimeCoupledLinearRaoReports.reportTimeCoupledLinearRaoGlobalOptimization(reportNode);
        TemporalData<NetworkActionsResult> noPreventiveNetworkActions = cracs.map(crac -> new NetworkActionsResultImpl(Map.of(crac.getPreventiveState(), Set.of())));
        TemporalData<IteratingLinearOptimizerInput> linearOptimizerInputs = MarmotUtils.smartMap(raoInputs,
            raoInput -> buildLinearOptimizerInput(raoInput, initialResults, initialSetpoints, noPreventiveNetworkActions, raoParameters, reportNode),
            PARALLELISM);

        TimeCoupledIteratingLinearOptimizerInput timeCoupledInput = new TimeCoupledIteratingLinearOptimizerInput(
            linearOptimizerInputs, objectiveFunction, timeCoupledRaoInput.getTimeCoupledConstraints());
        GlobalLinearOptimizationResult linearResult = TimeCoupledIteratingLinearOptimizer.optimize(
            timeCoupledInput, buildLinearOptimizerParameters(raoParameters, reportNode), PARALLELISM, optimizationReportNode);
        MarmotUtils.releaseAllWithoutOverwrite(raoInputs.map(RaoInput::getNetwork));

        // ----- RESULTS -----
        TemporalData<Set<FlowCnec>> allCnecs = cracs.map(Crac::getFlowCnecs);
        return new TimeCoupledRaoResultImpl(
            initialObjectiveFunctionResult,
            linearResult,
            MarmotUtils.getPostOptimizationResults(
                raoInputs,
                initialResults,
                linearResult,
                cracs.map(crac -> Set.of()),
                cracs.map(crac -> new AppliedRemedialActions()),
                allCnecs,
                raoParameters,
                reportNode));
    }

    private static IteratingLinearOptimizerInput buildLinearOptimizerInput(RaoInput raoInput,
                                                                           TemporalData<PrePerimeterResult> initialResults,
                                                                           TemporalData<RangeActionSetpointResult> initialSetpoints,
                                                                           TemporalData<NetworkActionsResult> noPreventiveNetworkActions,
                                                                           RaoParameters raoParameters,
                                                                           ReportNode reportNode) {
        OffsetDateTime timestamp = MarmotUtils.getTimestamp(raoInput);
        PrePerimeterResult initialResult = initialResults.getData(timestamp).orElseThrow();
        OptimizationPerimeter perimeter = GlobalOptimizationPerimeter.build(raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResult, reportNode)
            .copyWithFilteredAvailableHvdcRangeAction(raoInput.getNetwork());
        IteratingLinearOptimizerInput input = IteratingLinearOptimizerInput.create()
            .withNetwork(new LazyNetwork(raoInput.getNetwork()))
            .withOptimizationPerimeter(perimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterFlowResult(initialResult)
            .withPreOptimizationFlowResult(initialResult)
            .withPrePerimeterSetpoints(initialSetpoints.getData(timestamp).orElseThrow())
            .withPreOptimizationSensitivityResult(initialResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters))
            .withOutageInstant(raoInput.getCrac().getOutageInstant())
            .withAppliedNetworkActionsInPrimaryState(noPreventiveNetworkActions.getData(timestamp).orElseThrow())
            .build();
        MarmotUtils.releaseNetworkWithoutOverwrite(raoInput.getNetwork());
        MarmotUtils.releaseNetworkWithoutOverwrite(input.network());
        return input;
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        Set<State> allStates = new HashSet<>();
        cracs.getDataPerTimestamp().values().forEach(crac -> {
            allFlowCnecs.addAll(crac.getFlowCnecs());
            allStates.addAll(crac.getStates());
        });
        return ObjectiveFunction.build(allFlowCnecs, new HashSet<>(), globalInitialFlowResult, globalInitialFlowResult,
            Collections.emptySet(), raoParameters, allStates);
    }

    private static IteratingLinearOptimizerParameters buildLinearOptimizerParameters(RaoParameters parameters, ReportNode reportNode) {
        OpenRaoSearchTreeParameters extension = parameters.hasExtension(OpenRaoSearchTreeParameters.class)
            ? parameters.getExtension(OpenRaoSearchTreeParameters.class)
            : new OpenRaoSearchTreeParameters(reportNode);
        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder builder = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(parameters.getObjectiveFunctionParameters().getType())
            .withFlowUnit(getFlowUnit(parameters))
            .withRangeActionParameters(parameters.getRangeActionsOptimizationParameters())
            .withRangeActionParametersExtension(extension.getRangeActionsOptimizationParameters())
            .withMaxNumberOfIterations(extension.getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(ENABLED.equals(extension.getRangeActionsOptimizationParameters().getRaRangeShrinking())
                || ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(extension.getRangeActionsOptimizationParameters().getRaRangeShrinking()))
            .withSolverParameters(extension.getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxMinRelativeMarginParameters(parameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class))
            .withRaLimitationParameters(new RangeActionLimitationParameters())
            .withMinMarginParameters(extension.getMinMarginsParameters().orElse(new SearchTreeRaoCostlyMinMarginParameters()));
        parameters.getMnecParameters().ifPresent(builder::withMnecParameters);
        extension.getMnecParameters().ifPresent(builder::withMnecParametersExtension);
        parameters.getLoopFlowParameters().ifPresent(builder::withLoopFlowParameters);
        extension.getLoopFlowParameters().ifPresent(builder::withLoopFlowParametersExtension);
        return builder.build();
    }
}
