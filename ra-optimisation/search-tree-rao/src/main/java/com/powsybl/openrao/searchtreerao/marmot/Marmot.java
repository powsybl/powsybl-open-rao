/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
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
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

//import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPostOptimizationResults;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getTopologicalOptimizationResult;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runInitialPrePerimeterSensitivityAnalysis;

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
    public CompletableFuture<TemporalData<RaoResult>> run(InterTemporalRaoInputWithNetworkPaths raoInputWithNetworkPaths, RaoParameters raoParameters) {
        // 1. Run independent RAOs to compute optimal preventive topological remedial actions
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization");
        runTopologicalOptimization(raoInputWithNetworkPaths.getRaoInputs(), raoParameters);

        // if no inter-temporal constraints are defined, the results can be returned
        if (raoInputWithNetworkPaths.getPowerGradients().isEmpty()) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] No inter-temporal constraint provided; no need to re-optimize range actions");
            return CompletableFuture.completedFuture(topologicalOptimizationResults);
        }

        // 2. Apply preventive topological remedial actions
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Applying optimal topological actions on networks");
        applyPreventiveTopologicalActionsOnNetwork(raoInputWithNetworkPaths.getRaoInputs(), topologicalOptimizationResults);

        // TODO no working variant has been set
//        private static final String INITIAL_SCENARIO = "InitialScenario_with_topological_actions";
//        network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);

//        // 3. Run initial sensitivity analysis on all timestamps
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [start]");
        TemporalData<PrePerimeterResult> prePerimeterResults = runAllInitialPrePerimeterSensitivityAnalysis(raoInputWithNetworkPaths.getRaoInputs(), raoParameters);
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [end]");

//        // 4. Create and iteratively solve MIP to find optimal range actions' set-points
//        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] ----- Inter-temporal MIP");
//        LinearOptimizationResult linearOptimizationResults = optimizeLinearRemedialActions(raoInputWithNetworkPaths, prePerimeterResults, raoParameters, getPreventiveTopologicalActions(raoInputWithNetworkPaths.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), topologicalOptimizationResults));
//
//        // 5. Merge topological and linear result
//        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");
//        TemporalData<RaoResult> mergedRaoResults = mergeTopologicalAndLinearOptimizationResults(raoInputWithNetworkPaths.getRaoInputs(), prePerimeterResults, linearOptimizationResults, topologicalOptimizationResults);

        return CompletableFuture.completedFuture(null);
    }

    private static void runTopologicalOptimization(TemporalData<RaoInputWithNetworkPaths> raoInputs, RaoParameters raoParameters) {
        Set<String> consideredCnecs = new HashSet<>();
        raoInputs.getDataPerTimestamp().values().forEach(individualRaoInputWithNetworkPath -> {
            RaoInput individualRaoInput = RaoInput.build(Network.read(individualRaoInputWithNetworkPath.getPostIcsImportNetworkPath()), individualRaoInputWithNetworkPath.getCrac()).build();
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] Running RAO for timestamp {}", individualRaoInput.getCrac().getTimestamp().orElseThrow());
            FastRao.launchFilteredRao(individualRaoInput, raoParameters, null, consideredCnecs);
        });
    }

    private static void applyPreventiveTopologicalActionsOnNetwork(TemporalData<RaoInputWithNetworkPaths> raoInputs, TemporalData<RaoResult> topologicalOptimizationResults) {
        getTopologicalOptimizationResult(raoInputs, topologicalOptimizationResults)
            .getDataPerTimestamp()
            .values()
            .forEach(TopologicalOptimizationResult::applyTopologicalActions);
    }

    private static TemporalData<PrePerimeterResult> runAllInitialPrePerimeterSensitivityAnalysis(TemporalData<RaoInputWithNetworkPaths> raoInputs, RaoParameters raoParameters) {
        return raoInputs.map(individualRaoInput -> runInitialPrePerimeterSensitivityAnalysis(individualRaoInput, raoParameters));
    }

    private static TemporalData<NetworkActionsResult> getPreventiveTopologicalActions(TemporalData<Crac> cracs, TemporalData<RaoResult> raoResults) {
        Map<OffsetDateTime, NetworkActionsResult> preventiveTopologicalActions = new HashMap<>();
        cracs.getTimestamps().forEach(timestamp -> preventiveTopologicalActions.put(timestamp, new NetworkActionsResultImpl(raoResults.getData(timestamp).orElseThrow().getActivatedNetworkActionsDuringState(cracs.getData(timestamp).orElseThrow().getPreventiveState()))));
        return new TemporalDataImpl<>(preventiveTopologicalActions);
    }
