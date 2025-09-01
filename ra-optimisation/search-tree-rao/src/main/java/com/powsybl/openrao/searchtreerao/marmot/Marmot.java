/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.InterTemporalRaoProvider;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
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
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.marmot.results.InterTemporalRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.logCost;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPostOptimizationResults;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPreventiveOptimizationResults;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runSensitivityAnalysis;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runSensitivityAnalysisBasedOnInitialResult;

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
    public CompletableFuture<InterTemporalRaoResult> run(InterTemporalRaoInput raoInput, RaoParameters raoParameters) {
        // 1. Run initial sensitivity analysis on all timestamps
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [start]");
        TemporalData<PrePerimeterResult> initialResults = runAllSensitivityAnalyses(raoInput.getRaoInputs(), raoParameters);
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [end]");

        // 2. Run independent RAOs to compute optimal preventive topological remedial actions
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [start]");
        TemporalData<RaoResult> topologicalOptimizationResults = runTopologicalOptimization(raoInput.getRaoInputs(), raoParameters);
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [end]");

        // TODO : Add intertemporal constraint check if none violated then return
        boolean noInterTemporalConstraint = raoInput.getGeneratorConstraints().isEmpty();

        // 3. Apply preventive topological remedial actions (and preventive range actions if there are no inter-temporal constraints)
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Applying optimal topological actions on networks");
        ObjectiveFunction objectiveFunction = buildGlobalObjectiveFunction(raoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults.map(PrePerimeterResult::getFlowResult)), new GlobalFlowResult(initialResults.map(PrePerimeterResult::getFlowResult)), raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, objectiveFunction);
        applyPreventiveRemedialActionsOnNetwork(raoInput.getRaoInputs(), topologicalOptimizationResults, noInterTemporalConstraint);

        // 4. Run sensitivity analysis on all timestamps after topological actions are applied
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis after topological actions application [start]");
        TemporalData<PrePerimeterResult> postTopologicalActionsResults = runAllSensitivityAnalysesBasedOnInitialResult(raoInput.getRaoInputs(), raoParameters, initialResults.map(PrePerimeterResult::getFlowResult));
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis after topological actions application [end]");

        // 5. Evaluate objective function after topological actions application result
        TemporalData<NetworkActionsResult> preventiveTopologicalActions = getPreventiveTopologicalActions(raoInput.getRaoInputs().map(RaoInput::getCrac), topologicalOptimizationResults);
        LinearOptimizationResult postTopologicalOptimizationResult = getPostTopologicalOptimizationResult(initialResults.map(PrePerimeterResult::getRangeActionSetpointResult), postTopologicalActionsResults, objectiveFunction, topologicalOptimizationResults, raoInput.getRaoInputs().map(individualRaoInput -> individualRaoInput.getCrac().getPreventiveState()));

        // if no inter-temporal constraints are defined, the results can be returned
        if (noInterTemporalConstraint) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] No inter-temporal constraint provided; no need to re-optimize range actions");
            return CompletableFuture.completedFuture(new InterTemporalRaoResultImpl(initialObjectiveFunctionResult, postTopologicalOptimizationResult, topologicalOptimizationResults));
        }

        // 6. Create and iteratively solve MIP to find optimal range actions' set-points
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start]");
        GlobalLinearOptimizationResult globalLinearOptimizationResult = optimizeLinearRemedialActions(raoInput, initialResults, postTopologicalActionsResults, raoParameters, preventiveTopologicalActions, objectiveFunction);
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end]");

        // 7. Merge topological and linear result
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");
        InterTemporalRaoResultImpl interTemporalRaoResult = mergeTopologicalAndLinearOptimizationResults(raoInput.getRaoInputs(), initialResults, initialObjectiveFunctionResult, globalLinearOptimizationResult, topologicalOptimizationResults, raoParameters);

        // 8. Log initial and final results
        logCost("[MARMOT] Before topological optimizations: ", initialObjectiveFunctionResult, raoParameters, 10);
        logCost("[MARMOT] Before global linear optimization: ", postTopologicalOptimizationResult, raoParameters, 10);
        logCost("[MARMOT] After global linear optimization: ", globalLinearOptimizationResult, raoParameters, 10);

        return CompletableFuture.completedFuture(interTemporalRaoResult);
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters) {
        return raoInputs.map(individualRaoInput -> {
            String logMessage = "[MARMOT] Running RAO for timestamp %s [{}]".formatted(individualRaoInput.getCrac().getTimestamp().orElseThrow());
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info(logMessage, "start");
            RaoResult raoResult = Rao.find("SearchTreeRao").run(individualRaoInput, raoParameters);
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info(logMessage, "end");
            return raoResult;
        });
    }

    private static void applyPreventiveRemedialActionsOnNetwork(TemporalData<RaoInput> raoInputs, TemporalData<RaoResult> preventiveOptimizationResults, boolean applyPreventiveRangeActions) {
        getPreventiveOptimizationResults(raoInputs, preventiveOptimizationResults)
            .getDataPerTimestamp()
            .values()
            .forEach(preventiveOptimizationResult -> preventiveOptimizationResult.applyPreventiveRemedialActions(applyPreventiveRangeActions));
    }

    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalyses(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters) {
        return raoInputs.map(individualRaoInput -> runSensitivityAnalysis(individualRaoInput, raoParameters));
    }

    private static TemporalData<PrePerimeterResult> runAllSensitivityAnalysesBasedOnInitialResult(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters, TemporalData<FlowResult> initialFlowResults) {
        return new TemporalDataImpl<>(raoInputs.getDataPerTimestamp().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> runSensitivityAnalysisBasedOnInitialResult(entry.getValue(), raoParameters, initialFlowResults.getData(entry.getKey()).orElseThrow()))));
    }

    private static TemporalData<NetworkActionsResult> getPreventiveTopologicalActions(TemporalData<Crac> cracs, TemporalData<RaoResult> raoResults) {
        Map<OffsetDateTime, NetworkActionsResult> preventiveTopologicalActions = new HashMap<>();
        cracs.getTimestamps().forEach(timestamp -> {
            State preventiveState = cracs.getData(timestamp).orElseThrow().getPreventiveState();
            preventiveTopologicalActions.put(timestamp, new NetworkActionsResultImpl(Map.of(preventiveState, raoResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(preventiveState))));
        });
        return new TemporalDataImpl<>(preventiveTopologicalActions);
    }

    private static GlobalLinearOptimizationResult optimizeLinearRemedialActions(InterTemporalRaoInput raoInput, TemporalData<PrePerimeterResult> initialResults, TemporalData<PrePerimeterResult> postTopologicalActionsResults, RaoParameters parameters, TemporalData<NetworkActionsResult> preventiveTopologicalActions, ObjectiveFunction objectiveFunction) {

        // -- Build IteratingLinearOptimizerInterTemporalInput
        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInput::getCrac));
        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global
        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
        raoInput.getRaoInputs().getTimestamps().forEach(timestamp -> linearOptimizerInputPerTimestamp.put(timestamp, IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getNetwork())
            .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow())
            .withInitialFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPrePerimeterFlowResult(initialResults.getData(timestamp).orElseThrow())
            .withPreOptimizationFlowResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
            .withPrePerimeterSetpoints(initialResults.getData(timestamp).orElseThrow())
            .withPreOptimizationSensitivityResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
            .withOutageInstant(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getOutageInstant())
            .withAppliedNetworkActionsInPrimaryState(preventiveTopologicalActions.getData(timestamp).orElseThrow())
            .build()));
        InterTemporalIteratingLinearOptimizerInput interTemporalLinearOptimizerInput = new InterTemporalIteratingLinearOptimizerInput(new TemporalDataImpl<>(linearOptimizerInputPerTimestamp), objectiveFunction, raoInput.getGeneratorConstraints());

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
        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();

        return InterTemporalIteratingLinearOptimizer.optimize(interTemporalLinearOptimizerInput, linearOptimizerParameters);
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs) {
        return cracs.map(
            crac -> new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                MarmotUtils.getPreventivePerimeterCnecs(crac),
                new HashSet<>(),
                new HashSet<>(),
                crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE)
            )
        );
    }

    private static InterTemporalRaoResultImpl mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> initialResults, ObjectiveFunctionResult initialLinearOptimizationResult, GlobalLinearOptimizationResult globalLinearOptimizationResult, TemporalData<RaoResult> topologicalOptimizationResults, RaoParameters raoParameters) {
        return new InterTemporalRaoResultImpl(initialLinearOptimizationResult, globalLinearOptimizationResult, getPostOptimizationResults(raoInputs, initialResults, globalLinearOptimizationResult, topologicalOptimizationResults, raoParameters).map(PostOptimizationResult::merge));
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, FlowResult globalPrePerimeterFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(MarmotUtils::getPreventivePerimeterCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            globalInitialFlowResult,
            globalPrePerimeterFlowResult,
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
    }

    private LinearOptimizationResult getInitialObjectiveFunctionResult(TemporalData<PrePerimeterResult> prePerimeterResults, ObjectiveFunction objectiveFunction) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = prePerimeterResults.map(RangeActionActivationResultImpl::new);
        TemporalData<NetworkActionsResult> networkActionsResults = new TemporalDataImpl<>();
        return new GlobalLinearOptimizationResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult), prePerimeterResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL);
    }

    private LinearOptimizationResult getPostTopologicalOptimizationResult(TemporalData<RangeActionSetpointResult> allInitialSetPoints, TemporalData<PrePerimeterResult> prePerimeterResults, ObjectiveFunction objectiveFunction, TemporalData<RaoResult> topologicalOptimizationResults, TemporalData<State> preventiveStates) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = getRangeActionActivationResults(allInitialSetPoints, topologicalOptimizationResults, preventiveStates);
        TemporalData<NetworkActionsResult> networkActionsResults = getNetworkActionActivationResults(topologicalOptimizationResults, preventiveStates);
        return new GlobalLinearOptimizationResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult), prePerimeterResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL);
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

    @Override
    public String getVersion() {
        return VERSION;
    }
}
