/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
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
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.FastRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.logCost;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPostOptimizationResults;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getTopologicalOptimizationResult;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runInitialPrePerimeterSensitivityAnalysis;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Marmot implements InterTemporalRaoProvider {

    private static final String INTER_TEMPORAL_RAO = "InterTemporalRao";
    private static final String VERSION = "1.0.0";

    @Override
    public CompletableFuture<TemporalData<RaoResult>> run(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths, RaoParameters raoParameters) {
        // 1. Run independent RAOs to compute optimal preventive topological remedial actions
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [start]");
        TemporalData<RaoResult> topologicalOptimizationResults = runTopologicalOptimization(interTemporalRaoInputWithNetworkPaths.getRaoInputs(), raoParameters);
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [end]");

        // if no inter-temporal constraints are defined, the results can be returned
        // TODO : Add intertemporal constraint check if none violated then return
        if (interTemporalRaoInputWithNetworkPaths.getPowerGradients().isEmpty()) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] No inter-temporal constraint provided; no need to re-optimize range actions");
            return CompletableFuture.completedFuture(topologicalOptimizationResults);
        }

        TemporalData<PrePerimeterResult> initialResults = buildInitialResults(topologicalOptimizationResults);
        TemporalData<RaoInput> raoInputsWithImportedNetworks = new TemporalDataImpl<>();
        interTemporalRaoInputWithNetworkPaths.getRaoInputs().getDataPerTimestamp().forEach((datetime, individualRaoInputWithNetworkPath) -> {
            Network network = Network.read(individualRaoInputWithNetworkPath.getPostIcsImportNetworkPath());
            RaoInput individualRaoInput = RaoInput
                .build(network, individualRaoInputWithNetworkPath.getCrac())
                .build();
            RaoUtil.initData(individualRaoInput, raoParameters);
            network.getVariantManager().cloneVariant(individualRaoInput.getNetworkVariantId(), "InitialScenario");
            raoInputsWithImportedNetworks.add(datetime, individualRaoInput);
        });
        InterTemporalRaoInput interTemporalRaoInput = new InterTemporalRaoInput(raoInputsWithImportedNetworks, interTemporalRaoInputWithNetworkPaths.getPowerGradients());

        // 2. Apply preventive topological remedial actions
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Applying optimal topological actions on networks");
        applyPreventiveTopologicalActionsOnNetwork(interTemporalRaoInput.getRaoInputs(), topologicalOptimizationResults);

        TemporalData<AppliedRemedialActions> curativeRemedialActions = MarmotUtils.getAppliedRemedialActionsInCurative(interTemporalRaoInput.getRaoInputs(), topologicalOptimizationResults);
        // 3. Run initial sensitivity analysis on all timestamps
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [start]");
        TemporalData<PrePerimeterResult> prePerimeterResults = runAllInitialPrePerimeterSensitivityAnalysis(interTemporalRaoInput.getRaoInputs(), curativeRemedialActions, initialResults, raoParameters);
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [end]");

        // 4. Build objective function and initial result
        ObjectiveFunction objectiveFunction = buildGlobalObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), new GlobalFlowResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult)), raoParameters);
        TemporalData<NetworkActionsResult> preventiveTopologicalActions = getPreventiveTopologicalActions(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), topologicalOptimizationResults);
        LinearOptimizationResult initialLinearOptimizationResult = getInitialLinearOptimizationResult(prePerimeterResults, preventiveTopologicalActions, objectiveFunction);

        // 5. Create and iteratively solve MIP to find optimal range actions' set-points
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start]");
        LinearOptimizationResult linearOptimizationResults = optimizeLinearRemedialActions(interTemporalRaoInput, initialResults, prePerimeterResults, raoParameters, preventiveTopologicalActions, curativeRemedialActions, objectiveFunction);
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end]");

        // 6. Merge topological and linear result
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");
        TemporalData<RaoResult> mergedRaoResults = mergeTopologicalAndLinearOptimizationResults(interTemporalRaoInput.getRaoInputs(), initialResults, linearOptimizationResults, topologicalOptimizationResults);

        // 7. Log initial and final results
        logCost("[MARMOT] Before global linear optimization: ", initialLinearOptimizationResult, raoParameters, 10);
        logCost("[MARMOT] After global linear optimization: ", linearOptimizationResults, raoParameters, 10);

        return CompletableFuture.completedFuture(mergedRaoResults);
    }

    private TemporalData<PrePerimeterResult> buildInitialResults(TemporalData<RaoResult> topologicalOptimizationResults) {
        TemporalData<PrePerimeterResult> initialResults = new TemporalDataImpl<>();
        topologicalOptimizationResults.getDataPerTimestamp().forEach((timestamp, raoResult) ->
            initialResults.add(timestamp, ((FastRaoResultImpl) raoResult).getInitialResult()));
        return initialResults;
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(TemporalData<RaoInputWithNetworkPaths> raoInputs, RaoParameters raoParameters) {
        Set<String> consideredCnecs = new HashSet<>();
        TemporalData<RaoResult> individualResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((datetime, individualRaoInputWithNetworkPath) -> {
            RaoInput individualRaoInput = RaoInput
                .build(Network.read(individualRaoInputWithNetworkPath.getPostIcsImportNetworkPath()), individualRaoInputWithNetworkPath.getCrac())
                .build();
            String logMessage = "[MARMOT] Running RAO for timestamp %s [{}]".formatted(individualRaoInput.getCrac().getTimestamp().orElseThrow());
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info(logMessage, "start");
            RaoResult raoResult = FastRao.launchFilteredRao(individualRaoInput, raoParameters, null, consideredCnecs);
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info(logMessage, "end");
            individualResults.add(datetime, raoResult);
        });
        return individualResults;
        /*raoInputs.getDataPerTimestamp().values().forEach(raoInput -> {
            RaoUtil.initData(raoInput, raoParameters);
            raoInput.getNetwork().getVariantManager().cloneVariant(raoInput.getNetworkVariantId(), "InitialScenario");
        });
        return raoInputs.map(individualRaoInput -> {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Running RAO for timestamp {}", individualRaoInput.getCrac().getTimestamp().orElseThrow());
            return generateMockRaoResult(individualRaoInput);
        });*/
    }

    private static RaoResult generateMockRaoResult(RaoInput individualRaoInput) {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        Crac crac = individualRaoInput.getCrac();
        if (crac.getTimestamp().get().getHour() == 2) {
            Set<String> actionNames = Set.of("TOP_2NV_DOEL_PRA", "TOP_2N_AUBAN_PRA");
            Set<NetworkAction> actions = crac.getNetworkActions().stream()
                .filter(na -> actionNames.contains(na.getName()))
                .collect(Collectors.toSet());

            when(raoResult.getActivatedNetworkActionsDuringState(individualRaoInput.getCrac().getPreventiveState())).thenReturn(actions);
        } else {
            when(raoResult.getActivatedNetworkActionsDuringState(individualRaoInput.getCrac().getPreventiveState())).thenReturn(new HashSet<>());
        }
        return raoResult;
    }

    private static void applyPreventiveTopologicalActionsOnNetwork(TemporalData<RaoInput> raoInputs, TemporalData<RaoResult> topologicalOptimizationResults) {
        getTopologicalOptimizationResult(raoInputs, topologicalOptimizationResults)
            .getDataPerTimestamp()
            .values()
            .forEach(TopologicalOptimizationResult::applyTopologicalActions);
    }

    private static TemporalData<PrePerimeterResult> runAllInitialPrePerimeterSensitivityAnalysis(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters) {
        return raoInputs.map(individualRaoInput -> runInitialPrePerimeterSensitivityAnalysis(individualRaoInput, raoParameters));
    }

    private static TemporalData<PrePerimeterResult> runAllInitialPrePerimeterSensitivityAnalysis(TemporalData<RaoInput> raoInputs, TemporalData<AppliedRemedialActions> curativeRemedialActions, TemporalData<PrePerimeterResult> initialResults, RaoParameters raoParameters) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getTimestamps().forEach(timestamp ->
            prePerimeterResults.add(timestamp, runInitialPrePerimeterSensitivityAnalysis(
                raoInputs.getData(timestamp).orElseThrow(),
                curativeRemedialActions.getData(timestamp).orElseThrow(),
                initialResults.getData(timestamp).orElseThrow(),
                raoParameters)
        ));
        return prePerimeterResults;
    }

    private static TemporalData<NetworkActionsResult> getPreventiveTopologicalActions(TemporalData<Crac> cracs, TemporalData<RaoResult> raoResults) {
        Map<OffsetDateTime, NetworkActionsResult> preventiveTopologicalActions = new HashMap<>();
        cracs.getTimestamps().forEach(timestamp -> preventiveTopologicalActions.put(timestamp, new NetworkActionsResultImpl(raoResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(cracs.getData(timestamp).orElseThrow().getPreventiveState()))));
        return new TemporalDataImpl<>(preventiveTopologicalActions);
    }

    private static LinearOptimizationResult optimizeLinearRemedialActions(InterTemporalRaoInput raoInput, TemporalData<PrePerimeterResult> initialResults, TemporalData<PrePerimeterResult> prePerimeterResults, RaoParameters parameters, TemporalData<NetworkActionsResult> preventiveTopologicalActions, TemporalData<AppliedRemedialActions> curativeRemedialActions, ObjectiveFunction objectiveFunction) {

        // -- Build IteratingLinearOptimizerInterTemporalInput
        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInput::getCrac), prePerimeterResults);
        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global
        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
        raoInput.getRaoInputs().getTimestamps().forEach(timestamp -> linearOptimizerInputPerTimestamp.put(timestamp, IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getNetwork())
            .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow())
            .withInitialFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPrePerimeterFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPreOptimizationFlowResult(prePerimeterResults.getData(timestamp).orElseThrow())
            .withPrePerimeterSetpoints(prePerimeterResults.getData(timestamp).orElseThrow())
            .withPreOptimizationSensitivityResult(prePerimeterResults.getData(timestamp).orElseThrow())
            .withPreOptimizationAppliedRemedialActions(curativeRemedialActions.getData(timestamp).orElseThrow())
            .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
            .withOutageInstant(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getOutageInstant())
            .withAppliedNetworkActionsInPrimaryState(preventiveTopologicalActions.getData(timestamp).orElseThrow())
            .build()));
        InterTemporalIteratingLinearOptimizerInput interTemporalLinearOptimizerInput = new InterTemporalIteratingLinearOptimizerInput(new TemporalDataImpl<>(linearOptimizerInputPerTimestamp), objectiveFunction, raoInput.getPowerGradients());

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
            .withRaLimitationParameters(new RangeActionLimitationParameters());
        parameters.getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParameters);
        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();

        return InterTemporalIteratingLinearOptimizer.optimize(interTemporalLinearOptimizerInput, linearOptimizerParameters);
    }

    private static boolean doesPrePerimeterSetpointRespectRange(RangeAction<?> rangeAction, RangeActionSetpointResult prePerimeterSetpoints) {
        double preperimeterSetPoint = prePerimeterSetpoints.getSetpoint(rangeAction);
        double minSetPoint = rangeAction.getMinAdmissibleSetpoint(preperimeterSetPoint);
        double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(preperimeterSetPoint);

        if (preperimeterSetPoint < minSetPoint || preperimeterSetPoint > maxSetPoint) {
            BUSINESS_WARNS.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.",
                rangeAction.getId(), preperimeterSetPoint, minSetPoint, maxSetPoint);
            return false;
        } else {
            return true;
        }
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs, TemporalData<PrePerimeterResult> prePerimeterResults) {
        TemporalData<OptimizationPerimeter> optimizationPerimeters = new TemporalDataImpl<>();
        cracs.getTimestamps().forEach(timestamp -> {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            PrePerimeterResult prePerimeterResult = prePerimeterResults.getData(timestamp).orElseThrow();
            optimizationPerimeters.add(timestamp, new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                crac.getFlowCnecs(),
                new HashSet<>(),
                new HashSet<>(),
                crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE).stream().filter(rangeAction -> doesPrePerimeterSetpointRespectRange(rangeAction, prePerimeterResult)).collect(Collectors.toSet())
            ));
        });
        return optimizationPerimeters;
    }

    private static TemporalData<RaoResult> mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> initialResults, LinearOptimizationResult linearOptimizationResults, TemporalData<RaoResult> topologicalOptimizationResults) {
        return getPostOptimizationResults(raoInputs, initialResults, linearOptimizationResults, topologicalOptimizationResults).map(PostOptimizationResult::merge);
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult initialResult, FlowResult globalFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(Crac::getFlowCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            initialResult,
            globalFlowResult,
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private LinearOptimizationResult getInitialLinearOptimizationResult(TemporalData<PrePerimeterResult> prePerimeterResults, TemporalData<NetworkActionsResult> preventiveTopologicalActions, ObjectiveFunction objectiveFunction) {
        return new GlobalLinearOptimizationResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult), prePerimeterResults.map(PrePerimeterResult::getSensitivityResult), prePerimeterResults.map(RangeActionActivationResultImpl::new), preventiveTopologicalActions, objectiveFunction, LinearProblemStatus.OPTIMAL);
    }

    @Override
    public String getName() {
        return INTER_TEMPORAL_RAO;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
