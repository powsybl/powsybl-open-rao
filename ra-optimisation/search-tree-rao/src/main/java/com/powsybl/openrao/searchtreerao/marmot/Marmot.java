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
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.InterTemporalRaoProvider;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPostOptimizationResults;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getTopologicalOptimizationResult;
import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.runInitialPrePerimeterSensitivityAnalysis;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Marmot implements InterTemporalRaoProvider {

    private static final String INTER_TEMPORAL_RAO = "InterTemporalRao";
    private static final String VERSION = "1.0.0";

    @Override
    public CompletableFuture<TemporalData<RaoResult>> run(InterTemporalRaoInput raoInput, RaoParameters raoParameters) {
        // 1. Run independent RAOs to compute optimal preventive topological remedial actions
        TemporalData<RaoResult> topologicalOptimizationResults = runTopologicalOptimization(raoInput.getRaoInputs(), raoParameters);

        // if no inter-temporal constraints are defined, the results can be returned
        if (raoInput.getPowerGradients().isEmpty()) {
            return CompletableFuture.completedFuture(topologicalOptimizationResults);
        }

        // 2. Apply preventive topological remedial actions
        applyPreventiveTopologicalActionsOnNetwork(raoInput.getRaoInputs(), topologicalOptimizationResults);

        // 3. Run initial sensitivity analysis on all timestamps
        TemporalData<PrePerimeterResult> prePerimeterResults = runAllInitialPrePerimeterSensitivityAnalysis(raoInput.getRaoInputs(), raoParameters);

        // 4. Create and iteratively solve MIP to find optimal range actions' set-points
        TemporalData<LinearOptimizationResult> linearOptimizationResults = optimizeLinearRemedialActions(raoInput, prePerimeterResults, raoParameters);

        // 5. Merge topological and linear result
        TemporalData<RaoResult> mergedRaoResults = mergeTopologicalAndLinearOptimizationResults(raoInput.getRaoInputs(), prePerimeterResults, linearOptimizationResults, topologicalOptimizationResults);

        return CompletableFuture.completedFuture(mergedRaoResults);
    }

    private static TemporalData<RaoResult> runTopologicalOptimization(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters) {
        return raoInputs.map(individualRaoInput -> Rao.run(individualRaoInput, raoParameters));
    }

    private static void applyPreventiveTopologicalActionsOnNetwork(TemporalData<RaoInput> raoInputs, TemporalData<RaoResult> topologicalOptimizationResults) {
        getTopologicalOptimizationResult(raoInputs, topologicalOptimizationResults)
            .getDataPerTimestamp()
            .values()
            .forEach(TopologicalOptimizationResult::applyTopologicalActions);
        // TODO: also handle curative remedial actions
    }

    private static TemporalData<PrePerimeterResult> runAllInitialPrePerimeterSensitivityAnalysis(TemporalData<RaoInput> raoInputs, RaoParameters raoParameters) {
        // CONCATENATE RESULTS
        return raoInputs.map(individualRaoInput -> runInitialPrePerimeterSensitivityAnalysis(individualRaoInput, raoParameters));
    }

    private static TemporalData<LinearOptimizationResult> optimizeLinearRemedialActions(InterTemporalRaoInput raoInput, TemporalData<PrePerimeterResult> prePerimeterResults, RaoParameters parameters) {
        // TODO: create MIP with all timestamps and power gradient constraints
        //1) build optimization perimeters
        //2) objective function
        //3) iterating linear optimizer input
        //4) iterating linear optimizer parameters
        // Appel de optimize
        // Linear Problem Builder
        // Connexion des Filler

        // -- BUILD OBJECTIVE FUNCTION
        Set<FlowCnec> preventiveFlowCnecsForAllTimestamps = new HashSet<>();
        raoInput.getRaoInputs().getDataPerTimestamp().entrySet().stream().map(Map.Entry::getValue).map(RaoInput::getCrac).filter(crac -> preventiveFlowCnecsForAllTimestamps.addAll(crac.getFlowCnecs(crac.getPreventiveState())));
        Set<FlowCnec> preventiveLoopFlowCnecsForAllTimestamps = Collections.emptySet();
        // Only used in costly to fetch activated range actions
        Set<State> statesToOptimize = new HashSet<>();

        // TODO : complete class IntertemporalSensitivityResult
        IntertemporalSensitivityResult intertemporalSensitivityResult = new IntertemporalSensitivityResult(prePerimeterResults);
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(preventiveFlowCnecsForAllTimestamps,
            preventiveLoopFlowCnecsForAllTimestamps,
            intertemporalSensitivityResult,
            intertemporalSensitivityResult,
            Collections.emptySet(),
            parameters,
            statesToOptimize);

        // -- BUILD IteratingLinearOptimizerIntertemporalInput
        Set<OptimizationPerimeter> optimizationPerimeters = computeOptimizationPerimeters(raoInput.getRaoInputs().map(RaoInput::getCrac));

        // initialFlowResult, prePerimeterFlowResult, PreOptimizationFlowResult = prePerimeterResults
        // TODO : create a temporalData of InteratingLinearOptimizerInput instead this
        // TODO : withRaActivationFromParentLeaf not defined, check this is ok
        // TODO : withAppliedNetworkActionsInPrimaryState not defined, check this is ok
        // TODO : withOutageInstant : why not directly write integer value (in this case, not a paremeter)
        IteratingLinearOptimizerIntertemporalInput linearOptimizerInput = IteratingLinearOptimizerIntertemporalInput.create()
            .withNetwork(raoInput.getRaoInputs().map(RaoInput::getNetwork)) // TemporalData<Network>
            .withOptimizationPerimeter(optimizationPerimeters)
            .withInitialFlowResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult)) // TemporalData<FlowResult>
            .withPrePerimeterFlowResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult))
            .withPreOptimizationFlowResult(prePerimeterResults.map(PrePerimeterResult::getFlowResult))
            .withPrePerimeterSetpoints(prePerimeterResults.map(PrePerimeterResult::getRangeActionSetpointResult)) // TemporalData<RangeActionSetpointResult>
            .withPreOptimizationSensitivityResult(prePerimeterResults.map(PrePerimeterResult::getSensitivityResult))
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(raoInput.getRaoInputs().map(individualRaoInput -> ToolProvider.buildFromRaoInputAndParameters(individualRaoInput, parameters))) // TemporalData<ToolProvider>
            .withOutageInstant(searchTreeInput.getOutageInstant())
            .build();

        // build parameters
        // TODO : adapt, use parameters (RaoParameters), here these are search tree parameters)
        IteratingLinearOptimizerParameters linearOptimizerParameters = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(parameters.getObjectiveFunction())
            .withRangeActionParameters(parameters.getRangeActionParameters())
            .withMnecParameters(parameters.getMnecParameters())
            .withMaxMinRelativeMarginParameters(parameters.getMaxMinRelativeMarginParameters())
            .withLoopFlowParameters(parameters.getLoopFlowParameters())
            .withUnoptimizedCnecParameters(parameters.getUnoptimizedCnecParameters())
            .withRaLimitationParameters(getRaLimitationParameters(searchTreeInput.getOptimizationPerimeter(), parameters))
            .withSolverParameters(parameters.getSolverParameters())
            .withMaxNumberOfIterations(parameters.getMaxNumberOfIterations())
            .withRaRangeShrinking(parameters.getTreeParameters().raRangeShrinking())
            .build();

        // TODO include work done on ProblemFillerHelper, taking into account LinearProblemBuilder functionalities :
        // default method for non intertemporal, and for intertemporal deduce from parameters and input necessary fillers
        // au final : no new class for LinearBuilder
        // TODO : include this in IntertemporalLinearProblemBuilder
        IntertemporalIteratingLinearOptimizer.optimize(input, parameters, cracs.get(0).getOutageInstant());

        // TODO : create pseudo Leaf class fetching results from optimize in pair programming

        return new TemporalDataImpl<LinearOptimizationResult>();
    }

    private static Set<OptimizationPerimeter> computeOptimizationPerimeters(TemporalData<Crac> cracs) {
        return cracs.getDataPerTimestamp().values().stream().map(crac -> new PreventiveOptimizationPerimeter(
            crac.getPreventiveState(),
            MarmotUtils.getPreventivePerimeterCnecs(crac),
            new HashSet<>(),
            new HashSet<>(),
            crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE))).collect(Collectors.toSet());
    }

    private static TemporalData<RaoResult> mergeTopologicalAndLinearOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> prePerimeterResults, TemporalData<LinearOptimizationResult> linearOptimizationResults, TemporalData<RaoResult> topologicalOptimizationResults) {
        // TODO: add curative RAs (range action and topological)
        return getPostOptimizationResults(raoInputs, prePerimeterResults, linearOptimizationResults, topologicalOptimizationResults).map(PostOptimizationResult::merge);
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
