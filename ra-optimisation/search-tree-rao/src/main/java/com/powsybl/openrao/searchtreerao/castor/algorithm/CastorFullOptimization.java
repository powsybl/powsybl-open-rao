/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.StandardRangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.*;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;
import com.powsybl.iidm.network.Network;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.data.cracapi.range.RangeType.RELATIVE_TO_PREVIOUS_INSTANT;
import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.FAILURE;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.formatDouble;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyRemedialActions;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CastorFullOptimization {
    private static final String INITIAL_SCENARIO = "InitialScenario";
    private static final String PREVENTIVE_SCENARIO = "PreventiveScenario";
    private static final String SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT = "SecondPreventiveScenario";
    private static final String CONTINGENCY_SCENARIO = "ContingencyScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    private final RaoInput raoInput;
    private final RaoParameters raoParameters;
    private final java.time.Instant targetEndInstant;

    public CastorFullOptimization(RaoInput raoInput, RaoParameters raoParameters, java.time.Instant targetEndInstant) {
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
        initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac());
        if (initialOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Initial sensitivity analysis failed");
            return CompletableFuture.completedFuture(new FailedRaoResultImpl("Initial sensitivity analysis failed"));
        }
        RaoLogger.logSensitivityAnalysisResults("Initial sensitivity analysis: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            initialOutput,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // ----- PREVENTIVE PERIMETER OPTIMIZATION -----
        // run search tree on preventive perimeter
        java.time.Instant preventiveRaoStartInstant = java.time.Instant.now();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [start]");

        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

        if (stateTree.getContingencyScenarios().isEmpty()) {
            CompletableFuture<RaoResult> result = CompletableFuture.completedFuture(optimizePreventivePerimeter(raoInput, raoParameters, stateTree, toolProvider, initialOutput));
            BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
            return result;
        }

        OptimizationResult preventiveResult = optimizePreventivePerimeter(raoInput, raoParameters, stateTree, toolProvider, initialOutput).getOptimizationResult(raoInput.getCrac().getPreventiveState());
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
        java.time.Instant preventiveRaoEndInstant = java.time.Instant.now();
        long preventiveRaoTime = ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant);

        // ----- SENSI POST-PRA -----
        // mutualise the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs

        double preventiveOptimalCost = preventiveResult.getCost();
        TreeParameters automatonTreeParameters = TreeParameters.buildForAutomatonPerimeter(raoParameters);
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, preventiveOptimalCost);
        network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO, true);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        applyRemedialActions(network, preventiveResult, raoInput.getCrac().getPreventiveState());

        PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, raoInput.getCrac(), initialOutput, Collections.emptySet(), null);
        if (preCurativeSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions failed");
            return CompletableFuture.completedFuture(new FailedRaoResultImpl("Systematic sensitivity analysis after preventive remedial actions failed"));
        }
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after preventive remedial actions: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            preCurativeSensitivityAnalysisOutput,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        RaoResult mergedRaoResults;

        // ----- CURATIVE PERIMETERS OPTIMIZATION -----
        // optimize contingency scenarios (auto + curative instants)

        // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
        // (however RAO could continue depending on parameter optimize-curative-if-basecase-unsecure)
        if (shouldStopOptimisationIfPreventiveUnsecure(preventiveOptimalCost)) {
            BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(raoInput.getCrac().getPreventiveState(), initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, raoInput.getCrac());
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, preCurativeSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType(), NUMBER_LOGGED_ELEMENTS_END_RAO);

            return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters());
        }

        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [start]");
        Map<State, OptimizationResult> postContingencyResults = optimizeContingencyScenarios(raoInput.getCrac(), raoParameters, stateTree, toolProvider, automatonTreeParameters, curativeTreeParameters, network, initialOutput, preCurativeSensitivityAnalysisOutput, false);
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");

        // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----
        mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, postContingencyResults, raoInput.getCrac());
        boolean logFinalResultsOutsideOfSecondPreventive = true;
        // Run second preventive when necessary
        if (shouldRunSecondPreventiveRao(raoParameters, preventiveResult, postContingencyResults.values(), mergedRaoResults, targetEndInstant, preventiveRaoTime, raoInput.getCrac().getLastInstant())) {
            RaoResult secondPreventiveRaoResults = runSecondPreventiveAndAutoRao(raoInput, raoParameters, stateTree, automatonTreeParameters, toolProvider, prePerimeterSensitivityAnalysis, initialOutput, preventiveResult, postContingencyResults);
            if (secondPreventiveImprovesResults(secondPreventiveRaoResults, mergedRaoResults)) {
                mergedRaoResults = secondPreventiveRaoResults;
                mergedRaoResults.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST);
                logFinalResultsOutsideOfSecondPreventive = false;
            } else {
                mergedRaoResults.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION);
            }
        }
        // Log final results
        if (logFinalResultsOutsideOfSecondPreventive) {
            BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, stateTree.getBasecaseScenario(), preventiveResult, stateTree.getContingencyScenarios(), postContingencyResults, raoParameters.getObjectiveFunctionParameters().getType(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        }

        return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters());
    }

    private boolean shouldStopOptimisationIfPreventiveUnsecure(double preventiveOptimalCost) {
        return raoParameters.getObjectiveFunctionParameters().getPreventiveStopCriterion().equals(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE)
                && preventiveOptimalCost > 0
                && !raoParameters.getObjectiveFunctionParameters().getOptimizeCurativeIfPreventiveUnsecure();
    }

    /**
     * Return true if 2P has decreased cost
     */
    private boolean secondPreventiveImprovesResults(RaoResult secondPreventiveRaoResults, RaoResult mergedRaoResults) {
        if (secondPreventiveRaoResults instanceof FailedRaoResultImpl) {
            BUSINESS_LOGS.info("Second preventive failed. Falling back to previous solution:");
            return false;
        }
        if (mergedRaoResults.getComputationStatus() == ComputationStatus.FAILURE && secondPreventiveRaoResults.getComputationStatus() != ComputationStatus.FAILURE) {
            BUSINESS_LOGS.info("RAO has succeeded thanks to second preventive step when first preventive step had failed");
            return true;
        }
        Instant curativeInstant = raoInput.getCrac().getLastInstant();
        double firstPreventiveCost = mergedRaoResults.getCost(curativeInstant);
        double secondPreventiveCost = secondPreventiveRaoResults.getCost(curativeInstant);
        if (secondPreventiveCost > firstPreventiveCost) {
            BUSINESS_LOGS.info("Second preventive step has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to previous solution:",
                    formatDouble(firstPreventiveCost), formatDouble(mergedRaoResults.getFunctionalCost(curativeInstant)), formatDouble(mergedRaoResults.getVirtualCost(curativeInstant)),
                    formatDouble(secondPreventiveCost), formatDouble(secondPreventiveRaoResults.getFunctionalCost(curativeInstant)), formatDouble(secondPreventiveRaoResults.getVirtualCost(curativeInstant)));
            return false;
        }
        return true;
    }

    /**
     * Return initial result if RAO has increased cost
     */
    private CompletableFuture<RaoResult> postCheckResults(RaoResult raoResult, PrePerimeterResult initialResult, ObjectiveFunctionParameters objectiveFunctionParameters) {
        RaoResult finalRaoResult = raoResult;

        double initialCost = initialResult.getCost();
        double initialFunctionalCost = initialResult.getFunctionalCost();
        double initialVirtualCost = initialResult.getVirtualCost();
        Instant lastInstant = raoInput.getCrac().getLastInstant();
        double finalCost = finalRaoResult.getCost(lastInstant);
        double finalFunctionalCost = finalRaoResult.getFunctionalCost(lastInstant);
        double finalVirtualCost = finalRaoResult.getVirtualCost(lastInstant);

        if (objectiveFunctionParameters.getForbidCostIncrease() && finalCost > initialCost) {
            BUSINESS_LOGS.info("RAO has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to initial solution:",
                formatDouble(initialCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost),
                formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost));
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, initialResult, objectiveFunctionParameters.getType(), NUMBER_LOGGED_ELEMENTS_END_RAO);
            finalRaoResult = new UnoptimizedRaoResultImpl(initialResult);
            finalCost = initialCost;
            finalFunctionalCost = initialFunctionalCost;
            finalVirtualCost = initialVirtualCost;
            if (raoResult.getOptimizationStepsExecuted().equals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY)) {
                finalRaoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
            } else {
                finalRaoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
            }
        }

        // Log costs before and after RAO
        BUSINESS_LOGS.info("Cost before RAO = {} (functional: {}, virtual: {}), cost after RAO = {} (functional: {}, virtual: {})",
            formatDouble(initialCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost),
            formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost));

        return CompletableFuture.completedFuture(finalRaoResult);
    }

    private OneStateOnlyRaoResultImpl optimizePreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialResult) {

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(stateTree.getBasecaseScenario(), raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResult);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, raoInput.getCrac())
            .withTreeParameters(TreeParameters.buildForPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()))
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(initialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResult, initialResult, Collections.emptySet(), raoParameters))
            .withToolProvider(toolProvider)
            .withOutageInstant(raoInput.getCrac().getOutageInstant())
            .build();

        OptimizationResult optResult = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();
        applyRemedialActions(raoInput.getNetwork(), optResult, raoInput.getCrac().getPreventiveState());
        return new OneStateOnlyRaoResultImpl(raoInput.getCrac().getPreventiveState(), initialResult, optResult, searchTreeInput.getOptimizationPerimeter().getFlowCnecs());
    }

    private Map<State, OptimizationResult> optimizeContingencyScenarios(Crac crac,
                                                                        RaoParameters raoParameters,
                                                                        StateTree stateTree,
                                                                        ToolProvider toolProvider,
                                                                        TreeParameters automatonTreeParameters,
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
        AutomatonSimulator automatonSimulator = new AutomatonSimulator(crac, raoParameters, toolProvider, initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
        // Go through all contingency scenarios
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, newVariant, raoParameters.getMultithreadingParameters().getContingencyScenariosInParallel(), true)) {
            AtomicInteger remainingScenarios = new AtomicInteger(stateTree.getContingencyScenarios().size());
            List<ForkJoinTask<Object>> tasks = stateTree.getContingencyScenarios().stream().map(optimizedScenario ->
                networkPool.submit(() -> runScenario(crac, raoParameters, stateTree, toolProvider, automatonTreeParameters, curativeTreeParameters, initialSensitivityOutput, prePerimeterSensitivityOutput, automatonsOnly, optimizedScenario, networkPool, automatonSimulator, contingencyScenarioResults, remainingScenarios))
            ).toList();
            for (ForkJoinTask<Object> task : tasks) {
                try {
                    task.get();
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return contingencyScenarioResults;
    }

    private Object runScenario(Crac crac, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, TreeParameters automatonTreeParameters, TreeParameters curativeTreeParameters, PrePerimeterResult initialSensitivityOutput, PrePerimeterResult prePerimeterSensitivityOutput, boolean automatonsOnly, ContingencyScenario optimizedScenario, AbstractNetworkPool networkPool, AutomatonSimulator automatonSimulator, Map<State, OptimizationResult> contingencyScenarioResults, AtomicInteger remainingScenarios) throws InterruptedException {
        Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
        TECHNICAL_LOGS.info("Optimizing scenario post-contingency {}.", optimizedScenario.getContingency().getId());

        // Init variables
        Optional<State> automatonState = optimizedScenario.getAutomatonState();
        Set<State> curativeStates = new HashSet<>();
        optimizedScenario.getCurativePerimeters().forEach(perimeter -> curativeStates.addAll(perimeter.getAllStates()));
        PrePerimeterResult preCurativeResult = prePerimeterSensitivityOutput;
        double sensitivityFailureOvercost = raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost();

        // Simulate automaton instant
        boolean autoStateSensiFailed = false;
        if (automatonState.isPresent()) {
            AutomatonPerimeterResultImpl automatonResult = automatonSimulator.simulateAutomatonState(automatonState.get(), curativeStates, networkClone, stateTree, automatonTreeParameters);
            if (automatonResult.getComputationStatus() == ComputationStatus.FAILURE) {
                autoStateSensiFailed = true;
                contingencyScenarioResults.put(automatonState.get(), new SkippedOptimizationResultImpl(automatonState.get(), automatonResult.getActivatedNetworkActions(), automatonResult.getActivatedRangeActions(automatonState.get()), ComputationStatus.FAILURE, sensitivityFailureOvercost));
            } else {
                contingencyScenarioResults.put(automatonState.get(), automatonResult);
                preCurativeResult = automatonResult.getPostAutomatonSensitivityAnalysisOutput();
            }
        }
        // Do not simulate curative instant if last sensitivity analysis failed
        // -- if there was no automaton state, check prePerimeterSensitivityOutput sensi status
        // -- or if there was an automaton state that failed
        if (!automatonsOnly
            && automatonState.isEmpty()
            && !optimizedScenario.getCurativePerimeters().isEmpty()
            && prePerimeterSensitivityOutput.getSensitivityStatus(optimizedScenario.getCurativePerimeters().get(0).getRaOptimisationState()) == ComputationStatus.FAILURE
            || automatonState.isPresent()
            && autoStateSensiFailed
        ) {
            curativeStates.forEach(curativeState -> contingencyScenarioResults.put(curativeState, new SkippedOptimizationResultImpl(curativeState, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE, sensitivityFailureOvercost)));
        } else if (!automatonsOnly) {
            boolean allPreviousPerimetersSucceded = true;
            PrePerimeterResult previousPerimeterResult = preCurativeResult;
            // Optimize curative perimeters
            Map<State, OptimizationResult> resultsPerPerimeter = new HashMap<>();
            Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter = new HashMap<>();
            for (Perimeter curativePerimeter : optimizedScenario.getCurativePerimeters()) {
                State curativeState = curativePerimeter.getRaOptimisationState();
                if (previousPerimeterResult == null) {
                    previousPerimeterResult = getPreCurativePerimeterSensitivityAnalysis(crac, curativePerimeter, toolProvider).runBasedOnInitialResults(networkClone, raoInput.getCrac(), null, stateTree.getOperatorsNotSharingCras(), null);
                }
                prePerimeterResultPerPerimeter.put(curativePerimeter.getRaOptimisationState(), previousPerimeterResult);
                if (allPreviousPerimetersSucceded) {
                    OptimizationResult curativeResult = optimizeCurativePerimeter(curativePerimeter, crac, networkClone,
                        raoParameters, stateTree, toolProvider, curativeTreeParameters, initialSensitivityOutput, previousPerimeterResult, resultsPerPerimeter, prePerimeterResultPerPerimeter);
                    allPreviousPerimetersSucceded = curativeResult.getSensitivityStatus() == DEFAULT;
                    contingencyScenarioResults.put(curativeState, curativeResult);
                    applyRemedialActions(networkClone, curativeResult, curativeState);
                    previousPerimeterResult = null;
                    if (allPreviousPerimetersSucceded) {
                        resultsPerPerimeter.put(curativePerimeter.getRaOptimisationState(), curativeResult);
                    }
                } else {
                    contingencyScenarioResults.put(curativeState, new SkippedOptimizationResultImpl(curativeState, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE, sensitivityFailureOvercost));
                }
            }
        }
        TECHNICAL_LOGS.debug("Remaining post-contingency scenarios to optimize: {}", remainingScenarios.decrementAndGet());
        networkPool.releaseUsedNetwork(networkClone);
        return null;
    }

    private PrePerimeterSensitivityAnalysis getPreCurativePerimeterSensitivityAnalysis(Crac crac, Perimeter curativePerimeter, ToolProvider toolProvider) {
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(curativePerimeter.getRaOptimisationState());
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getPotentiallyAvailableRangeActions(curativePerimeter.getRaOptimisationState()));
        for (State curativeState : curativePerimeter.getAllStates()) {
            flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
        }
        return new PrePerimeterSensitivityAnalysis(flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProvider);
    }

    private OptimizationResult optimizeCurativePerimeter(Perimeter curativePerimeter,
                                                         Crac crac,
                                                         Network network,
                                                         RaoParameters raoParameters,
                                                         StateTree stateTree,
                                                         ToolProvider toolProvider,
                                                         TreeParameters curativeTreeParameters,
                                                         PrePerimeterResult initialSensitivityOutput,
                                                         PrePerimeterResult prePerimeterSensitivityOutput,
                                                         Map<State, OptimizationResult> resultsPerPerimeter,
                                                         Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter) {
        State curativeState = curativePerimeter.getRaOptimisationState();
        TECHNICAL_LOGS.info("Optimizing curative state {}.", curativeState.getId());

        Set<State> filteredStates = curativePerimeter.getAllStates().stream()
            .filter(state -> !prePerimeterSensitivityOutput.getSensitivityStatus(state).equals(ComputationStatus.FAILURE))
            .collect(Collectors.toSet());

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> filteredStates.contains(flowCnec.getState()))
            .collect(Collectors.toSet());

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(flowCnecs, loopFlowCnecs, initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), raoParameters);
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(prePerimeterSensitivityOutput);
        boolean stopCriterionReached = isStopCriterionChecked(objectiveFunctionResult, curativeTreeParameters);
        if (stopCriterionReached) {
            NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Collections.emptySet());

            Map<RangeAction<?>, Double> rangeActionSetpointMap = crac.getPotentiallyAvailableRangeActions(curativeState)
                .stream()
                .collect(Collectors.toMap(rangeAction -> rangeAction, prePerimeterSensitivityOutput::getSetpoint));
            RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(rangeActionSetpointMap);
            RangeActionActivationResult rangeActionsResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
            return new OptimizationResultImpl(objectiveFunctionResult, prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, networkActionsResult, rangeActionsResult);
        }

        OptimizationPerimeter optPerimeter = CurativeOptimizationPerimeter.buildForStates(curativeState, curativePerimeter.getAllStates(), crac, network, raoParameters, prePerimeterSensitivityOutput);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(curativeTreeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()))
            .build();

        searchTreeParameters.decreaseRemedialActionUsageLimits(resultsPerPerimeter, prePerimeterResultPerPerimeter);

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialSensitivityOutput)
            .withPrePerimeterResult(prePerimeterSensitivityOutput)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, false).run().join();
        TECHNICAL_LOGS.info("Curative state {} has been optimized.", curativeState.getId());
        return result;
    }

    static boolean isStopCriterionChecked(ObjectiveFunctionResult result, TreeParameters treeParameters) {
        if (result.getVirtualCost() > 1e-6) {
            return false;
        }
        if (result.getFunctionalCost() < -Double.MAX_VALUE / 2 && result.getVirtualCost() < 1e-6) {
            return true;
        }

        if (treeParameters.stopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
            return false;
        } else if (treeParameters.stopCriterion().equals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE)) {
            return result.getCost() < treeParameters.targetObjectiveValue();
        } else {
            throw new OpenRaoException("Unexpected stop criterion: " + treeParameters.stopCriterion());
        }
    }

    // ========================================
    // region Second preventive RAO
    // ========================================

    /**
     * This function decides if a 2nd preventive RAO should be run. It checks the user parameter first, then takes the
     * decision depending on the curative RAO results and the curative RAO stop criterion.
     */
    static boolean shouldRunSecondPreventiveRao(RaoParameters raoParameters, OptimizationResult firstPreventiveResult, Collection<OptimizationResult> curativeRaoResults, RaoResult postFirstRaoResult, java.time.Instant targetEndInstant, long estimatedPreventiveRaoTimeInSeconds, Instant lastCurativeInstant) {
        if (raoParameters.getSecondPreventiveRaoParameters().getExecutionCondition().equals(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED)) {
            return false;
        }
        if (!Objects.isNull(targetEndInstant) && ChronoUnit.SECONDS.between(java.time.Instant.now(), targetEndInstant) < estimatedPreventiveRaoTimeInSeconds) {
            BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
            return false;
        }
        if (raoParameters.getSecondPreventiveRaoParameters().getExecutionCondition().equals(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE)
                && postFirstRaoResult.getCost(lastCurativeInstant) <= postFirstRaoResult.getCost(null)) {
            BUSINESS_LOGS.info("Cost has not increased during RAO, there is no need to run a 2nd preventive RAO.");
            // it is not necessary to compare initial & post-preventive costs since the preventive RAO cannot increase its own cost
            // only compare initial cost with the curative costs
            return false;
        }
        if (raoParameters.getObjectiveFunctionParameters().getPreventiveStopCriterion().equals(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE)
                && firstPreventiveResult.getCost() > 0) {
            // in case of curative optimization even if preventive unsecure (see parameter optimize-curative-if-preventive-unsecure)
            // we do not want to run a second preventive that would not be able to fix the situation, to save time
            BUSINESS_LOGS.info("First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time.");
            return false;
        }
        ObjectiveFunctionParameters.CurativeStopCriterion curativeStopCriterion = raoParameters.getObjectiveFunctionParameters().getCurativeStopCriterion();
        switch (curativeStopCriterion) {
            case MIN_OBJECTIVE:
                // Run 2nd preventive RAO in all cases
                return true;
            case SECURE:
                // Run 2nd preventive RAO if one perimeter of the curative optimization is unsecure
                return isAnyResultUnsecure(curativeRaoResults);
            case PREVENTIVE_OBJECTIVE:
                // Run 2nd preventive RAO if the final result has a worse cost than the preventive perimeter
                return isFinalCostWorseThanPreventive(raoParameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), firstPreventiveResult, postFirstRaoResult, lastCurativeInstant);
            case PREVENTIVE_OBJECTIVE_AND_SECURE:
                // Run 2nd preventive RAO if the final result has a worse cost than the preventive perimeter or is unsecure
                return isAnyResultUnsecure(curativeRaoResults) || isFinalCostWorseThanPreventive(raoParameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), firstPreventiveResult, postFirstRaoResult, lastCurativeInstant);
            default:
                throw new OpenRaoException(String.format("Unknown curative RAO stop criterion: %s", curativeStopCriterion));
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
    private static boolean isFinalCostWorseThanPreventive(double curativeMinObjImprovement, OptimizationResult preventiveResult, RaoResult postFirstRaoResult, Instant curativeInstant) {
        return postFirstRaoResult.getCost(curativeInstant) > preventiveResult.getCost() - curativeMinObjImprovement;
    }

    private RaoResult runSecondPreventiveAndAutoRao(RaoInput raoInput,
                                                    RaoParameters parameters,
                                                    StateTree stateTree,
                                                    TreeParameters automatonTreeParameters,
                                                    ToolProvider toolProvider,
                                                    PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis,
                                                    PrePerimeterResult initialOutput,
                                                    OptimizationResult firstPreventiveResult,
                                                    Map<State, OptimizationResult> postContingencyResults) {
        // Run 2nd preventive RAO
        SecondPreventiveRaoResult secondPreventiveRaoResult;
        try {
            secondPreventiveRaoResult = runSecondPreventiveRao(raoInput, parameters, stateTree, toolProvider, prePerimeterSensitivityAnalysis, initialOutput, firstPreventiveResult, postContingencyResults);
            if (secondPreventiveRaoResult.postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                return new FailedRaoResultImpl("Post-PRA sensitivity analysis failed during 2nd preventive RAO");
            }
        } catch (OpenRaoException e) {
            BUSINESS_LOGS.error(e.getMessage());
            return new FailedRaoResultImpl(e.getMessage());
        }

        // Run 2nd automaton simulation and update results
        BUSINESS_LOGS.info("----- Second automaton simulation [start]");
        Map<State, OptimizationResult> newPostContingencyResults = optimizeContingencyScenarios(raoInput.getCrac(), raoParameters, stateTree, toolProvider, automatonTreeParameters, null, raoInput.getNetwork(), initialOutput, secondPreventiveRaoResult.postPraSensitivityAnalysisOutput, true);
        BUSINESS_LOGS.info("----- Second automaton simulation [end]");

        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        // Always re-run curative sensitivity analysis (re-run is necessary in several specific cases)
        // -- Gather all post contingency remedial actions
        // ---- Curative remedial actions :
        // ------ appliedCras from secondPreventiveRaoResult
        AppliedRemedialActions appliedArasAndCras = secondPreventiveRaoResult.appliedArasAndCras().copyCurative();
        // ------ + curative range actions optimized during second preventive with global optimization
        if (raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) {
            for (Map.Entry<State, OptimizationResult> entry : postContingencyResults.entrySet()) {
                State state = entry.getKey();
                if (!state.getInstant().isCurative()) {
                    continue;
                }
                secondPreventiveRaoResult.perimeterResult().getActivatedRangeActions(state)
                    .forEach(rangeAction -> appliedArasAndCras.addAppliedRangeAction(state, rangeAction, secondPreventiveRaoResult.perimeterResult.getOptimizedSetpoint(rangeAction, state)));
            }
        }
        // ---- Auto remedial actions : computed during second auto, saved in newPostContingencyResults
        // ---- only RAs from perimeters that haven't failed are included in appliedArasAndCras
        // ---- this check is only performed here because SkippedOptimizationResultImpl with appliedRas can only be generated for AUTO instant
        newPostContingencyResults.entrySet().stream().filter(entry ->
                !(entry.getValue() instanceof SkippedOptimizationResultImpl) && entry.getKey().getInstant().isAuto())
            .forEach(entry -> {
                appliedArasAndCras.addAppliedNetworkActions(entry.getKey(), entry.getValue().getActivatedNetworkActions());
                entry.getValue().getActivatedRangeActions(entry.getKey()).forEach(rangeAction -> appliedArasAndCras.addAppliedRangeAction(entry.getKey(), rangeAction, entry.getValue().getOptimizedSetpoint(rangeAction, entry.getKey())));
            });
        // Run curative sensitivity analysis with appliedArasAndCras
        // TODO: this is too slow, we can replace it with load-flow computations or security analysis since we don't need sensitivity values
        PrePerimeterResult postCraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(raoInput.getNetwork(), raoInput.getCrac(), initialOutput, Collections.emptySet(), appliedArasAndCras);
        if (postCraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
            return new FailedRaoResultImpl("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
        }
        for (Map.Entry<State, OptimizationResult> entry : postContingencyResults.entrySet()) {
            State state = entry.getKey();
            if (!state.getInstant().isCurative()) {
                continue;
            }
            // Specific case : curative state was previously skipped because it led to a sensitivity analysis failure.
            // Curative state is still a SkippedOptimizationResultImpl, but its computation status must be updated
            if (entry.getValue() instanceof SkippedOptimizationResultImpl) {
                newPostContingencyResults.put(state, new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), postCraSensitivityAnalysisOutput.getSensitivityStatus(entry.getKey()), raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost()));
            } else {
                newPostContingencyResults.put(state, new CurativeWithSecondPraoResult(state, entry.getValue(), secondPreventiveRaoResult.perimeterResult(), secondPreventiveRaoResult.remedialActionsExcluded(), postCraSensitivityAnalysisOutput));
            }
        }
        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, postCraSensitivityAnalysisOutput, parameters.getObjectiveFunctionParameters().getType(), NUMBER_LOGGED_ELEMENTS_END_RAO);

        return new PreventiveAndCurativesRaoResultImpl(stateTree,
            initialOutput,
            firstPreventiveResult,
            secondPreventiveRaoResult.perimeterResult(),
            secondPreventiveRaoResult.remedialActionsExcluded(),
            secondPreventiveRaoResult.postPraSensitivityAnalysisOutput(),
            newPostContingencyResults,
            postCraSensitivityAnalysisOutput,
            raoInput.getCrac());
    }

    private record SecondPreventiveRaoResult(OptimizationResult perimeterResult,
                                             PrePerimeterResult postPraSensitivityAnalysisOutput,
                                             Set<RemedialAction<?>> remedialActionsExcluded,
                                             AppliedRemedialActions appliedArasAndCras) {
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
                                                             OptimizationResult firstPreventiveResult,
                                                             Map<State, OptimizationResult> postContingencyResults) {
        Network network = raoInput.getNetwork();
        // Go back to the initial state of the network, saved in the SECOND_PREVENTIVE_STATE variant
        network.getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);

        // Get the applied network actions for every contingency perimeter
        AppliedRemedialActions appliedArasAndCras = new AppliedRemedialActions();
        Crac crac = raoInput.getCrac();
        if (crac.hasAutoInstant()) {
            addAppliedNetworkActionsPostContingency(crac.getInstants(InstantKind.AUTO), appliedArasAndCras, postContingencyResults);
        }
        addAppliedNetworkActionsPostContingency(crac.getInstants(InstantKind.CURATIVE), appliedArasAndCras, postContingencyResults);
        // Get the applied range actions for every auto contingency perimeter
        if (crac.hasAutoInstant()) {
            addAppliedRangeActionsPostContingency(crac.getInstants(InstantKind.AUTO), appliedArasAndCras, postContingencyResults);
        }

        // Apply 1st preventive results for range actions that are both preventive and auto or curative. This way we are sure
        // that the optimal setpoints of the curative results stay coherent with their allowed range and close to
        // optimality in their perimeters. These range actions will be excluded from 2nd preventive RAO.
        Set<RemedialAction<?>> remedialActionsExcluded = new HashSet<>();
        if (!parameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) { // keep old behaviour
            remedialActionsExcluded = new HashSet<>(getRangeActionsExcludedFromSecondPreventive(crac, firstPreventiveResult, postContingencyResults));
            applyPreventiveResultsForAutoOrCurativeRangeActions(network, firstPreventiveResult, crac);
            addAppliedRangeActionsPostContingency(crac.getInstants(InstantKind.CURATIVE), appliedArasAndCras, postContingencyResults);
        }

        // Run a first sensitivity computation using initial network and applied CRAs
        // If any sensitivity computation fails, fail and fall back to 1st preventive result
        // TODO: can we / do we want to improve this behavior by excluding the failed contingencies?
        PrePerimeterResult sensiWithPostContingencyRemedialActions = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, stateTree.getOperatorsNotSharingCras(), appliedArasAndCras);
        if (sensiWithPostContingencyRemedialActions.getSensitivityStatus() == FAILURE) {
            throw new OpenRaoException("Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed");
        }
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            sensiWithPostContingencyRemedialActions,
            parameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // Run second preventive RAO
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        String newVariant = RandomizedString.getRandomizedString("SecondPreventive", raoInput.getNetwork().getVariantManager().getVariantIds(), 10);
        raoInput.getNetwork().getVariantManager().cloneVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT, newVariant, true);
        raoInput.getNetwork().getVariantManager().setWorkingVariant(newVariant);
        OptimizationResult secondPreventiveResult = optimizeSecondPreventivePerimeter(raoInput, parameters, stateTree, toolProvider, initialOutput, sensiWithPostContingencyRemedialActions, firstPreventiveResult, postContingencyResults, appliedArasAndCras)
                .join().getOptimizationResult(crac.getPreventiveState());
        // Re-run sensitivity computation based on PRAs without CRAs, to access after PRA results
        PrePerimeterResult postPraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, stateTree.getOperatorsNotSharingCras(), null);
        if (postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed");
        }
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");
        return new SecondPreventiveRaoResult(secondPreventiveResult, postPraSensitivityAnalysisOutput, remedialActionsExcluded, appliedArasAndCras);
    }

    static void addAppliedNetworkActionsPostContingency(Set<Instant> instants, AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults) {
        instants.forEach(instant ->
            postContingencyResults.forEach((state, optimizationResult) -> {
                if (state.getInstant().equals(instant)) {
                    appliedRemedialActions.addAppliedNetworkActions(state, optimizationResult.getActivatedNetworkActions());
                }
            })
        );
    }

    static void addAppliedRangeActionsPostContingency(Set<Instant> instants, AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults) {
        // Add all range actions that were activated.
        // Curative/ preventive duplicates are handled via exclusion from 2nd preventive
        instants.forEach(instant ->
            postContingencyResults.forEach((state, optimizationResult) -> {
                if (state.getInstant().equals(instant)) {
                    optimizationResult.getActivatedRangeActions(state).forEach(rangeAction -> appliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)));
                }
            })
        );
    }

    private CompletableFuture<OneStateOnlyRaoResultImpl> optimizeSecondPreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialOutput, PrePerimeterResult prePerimeterResult, OptimizationResult firstPreventiveResult, Map<State, OptimizationResult> postContingencyResults, AppliedRemedialActions appliedCras) {

        OptimizationPerimeter optPerimeter;
        Crac crac = raoInput.getCrac();
        Instant preventiveInstant = crac.getPreventiveInstant();
        State preventiveState = crac.getPreventiveState();
        Set<RangeAction<?>> excludedRangeActions = getRangeActionsExcludedFromSecondPreventive(crac, firstPreventiveResult, postContingencyResults);

        if (raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) {
            optPerimeter = GlobalOptimizationPerimeter.build(crac, raoInput.getNetwork(), raoParameters, prePerimeterResult);
        } else {
            Set<RangeAction<?>> rangeActionsFor2p = new HashSet<>(crac.getRangeActions());
            excludedRangeActions.forEach(rangeAction -> {
                BUSINESS_WARNS.warn("Range action {} will not be considered in 2nd preventive RAO as it is also auto/curative (or its network element has an associated ARA/CRA)", rangeAction.getId());
                rangeActionsFor2p.remove(rangeAction);
            });
            optPerimeter = PreventiveOptimizationPerimeter.buildWithAllCnecs(crac, rangeActionsFor2p, raoInput.getNetwork(), raoParameters, prePerimeterResult);
        }

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(TreeParameters.buildForSecondPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()))
            .build();

        // update RaUsageLimits with already applied RangeActions
        if (!raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions() && searchTreeParameters.getRaLimitationParameters().containsKey(preventiveInstant)) {
            Set<RangeAction<?>> activatedPreventiveRangeActions = firstPreventiveResult.getActivatedRangeActions(preventiveState);
            Set<RangeAction<?>> excludedActivatedRangeActions = excludedRangeActions.stream().filter(activatedPreventiveRangeActions::contains).collect(Collectors.toSet());
            searchTreeParameters.setRaLimitationsForSecondPreventive(searchTreeParameters.getRaLimitationParameters().get(preventiveInstant), excludedActivatedRangeActions, preventiveInstant);
        }

        if (raoParameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao()) {
            // Set the optimal set of network actions decided in 1st preventive RAO as a hint for 2nd preventive RAO
            searchTreeParameters.getNetworkActionParameters().addNetworkActionCombination(new NetworkActionCombination(firstPreventiveResult.getActivatedNetworkActions(), true));
        }

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialOutput)
            .withPrePerimeterResult(prePerimeterResult)
            .withPreOptimizationAppliedNetworkActions(appliedCras) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialOutput, prePerimeterResult, new HashSet<>(), raoParameters))
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();

        // apply PRAs
        raoInput.getNetwork().getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        result.getActivatedRangeActions(preventiveState).forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), result.getOptimizedSetpoint(rangeAction, preventiveState)));
        result.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(preventiveState, prePerimeterResult, result, optPerimeter.getFlowCnecs()));
    }

    /**
     * This method applies range action results on the network, for range actions that are auto or curative
     * It is used for second preventive optimization along with 1st preventive results in order to keep the result
     * of 1st preventive for range actions that are both preventive and auto or curative
     */
    static void applyPreventiveResultsForAutoOrCurativeRangeActions(Network network, OptimizationResult preventiveResult, Crac crac) {
        preventiveResult.getActivatedRangeActions(crac.getPreventiveState()).stream()
            .filter(rangeAction -> isRangeActionAutoOrCurative(rangeAction, crac))
            .forEach(rangeAction -> rangeAction.apply(network, preventiveResult.getOptimizedSetpoint(rangeAction, crac.getPreventiveState())));
    }

    /**
     * Returns the set of range actions that are excluded from the 2nd preventive RAO.
     * The concerned range actions meet certain criterion.
     * 1- The RA has a range limit relative to the previous instant.
     * This way we avoid incoherence between preventive & curative tap positions.
     * 2- For the remaining RAs we are going to remove some for the reason explained below.
     * Let's consider a rangeAction that has the same tap in preventive and in another state.
     * If so, considering it in the second preventive optimization could change its tap for preventive only.
     * Therefore, the RA would no longer have the same taps in preventive and for the given contingency state: It's consider used for the given state.
     * That could lead the RAO to wrongly exceed the RaUsageLimits for the given state.
     * To avoid this, we don't want to optimize these RAs.
     * For the same reason, we are going to check preventive RAs that share the same network elements as auto or curative RAs.
     */
    static Set<RangeAction<?>> getRangeActionsExcludedFromSecondPreventive(Crac crac, OptimizationResult firstPreventiveResult, Map<State, OptimizationResult> contingencyResults) {

        // Excludes every non-preventive RA.
        Set<RangeAction<?>> nonPreventiveRangeActions = crac.getRangeActions().stream().filter(ra -> !isRangeActionPreventive(ra, crac)).collect(Collectors.toSet());
        Set<RangeAction<?>> rangeActionsToExclude = new HashSet<>(nonPreventiveRangeActions);

        // Gathers PRAs that are also ARA/CRAs.
        Set<RangeAction<?>> multipleInstantRangeActions = crac.getRangeActions().stream()
            .filter(ra -> isRangeActionPreventive(ra, crac) && isRangeActionAutoOrCurative(ra, crac))
            .collect(Collectors.toSet());

        // Excludes the ones that have a range limit relative to the previous instant.
        multipleInstantRangeActions.stream().filter(CastorFullOptimization::raHasRelativeToPreviousInstantRange).forEach(rangeActionsToExclude::add);
        rangeActionsToExclude.forEach(multipleInstantRangeActions::remove);

        // We look for PRAs that share the same network element as ARA/CRAs as the same rules apply to them.
        Map<RangeAction<?>, Set<RangeAction<?>>> correspondanceMap = new HashMap<>();
        crac.getRangeActions().stream().filter(ra -> isRangeActionPreventive(ra, crac) && !isRangeActionAutoOrCurative(ra, crac)).forEach(pra -> {
            Set<NetworkElement> praNetworkElements = pra.getNetworkElements();
            for (RangeAction<?> cra : nonPreventiveRangeActions) {
                if (cra.getNetworkElements().equals(praNetworkElements)) {
                    if (raHasRelativeToPreviousInstantRange(cra)) {
                        // Excludes PRAs which share the same network element as an ARA/CRA with a range limit relative to the previous instant.
                        rangeActionsToExclude.add(pra);
                        correspondanceMap.remove(pra);
                        break;
                    } else {
                        // Gathers PRAs with their associated ARA/CRAs inside a map.
                        correspondanceMap.putIfAbsent(pra, new HashSet<>());
                        correspondanceMap.get(pra).add(cra);
                    }
                }
            }
        });

        // If first preventive diverged, we want to remove every range action that is both preventive and auto or curative.
        if (firstPreventiveResult instanceof SkippedOptimizationResultImpl) {
            multipleInstantRangeActions.addAll(correspondanceMap.keySet());
            return multipleInstantRangeActions;
        }

        // Excludes RAs that put crac RaUsageLimits at risk.
        // First, we filter out state that diverged because we know no set-point was chosen for this state.
        Map<State, OptimizationResult> newContingencyResults = new HashMap<>(contingencyResults);
        newContingencyResults.entrySet().removeIf(entry -> entry.getValue() instanceof SkippedOptimizationResultImpl);

        // Then, we build a map that gives for each RA, its tap at each state it's available at.
        State preventiveState = crac.getPreventiveState();
        Map<State, Map<RangeAction<?>, Double>> setPointResults = buildSetPointResultsMap(crac, firstPreventiveResult, newContingencyResults, correspondanceMap, multipleInstantRangeActions, preventiveState);

        // Finally, we filter out RAs that put crac RaUsageLimits at risk.
        rangeActionsToExclude.addAll(getRangeActionsToRemove(crac, preventiveState, setPointResults, newContingencyResults));
        return rangeActionsToExclude;
    }

    /**
     * Creates a map that gives for a given state, each available RA with its tap.
     * The only subtlety being that RAs sharing exactly the same network elements are considered to be only one RA.
     */
    private static Map<State, Map<RangeAction<?>, Double>> buildSetPointResultsMap(Crac crac, OptimizationResult firstPreventiveResult, Map<State, OptimizationResult> contingencyResults, Map<RangeAction<?>, Set<RangeAction<?>>> correspondanceMap, Set<RangeAction<?>> multipleInstantRangeActions, State preventiveState) {
        Map<State, Map<RangeAction<?>, Double>> setPointResults = new HashMap<>(Map.of(preventiveState, new HashMap<>()));
        correspondanceMap.forEach((pra, associatedCras) -> {
            setPointResults.get(preventiveState).put(pra, firstPreventiveResult.getOptimizedSetpoint(pra, preventiveState));
            associatedCras.forEach(cra -> contingencyResults.forEach((state, result) -> {
                if (isRangeActionAvailableInState(cra, state, crac)) {
                    setPointResults.putIfAbsent(state, new HashMap<>());
                    setPointResults.get(state).put(pra, result.getOptimizedSetpoint(cra, state));
                }
            }));
        });
        multipleInstantRangeActions.forEach(ra -> {
            setPointResults.get(preventiveState).put(ra, firstPreventiveResult.getOptimizedSetpoint(ra, preventiveState));
            contingencyResults.forEach((state, result) -> {
                if (isRangeActionAvailableInState(ra, state, crac)) {
                    setPointResults.putIfAbsent(state, new HashMap<>());
                    setPointResults.get(state).put(ra, result.getOptimizedSetpoint(ra, state));
                }
            });
        });
        return setPointResults;
    }

    /**
     * Checks if raUsageLimits are at risk if we choose to re-optimize a range action.
     * Returns True if it's at risk, False otherwise.
     */
    private static boolean shouldRemoveRaDueToUsageLimits(String operator, RaUsageLimits raUsageLimits, Set<RangeAction<?>> activatableRangeActions, Set<NetworkAction> activatedNetworkActions) {
        if (operator == null) {
            return raUsageLimits.getMaxRa() < activatableRangeActions.size() + activatedNetworkActions.size();
        }

        Set<RemedialAction<?>> activatableRemedialActions = new HashSet<>(activatableRangeActions);
        activatableRemedialActions.addAll(activatedNetworkActions);

        long activatableRangeActionsForTheTso = activatableRangeActions.stream().filter(ra -> operator.equals(ra.getOperator())).count();
        long activatableRemedialActionsForTheTso = activatableRemedialActions.stream().filter(ra -> operator.equals(ra.getOperator())).count();
        long activatableTsos = activatableRemedialActions.stream().map(RemedialAction::getOperator).filter(Objects::nonNull).distinct().count();

        int limitingRangeActionValueForTheTso = raUsageLimits.getMaxPstPerTso().getOrDefault(operator, Integer.MAX_VALUE);
        int limitingRemedialActionValueForTheTso = raUsageLimits.getMaxRaPerTso().getOrDefault(operator, Integer.MAX_VALUE);

        return raUsageLimits.getMaxRa() < activatableRangeActions.size() + activatedNetworkActions.size()
            || limitingRangeActionValueForTheTso < activatableRangeActionsForTheTso
            || limitingRemedialActionValueForTheTso < activatableRemedialActionsForTheTso
            || raUsageLimits.getMaxTso() < activatableTsos;
    }

    /**
     * Gathers every range action that should not be considered in the second preventive if those 2 criterion are met :
     * 1- The range action has the same tap in preventive and in a contingency scenario.
     * 2- For the given state, the crac has limiting RaUsageLimits.
     */
    private static Set<RangeAction<?>> getRangeActionsToRemove(Crac crac, State preventiveState, Map<State, Map<RangeAction<?>, Double>> setPointResults, Map<State, OptimizationResult> contingencyResults) {
        Set<RangeAction<?>> rangeActionsToRemove = new HashSet<>();
        setPointResults.forEach((state, spMap) -> {
            if (!state.isPreventive()) {
                Set<RangeAction<?>> activatableRangeActions = crac.getPotentiallyAvailableRangeActions(state);
                Set<NetworkAction> activatedNetworkActions = contingencyResults.get(state).getActivatedNetworkActions();
                spMap.forEach((ra, setPoint) -> {
                    if (setPoint.equals(setPointResults.get(preventiveState).get(ra))
                        && crac.getRaUsageLimitsPerInstant().containsKey(state.getInstant())
                        && shouldRemoveRaDueToUsageLimits(ra.getOperator(), crac.getRaUsageLimits(state.getInstant()), activatableRangeActions, activatedNetworkActions)) {
                        rangeActionsToRemove.add(ra);
                    }
                });
            }
        });
        return rangeActionsToRemove;
    }

    /**
     * Returns True if the rangeAction has a RELATIVE_TO_PREVIOUS_INSTANT range. Else, returns False.
     */
    private static boolean raHasRelativeToPreviousInstantRange(RangeAction<?> rangeAction) {
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            return pstRangeAction.getRanges().stream().anyMatch(tapRange -> tapRange.getRangeType().equals(RELATIVE_TO_PREVIOUS_INSTANT));
        }
        return ((StandardRangeAction<?>) rangeAction).getRanges().stream().anyMatch(standardRange -> standardRange.getRangeType().equals(RELATIVE_TO_PREVIOUS_INSTANT));
    }

    static boolean isRangeActionPreventive(RangeAction<?> rangeAction, Crac crac) {
        return isRangeActionAvailableInState(rangeAction, crac.getPreventiveState(), crac);
    }

    static boolean isRangeActionAutoOrCurative(RangeAction<?> rangeAction, Crac crac) {
        return crac.getStates().stream()
                .filter(state -> state.getInstant().isAuto() || state.getInstant().isCurative())
                .anyMatch(state -> isRangeActionAvailableInState(rangeAction, state, crac));
    }

    static boolean isRangeActionAvailableInState(RangeAction<?> rangeAction, State state, Crac crac) {
        return crac.getPotentiallyAvailableRangeActions(state).contains(rangeAction);
    }

    // ========================================
    // endregion
    // ========================================
}
