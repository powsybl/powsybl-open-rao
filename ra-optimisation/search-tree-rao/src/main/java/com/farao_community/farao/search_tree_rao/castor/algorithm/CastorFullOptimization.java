/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.farao_community.farao.search_tree_rao.commons.RaoLogger;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.CurativeOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.GlobalOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.PreventiveOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.TreeParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.*;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTree;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.*;
import static com.farao_community.farao.search_tree_rao.commons.RaoLogger.formatDouble;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CastorFullOptimization {
    private static final String PREVENTIVE_SCENARIO = "PreventiveScenario";
    private static final String SECOND_PREVENTIVE_SCENARIO = "SecondPreventiveScenario";
    private static final String CONTINGENCY_SCENARIO = "ContingencyScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    private final RaoInput raoInput;
    private final RaoParameters raoParameters;
    private final Instant targetEndInstant;

    public CastorFullOptimization(RaoInput raoInput, RaoParameters raoParameters, Instant targetEndInstant) {
        this.raoInput = raoInput;
        this.raoParameters = raoParameters;
        this.targetEndInstant = targetEndInstant;
    }

    public CompletableFuture<RaoResult> run() {

        RaoUtil.initData(raoInput, raoParameters);
        StateTree stateTree = new StateTree(raoInput.getCrac());
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);

        // ----- INITIAL SENSI -----
        // compute initial sensitivity on all CNECs
        // (this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters)
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            raoInput.getCrac().getFlowCnecs(),
            raoInput.getCrac().getRangeActions(),
            raoParameters,
            toolProvider);

        PrePerimeterResult initialOutput;
        try {
            initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac());
            RaoLogger.logSensitivityAnalysisResults("Initial sensitivity analysis: ",
                    prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                    new RangeActionActivationResultImpl(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(raoInput.getNetwork(), raoInput.getCrac().getRangeActions())),
                    initialOutput,
                    raoParameters,
                    NUMBER_LOGGED_ELEMENTS_DURING_RAO);
        } catch (SensitivityAnalysisException e) {
            BUSINESS_LOGS.error("Initial sensitivity analysis failed :", e);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl());
        }

        // ----- PREVENTIVE PERIMETER OPTIMIZATION -----
        // run search tree on preventive perimeter
        Instant preventiveRaoStartInstant = Instant.now();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [start]");

        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

        if (stateTree.getContingencyScenarios().isEmpty()) {
            return CompletableFuture.completedFuture(optimizePreventivePerimeter(raoInput, raoParameters, stateTree, toolProvider, initialOutput));
        }

        PerimeterResult preventiveResult = optimizePreventivePerimeter(raoInput, raoParameters, stateTree, toolProvider, initialOutput).getPerimeterResult(OptimizationState.AFTER_PRA, raoInput.getCrac().getPreventiveState());
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
        Instant preventiveRaoEndInstant = Instant.now();
        long preventiveRaoTime = ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant);

        // ----- SENSI POST-PRA -----
        // mutualise the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs

        double preventiveOptimalCost = preventiveResult.getCost();
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class), preventiveOptimalCost);
        applyRemedialActions(network, preventiveResult, raoInput.getCrac().getPreventiveState());

        PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, raoInput.getCrac(), initialOutput, initialOutput, Collections.emptySet(), null);
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after preventive remedial actions: ",
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                new RangeActionActivationResultImpl(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(raoInput.getNetwork(), raoInput.getCrac().getRangeActions())),
                preCurativeSensitivityAnalysisOutput,
                raoParameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        RaoResult mergedRaoResults;

        // ----- CURATIVE PERIMETERS OPTIMIZATION -----
        // optimize contingency scenarios (auto + curative instants)

        // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
        if (raoParameters.getExtension(SearchTreeRaoParameters.class).getPreventiveRaoStopCriterion().equals(SearchTreeRaoParameters.PreventiveRaoStopCriterion.SECURE)
            && preventiveOptimalCost > 0) {
            BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(raoInput.getCrac().getPreventiveState(), initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput);
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, preCurativeSensitivityAnalysisOutput, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);

            return postCheckResults(mergedRaoResults, initialOutput, raoParameters);
        }

        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [start]");
        Map<State, OptimizationResult> postContingencyResults = optimizeContingencyScenarios(raoInput.getCrac(), raoParameters, stateTree, toolProvider, curativeTreeParameters, network, initialOutput, preCurativeSensitivityAnalysisOutput, false);
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");

        // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----
        mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, postContingencyResults);
        boolean logFinalResultsOutsideOfSecondPreventive = true;
        // Run second preventive when necessary
        if (shouldRunSecondPreventiveRao(raoParameters, preventiveResult, postContingencyResults.values(), mergedRaoResults, targetEndInstant, preventiveRaoTime)) {
            RaoResult secondPreventiveRaoResults = runSecondPreventiveAndAutoRao(raoInput, raoParameters, stateTree, toolProvider, prePerimeterSensitivityAnalysis, initialOutput, preventiveResult, postContingencyResults);
            if (secondPreventiveImprovesResults(secondPreventiveRaoResults, mergedRaoResults)) {
                mergedRaoResults = secondPreventiveRaoResults;
                logFinalResultsOutsideOfSecondPreventive = false;
            }
        }
        // Log final results
        if (logFinalResultsOutsideOfSecondPreventive) {
            BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, stateTree.getBasecaseScenario(), preventiveResult, stateTree.getContingencyScenarios(), postContingencyResults, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        }

        return postCheckResults(mergedRaoResults, initialOutput, raoParameters);
    }

    /**
     * Return true if 2P has decreased cost
     */
    private boolean secondPreventiveImprovesResults(RaoResult secondPreventiveRaoResults, RaoResult mergedRaoResults) {
        if (secondPreventiveRaoResults.getCost(OptimizationState.AFTER_CRA) > mergedRaoResults.getCost(OptimizationState.AFTER_CRA)) {
            BUSINESS_LOGS.info("Second preventive step has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to previous solution:",
                    formatDouble(mergedRaoResults.getCost(OptimizationState.AFTER_CRA)), formatDouble(mergedRaoResults.getFunctionalCost(OptimizationState.AFTER_CRA)), formatDouble(mergedRaoResults.getVirtualCost(OptimizationState.AFTER_CRA)),
                    formatDouble(secondPreventiveRaoResults.getCost(OptimizationState.AFTER_CRA)), formatDouble(secondPreventiveRaoResults.getFunctionalCost(OptimizationState.AFTER_CRA)), formatDouble(secondPreventiveRaoResults.getVirtualCost(OptimizationState.AFTER_CRA)));
            return false;
        }
        return true;
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

    private void applyRemedialActions(Network network, OptimizationResult optResult, State state) {
        optResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        optResult.getActivatedRangeActions(state).forEach(rangeAction -> rangeAction.apply(network, optResult.getOptimizedSetpoint(rangeAction, state)));
    }

    private SearchTreeRaoResult optimizePreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialResult) {

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(stateTree.getBasecaseScenario(), raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResult);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, raoInput.getCrac())
            .withTreeParameters(TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class)))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), raoInput.getCrac()))
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(initialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResult, initialResult, initialResult, raoInput.getCrac(), Collections.emptySet(), raoParameters))
            .withToolProvider(toolProvider)
            .build();

        OptimizationResult optResult = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();
        applyRemedialActions(raoInput.getNetwork(), optResult, raoInput.getCrac().getPreventiveState());
        return new OneStateOnlyRaoResultImpl(raoInput.getCrac().getPreventiveState(), initialResult, optResult, searchTreeInput.getOptimizationPerimeter().getFlowCnecs());
    }

    private Map<State, OptimizationResult> optimizeContingencyScenarios(Crac crac,
                                                                        RaoParameters raoParameters,
                                                                        StateTree stateTree,
                                                                        ToolProvider toolProvider,
                                                                        TreeParameters curativeTreeParameters,
                                                                        Network network,
                                                                        PrePerimeterResult initialSensitivityOutput,
                                                                        PrePerimeterResult prePerimeterSensitivityOutput,
                                                                        boolean automatonsOnly) {
        Map<State, OptimizationResult> contingencyScenarioResults = new ConcurrentHashMap<>();
        // Create a new variant
        String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);
        // Create an automaton simulator
        AutomatonSimulator automatonSimulator = new AutomatonSimulator(crac, raoParameters, toolProvider, initialSensitivityOutput, prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
        // Go through all contingency scenarios
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, newVariant, raoParameters.getPerimetersInParallel())) {
            AtomicInteger remainingScenarios = new AtomicInteger(stateTree.getContingencyScenarios().size());
            CountDownLatch contingencyCountDownLatch = new CountDownLatch(stateTree.getContingencyScenarios().size());
            stateTree.getContingencyScenarios().forEach(optimizedScenario ->
                networkPool.submit(() -> {
                    Network networkClone = null;
                    try {
                        networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
                    } catch (InterruptedException e) {
                        contingencyCountDownLatch.countDown();
                        Thread.currentThread().interrupt();
                        throw new FaraoException(e);
                    }
                    try {
                        TECHNICAL_LOGS.info("Optimizing scenario post-contingency {}.", optimizedScenario.getContingency().getId());

                        // Init variables
                        Optional<State> automatonState = optimizedScenario.getAutomatonState();
                        State curativeState = optimizedScenario.getCurativeState();
                        PrePerimeterResult preCurativeResult = prePerimeterSensitivityOutput;

                        // Simulate automaton instant
                        if (automatonState.isPresent()) {
                            AutomatonPerimeterResultImpl automatonResult = automatonSimulator.simulateAutomatonState(automatonState.get(), curativeState, networkClone);
                            contingencyScenarioResults.put(automatonState.get(), automatonResult);
                            preCurativeResult = automatonResult.getPostAutomatonSensitivityAnalysisOutput();
                        }

                        if (!automatonsOnly) {
                            // Optimize curative instant
                            OptimizationResult curativeResult = optimizeCurativeState(curativeState, crac, networkClone,
                                raoParameters, stateTree, toolProvider, curativeTreeParameters, initialSensitivityOutput, preCurativeResult);
                            contingencyScenarioResults.put(curativeState, curativeResult);
                        }
                    } catch (Exception e) {
                        BUSINESS_LOGS.error("Scenario post-contingency {} could not be optimized.", optimizedScenario.getContingency().getId(), e);
                    }
                    TECHNICAL_LOGS.info("Remaining post-contingency scenarios to optimize: {}", remainingScenarios.decrementAndGet());
                    try {
                        networkPool.releaseUsedNetwork(networkClone);
                        contingencyCountDownLatch.countDown();
                    } catch (InterruptedException ex) {
                        contingencyCountDownLatch.countDown();
                        Thread.currentThread().interrupt();
                        throw new FaraoException(ex);
                    }
                })
            );
            boolean success = contingencyCountDownLatch.await(24, TimeUnit.HOURS);
            if (!success) {
                throw new FaraoException("At least one post-contingency state could not be optimized within the given time (24 hours). This should not happen.");
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return contingencyScenarioResults;
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

        OptimizationPerimeter optPerimeter = CurativeOptimizationPerimeter.build(curativeState, crac, network, raoParameters, prePerimeterSensitivityOutput);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(curativeTreeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), raoInput.getCrac()))
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialSensitivityOutput)
            .withPrePerimeterResult(prePerimeterSensitivityOutput)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialSensitivityOutput, prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, raoInput.getCrac(), stateTree.getOperatorsNotSharingCras(), raoParameters))
            .withToolProvider(toolProvider)
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, false).run().join();
        TECHNICAL_LOGS.info("Curative state {} has been optimized.", curativeState.getId());
        return result;
    }

    // ========================================
    // region Second preventive RAO
    // ========================================

    /**
     * This function decides if a 2nd preventive RAO should be run. It checks the user parameter first, then takes the
     * decision depending on the curative RAO results and the curative RAO stop criterion.
     */
    static boolean shouldRunSecondPreventiveRao(RaoParameters raoParameters, OptimizationResult firstPreventiveResult, Collection<OptimizationResult> curativeRaoResults, RaoResult postFirstRaoResult, Instant targetEndInstant, long estimatedPreventiveRaoTimeInSeconds) {
        if (raoParameters.getExtension(SearchTreeRaoParameters.class) == null
            || raoParameters.getExtension(SearchTreeRaoParameters.class).getSecondPreventiveOptimizationCondition().equals(SearchTreeRaoParameters.SecondPreventiveRaoCondition.DISABLED)) {
            return false;
        }
        if (!Objects.isNull(targetEndInstant) && ChronoUnit.SECONDS.between(Instant.now(), targetEndInstant) < estimatedPreventiveRaoTimeInSeconds) {
            BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
            return false;
        }
        if (raoParameters.getExtension(SearchTreeRaoParameters.class).getSecondPreventiveOptimizationCondition().equals(SearchTreeRaoParameters.SecondPreventiveRaoCondition.COST_INCREASE)
            && postFirstRaoResult.getCost(OptimizationState.AFTER_CRA) <= postFirstRaoResult.getCost(OptimizationState.INITIAL)) {
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
                return isAnyResultUnsecure(curativeRaoResults);
            case PREVENTIVE_OBJECTIVE:
                // Run 2nd preventive RAO if the final result has a worse cost than the preventive perimeter
                return isFinalCostWorseThanPreventive(raoParameters, firstPreventiveResult, postFirstRaoResult);
            case PREVENTIVE_OBJECTIVE_AND_SECURE:
                // Run 2nd preventive RAO if the final result has a worse cost than the preventive perimeter or is unsecure
                return isAnyResultUnsecure(curativeRaoResults) || isFinalCostWorseThanPreventive(raoParameters, firstPreventiveResult, postFirstRaoResult);
            default:
                throw new FaraoException(String.format("Unknown curative RAO stop criterion: %s", curativeRaoStopCriterion));
        }
    }

    /**
     * Returns true if any result has a positive functional cost
     */
    private static boolean isAnyResultUnsecure(Collection<OptimizationResult> results) {
        return results.stream().anyMatch(optimizationResult -> optimizationResult.getFunctionalCost() >= 0 || optimizationResult.getVirtualCost() > 1e-6);
    }

    /**
     * Returns true if final cost (after PRAO + ARAO + CRAO) is worse than the cost at the end of the preventive perimeter
     */
    private static boolean isFinalCostWorseThanPreventive(RaoParameters raoParameters, OptimizationResult preventiveResult, RaoResult postFirstRaoResult) {
        double minExpectedImprovement = raoParameters.getExtension(SearchTreeRaoParameters.class).getCurativeRaoMinObjImprovement();
        return postFirstRaoResult.getCost(OptimizationState.AFTER_CRA) > preventiveResult.getCost() - minExpectedImprovement;
    }

    private RaoResult runSecondPreventiveAndAutoRao(RaoInput raoInput,
                                                    RaoParameters parameters,
                                                    StateTree stateTree,
                                                    ToolProvider toolProvider,
                                                    PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis,
                                                    PrePerimeterResult initialOutput,
                                                    PerimeterResult firstPreventiveResult,
                                                    Map<State, OptimizationResult> postContingencyResults) {
        // Run 2nd preventive RAO
        Map<State, OptimizationResult> curativeResults = postContingencyResults.entrySet().stream().filter(entry -> entry.getKey().getInstant().equals(com.farao_community.farao.data.crac_api.Instant.CURATIVE)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        SecondPreventiveRaoResult secondPreventiveRaoResult = runSecondPreventiveRao(raoInput, parameters, stateTree, toolProvider, prePerimeterSensitivityAnalysis, initialOutput, firstPreventiveResult, curativeResults);

        // Run 2nd automaton simulation and update results
        BUSINESS_LOGS.info("----- Second automaton simulation [start]");
        Map<State, OptimizationResult> newPostContingencyResults = optimizeContingencyScenarios(raoInput.getCrac(), raoParameters, stateTree, toolProvider, null, raoInput.getNetwork(), initialOutput, secondPreventiveRaoResult.postPraSensitivityAnalysisOutput, true);
        BUSINESS_LOGS.info("----- Second automaton simulation [end]");

        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        if (newPostContingencyResults.entrySet().stream().anyMatch(entry -> !entry.getValue().getActivatedNetworkActions().isEmpty() || !entry.getValue().getActivatedRangeActions(entry.getKey()).isEmpty())) {
            // If any activated ARA, re-run curative sensitivity analysis
            AppliedRemedialActions appliedArasAndCras = secondPreventiveRaoResult.appliedCras.copy();
            addAppliedNetworkActionsPostContingency(appliedArasAndCras, newPostContingencyResults);
            addAppliedRangeActionsPostContingency(appliedArasAndCras, newPostContingencyResults);
            PrePerimeterResult postCraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(raoInput.getNetwork(), raoInput.getCrac(), initialOutput, initialOutput, Collections.emptySet(), appliedArasAndCras);
            for (Map.Entry<State, OptimizationResult> entry : postContingencyResults.entrySet()) {
                State state = entry.getKey();
                if (!state.getInstant().equals(com.farao_community.farao.data.crac_api.Instant.CURATIVE)) {
                    continue;
                }
                newPostContingencyResults.put(state, new CurativeWithSecondPraoResult(state, entry.getValue(), secondPreventiveRaoResult.perimeterResult, secondPreventiveRaoResult.remedialActionsExcluded, postCraSensitivityAnalysisOutput));
            }
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, postCraSensitivityAnalysisOutput, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);
            return new PreventiveAndCurativesRaoResultImpl(stateTree,
                initialOutput,
                firstPreventiveResult,
                secondPreventiveRaoResult.perimeterResult,
                secondPreventiveRaoResult.remedialActionsExcluded,
                secondPreventiveRaoResult.postPraSensitivityAnalysisOutput,
                newPostContingencyResults,
                postCraSensitivityAnalysisOutput);
        } else {
            for (Map.Entry<State, OptimizationResult> entry : postContingencyResults.entrySet()) {
                State state = entry.getKey();
                if (!state.getInstant().equals(com.farao_community.farao.data.crac_api.Instant.CURATIVE)) {
                    continue;
                }
                newPostContingencyResults.put(state, new CurativeWithSecondPraoResult(state, entry.getValue(), secondPreventiveRaoResult.perimeterResult, secondPreventiveRaoResult.remedialActionsExcluded));
            }
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, secondPreventiveRaoResult.perimeterResult, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);
            return new PreventiveAndCurativesRaoResultImpl(stateTree,
                initialOutput,
                firstPreventiveResult,
                secondPreventiveRaoResult.perimeterResult,
                secondPreventiveRaoResult.remedialActionsExcluded,
                secondPreventiveRaoResult.postPraSensitivityAnalysisOutput,
                newPostContingencyResults);
        }

    }

    private static class SecondPreventiveRaoResult {
        private final PerimeterResult perimeterResult;
        private final PrePerimeterResult postPraSensitivityAnalysisOutput;
        private final Set<RemedialAction<?>> remedialActionsExcluded;
        private final AppliedRemedialActions appliedCras;

        public SecondPreventiveRaoResult(PerimeterResult perimeterResult, PrePerimeterResult postPraSensitivityAnalysisOutput, Set<RemedialAction<?>> remedialActionsExcluded, AppliedRemedialActions appliedCras) {
            this.perimeterResult = perimeterResult;
            this.postPraSensitivityAnalysisOutput = postPraSensitivityAnalysisOutput;
            this.remedialActionsExcluded = remedialActionsExcluded;
            this.appliedCras = appliedCras;
        }
    }

    /**
     * Main function to run 2nd preventive RAO
     * Using 1st preventive and curative results, it ets up network and range action contexts, then calls the optimizer
     * It finally merges the three results into one RaoResult object
     */
    private SecondPreventiveRaoResult runSecondPreventiveRao(RaoInput raoInput,
                                                             RaoParameters parameters,
                                                             StateTree stateTree,
                                                             ToolProvider toolProvider,
                                                             PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis,
                                                             PrePerimeterResult initialOutput,
                                                             PerimeterResult firstPreventiveResult,
                                                             Map<State, OptimizationResult> curativeResults) {
        Network network = raoInput.getNetwork();
        // Go back to the initial state of the network, saved in the SECOND_PREVENTIVE_STATE variant
        network.getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO);

        // Get the applied network actions for every contingency perimeter
        AppliedRemedialActions appliedCras = new AppliedRemedialActions();
        addAppliedNetworkActionsPostContingency(appliedCras, curativeResults);

        // Apply 1st preventive results for range actions that are both preventive and curative. This way we are sure
        // that the optimal setpoints of the curative results stay coherent with their allowed range and close to
        // optimality in their perimeters. These range actions will be excluded from 2nd preventive RAO.
        Set<RemedialAction<?>> remedialActionsExcluded = new HashSet<>();
        if (!parameters.getExtension(SearchTreeRaoParameters.class).isGlobalOptimizationInSecondPreventive()) { // keep old behaviour
            remedialActionsExcluded = new HashSet<>(getRangeActionsExcludedFromSecondPreventive(raoInput.getCrac()));
            applyPreventiveResultsForCurativeRangeActions(network, firstPreventiveResult, raoInput.getCrac());
            addAppliedRangeActionsPostContingency(appliedCras, curativeResults);
        }

        // Run a first sensitivity computation using initial network and applied CRAs
        PrePerimeterResult sensiWithPostContingencyRemedialActions = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, raoInput.getCrac(), initialOutput, initialOutput, stateTree.getOperatorsNotSharingCras(), appliedCras);
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ",
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                new RangeActionActivationResultImpl(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(raoInput.getNetwork(), raoInput.getCrac().getRangeActions())),
                sensiWithPostContingencyRemedialActions,
                parameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // Run second preventive RAO
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        PerimeterResult secondPreventiveResult = optimizeSecondPreventivePerimeter(raoInput, parameters, stateTree, toolProvider, initialOutput, sensiWithPostContingencyRemedialActions, firstPreventiveResult.getActivatedNetworkActions(), appliedCras)
            .join().getPerimeterResult(OptimizationState.AFTER_CRA, raoInput.getCrac().getPreventiveState());
        // Re-run sensitivity computation based on PRAs without CRAs, to access OptimizationState.AFTER_PRA results
        PrePerimeterResult postPraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, raoInput.getCrac(), initialOutput, initialOutput, stateTree.getOperatorsNotSharingCras(), null);
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");

        return new SecondPreventiveRaoResult(secondPreventiveResult, postPraSensitivityAnalysisOutput, remedialActionsExcluded, appliedCras);
    }

    static void addAppliedNetworkActionsPostContingency(AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults) {
        postContingencyResults.forEach((state, optimizationResult) -> appliedRemedialActions.addAppliedNetworkActions(state, optimizationResult.getActivatedNetworkActions()));
    }

    static void addAppliedRangeActionsPostContingency(AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults) {
        // Add all range actions that were activated in curative, even if they are also preventive (they will be excluded from 2nd preventive)
        postContingencyResults.forEach((state, optimizationResult) ->
            optimizationResult.getActivatedRangeActions(state).forEach(rangeAction -> appliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)))
        );
    }

    private CompletableFuture<SearchTreeRaoResult> optimizeSecondPreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialOutput, PrePerimeterResult prePerimeterResult, Set<NetworkAction> optimalNetworkActionsInFirstPreventiveRao, AppliedRemedialActions appliedCras) {

        OptimizationPerimeter optPerimeter;
        if (raoParameters.getExtension(SearchTreeRaoParameters.class).isGlobalOptimizationInSecondPreventive()) {
            optPerimeter = GlobalOptimizationPerimeter.build(raoInput.getCrac(), raoInput.getNetwork(), raoParameters, prePerimeterResult);
        } else {
            Set<RangeAction<?>> rangeActionsFor2p = new HashSet<>(raoInput.getCrac().getRangeActions());
            removeRangeActionsExcludedFromSecondPreventive(rangeActionsFor2p, raoInput.getCrac());
            optPerimeter = PreventiveOptimizationPerimeter.buildWithAllCnecs(raoInput.getCrac(), rangeActionsFor2p, raoInput.getNetwork(), raoParameters, prePerimeterResult);
        }

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, raoInput.getCrac())
            .withTreeParameters(TreeParameters.buildForSecondPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class)))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), raoInput.getCrac()))
            .build();

        if (raoParameters.getExtension(SearchTreeRaoParameters.class).isSecondPreventiveHintFromFirstPreventive()) {
            // Set the optimal set of network actions decided in 1st preventive RAO as a hint for 2nd preventive RAO
            searchTreeParameters.getNetworkActionParameters().addNetworkActionCombination(new NetworkActionCombination(optimalNetworkActionsInFirstPreventiveRao, true));
        }

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialOutput)
            .withPrePerimeterResult(prePerimeterResult)
            .withPreOptimizationAppliedNetworkActions(appliedCras) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialOutput, prePerimeterResult, prePerimeterResult, raoInput.getCrac(), new HashSet<>(), raoParameters))
            .withToolProvider(toolProvider)
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();

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
     * This method applies range action results on the network, for range actions that are curative
     * It is used for second preventive optimization along with 1st preventive results in order to keep the result
     * of 1st preventive for range actions that are both preventive and curative
     */
    static void applyPreventiveResultsForCurativeRangeActions(Network network, PerimeterResult preventiveResult, Crac crac) {
        preventiveResult.getActivatedRangeActions(crac.getPreventiveState()).stream()
            .filter(rangeAction -> isRangeActionCurative(rangeAction, crac))
            .forEach(rangeAction -> rangeAction.apply(network, preventiveResult.getOptimizedSetpoint(rangeAction, crac.getPreventiveState())));
    }

    /**
     * Returns the set of range actions that were excluded from the 2nd preventive RAO.
     * It consists of range actions that are both preventive and curative, since they mustn't be re-optimized during 2nd preventive.
     */
    static Set<RangeAction<?>> getRangeActionsExcludedFromSecondPreventive(Crac crac) {
        return crac.getRangeActions().stream()
            .filter(rangeAction -> isRangeActionCurative(rangeAction, crac))
            .collect(Collectors.toSet());
    }

    static boolean isRangeActionPreventive(RangeAction<?> rangeAction, Crac crac) {
        return isRangeActionAvailableInState(rangeAction, crac.getPreventiveState(), crac);
    }

    static boolean isRangeActionCurative(RangeAction<?> rangeAction, Crac crac) {
        return crac.getStates().stream()
            .filter(state -> state.getInstant().equals(com.farao_community.farao.data.crac_api.Instant.CURATIVE))
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