//
//    private static LinearOptimizationResult optimizeLinearRemedialActions(InterTemporalRaoInputWithNetworkPaths raoInput, TemporalData<PrePerimeterResult> prePerimeterResults, RaoParameters parameters, TemporalData<NetworkActionsResult> preventiveTopologicalActions) {
//        // -- Build objective function
//        ObjectiveFunction objectiveFunction = buildGlobalObjectiveFunction(raoInput.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), new GlobalFlowResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult)), parameters);
//// TODO  : load network.
//        // -- Build IteratingLinearOptimizerInterTemporalInput
//        TemporalData<OptimizationPerimeter> optimizationPerimeterPerTimestamp = computeOptimizationPerimetersPerTimestamp(raoInput.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac));
//        // no objective function defined in individual IteratingLinearOptimizerInputs as it is global
//        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
//        raoInput.getRaoInputs().getTimestamps().forEach(timestamp -> linearOptimizerInputPerTimestamp.put(timestamp, IteratingLinearOptimizerInput.create()
//            .withNetwork(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getNetwork())
//            .withOptimizationPerimeter(optimizationPerimeterPerTimestamp.getData(timestamp).orElseThrow())
//            .withInitialFlowResult(prePerimeterResults.getData(timestamp).orElseThrow())
//            .withPrePerimeterFlowResult(prePerimeterResults.getData(timestamp).orElseThrow())
//            .withPreOptimizationFlowResult(prePerimeterResults.getData(timestamp).orElseThrow())
//            .withPrePerimeterSetpoints(prePerimeterResults.getData(timestamp).orElseThrow())
//            .withPreOptimizationSensitivityResult(prePerimeterResults.getData(timestamp).orElseThrow())
//            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
//            .withToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput.getRaoInputs().getData(timestamp).orElseThrow(), parameters))
//            .withOutageInstant(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getOutageInstant())
//            .withAppliedNetworkActionsInPrimaryState(preventiveTopologicalActions.getData(timestamp).orElseThrow())
//            .build()));
//        InterTemporalIteratingLinearOptimizerInput interTemporalLinearOptimizerInput = new InterTemporalIteratingLinearOptimizerInput(new TemporalDataImpl<>(linearOptimizerInputPerTimestamp), objectiveFunction, raoInput.getPowerGradients());
//
//        // Build parameters
//        // Unoptimized cnec parameters ignored because only PRAs
//        // TODO: define static method to define Ra Limitation Parameters from crac and topos (mutualize with search tree) : SearchTreeParameters::decreaseRemedialActionsUsageLimits
//        IteratingLinearOptimizerParameters.LinearOptimizerParametersBuilder linearOptimizerParametersBuilder = IteratingLinearOptimizerParameters.create()
//            .withObjectiveFunction(parameters.getObjectiveFunctionParameters().getType())
//            .withObjectiveFunctionUnit(parameters.getObjectiveFunctionParameters().getUnit())
//            .withRangeActionParameters(parameters.getRangeActionsOptimizationParameters())
//            .withRangeActionParametersExtension(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters())
//            .withMaxNumberOfIterations(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getMaxMipIterations())
//            .withRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()) || SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO.equals(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking()))
//            .withSolverParameters(parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
//            .withMaxMinRelativeMarginParameters(parameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class))
//            .withRaLimitationParameters(new RangeActionLimitationParameters());
//        parameters.getMnecParameters().ifPresent(linearOptimizerParametersBuilder::withMnecParameters);
//        parameters.getLoopFlowParameters().ifPresent(linearOptimizerParametersBuilder::withLoopFlowParameters);
//        IteratingLinearOptimizerParameters linearOptimizerParameters = linearOptimizerParametersBuilder.build();
//
//        // TODO: include applied topo PRAs to compute total activation cost?
//        return InterTemporalIteratingLinearOptimizer.optimize(interTemporalLinearOptimizerInput, linearOptimizerParameters);
//    }

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

//    private static TemporalData<RaoResult> mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInputWithNetworkPaths> raoInputsWithNetworkPaths, TemporalData<PrePerimeterResult> prePerimeterResults, LinearOptimizationResult linearOptimizationResults, TemporalData<RaoResult> topologicalOptimizationResults) {
//        return getPostOptimizationResults(raoInputsWithNetworkPaths, prePerimeterResults, linearOptimizationResults, topologicalOptimizationResults).map(PostOptimizationResult::merge);
//    }

    private static ObjectiveFunction buildGlobalObjectiveFunction(TemporalData<Crac> cracs, FlowResult globalFlowResult, RaoParameters raoParameters) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        cracs.map(MarmotUtils::getPreventivePerimeterCnecs).getDataPerTimestamp().values().forEach(allFlowCnecs::addAll);
        Set<State> allOptimizedStates = new HashSet<>(cracs.map(Crac::getPreventiveState).getDataPerTimestamp().values());
        return ObjectiveFunction.build(allFlowCnecs,
            new HashSet<>(), // no loop flows for now
            globalFlowResult,
            globalFlowResult,
            Collections.emptySet(),
            raoParameters,
            allOptimizedStates);
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
