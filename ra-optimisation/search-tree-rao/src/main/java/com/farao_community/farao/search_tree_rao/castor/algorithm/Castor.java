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
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
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
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.*;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.IteratingLinearOptimizer;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.*;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTree;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTreeBloomer;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTreeProblem;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.TreeParameters;
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

import static com.farao_community.farao.commons.Unit.MEGAWATT;
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
            raoInput.getCrac().getRangeActions(),
            raoInput.getCrac().getFlowCnecs(),
            toolProvider,
            parameters,
            basicLinearOptimizerBuilder(parameters, computePerimeterCnecs(raoInput.getCrac(), raoInput.getPerimeter())).build()
        );

        PrePerimeterResult initialOutput;
        try {
            initialOutput = prePerimeterSensitivityAnalysis.run(raoInput.getNetwork());
        } catch (SensitivityAnalysisException e) {
            BUSINESS_LOGS.error("Initial sensitivity analysis failed :", e);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl());
        }

        logOverallObjectiveFunction(raoInput.getCrac(), parameters, initialOutput, initialOutput, toolProvider, "Initial sensitivity analysis: ");

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
        applyRemedialActions(network, preventiveResult);

        PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOn(network, preventiveResult);
        logOverallObjectiveFunction(raoInput.getCrac(), parameters, initialOutput, preCurativeSensitivityAnalysisOutput, toolProvider, "Systematic sensitivity analysis after preventive remedial actions: ");

        RaoResult mergedRaoResults;

        // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
        if (parameters.getExtension(SearchTreeRaoParameters.class).getPreventiveRaoStopCriterion().equals(SearchTreeRaoParameters.PreventiveRaoStopCriterion.SECURE)
            && preventiveOptimalCost > 0) {
            BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput);
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

    private void applyRemedialActions(Network network, PerimeterResult perimeterResult) {
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        perimeterResult.getActivatedRangeActions().forEach(rangeAction -> rangeAction.apply(network, perimeterResult.getOptimizedSetPoint(rangeAction)));
    }

    /**
     * This method applies range action results on the network, for range actions that are curative
     * It is used for second preventive optimization along with 1st preventive results in order to keep the result
     * of 1st preventive for range actions that are both preventive and curative
     */
    static void applyPreventiveResultsForCurativeRangeActions(Network network, PerimeterResult perimeterResult, Crac crac) {
        perimeterResult.getActivatedRangeActions().stream()
            .filter(rangeAction -> isRangeActionCurative(rangeAction, crac))
            .forEach(rangeAction -> rangeAction.apply(network, perimeterResult.getOptimizedSetPoint(rangeAction)));
    }

    private static LinearOptimizerParameters.LinearOptimizerParametersBuilder basicLinearOptimizerBuilder(RaoParameters raoParameters, Set<FlowCnec> flowCnecs) {
        LinearOptimizerParameters.LinearOptimizerParametersBuilder builder = LinearOptimizerParameters.create()
            .withObjectiveFunction(raoParameters.getObjectiveFunction())
            .withPstSensitivityThreshold(raoParameters.getPstSensitivityThreshold())
            .withHvdcSensitivityThreshold(raoParameters.getHvdcSensitivityThreshold())
            .withInjectionSensitivityThreshold(raoParameters.getInjectionRaSensitivityThreshold())
            .withSolver(raoParameters.getSolver())
            .withRelativeMipGap(raoParameters.getRelativeMipGap())
            .withSolverSpecificParameters(raoParameters.getSolverSpecificParameters())
            .withPstOptimizationApproximation(raoParameters.getPstOptimizationApproximation());

        if (raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE
            || raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT) {
            builder.withMaxMinMarginParameters(new MaxMinMarginParameters(
                    raoParameters.getPstPenaltyCost(),
                    raoParameters.getHvdcPenaltyCost(),
                    raoParameters.getInjectionRaPenaltyCost(), getLargestCnecThreshold(flowCnecs)));

        } else if (raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE
            || raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT) {
            MaxMinRelativeMarginParameters maxMinRelativeMarginParameters = new MaxMinRelativeMarginParameters(
                raoParameters.getPstPenaltyCost(),
                raoParameters.getHvdcPenaltyCost(),
                raoParameters.getInjectionRaPenaltyCost(),
                    getLargestCnecThreshold(flowCnecs),
                raoParameters.getNegativeMarginObjectiveCoefficient(),
                raoParameters.getPtdfSumLowerBound());
            builder.withMaxMinRelativeMarginParameters(maxMinRelativeMarginParameters);

        } else {
            throw new FaraoException(String.format("Unhandled objective function %s", raoParameters.getObjectiveFunction()));
        }

        if (raoParameters.isRaoWithMnecLimitation()) {
            MnecParameters mnecParameters = new MnecParameters(
                raoParameters.getMnecAcceptableMarginDiminution(),
                raoParameters.getMnecViolationCost(),
                raoParameters.getMnecConstraintAdjustmentCoefficient());
            builder.withMnecParameters(mnecParameters);
        }

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowParameters loopFlowParameters = new LoopFlowParameters(
                raoParameters.getLoopFlowApproximationLevel(),
                raoParameters.getLoopFlowAcceptableAugmentation(),
                raoParameters.getLoopFlowViolationCost(),
                raoParameters.getLoopFlowConstraintAdjustmentCoefficient());
            builder.withLoopFlowParameters(loopFlowParameters);
        }
        return builder;
    }

    /**
     * Build an objective function upon all CNECs in the CRAC and logs the cost and the most limiting elements, for a given preperimeter result
     */
    static void logOverallObjectiveFunction(Crac crac, RaoParameters raoParameters, PrePerimeterResult initialOutput,
                                            PrePerimeterResult prePerimeterResult, ToolProvider toolProvider, String prefix) {
        if (!BUSINESS_LOGS.isInfoEnabled()) {
            return;
        }
        LinearOptimizerParameters.LinearOptimizerParametersBuilder builder = basicLinearOptimizerBuilder(raoParameters, crac.getFlowCnecs());
        LinearOptimizerParameters linearOptimizerParameters = builder.build();
        ObjectiveFunction objectiveFunction = createObjectiveFunction(
            crac.getFlowCnecs(),
            initialOutput,
            prePerimeterResult,
            raoParameters,
            linearOptimizerParameters,
            toolProvider
        );
        ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = objectiveFunction.evaluate(prePerimeterResult, prePerimeterResult.getSensitivityStatus());
        BUSINESS_LOGS.info(prefix + "cost = {} (functional: {}, virtual: {})",
            formatDouble(prePerimeterObjectiveFunctionResult.getCost()),
            formatDouble(prePerimeterObjectiveFunctionResult.getFunctionalCost()),
            formatDouble(prePerimeterObjectiveFunctionResult.getVirtualCost()));
        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, prePerimeterResult, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
    }

    static LinearOptimizerParameters createPreventiveLinearOptimizerParameters(RaoParameters raoParameters, Set<FlowCnec> flowCnecs) {
        return basicLinearOptimizerBuilder(raoParameters, flowCnecs).build();
    }

    static LinearOptimizerParameters createCurativeLinearOptimizerParameters(RaoParameters raoParameters, StateTree stateTree, Set<FlowCnec> cnecs) {
        LinearOptimizerParameters.LinearOptimizerParametersBuilder builder = basicLinearOptimizerBuilder(raoParameters, cnecs);
        SearchTreeRaoParameters parameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (parameters != null && !parameters.getCurativeRaoOptimizeOperatorsNotSharingCras()) {
            UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(
                stateTree.getOperatorsNotSharingCras(),
                getLargestCnecThreshold(cnecs));
            builder.withUnoptimizedCnecParameters(unoptimizedCnecParameters);
        }
        return builder.build();
    }

    static double getLargestCnecThreshold(Set<FlowCnec> flowCnecs) {
        double max = 0;
        for (FlowCnec flowCnec : flowCnecs) {
            if (flowCnec.isOptimized()) {
                Optional<Double> minFlow = flowCnec.getLowerBound(Side.LEFT, MEGAWATT);
                if (minFlow.isPresent() && Math.abs(minFlow.get()) > max) {
                    max = Math.abs(minFlow.get());
                }
                Optional<Double> maxFlow = flowCnec.getUpperBound(Side.LEFT, MEGAWATT);
                if (maxFlow.isPresent() && Math.abs(maxFlow.get()) > max) {
                    max = Math.abs(maxFlow.get());
                }
            }
        }
        return max;
    }

    CompletableFuture<RaoResult> optimizeOneStateOnly(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider) {
        Set<FlowCnec> perimeterCnecs = computePerimeterCnecs(raoInput.getCrac(), raoInput.getPerimeter());
        TreeParameters treeParameters = raoInput.getOptimizedState().equals(raoInput.getCrac().getPreventiveState()) ?
            TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class)) :
            TreeParameters.buildForCurativePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class), -Double.MAX_VALUE);
        LinearOptimizerParameters linearOptimizerParameters = createCurativeLinearOptimizerParameters(raoParameters, stateTree, perimeterCnecs);

        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            raoInput.getCrac().getRangeActions(raoInput.getOptimizedState(), UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED),
            perimeterCnecs,
            toolProvider,
            raoParameters,
            linearOptimizerParameters
        );
        PrePerimeterResult prePerimeterResult = prePerimeterSensitivityAnalysis.run(raoInput.getNetwork());

        SearchTreeInput searchTreeInput = buildSearchTreeInput(
            raoInput.getCrac(),
            raoInput.getNetwork(),
            raoInput.getOptimizedState(),
            raoInput.getPerimeter(),
            prePerimeterResult,
            prePerimeterResult,
            treeParameters,
            raoParameters,
            linearOptimizerParameters,
            toolProvider,
            false,
            null
        );

        OptimizationResult optimizationResult = new SearchTree().run(searchTreeInput, treeParameters, linearOptimizerParameters, true).join();

        optimizationResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), optimizationResult.getOptimizedSetPoint(rangeAction)));
        optimizationResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(raoInput.getOptimizedState(), prePerimeterResult, optimizationResult, searchTreeInput.getFlowCnecs()));
    }

    private SearchTreeRaoResult optimizePreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult prePerimeterResult) {
        TreeParameters preventiveTreeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class));
        LinearOptimizerParameters linearOptimizerParameters = createPreventiveLinearOptimizerParameters(raoParameters, computePerimeterCnecs(raoInput.getCrac(), raoInput.getPerimeter()));
        SearchTreeInput searchTreeInput = buildSearchTreeInput(
            raoInput.getCrac(),
            raoInput.getNetwork(),
            raoInput.getCrac().getPreventiveState(),
            stateTree.getBasecaseScenario().getAllStates(),
            prePerimeterResult,
            prePerimeterResult,
            preventiveTreeParameters,
            raoParameters,
            linearOptimizerParameters,
            toolProvider,
            false,
            null
        );

        OptimizationResult perimeterResult = new SearchTree().run(searchTreeInput, preventiveTreeParameters, linearOptimizerParameters, true).join();

        perimeterResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), perimeterResult.getOptimizedSetPoint(rangeAction)));
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return new OneStateOnlyRaoResultImpl(raoInput.getCrac().getPreventiveState(), prePerimeterResult, perimeterResult, searchTreeInput.getFlowCnecs());
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
                            AutomatonPerimeterResultImpl automatonResult = simulateAutomatonState(automatonState.get(), curativeState, crac, networkClone, raoParameters, toolProvider, prePerimeterSensitivityOutput);
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
                                                                PrePerimeterResult prePerimeterSensitivityOutput) {
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
            .filter(na -> SearchTree.isRemedialActionAvailable(na, automatonState, prePerimeterSensitivityOutput))
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
            curativeRangeActions,
            flowCnecs,
            toolProvider,
            raoParameters,
            basicLinearOptimizerBuilder(raoParameters, flowCnecs).build()
        );
        // Run computation
        PrePerimeterResult postAutomatonSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOn(network, prePerimeterSensitivityOutput);
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
        Set<FlowCnec> cnecs = computePerimeterCnecs(crac, Collections.singleton(curativeState));
        LinearOptimizerParameters linearOptimizerParameters = createCurativeLinearOptimizerParameters(raoParameters, stateTree, cnecs);

        SearchTreeInput searchTreeInput = buildSearchTreeInput(
            crac,
            network,
            curativeState,
            Collections.singleton(curativeState),
            initialSensitivityOutput,
            prePerimeterSensitivityOutput,
            curativeTreeParameters,
            raoParameters,
            linearOptimizerParameters,
            toolProvider,
            false,
            null
        );

        OptimizationResult result = new SearchTree().run(searchTreeInput, curativeTreeParameters, linearOptimizerParameters, false).join();
        TECHNICAL_LOGS.info("Curative state {} has been optimized.", curativeState.getId());
        return result;
    }

    static SearchTreeInput buildSearchTreeInput(Crac crac,
                                                Network network,
                                                State optimizedState,
                                                Set<State> perimeter,
                                                PrePerimeterResult initialOutput,
                                                PrePerimeterResult prePerimeterOutput,
                                                TreeParameters treeParameters,
                                                RaoParameters raoParameters,
                                                LinearOptimizerParameters linearOptimizerParameters,
                                                ToolProvider toolProvider,
                                                boolean isSecondPreventiveRao,
                                                AppliedRemedialActions appliedRemedialActions) {
        if (isSecondPreventiveRao) {
            Objects.requireNonNull(appliedRemedialActions);
        }
        SearchTreeInput searchTreeInput = new SearchTreeInput();

        searchTreeInput.setNetwork(network);
        Set<FlowCnec> cnecs;
        cnecs = isSecondPreventiveRao ? crac.getFlowCnecs() : computePerimeterCnecs(crac, perimeter);
        searchTreeInput.setFlowCnecs(cnecs);
        searchTreeInput.setOptimizedState(optimizedState);
        searchTreeInput.setNetworkActions(crac.getNetworkActions(optimizedState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED));

        Set<RangeAction<?>> rangeActions = crac.getRangeActions(optimizedState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        removeRangeActionsWithWrongInitialSetpoint(rangeActions, prePerimeterOutput);
        removeAlignedRangeActionsWithDifferentInitialSetpoints(rangeActions, prePerimeterOutput);
        if (isSecondPreventiveRao) {
            removeRangeActionsExcludedFromSecondPreventive(rangeActions, crac);
        }
        searchTreeInput.setRangeActions(rangeActions);

        ObjectiveFunction objectiveFunction = createObjectiveFunction(
            cnecs,
            initialOutput,
            prePerimeterOutput,
            raoParameters,
            linearOptimizerParameters,
            toolProvider
        );
        searchTreeInput.setObjectiveFunction(objectiveFunction);
        searchTreeInput.setIteratingLinearOptimizer(new IteratingLinearOptimizer(objectiveFunction, raoParameters.getMaxIterations(), raoParameters.getPstOptimizationApproximation()));

        searchTreeInput.setSearchTreeProblem(new SearchTreeProblem(
            initialOutput,
            prePerimeterOutput,
            prePerimeterOutput,
            cnecs,
            toolProvider.getLoopFlowCnecs(cnecs),
            linearOptimizerParameters
        ));

        SearchTreeComputer.SearchTreeComputerBuilder searchTreeComputerBuilder = SearchTreeComputer.create()
            .withToolProvider(toolProvider)
            .withCnecs(cnecs)
            .withAppliedRemedialActions(appliedRemedialActions);
        if (linearOptimizerParameters.hasRelativeMargins()) {
            searchTreeComputerBuilder.withPtdfsResults(initialOutput);
        }
        searchTreeInput.setSearchTreeComputer(searchTreeComputerBuilder.build());

        searchTreeInput.setSearchTreeBloomer(new SearchTreeBloomer(
            network,
            prePerimeterOutput,
            treeParameters.getMaxRa(),
            treeParameters.getMaxTso(),
            treeParameters.getMaxTopoPerTso(),
            treeParameters.getMaxRaPerTso(),
            treeParameters.getSkipNetworkActionsFarFromMostLimitingElement(),
            raoParameters.getExtension(SearchTreeRaoParameters.class).getMaxNumberOfBoundariesForSkippingNetworkActions(),
            raoParameters.getExtension(SearchTreeRaoParameters.class).getNetworkActionCombinations(crac)
        ));
        searchTreeInput.setPrePerimeterOutput(prePerimeterOutput);

        return searchTreeInput;
    }

    static ObjectiveFunction createObjectiveFunction(Set<FlowCnec> cnecs,
                                                     FlowResult initialFlowResult,
                                                     FlowResult prePerimeterFlowResult,
                                                     RaoParameters raoParameters,
                                                     LinearOptimizerParameters linearOptimizerParameters,
                                                     ToolProvider toolProvider) {
        ObjectiveFunction.ObjectiveFunctionBuilder objectiveFunctionBuilder = ObjectiveFunction.create();
        addMinMarginObjectiveFunction(cnecs, prePerimeterFlowResult, objectiveFunctionBuilder, linearOptimizerParameters);

        addMnecVirtualCostEvaluator(cnecs, initialFlowResult, raoParameters, objectiveFunctionBuilder);
        addLoopflowVirtualCostEvaluator(cnecs, initialFlowResult, raoParameters, toolProvider, objectiveFunctionBuilder);
        addSensiFallbackVirtualCostEvaluator(raoParameters, objectiveFunctionBuilder);
        return objectiveFunctionBuilder.build();
    }

    private static void addMinMarginObjectiveFunction(Set<FlowCnec> cnecs, FlowResult prePerimeterFlowResult, ObjectiveFunction.ObjectiveFunctionBuilder objectiveFunctionBuilder, LinearOptimizerParameters linearOptimizerParameters) {
        ObjectiveFunctionHelper.addMinMarginObjectiveFunction(cnecs, prePerimeterFlowResult, objectiveFunctionBuilder, linearOptimizerParameters.hasRelativeMargins(), linearOptimizerParameters.getUnoptimizedCnecParameters(), linearOptimizerParameters.getUnit());
    }

    private static void addSensiFallbackVirtualCostEvaluator(RaoParameters raoParameters, ObjectiveFunction.ObjectiveFunctionBuilder objectiveFunctionBuilder) {
        objectiveFunctionBuilder.withVirtualCostEvaluator(new SensitivityFallbackOvercostEvaluator(
            raoParameters.getFallbackOverCost()
        ));
    }

    private static void addLoopflowVirtualCostEvaluator(Set<FlowCnec> cnecs, FlowResult initialFlowResult, RaoParameters raoParameters, ToolProvider toolProvider, ObjectiveFunction.ObjectiveFunctionBuilder objectiveFunctionBuilder) {
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            objectiveFunctionBuilder.withVirtualCostEvaluator(new LoopFlowViolationCostEvaluator(
                toolProvider.getLoopFlowCnecs(cnecs),
                initialFlowResult,
                raoParameters.getLoopFlowParameters()
            ));
        }
    }

    private static void addMnecVirtualCostEvaluator(Set<FlowCnec> cnecs, FlowResult initialFlowResult, RaoParameters raoParameters, ObjectiveFunction.ObjectiveFunctionBuilder objectiveFunctionBuilder) {
        if (raoParameters.isRaoWithMnecLimitation()) {
            objectiveFunctionBuilder.withVirtualCostEvaluator(new MnecViolationCostEvaluator(
                cnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                initialFlowResult,
                raoParameters.getMnecParameters()
            ));
        }
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
    static void removeRangeActionsWithWrongInitialSetpoint(Set<RangeAction<?>> rangeActions, RangeActionResult prePerimeterSetPoints) {
        //a temp set is needed to avoid ConcurrentModificationExceptions when trying to remove a range action from a set we are looping on
        Set<RangeAction<?>> rangeActionsToRemove = new HashSet<>();
        for (RangeAction<?> rangeAction : rangeActions) {
            double preperimeterSetPoint = prePerimeterSetPoints.getOptimizedSetPoint(rangeAction);
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
    static void removeAlignedRangeActionsWithDifferentInitialSetpoints(Set<RangeAction<?>> rangeActions, RangeActionResult prePerimeterSetPoints) {
        Set<String> groups = rangeActions.stream().map(RangeAction::getGroupId)
            .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        for (String group : groups) {
            Set<RangeAction<?>> groupRangeActions = rangeActions.stream().filter(rangeAction -> rangeAction.getGroupId().isPresent() && rangeAction.getGroupId().get().equals(group)).collect(Collectors.toSet());
            double preperimeterSetPoint = prePerimeterSetPoints.getOptimizedSetPoint(groupRangeActions.iterator().next());
            if (groupRangeActions.stream().anyMatch(rangeAction -> Math.abs(prePerimeterSetPoints.getOptimizedSetPoint(rangeAction) - preperimeterSetPoint) > 1e-6)) {
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
        AppliedRemedialActions appliedRemedialActions = getAppliedRemedialActionsInCurative(postContingencyResults, preCurativeSensitivityAnalysisOutput);

        // Run a first sensitivity computation using initial network and applied CRAs
        PrePerimeterResult sensiWithCurativeRemedialActions = prePerimeterSensitivityAnalysis.run(network, appliedRemedialActions);
        logOverallObjectiveFunction(raoInput.getCrac(), parameters, initialOutput, sensiWithCurativeRemedialActions, toolProvider, "Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ");

        // Run second preventive RAO
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        PerimeterResult secondPreventiveResult = optimizeSecondPreventivePerimeter(raoInput, parameters, stateTree, toolProvider, initialOutput, sensiWithCurativeRemedialActions, appliedRemedialActions)
            .join().getPerimeterResult(OptimizationState.AFTER_CRA, raoInput.getCrac().getPreventiveState());
        // Re-run sensitivity computation based on PRAs without CRAs, to access OptimizationState.AFTER_PRA results
        PrePerimeterResult updatedPreCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOn(network, secondPreventiveResult);
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");

        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        Set<RemedialAction<?>> remedialActionsExcluded = new HashSet<>(getRangeActionsExcludedFromSecondPreventive(raoInput.getCrac()));

        // log results
        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, secondPreventiveResult, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);

        return new SecondPreventiveAndCurativesRaoResultImpl(initialOutput, firstPreventiveResult, secondPreventiveResult, updatedPreCurativeSensitivityAnalysisOutput, postContingencyResults, remedialActionsExcluded);
    }

    static AppliedRemedialActions getAppliedRemedialActionsInCurative(Map<State, OptimizationResult> curativeResults, PrePerimeterResult preCurativeResults) {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        curativeResults.forEach((state, optimizationResult) -> appliedRemedialActions.addAppliedNetworkActions(state, optimizationResult.getActivatedNetworkActions()));
        // Add all range actions that were activated in curative, even if they are also preventive (they will be excluded from 2nd preventive)
        curativeResults.forEach((state, optimizationResult) ->
            (new PerimeterResultImpl(preCurativeResults, optimizationResult)).getActivatedRangeActions()
                .forEach(rangeAction -> appliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizationResult.getOptimizedSetPoint(rangeAction)))
        );
        return appliedRemedialActions;
    }

    private CompletableFuture<SearchTreeRaoResult> optimizeSecondPreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialOutput, PrePerimeterResult prePerimeterResult, AppliedRemedialActions appliedRemedialActions) {
        TreeParameters preventiveTreeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class));
        LinearOptimizerParameters linearOptimizerParameters = createPreventiveLinearOptimizerParameters(raoParameters, computePerimeterCnecs(raoInput.getCrac(), raoInput.getPerimeter()));
        SearchTreeInput searchTreeInput = buildSearchTreeInput(
            raoInput.getCrac(),
            raoInput.getNetwork(),
            raoInput.getCrac().getPreventiveState(),
            stateTree.getBasecaseScenario().getAllStates(),
            initialOutput,
            prePerimeterResult,
            preventiveTreeParameters,
            raoParameters,
            linearOptimizerParameters,
            toolProvider,
            true,
            appliedRemedialActions
        );

        OptimizationResult perimeterResult = new SearchTree().run(searchTreeInput, preventiveTreeParameters, linearOptimizerParameters, true).join();

        perimeterResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), perimeterResult.getOptimizedSetPoint(rangeAction)));
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(raoInput.getCrac().getPreventiveState(), prePerimeterResult, perimeterResult, searchTreeInput.getFlowCnecs()));
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
