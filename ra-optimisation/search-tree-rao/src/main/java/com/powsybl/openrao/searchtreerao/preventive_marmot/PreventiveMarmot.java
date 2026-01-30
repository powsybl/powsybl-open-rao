/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.preventive_marmot;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.marmot.InterTemporalIteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.marmot.InterTemporalIteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.marmot.results.InterTemporalRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.jgrapht.alg.util.Pair;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.logCost;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class PreventiveMarmot implements InterTemporalRaoProvider {

    private static final String INITIAL_SCENARIO = "InitialScenario";
    private static final String MIP_SCENARIO = "MipScenario";

    @Override
    public String getName() {
        return "PreventiveMarmot";
    }

    public Pair<TemporalData<PrePerimeterResult>, TemporalData<Set<FlowCnec>>> buildInitialResultsFromNetworks(InterTemporalRaoInput interTemporalRaoInput, RaoParameters raoParameters) {
        Map<OffsetDateTime, PrePerimeterResult> results = new LinkedHashMap<>();
        Map<OffsetDateTime, Set<FlowCnec>> allCnecs = new LinkedHashMap<>();
        for (OffsetDateTime ts : interTemporalRaoInput.getTimestampsToRun()) {
            Crac crac = interTemporalRaoInput.getRaoInputs().getData(ts).orElseThrow().getCrac();
            Network network = interTemporalRaoInput.getRaoInputs().getData(ts).orElseThrow().getNetwork();
            Set<FlowCnec> cnecs = crac.getFlowCnecs(crac.getPreventiveState());
            if (crac.getOutageInstant() != null) {
                crac.getStates(crac.getOutageInstant()).forEach(state -> cnecs.addAll(crac.getFlowCnecs(state)));
            }
            ToolProvider toolProvider = ToolProvider.create().withNetwork(network).withRaoParameters(raoParameters).build();
            PrePerimeterResult result = new PrePerimeterSensitivityAnalysis(crac, cnecs, crac.getRangeActions(crac.getPreventiveState()), raoParameters, toolProvider).runInitialSensitivityAnalysis(network);
            results.put(ts, result);
            allCnecs.put(ts, cnecs);
        }
        return Pair.of(new TemporalDataImpl<>(results), new TemporalDataImpl<>(allCnecs));
    }

    @Override
    public CompletableFuture<InterTemporalRaoResult> run(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths, RaoParameters raoParameters) {

        InterTemporalRaoInput interTemporalRaoInput = importNetworksFromInterTemporalRaoInputWithNetworkPaths(interTemporalRaoInputWithNetworkPaths);

        Pair<TemporalData<PrePerimeterResult>, TemporalData<Set<FlowCnec>>> pair = buildInitialResultsFromNetworks(interTemporalRaoInput, raoParameters);
        TemporalData<PrePerimeterResult> initialResults = pair.getFirst();
        TemporalData<Set<FlowCnec>> consideredCnecs = pair.getSecond();

        ObjectiveFunction fullObjectiveFunction = buildGlobalObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), raoParameters);
        LinearOptimizationResult initialObjectiveFunctionResult = getInitialObjectiveFunctionResult(initialResults, fullObjectiveFunction);
        logCost("Before optimization: ", initialObjectiveFunctionResult, raoParameters, 10);

        TemporalData<PrePerimeterResult> loadFlowResults;
        GlobalLinearOptimizationResult linearOptimizationResults;
        GlobalLinearOptimizationResult fullResults;

        // Clone the PostTopoScenario variant to make sure we work on a clean variant every time
        interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().values().forEach(raoInput -> {
            raoInput.getNetwork().getVariantManager().cloneVariant(INITIAL_SCENARIO, MIP_SCENARIO, true);
            raoInput.getNetwork().getVariantManager().setWorkingVariant(MIP_SCENARIO);
        });

        TemporalData<RangeActionSetpointResult> initialSetpointResults = getInitialSetpointResults(initialResults, interTemporalRaoInput.getRaoInputs());

        ObjectiveFunction filteredObjectiveFunction = buildFilteredObjectiveFunction(interTemporalRaoInput.getRaoInputs().map(RaoInput::getCrac), new GlobalFlowResult(initialResults), raoParameters, consideredCnecs);

        TECHNICAL_LOGS.info("Global range actions optimization [start]");
        linearOptimizationResults = optimizeLinearRemedialActions(interTemporalRaoInput, initialResults, initialSetpointResults, initialResults, raoParameters, consideredCnecs, filteredObjectiveFunction);
        TECHNICAL_LOGS.info("Global range actions optimization [end]");

        loadFlowResults = applyActionsAndRunFullLoadflow(interTemporalRaoInput.getRaoInputs(), linearOptimizationResults, initialResults, raoParameters);

        TemporalData<NetworkActionsResult> preventiveTopologicalActions = new TemporalDataImpl<>(interTemporalRaoInput.getTimestampsToRun().stream().collect(Collectors.toMap(Function.identity(), ts -> new NetworkActionsResultImpl(Map.of()))));
        TemporalData<RangeActionActivationResult> rangeActionActivationResultTemporalData = linearOptimizationResults.getRangeActionActivationResultTemporalData();
        fullResults = new GlobalLinearOptimizationResult(loadFlowResults, loadFlowResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResultTemporalData, preventiveTopologicalActions, fullObjectiveFunction, LinearProblemStatus.OPTIMAL);

        logCost("[MARMOT] next iteration of MIP: ", fullResults, raoParameters, 10);

        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end]");

        // 7. Merge topological and linear result
        TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");
        InterTemporalRaoResultImpl interTemporalRaoResult = mergeTopologicalAndLinearOptimizationResults(interTemporalRaoInput.getRaoInputs(), initialResults, initialObjectiveFunctionResult, fullResults, raoParameters);

        // 8. Log initial and final results
        logCost("[MARMOT] Before topological optimizations: ", initialObjectiveFunctionResult, raoParameters, 10);
        logCost("[MARMOT] After global linear optimization: ", fullResults, raoParameters, 10);

        return CompletableFuture.completedFuture(interTemporalRaoResult);
    }

    private TemporalData<RangeActionSetpointResult> getInitialSetpointResults(TemporalData<PrePerimeterResult> postTopologicalActionsResults, TemporalData<RaoInput> raoInputs) {
        TemporalData<RangeActionSetpointResult> initialSetpointResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            Map<RangeAction<?>, Double> setPointMap = new HashMap<>();
            raoInput.getCrac().getRangeActions().forEach(rangeAction -> setPointMap.put(rangeAction, postTopologicalActionsResults.getData(timestamp).orElseThrow().getSetpoint(rangeAction)));
            RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(setPointMap);
            initialSetpointResults.put(timestamp, rangeActionSetpointResult);
        });
        return initialSetpointResults;
    }

    private static TemporalData<PrePerimeterResult> applyActionsAndRunFullLoadflow(TemporalData<RaoInput> raoInputs, LinearOptimizationResult filteredResult, TemporalData<PrePerimeterResult> initialResults, RaoParameters raoParameters) {
        TemporalData<PrePerimeterResult> prePerimeterResults = new TemporalDataImpl<>();
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            // duplicate the postTopoScenario variant and switch to the new clone
            raoInput.getNetwork().getVariantManager().cloneVariant(INITIAL_SCENARIO, "PostPreventiveScenario", true);
            raoInput.getNetwork().getVariantManager().setWorkingVariant("PostPreventiveScenario");
            State preventiveState = raoInput.getCrac().getPreventiveState();
            raoInput.getCrac().getRangeActions(preventiveState).forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), filteredResult.getOptimizedSetpoint(rangeAction, preventiveState)));
            prePerimeterResults.put(timestamp, runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(raoInputs.getData(timestamp).orElseThrow(), new AppliedRemedialActions(), initialResults.getData(timestamp).orElseThrow(), raoParameters));
            // switch back to the postTopoScenario to avoid keeping applied range actions when entering the MIP
            raoInput.getNetwork().getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
        });
        return prePerimeterResults;
    }

    private InterTemporalRaoInput importNetworksFromInterTemporalRaoInputWithNetworkPaths(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths) {
        return new InterTemporalRaoInput(interTemporalRaoInputWithNetworkPaths.getRaoInputs().map(raoInputWithNetworksPath -> {
            RaoInput raoInput = raoInputWithNetworksPath.toRaoInputWithPostIcsImportNetworkPath();
            raoInput.getNetwork().getVariantManager().cloneVariant(raoInput.getNetworkVariantId(), INITIAL_SCENARIO);
            return raoInput;
        }), interTemporalRaoInputWithNetworkPaths.getTimestampsToRun(), interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints());
    }

    private static GlobalLinearOptimizationResult optimizeLinearRemedialActions(InterTemporalRaoInput raoInput, TemporalData<PrePerimeterResult> initialResults, TemporalData<RangeActionSetpointResult> initialSetpoints, TemporalData<PrePerimeterResult> postTopologicalActionsResults, RaoParameters parameters, TemporalData<Set<FlowCnec>> consideredCnecs, ObjectiveFunction objectiveFunction) {

        // -- Build IteratingLinearOptimizerInterTemporalInput
        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInput::getCrac), consideredCnecs);
        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global

        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
        raoInput.getRaoInputs().getTimestamps().forEach(timestamp ->
            linearOptimizerInputPerTimestamp.put(timestamp,
                IteratingLinearOptimizerInput.create()
                    .withNetwork(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getNetwork())
                    .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow().copyWithFilteredAvailableHvdcRangeAction(raoInput.getRaoInputs().getData(timestamp).get().getNetwork()))
                    .withInitialFlowResult(initialResults.getData(timestamp).orElseThrow())
                    .withPrePerimeterFlowResult(initialResults.getData(timestamp).orElseThrow())
                    .withPreOptimizationFlowResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
                    .withPrePerimeterSetpoints(initialSetpoints.getData(timestamp).orElseThrow())
                    .withPreOptimizationSensitivityResult(postTopologicalActionsResults.getData(timestamp).orElseThrow())
                    .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
                    .withAppliedNetworkActionsInPrimaryState(new NetworkActionsResultImpl(Map.of()))
                    .withOutageInstant(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getOutageInstant()).build()));
        InterTemporalIteratingLinearOptimizerInput interTemporalLinearOptimizerInput = new InterTemporalIteratingLinearOptimizerInput(
            new TemporalDataImpl<>(linearOptimizerInputPerTimestamp), objectiveFunction, raoInput.getIntertemporalConstraints());

        // Build parameters
        // Unoptimized cnec parameters ignored because only PRAs
        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder linearOptimizerParametersBuilder = IteratingLinearOptimizerParameters.create().withObjectiveFunction(parameters.getObjectiveFunctionParameters().getType()).withFlowUnit(getFlowUnit(parameters)).withRangeActionParameters(parameters.getRangeActionsOptimizationParameters()).withRangeActionParametersExtension(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters()).withMaxNumberOfIterations(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getMaxMipIterations()).withRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()) || SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking())).withSolverParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver()).withMaxMinRelativeMarginParameters(parameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class)).withRaLimitationParameters(new RangeActionLimitationParameters()).withMinMarginParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().orElse(new SearchTreeRaoCostlyMinMarginParameters()));
        parameters.getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParametersExtension);
        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
        parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParametersExtension);
        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();

        return InterTemporalIteratingLinearOptimizer.optimize(interTemporalLinearOptimizerInput, linearOptimizerParameters);
    }

    private static TemporalData<OptimizationPerimeter> computeOptimizationPerimetersPerTimestamp(TemporalData<Crac> cracs, TemporalData<Set<FlowCnec>> consideredCnecs) {
        TemporalData<OptimizationPerimeter> optimizationPerimeters = new TemporalDataImpl<>();
        cracs.getTimestamps().forEach(timestamp -> {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            optimizationPerimeters.put(timestamp, new PreventiveOptimizationPerimeter(crac.getPreventiveState(), consideredCnecs.getData(timestamp).orElseThrow(), new HashSet<>(), // no loopflows for now
                new HashSet<>(), // don't re-optimize topological actions in Marmot
                crac.getRangeActions(crac.getPreventiveState())));
        });
        return optimizationPerimeters;
    }

    private static InterTemporalRaoResultImpl mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> initialResults, ObjectiveFunctionResult initialLinearOptimizationResult, GlobalLinearOptimizationResult globalLinearOptimizationResult, RaoParameters raoParameters) {
        TemporalData<RaoResult> topologicalOptimizationResults = new TemporalDataImpl<>(Map.of());
        for (OffsetDateTime ts : raoInputs.getTimestamps()) {
            topologicalOptimizationResults.put(ts, new FailedRaoResultImpl("skipped topo RAO"));
        }
        return new InterTemporalRaoResultImpl(initialLinearOptimizationResult, globalLinearOptimizationResult, getPostOptimizationResults(raoInputs, initialResults, globalLinearOptimizationResult, topologicalOptimizationResults, raoParameters));
    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(MarmotUtils::getPreventivePerimeterCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(allFlowCnecs, new HashSet<>(), // no loop flows for now
            globalInitialFlowResult, globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(), raoParameters, allOptimizedStates);
    }

    private static ObjectiveFunction buildFilteredObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalInitialFlowResult, RaoParameters raoParameters, TemporalData<Set<FlowCnec>> consideredCnecs) {
        Set<FlowCnec> flatConsideredCnecs = new HashSet<>();
        consideredCnecs.getDataPerTimestamp().values().forEach(flatConsideredCnecs::addAll);

        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(flatConsideredCnecs, new HashSet<>(), // no loop flows for now
            globalInitialFlowResult, globalInitialFlowResult, // always building from preventive so prePerimeter = initial
            Collections.emptySet(), raoParameters, allOptimizedStates);
    }

    private LinearOptimizationResult getInitialObjectiveFunctionResult(TemporalData<PrePerimeterResult> prePerimeterResults, ObjectiveFunction objectiveFunction) {
        TemporalData<RangeActionActivationResult> rangeActionActivationResults = prePerimeterResults.map(RangeActionActivationResultImpl::new);
        TemporalData<NetworkActionsResult> networkActionsResults = new TemporalDataImpl<>();
        return new GlobalLinearOptimizationResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult), prePerimeterResults.map(PrePerimeterResult::getSensitivityResult), rangeActionActivationResults, networkActionsResults, objectiveFunction, LinearProblemStatus.OPTIMAL);
    }
}
