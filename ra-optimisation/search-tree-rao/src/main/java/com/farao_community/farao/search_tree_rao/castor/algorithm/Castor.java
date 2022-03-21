/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.loopflow_computation.LoopFlowComputationWithXnodeGlskHandler;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.search_tree_rao.commons.*;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.CurativeOptimizationContext;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.GlobalOptimizationContext;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_contexts.PreventiveOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.*;
import com.farao_community.farao.search_tree_rao.commons.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.*;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.*;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTree;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.*;
import static com.farao_community.farao.search_tree_rao.commons.RaoLogger.formatDouble;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class Castor implements RaoProvider {
    private static final String SEARCH_TREE_RAO = "SearchTreeRao";
    private static final String PREVENTIVE_SCENARIO = "PreventiveScenario";
    private static final String SECOND_PREVENTIVE_SCENARIO = "SecondPreventiveScenario";
    private static final String CONTINGENCY_SCENARIO = "ContingencyScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run

    @Override
    public String getName() {
        return SEARCH_TREE_RAO;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    public Castor() {
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        return run(raoInput, parameters, null);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
        RaoUtil.initData(raoInput, parameters);

        StateTree stateTree = new StateTree(raoInput.getCrac());
        ToolProvider.ToolProviderBuilder toolProviderBuilder = ToolProvider.create()
            .withNetwork(raoInput.getNetwork())
            .withRaoParameters(parameters);
        if (raoInput.getReferenceProgram() != null) {
            toolProviderBuilder.withLoopFlowComputation(
                raoInput.getReferenceProgram(),
                raoInput.getGlskProvider(),
                new LoopFlowComputationWithXnodeGlskHandler(
                    raoInput.getGlskProvider(),
                    raoInput.getReferenceProgram(),
                    raoInput.getCrac().getContingencies(),
                    raoInput.getNetwork()
                )
            );
        }
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            toolProviderBuilder.withAbsolutePtdfSumsComputation(
                raoInput.getGlskProvider(),
                new AbsolutePtdfSumsComputation(
                    raoInput.getGlskProvider(),
                    parameters.getRelativeMarginPtdfBoundaries(),
                    raoInput.getNetwork()
                )
            );
        }
        ToolProvider toolProvider = toolProviderBuilder.build();

        // optimization is made on one given state only
        if (raoInput.getOptimizedState() != null) {
            try {
                return optimizeOneStateOnly(raoInput, parameters, stateTree, toolProvider);
            } catch (Exception e) {
                BUSINESS_LOGS.error("Optimizing state \"{}\" failed: ", raoInput.getOptimizedState().getId(), e);
                return CompletableFuture.completedFuture(new FailedRaoResultImpl());
            }
        }

        // compute initial sensitivity on all CNECs
        // this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            raoInput.getCrac().getFlowCnecs(),
            raoInput.getCrac().getRangeActions(),
            parameters,
            toolProvider);

        PrePerimeterResult initialOutput;
        try {
            initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork());
        } catch (SensitivityAnalysisException e) {
            BUSINESS_LOGS.error("Initial sensitivity analysis failed :", e);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl());
        }

        logOverallObjectiveFunction( "Initial sensitivity analysis: ", raoInput.getCrac(), initialOutput, initialOutput, new HashSet<>(), parameters, toolProvider);

        // optimize preventive perimeter
        Instant preventiveRaoStartInstant = Instant.now();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [start]");

        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

        if (stateTree.getContingencyScenarios().isEmpty()) {
            return CompletableFuture.completedFuture(optimizePreventivePerimeter(raoInput, parameters, stateTree, toolProvider, initialOutput));
        }

        PerimeterResult preventiveResult = optimizePreventivePerimeter(raoInput, parameters, stateTree, toolProvider, initialOutput).getPerimeterResult(OptimizationState.AFTER_PRA, raoInput.getCrac().getPreventiveState());
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
        Instant preventiveRaoEndInstant = Instant.now();
        long preventiveRaoTime = ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant);

        // optimize contingency scenarios (auto + curative instants)
        double preventiveOptimalCost = preventiveResult.getCost();
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(parameters.getExtension(SearchTreeRaoParameters.class), preventiveOptimalCost);
        applyRemedialActions(network, preventiveResult, raoInput.getCrac().getPreventiveState());

        PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, preventiveResult, stateTree.getOperatorsNotSharingCras(), null);
        logOverallObjectiveFunction( "Systematic sensitivity analysis after preventive remedial actions: ", raoInput.getCrac(), initialOutput, preCurativeSensitivityAnalysisOutput, stateTree.getOperatorsNotSharingCras(), parameters, toolProvider);

        RaoResult mergedRaoResults;

        // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
        if (parameters.getExtension(SearchTreeRaoParameters.class).getPreventiveRaoStopCriterion().equals(SearchTreeRaoParameters.PreventiveRaoStopCriterion.SECURE)
            && preventiveOptimalCost > 0) {
            BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(raoInput.getCrac().getPreventiveState(), initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput);
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, preCurativeSensitivityAnalysisOutput, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);

            return postCheckResults(mergedRaoResults, initialOutput, parameters);
        }

        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [start]");
        Map<State, OptimizationResult> postContingencyResults = optimizeContingencyScenarios(raoInput.getCrac(), parameters, stateTree, toolProvider, curativeTreeParameters, network, initialOutput, preCurativeSensitivityAnalysisOutput);
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");

        // second preventive RAO
        if (shouldRunSecondPreventiveRao(parameters, initialOutput, preventiveResult, postContingencyResults, targetEndInstant, preventiveRaoTime)) {
            mergedRaoResults = runSecondPreventiveRao(raoInput, parameters, stateTree, toolProvider, prePerimeterSensitivityAnalysis, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, postContingencyResults);
        } else {
            BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, postContingencyResults);
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, stateTree.getBasecaseScenario(), preventiveResult, stateTree.getContingencyScenarios(), postContingencyResults, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        }

        return postCheckResults(mergedRaoResults, initialOutput, parameters);
    }

    /**
     * Return initial result if RAO has increased cost
     */
    private CompletableFuture<RaoResult> postCheckResults(RaoResult raoResult, PrePerimeterResult initialResult, RaoParameters raoParameters) {
        RaoResult finalRaoResult = raoResult;
        if (raoParameters.getForbidCostIncrease() && raoResult.getCost(OptimizationState.AFTER_CRA) > initialResult.getCost()) {
            BUSINESS_LOGS.info("RAO has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to initial solution:",
                formatDouble(initialResult.getCost()), formatDouble(initialResult.getFunctionalCost()), formatDouble(initialResult.getVirtualCost()),
                formatDouble(raoResult.getCost(OptimizationState.AFTER_CRA)), formatDouble(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)), formatDouble(raoResult.getVirtualCost(OptimizationState.AFTER_CRA)));
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, initialResult, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);
            finalRaoResult = new UnoptimizedRaoResultImpl(initialResult);
        }

        // Log costs before and after RAO
        BUSINESS_LOGS.info("Cost before RAO = {} (functional: {}, virtual: {}), cost after RAO = {} (functional: {}, virtual: {})",
            formatDouble(initialResult.getCost()), formatDouble(initialResult.getFunctionalCost()), formatDouble(initialResult.getVirtualCost()),
            formatDouble(finalRaoResult.getCost(OptimizationState.AFTER_CRA)), formatDouble(finalRaoResult.getFunctionalCost(OptimizationState.AFTER_CRA)), formatDouble(finalRaoResult.getVirtualCost(OptimizationState.AFTER_CRA)));

        return CompletableFuture.completedFuture(finalRaoResult);
    }

    private void applyRemedialActions(Network network, PerimeterResult perimeterResult, State state) {
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        perimeterResult.getActivatedRangeActions().forEach(rangeAction -> rangeAction.apply(network, perimeterResult.getOptimizedSetpoint(rangeAction, state)));
    }

    /**
     * This method applies range action results on the network, for range actions that are curative
     * It is used for second preventive optimization along with 1st preventive results in order to keep the result
     * of 1st preventive for range actions that are both preventive and curative
     */
    static void applyPreventiveResultsForCurativeRangeActions(Network network, PerimeterResult perimeterResult, Crac crac) {
        perimeterResult.getActivatedRangeActions().stream()
            .filter(rangeAction -> isRangeActionCurative(rangeAction, crac))
            .forEach(rangeAction -> rangeAction.apply(network, perimeterResult.getOptimizedSetpoint(rangeAction, crac.getPreventiveState())));
    }

    /**
     * Build an objective function upon all CNECs in the CRAC and logs the cost and the most limiting elements, for a given preperimeter result
     */
    static void logOverallObjectiveFunction(String prefix,
                                            Crac crac,
                                            PrePerimeterResult initialOutput,
                                            PrePerimeterResult prePerimeterResult,
                                            Set<String> operatorsNotSharingCras,
                                            RaoParameters raoParameters,
                                            ToolProvider toolProvider) {
        if (!BUSINESS_LOGS.isInfoEnabled()) {
            return;
        }

        ObjectiveFunction objectiveFunction = ObjectiveFunctionSmartBuilder.build(
            crac.getFlowCnecs(),
            toolProvider.getLoopFlowCnecs(crac.getFlowCnecs()),
            initialOutput,
            prePerimeterResult,
            operatorsNotSharingCras,
            raoParameters);

        ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = objectiveFunction.evaluate(prePerimeterResult, prePerimeterResult.getSensitivityStatus());
        BUSINESS_LOGS.info(prefix + "cost = {} (functional: {}, virtual: {})",
            formatDouble(prePerimeterObjectiveFunctionResult.getCost()),
            formatDouble(prePerimeterObjectiveFunctionResult.getFunctionalCost()),
            formatDouble(prePerimeterObjectiveFunctionResult.getVirtualCost()));
        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, prePerimeterResult, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
    }

    CompletableFuture<RaoResult> optimizeOneStateOnly(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider) {

        // compute initial sensitivity on CNECs of the only optimized state
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            raoInput.getCrac().getFlowCnecs(raoInput.getOptimizedState()),
            raoInput.getCrac().getRangeActions(raoInput.getOptimizedState()),
            raoParameters,
            toolProvider);

        PrePerimeterResult initialResults;
        try {
            initialResults = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork());
        } catch (SensitivityAnalysisException e) {
            BUSINESS_LOGS.error("Initial sensitivity analysis failed :", e);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl());
        }

        // prepare search tree inputs
        OptimizationPerimeter optPerimeter;
        TreeParameters treeParameters;

        if (raoInput.getOptimizedState().equals(raoInput.getCrac().getPreventiveState())) {
            optPerimeter = PreventiveOptimizationPerimeter.buildWithPreventiveCnecsOnly(raoInput.getOptimizedState(), raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResults);
            treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class));

        } else {
            optPerimeter = CurativeOptimizationContext.build(raoInput.getOptimizedState(), raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResults);
            treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class), -Double.MAX_VALUE);
        }

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .with0bjectiveFunction(raoParameters.getObjectiveFunction())
            .withTreeParameters(treeParameters)
            .withNetworkActionParameters(NetworkActionParameters.buildFromRaoParameters(raoParameters, raoInput.getCrac()))
            .withGlobalRemedialActionLimitationParameters(GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters))
            .withRangeActionParameters(RangeActionParameters.buildFromRaoParameters(raoParameters))
            .withMnecParameters(MnecParameters.buildFromRaoParameters(raoParameters))
            .withMaxMinRelativeMarginParameters(MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters))
            .withLoopFlowParameters(LoopFlowParameters.buildFromRaoParameters(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), optPerimeter.getFlowCnecs()))
            .withSolverParameters(SolverParameters.buildFromRaoParameters(raoParameters))
            .withMaxNumberOfIterations(raoParameters.getMaxIterations())
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResults)
            .withPrePerimeterResult(initialResults)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunctionSmartBuilder.build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResults, initialResults, stateTree.getOperatorsNotSharingCras(), raoParameters))
            .withToolProvider(toolProvider)
            .build();

        OptimizationResult optimizationResult = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();

        optimizationResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), optimizationResult.getOptimizedSetpoint(rangeAction, raoInput.getOptimizedState())));
        optimizationResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(raoInput.getOptimizedState(), initialResults, optimizationResult, optPerimeter.getFlowCnecs()));
    }

    private SearchTreeRaoResult optimizePreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialResult) {

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFullPreventivePerimeter(stateTree.getBasecaseScenario(),raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResult);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .with0bjectiveFunction(raoParameters.getObjectiveFunction())
            .withTreeParameters(TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class)))
            .withNetworkActionParameters(NetworkActionParameters.buildFromRaoParameters(raoParameters, raoInput.getCrac()))
            .withGlobalRemedialActionLimitationParameters(GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters))
            .withRangeActionParameters(RangeActionParameters.buildFromRaoParameters(raoParameters))
            .withMnecParameters(MnecParameters.buildFromRaoParameters(raoParameters))
            .withMaxMinRelativeMarginParameters(MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters))
            .withLoopFlowParameters(LoopFlowParameters.buildFromRaoParameters(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), optPerimeter.getFlowCnecs()))
            .withSolverParameters(SolverParameters.buildFromRaoParameters(raoParameters))
            .withMaxNumberOfIterations(raoParameters.getMaxIterations())
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(initialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunctionSmartBuilder.build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResult, initialResult, stateTree.getOperatorsNotSharingCras(), raoParameters))
            .withToolProvider(toolProvider)
            .build();

        OptimizationResult perimeterResult = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();

        perimeterResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), perimeterResult.getOptimizedSetpoint(rangeAction, raoInput.getCrac().getPreventiveState())));
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return new OneStateOnlyRaoResultImpl(raoInput.getCrac().getPreventiveState(), initialResult, perimeterResult, searchTreeInput.getOptimizationPerimeter().getFlowCnecs());
    }

    private Map<State, OptimizationResult> optimizeContingencyScenarios(Crac crac,
                                                                        RaoParameters raoParameters,
                                                                        StateTree stateTree,
                                                                        ToolProvider toolProvider,
                                                                        TreeParameters curativeTreeParameters,
                                                                        Network network,
                                                                        PrePerimeterResult initialSensitivityOutput,
                                                                        PrePerimeterResult prePerimeterSensitivityOutput) {
        Map<State, OptimizationResult> contingencyScenarioResults = new ConcurrentHashMap<>();
        // Create a new variant
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        network.getVariantManager().cloneVariant(PREVENTIVE_SCENARIO, CONTINGENCY_SCENARIO);
        network.getVariantManager().setWorkingVariant(CONTINGENCY_SCENARIO);
        // Go through all contingency scenarios
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, CONTINGENCY_SCENARIO, raoParameters.getPerimetersInParallel())) {
            stateTree.getContingencyScenarios().forEach(optimizedScenario ->
                networkPool.submit(() -> {
                    try {
                        // Create a network copy to work on
                        Network networkClone = networkPool.getAvailableNetwork();

                        TECHNICAL_LOGS.info("Optimizing scenario post-contingency {}.", optimizedScenario.getContingency().getId());

                        // Init variables
                        Optional<State> automatonState = optimizedScenario.getAutomatonState();
                        State curativeState = optimizedScenario.getCurativeState();
                        PrePerimeterResult preCurativeResult = prePerimeterSensitivityOutput;

                        // Simulate automaton instant
                        if (automatonState.isPresent()) {
                            AutomatonPerimeterResultImpl automatonResult = simulateAutomatonState(automatonState.get(), curativeState, crac, networkClone, raoParameters, toolProvider, initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras());
                            contingencyScenarioResults.put(automatonState.get(), automatonResult);
                            preCurativeResult = automatonResult.getPostAutomatonSensitivityAnalysisOutput();
                        }

                        // Optimize curative instant
                        OptimizationResult curativeResult = optimizeCurativeState(curativeState, crac, networkClone,
                            raoParameters, stateTree, toolProvider, curativeTreeParameters, initialSensitivityOutput, preCurativeResult);
                        contingencyScenarioResults.put(curativeState, curativeResult);
                        // Release network copy
                        networkPool.releaseUsedNetwork(networkClone);
                    } catch (Exception e) {
                        BUSINESS_LOGS.error("Scenario post-contingency {} could not be optimized.", optimizedScenario.getContingency().getId(), e);
                        Thread.currentThread().interrupt();
                    }
                })
            );
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return contingencyScenarioResults;
    }

    private AutomatonPerimeterResultImpl simulateAutomatonState(State automatonState,
                                                                State curativeState,
                                                                Crac crac,
                                                                Network network,
                                                                RaoParameters raoParameters,
                                                                ToolProvider toolProvider,
                                                                FlowResult initialFlowResult,
                                                                PrePerimeterResult prePerimeterSensitivityOutput,
                                                                Set<String> operatorsNotSharingCras) {

        TECHNICAL_LOGS.info("Optimizing automaton state {}.", automatonState.getId());
        if (!crac.getRangeActions(automatonState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED).isEmpty()) {
            BUSINESS_WARNS.warn("CRAC has range action automatons. These are not supported yet.");
        }
        if (!crac.getNetworkActions(automatonState, UsageMethod.AVAILABLE).isEmpty()) {
            BUSINESS_WARNS.warn("CRAC has network action automatons with usage method AVAILABLE. These are not supported.");
        }
        TECHNICAL_LOGS.info("Initial situation:");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, prePerimeterSensitivityOutput, Set.of(automatonState, curativeState), raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // Apply network actions
        // First get forced network actions
        Set<NetworkAction> appliedNetworkActions = crac.getNetworkActions(automatonState, UsageMethod.FORCED);
        // Then add those with an OnFlowConstraint usage rule if their constraint is verified
        crac.getNetworkActions(automatonState, UsageMethod.TO_BE_EVALUATED).stream()
            .filter(na -> RaoUtil.isRemedialActionAvailable(na, automatonState, prePerimeterSensitivityOutput))
            .forEach(appliedNetworkActions::add);
        // Apply
        appliedNetworkActions.forEach(na -> {
            TECHNICAL_LOGS.debug("Activating automaton {}.", na.getId());
            na.apply(network);
        });

        // Run sensitivity computation before running curative RAO later
        // Get curative range actions
        Set<RangeAction<?>> curativeRangeActions = crac.getRangeActions(curativeState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        // Get cnecs
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(automatonState);
        flowCnecs.addAll(crac.getFlowCnecs(curativeState));
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            flowCnecs,
            curativeRangeActions,
            raoParameters,
            toolProvider);

        // Run computation
        PrePerimeterResult postAutomatonSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, postAutomatonSensitivityAnalysisOutput, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // Build and return optimization result
        AutomatonPerimeterResultImpl automatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(postAutomatonSensitivityAnalysisOutput, appliedNetworkActions);
        TECHNICAL_LOGS.info("Automaton state {} has been optimized.", automatonState.getId());

        RaoLogger.logOptimizationSummary(BUSINESS_LOGS, automatonState, automatonPerimeterResultImpl.getActivatedNetworkActions().size(), automatonPerimeterResultImpl.getActivatedRangeActions().size(), null, null, automatonPerimeterResultImpl);

        return automatonPerimeterResultImpl;
    }

    private OptimizationResult optimizeCurativeState(State curativeState,
                                                     Crac crac,
                                                     Network network,
                                                     RaoParameters raoParameters,
                                                     StateTree stateTree,
                                                     ToolProvider toolProvider,
                                                     TreeParameters curativeTreeParameters,
                                                     PrePerimeterResult initialSensitivityOutput,
                                                     PrePerimeterResult prePerimeterSensitivityOutput) {
        TECHNICAL_LOGS.info("Optimizing curative state {}.", curativeState.getId());


        OptimizationPerimeter optPerimeter = CurativeOptimizationContext.build(curativeState, crac, network, raoParameters, prePerimeterSensitivityOutput);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .with0bjectiveFunction(raoParameters.getObjectiveFunction())
            .withTreeParameters(curativeTreeParameters)
            .withNetworkActionParameters(NetworkActionParameters.buildFromRaoParameters(raoParameters, crac))
            .withGlobalRemedialActionLimitationParameters(GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters))
            .withRangeActionParameters(RangeActionParameters.buildFromRaoParameters(raoParameters))
            .withMnecParameters(MnecParameters.buildFromRaoParameters(raoParameters))
            .withMaxMinRelativeMarginParameters(MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters))
            .withLoopFlowParameters(LoopFlowParameters.buildFromRaoParameters(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), optPerimeter.getFlowCnecs()))
            .withSolverParameters(SolverParameters.buildFromRaoParameters(raoParameters))
            .withMaxNumberOfIterations(raoParameters.getMaxIterations())
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialSensitivityOutput)
            .withPrePerimeterResult(prePerimeterSensitivityOutput)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunctionSmartBuilder.build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), raoParameters))
            .withToolProvider(toolProvider)
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, false).run().join();
        TECHNICAL_LOGS.info("Curative state {} has been optimized.", curativeState.getId());
        return result;
    }

    public static Set<FlowCnec> computePerimeterCnecs(Crac crac, Set<State> perimeter) {
        if (perimeter != null) {
            Set<FlowCnec> cnecs = new HashSet<>();
            perimeter.forEach(state -> cnecs.addAll(crac.getFlowCnecs(state)));
            return cnecs;
        } else {
            return crac.getFlowCnecs();
        }
    }

    /**
     * If range action's initial setpoint does not respect its allowed range, this function filters it out
     */
    static void removeRangeActionsWithWrongInitialSetpoint(Set<RangeAction<?>> rangeActions, RangeActionSetpointResult prePerimeterSetPoints) {
        //a temp set is needed to avoid ConcurrentModificationExceptions when trying to remove a range action from a set we are looping on
        Set<RangeAction<?>> rangeActionsToRemove = new HashSet<>();
        for (RangeAction<?> rangeAction : rangeActions) {
            double preperimeterSetPoint = prePerimeterSetPoints.getSetpoint(rangeAction);
            double minSetPoint = rangeAction.getMinAdmissibleSetpoint(preperimeterSetPoint);
            double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(preperimeterSetPoint);
            if (preperimeterSetPoint < minSetPoint || preperimeterSetPoint > maxSetPoint) {
                BUSINESS_WARNS.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.",
                    rangeAction.getId(), preperimeterSetPoint, minSetPoint, maxSetPoint);
                rangeActionsToRemove.add(rangeAction);
            }
        }
        rangeActionsToRemove.forEach(rangeActions::remove);
    }

    /**
     * If aligned range actionsé initial setpoint are different, this function filters them out
     */
    static void removeAlignedRangeActionsWithDifferentInitialSetpoints(Set<RangeAction<?>> rangeActions, RangeActionSetpointResult prePerimeterSetPoints) {
        Set<String> groups = rangeActions.stream().map(RangeAction::getGroupId)
            .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        for (String group : groups) {
            Set<RangeAction<?>> groupRangeActions = rangeActions.stream().filter(rangeAction -> rangeAction.getGroupId().isPresent() && rangeAction.getGroupId().get().equals(group)).collect(Collectors.toSet());
            double preperimeterSetPoint = prePerimeterSetPoints.getSetpoint(groupRangeActions.iterator().next());
            if (groupRangeActions.stream().anyMatch(rangeAction -> Math.abs(prePerimeterSetPoints.getSetpoint(rangeAction) - preperimeterSetPoint) > 1e-6)) {
                BUSINESS_WARNS.warn("Range actions of group {} do not have the same initial setpoint. They will be filtered out of the linear problem.", group);
                rangeActions.removeAll(groupRangeActions);
            }
        }
    }

    // ========================================
    // region Second preventive RAO
    // ========================================

    /**
     * This function decides if a 2nd preventive RAO should be run. It checks the user parameter first, then takes the
     * decision depending on the curative RAO results and the curative RAO stop criterion.
     */
    static boolean shouldRunSecondPreventiveRao(RaoParameters raoParameters, ObjectiveFunctionResult initialObjectiveFunctionResult, OptimizationResult firstPreventiveResult, Map<State, OptimizationResult> curativeRaoResults, Instant targetEndInstant, long estimatedPreventiveRaoTimeInSeconds) {
        if (raoParameters.getExtension(SearchTreeRaoParameters.class) == null
            || raoParameters.getExtension(SearchTreeRaoParameters.class).getSecondPreventiveOptimizationCondition().equals(SearchTreeRaoParameters.SecondPreventiveRaoCondition.DISABLED)) {
            return false;
        }
        if (!Objects.isNull(targetEndInstant) && ChronoUnit.SECONDS.between(Instant.now(), targetEndInstant) < estimatedPreventiveRaoTimeInSeconds) {
            BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
            return false;
        }
        if (raoParameters.getExtension(SearchTreeRaoParameters.class).getSecondPreventiveOptimizationCondition().equals(SearchTreeRaoParameters.SecondPreventiveRaoCondition.COST_INCREASE)
            && curativeRaoResults.values().stream().noneMatch(optimizationResult -> optimizationResult.getCost() > initialObjectiveFunctionResult.getCost())) {
            BUSINESS_LOGS.info("Cost has not increased during RAO, there is no need to run a 2nd preventive RAO.");
            // it is not necessary to compare initial & post-preventive costs since the preventive RAO cannot increase its own cost
            // only compare initial cost with the curative costs
            return false;
        }
        SearchTreeRaoParameters.CurativeRaoStopCriterion curativeRaoStopCriterion = raoParameters.getExtension(SearchTreeRaoParameters.class).getCurativeRaoStopCriterion();
        switch (curativeRaoStopCriterion) {
            case MIN_OBJECTIVE:
                // Run 2nd preventive RAO in all cases
                return true;
            case SECURE:
                // Run 2nd preventive RAO if one perimeter of the curative optimization is unsecure
                return isAnyResultUnsecure(curativeRaoResults.values());
            case PREVENTIVE_OBJECTIVE:
                // Run 2nd preventive RAO if one perimeter of the curative optimization has a worse cost than the preventive perimeter
                return isAnyCurativeWorseThanPreventive(raoParameters, firstPreventiveResult, curativeRaoResults.values());
            case PREVENTIVE_OBJECTIVE_AND_SECURE:
                // Run 2nd preventive RAO if one perimeter of the curative optimization has a worse cost than the preventive perimeter or is unsecure
                return isAnyResultUnsecure(curativeRaoResults.values()) || isAnyCurativeWorseThanPreventive(raoParameters, firstPreventiveResult, curativeRaoResults.values());
            default:
                throw new FaraoException(String.format("Unknown curative RAO stop criterion: %s", curativeRaoStopCriterion));
        }
    }

    /**
     * Returns true if any result has a positive functional cost
     */
    private static boolean isAnyResultUnsecure(Collection<OptimizationResult> results) {
        return results.stream().anyMatch(optimizationResult -> optimizationResult.getFunctionalCost() >= 0);
    }

    /**
     * Returns true if any curative result has an objective function value superior to the preventive's + the minimum
     * needed improvement as per the RAO parameters
     */
    private static boolean isAnyCurativeWorseThanPreventive(RaoParameters raoParameters, OptimizationResult preventiveResult, Collection<OptimizationResult> curativeRaoResults) {
        double minExpectedImprovement = raoParameters.getExtension(SearchTreeRaoParameters.class).getCurativeRaoMinObjImprovement();
        return curativeRaoResults.stream().anyMatch(optimizationResult -> optimizationResult.getCost() > preventiveResult.getCost() - minExpectedImprovement);
    }

    /**
     * Main function to run 2nd preventive RAO
     * Using 1st preventive and curative results, it ets up network and range action contexts, then calls the optimizer
     * It finally merges the three results into one RaoResult object
     */
    private RaoResult runSecondPreventiveRao(RaoInput raoInput,
                                             RaoParameters parameters,
                                             StateTree stateTree,
                                             ToolProvider toolProvider,
                                             PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis,
                                             PrePerimeterResult initialOutput,
                                             PerimeterResult firstPreventiveResult,
                                             PrePerimeterResult preCurativeSensitivityAnalysisOutput,
                                             Map<State, OptimizationResult> postContingencyResults) {
        Network network = raoInput.getNetwork();
        // Go back to the initial state of the network, saved in the SECOND_PREVENTIVE_STATE variant
        network.getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO);
        // Apply 1st preventive results for range actions that are both preventive and curative. This way we are sure
        // that the optimal setpoints of the curative results stay coherent with their allowed range and close to
        // optimality in their perimeters. These range actions will be excluded from 2nd preventive RAO.
        applyPreventiveResultsForCurativeRangeActions(network, firstPreventiveResult, raoInput.getCrac());
        // Get the applied remedial actions for every curative perimeter
        AppliedRemedialActions appliedCras = getAppliedRemedialActionsInCurative(postContingencyResults, preCurativeSensitivityAnalysisOutput);

        // Run a first sensitivity computation using initial network and applied CRAs
        PrePerimeterResult sensiWithCurativeRemedialActions = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, stateTree.getOperatorsNotSharingCras(), appliedCras);
        logOverallObjectiveFunction("Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ",
            raoInput.getCrac(), initialOutput, sensiWithCurativeRemedialActions, stateTree.getOperatorsNotSharingCras(), parameters, toolProvider);

        // Run second preventive RAO
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        PerimeterResult secondPreventiveResult = optimizeSecondPreventivePerimeter(raoInput, parameters, stateTree, toolProvider, initialOutput, sensiWithCurativeRemedialActions, appliedCras)
            .join().getPerimeterResult(OptimizationState.AFTER_CRA, raoInput.getCrac().getPreventiveState());
        // Re-run sensitivity computation based on PRAs without CRAs, to access OptimizationState.AFTER_PRA results
        PrePerimeterResult updatedPreCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, stateTree.getOperatorsNotSharingCras(), null);
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");

        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        Set<RemedialAction<?>> remedialActionsExcluded = new HashSet<>(getRangeActionsExcludedFromSecondPreventive(raoInput.getCrac()));

        // log results
        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, secondPreventiveResult, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);

        return new SecondPreventiveAndCurativesRaoResultImpl(initialOutput, raoInput.getCrac().getPreventiveState(), firstPreventiveResult, secondPreventiveResult, updatedPreCurativeSensitivityAnalysisOutput, postContingencyResults, remedialActionsExcluded);
    }

    static AppliedRemedialActions getAppliedRemedialActionsInCurative(Map<State, OptimizationResult> curativeResults, PrePerimeterResult preCurativeResults) {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        curativeResults.forEach((state, optimizationResult) -> appliedRemedialActions.addAppliedNetworkActions(state, optimizationResult.getActivatedNetworkActions()));
        // Add all range actions that were activated in curative, even if they are also preventive (they will be excluded from 2nd preventive)
        curativeResults.forEach((state, optimizationResult) ->
            (new PerimeterResultImpl(preCurativeResults, optimizationResult)).getActivatedRangeActions()
                .forEach(rangeAction -> appliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)))
        );
        return appliedRemedialActions;
    }

    private CompletableFuture<SearchTreeRaoResult> optimizeSecondPreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialOutput, PrePerimeterResult prePerimeterResult, AppliedRemedialActions appliedCras) {

        OptimizationPerimeter optPerimeter = GlobalOptimizationContext.build(raoInput.getCrac(), raoInput.getNetwork(), raoParameters, prePerimeterResult);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .with0bjectiveFunction(raoParameters.getObjectiveFunction())
            .withTreeParameters(TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class)))
            .withNetworkActionParameters(NetworkActionParameters.buildFromRaoParameters(raoParameters, raoInput.getCrac()))
            .withGlobalRemedialActionLimitationParameters(GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters))
            .withRangeActionParameters(RangeActionParameters.buildFromRaoParameters(raoParameters))
            .withMnecParameters(MnecParameters.buildFromRaoParameters(raoParameters))
            .withMaxMinRelativeMarginParameters(MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters))
            .withLoopFlowParameters(LoopFlowParameters.buildFromRaoParameters(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), optPerimeter.getFlowCnecs()))
            .withSolverParameters(SolverParameters.buildFromRaoParameters(raoParameters))
            .withMaxNumberOfIterations(raoParameters.getMaxIterations())
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialOutput)
            .withPrePerimeterResult(prePerimeterResult)
            .withPreOptimizationAppliedNetworkActions(appliedCras) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunctionSmartBuilder.build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialOutput, prePerimeterResult, stateTree.getOperatorsNotSharingCras(), raoParameters))
            .withToolProvider(toolProvider)
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, false).run().join();

        // apply PRAs
        result.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), result.getOptimizedSetpoint(rangeAction, raoInput.getCrac().getPreventiveState())));
        result.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(raoInput.getCrac().getPreventiveState(), prePerimeterResult, result, optPerimeter.getFlowCnecs()));
    }

    /**
     * For second preventive optimization, we shouldn't re-optimize range actions that are also curative
     */
    static void removeRangeActionsExcludedFromSecondPreventive(Set<RangeAction<?>> rangeActions, Crac crac) {
        Set<RangeAction<?>> rangeActionsToRemove = new HashSet<>(rangeActions);
        rangeActionsToRemove.retainAll(getRangeActionsExcludedFromSecondPreventive(crac));
        rangeActionsToRemove.forEach(rangeAction ->
            BUSINESS_WARNS.warn("Range action {} will not be considered in 2nd preventive RAO as it is also curative (or its network element has an associated CRA)", rangeAction.getId())
        );
        rangeActions.removeAll(rangeActionsToRemove);
    }

    /**
     * Returns the set of range actions that were excluded from the 2nd preventive RAO.
     * It consists of range actions that are both preventive and curative, since they mustn't be re-optimized during 2nd preventive.
     */
    static Set<RangeAction<?>> getRangeActionsExcludedFromSecondPreventive(Crac crac) {
        // TODO :  we can avoid excluding (PRA+CRA) range actions that were not activated in any curative perimeter
        return crac.getRangeActions().stream().filter(rangeAction -> isRangeActionPreventive(rangeAction, crac) && isRangeActionCurative(rangeAction, crac)).collect(Collectors.toSet());
    }

    static boolean isRangeActionPreventive(RangeAction<?> rangeAction, Crac crac) {
        return isRangeActionAvailableInState(rangeAction, crac.getPreventiveState(), crac);
    }

    static boolean isRangeActionCurative(RangeAction<?> rangeAction, Crac crac) {
        return crac.getStates().stream()
            .filter(state -> !state.equals(crac.getPreventiveState()))
            .anyMatch(state -> isRangeActionAvailableInState(rangeAction, state, crac));
    }

    static boolean isRangeActionAvailableInState(RangeAction<?> rangeAction, State state, Crac crac) {
        Set<RangeAction<?>> rangeActionsForState = crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        if (rangeActionsForState.contains(rangeAction)) {
            return true;
        } else {
            return rangeActionsForState.stream()
                .anyMatch(otherRangeAction -> otherRangeAction.getNetworkElements().equals(rangeAction.getNetworkElements()));
        }
    }

    // ========================================
    // endregion
    // ========================================
}
