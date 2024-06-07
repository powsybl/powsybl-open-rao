/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.commons.*;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
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

import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.FAILURE;
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

    public CompletableFuture<RaoResult> run(ReportNode reportNode) {
        ReportNode raoReportNode = CastorAlgorithmReports.reportRunCastorFullOptimization(reportNode);
        RaoUtil.initData(raoInput, raoParameters, raoReportNode);
        StateTree stateTree = new StateTree(raoInput.getCrac(), raoReportNode);
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters, raoReportNode);

        // ----- INITIAL SENSI -----
        // compute initial sensitivity on all CNECs
        // (this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters)
        ReportNode inititalSensitivityReportNode = CastorAlgorithmReports.reportInitialSensitivity(raoReportNode);
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            raoInput.getCrac().getFlowCnecs(),
            raoInput.getCrac().getRangeActions(),
            raoParameters,
            toolProvider);

        PrePerimeterResult initialOutput;
        initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac(), inititalSensitivityReportNode);
        RaoLogger.logSensitivityAnalysisResults("Initial sensitivity analysis: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            new RangeActionActivationResultImpl(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(raoInput.getNetwork(), raoInput.getCrac().getRangeActions())),
            initialOutput,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO,
            inititalSensitivityReportNode);
        if (initialOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            CastorAlgorithmReports.reportInitialSensitivityFailure(inititalSensitivityReportNode);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl());
        }

        // ----- PREVENTIVE PERIMETER OPTIMIZATION -----
        // run search tree on preventive perimeter
        java.time.Instant preventiveRaoStartInstant = java.time.Instant.now();
        ReportNode preventivePerimeterReportNode = CastorAlgorithmReports.reportPreventivePerimeter(raoReportNode);

        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

        if (stateTree.getContingencyScenarios().isEmpty()) {
            CompletableFuture<RaoResult> result = CompletableFuture.completedFuture(optimizePreventivePerimeter(raoInput, raoParameters, stateTree, toolProvider, initialOutput, preventivePerimeterReportNode));
            CastorAlgorithmReports.reportPreventivePerimeterEnd(preventivePerimeterReportNode);
            return result;
        }

        PerimeterResult preventiveResult = optimizePreventivePerimeter(raoInput, raoParameters, stateTree, toolProvider, initialOutput, preventivePerimeterReportNode).getPerimeterResult(raoInput.getCrac().getPreventiveState());
        CastorAlgorithmReports.reportPreventivePerimeterEnd(preventivePerimeterReportNode);
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

        PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, raoInput.getCrac(), initialOutput, initialOutput, Collections.emptySet(), null, reportNode);
        if (preCurativeSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            CastorAlgorithmReports.reportPreCurativeSensitivityAnalysisFailed(raoReportNode);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl());
        }
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after preventive remedial actions: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            new RangeActionActivationResultImpl(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(raoInput.getNetwork(), raoInput.getCrac().getRangeActions())),
            preCurativeSensitivityAnalysisOutput,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO,
            raoReportNode);

        RaoResult mergedRaoResults;

        // ----- CURATIVE PERIMETERS OPTIMIZATION -----
        // optimize contingency scenarios (auto + curative instants)

        // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
        // (however RAO could continue depending on parameter optimize-curative-if-basecase-unsecure)
        if (shouldStopOptimisationIfPreventiveUnsecure(preventiveOptimalCost)) {
            CastorAlgorithmReports.reportDoNotOptimizeCurativeBecausePreventiveUnsecure(raoReportNode);
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(raoInput.getCrac().getPreventiveState(), initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, raoInput.getCrac());
            // log results
            RaoLogger.logMostLimitingElementsResults(preCurativeSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType(), NUMBER_LOGGED_ELEMENTS_END_RAO, raoReportNode, TypedValue.INFO_SEVERITY);

            return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters(), raoReportNode);
        }

        ReportNode postContingencyPerimetersReportNode = CastorAlgorithmReports.reportPostContingencyPerimeters(raoReportNode);
        Map<State, OptimizationResult> postContingencyResults = optimizeContingencyScenarios(raoInput.getCrac(), raoParameters, stateTree, toolProvider, automatonTreeParameters, curativeTreeParameters, network, initialOutput, preCurativeSensitivityAnalysisOutput, false, postContingencyPerimetersReportNode);
        CastorAlgorithmReports.reportPostContingencyPerimetersEnd(postContingencyPerimetersReportNode);

        // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----
        mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, postContingencyResults, raoInput.getCrac());
        boolean logFinalResultsOutsideOfSecondPreventive = true;
        // Run second preventive when necessary
        if (shouldRunSecondPreventiveRao(raoParameters, preventiveResult, postContingencyResults.values(), mergedRaoResults, targetEndInstant, preventiveRaoTime, raoInput.getCrac().getLastInstant(), raoReportNode)) {
            // TODO: check if search tree has to be ran again
            RaoResult secondPreventiveRaoResults = runSecondPreventiveAndAutoRao(raoInput, raoParameters, stateTree, automatonTreeParameters, toolProvider, prePerimeterSensitivityAnalysis, initialOutput, preventiveResult, postContingencyResults, raoReportNode);
            if (secondPreventiveImprovesResults(secondPreventiveRaoResults, mergedRaoResults, raoReportNode)) {
                mergedRaoResults = secondPreventiveRaoResults;
                mergedRaoResults.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST);
                logFinalResultsOutsideOfSecondPreventive = false;
            } else {
                mergedRaoResults.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION);
            }
        }
        // Log final results
        if (logFinalResultsOutsideOfSecondPreventive) {
            CastorAlgorithmReports.reportResultsMergingPreventiveAndPostContingency(raoReportNode);
            RaoLogger.logMostLimitingElementsResults(stateTree.getBasecaseScenario(), preventiveResult, stateTree.getContingencyScenarios(), postContingencyResults, raoParameters.getObjectiveFunctionParameters().getType(), NUMBER_LOGGED_ELEMENTS_END_RAO, raoReportNode);
        }

        return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters(), raoReportNode);
    }

    private boolean shouldStopOptimisationIfPreventiveUnsecure(double preventiveOptimalCost) {
        return raoParameters.getObjectiveFunctionParameters().getPreventiveStopCriterion().equals(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE)
                && preventiveOptimalCost > 0
                && !raoParameters.getObjectiveFunctionParameters().getOptimizeCurativeIfPreventiveUnsecure();
    }

    /**
     * Return true if 2P has decreased cost
     */
    private boolean secondPreventiveImprovesResults(RaoResult secondPreventiveRaoResults, RaoResult mergedRaoResults, ReportNode raoReportNode) {
        if (secondPreventiveRaoResults instanceof FailedRaoResultImpl) {
            CastorAlgorithmReports.reportSecondPreventiveFailed(raoReportNode);
            return false;
        }
        if (mergedRaoResults.getComputationStatus() == ComputationStatus.FAILURE && secondPreventiveRaoResults.getComputationStatus() != ComputationStatus.FAILURE) {
            CastorAlgorithmReports.reportSecondPreventiveFixedSituation(raoReportNode);
            return true;
        }
        Instant curativeInstant = raoInput.getCrac().getLastInstant();
        double firstPreventiveCost = mergedRaoResults.getCost(curativeInstant);
        double secondPreventiveCost = secondPreventiveRaoResults.getCost(curativeInstant);
        if (secondPreventiveCost > firstPreventiveCost) {
            CastorAlgorithmReports.reportSecondPreventiveIncreasedOverallCost(
                    raoReportNode,
                    firstPreventiveCost,
                    mergedRaoResults.getFunctionalCost(curativeInstant),
                    mergedRaoResults.getVirtualCost(curativeInstant),
                    secondPreventiveCost,
                    secondPreventiveRaoResults.getFunctionalCost(curativeInstant),
                    secondPreventiveRaoResults.getVirtualCost(curativeInstant)
            );
            return false;
        }
        return true;
    }

    /**
     * Return initial result if RAO has increased cost
     */
    private CompletableFuture<RaoResult> postCheckResults(RaoResult raoResult, PrePerimeterResult initialResult, ObjectiveFunctionParameters objectiveFunctionParameters, ReportNode reportNode) {
        RaoResult finalRaoResult = raoResult;

        double initialCost = initialResult.getCost();
        double initialFunctionalCost = initialResult.getFunctionalCost();
        double initialVirtualCost = initialResult.getVirtualCost();
        Instant lastInstant = raoInput.getCrac().getLastInstant();
        double finalCost = finalRaoResult.getCost(lastInstant);
        double finalFunctionalCost = finalRaoResult.getFunctionalCost(lastInstant);
        double finalVirtualCost = finalRaoResult.getVirtualCost(lastInstant);

        if (objectiveFunctionParameters.getForbidCostIncrease() && finalCost > initialCost) {
            CastorAlgorithmReports.reportRaoIncreasedOverallCost(reportNode, initialCost, initialFunctionalCost, initialVirtualCost, finalCost, finalFunctionalCost, finalVirtualCost);
            // log results
            RaoLogger.logMostLimitingElementsResults(initialResult, objectiveFunctionParameters.getType(), NUMBER_LOGGED_ELEMENTS_END_RAO, reportNode, TypedValue.INFO_SEVERITY);
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
        CastorAlgorithmReports.reportRaoCostEvolution(reportNode, initialCost, initialFunctionalCost, initialVirtualCost, finalCost, finalFunctionalCost, finalVirtualCost);

        return CompletableFuture.completedFuture(finalRaoResult);
    }

    private OneStateOnlyRaoResultImpl optimizePreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialResult, ReportNode reportNode) {

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(stateTree.getBasecaseScenario(), raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResult, reportNode);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, raoInput.getCrac())
            .withTreeParameters(TreeParameters.buildForPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras(), raoInput.getCrac(), reportNode))
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(initialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResult, initialResult, initialResult, raoInput.getCrac(), Collections.emptySet(), raoParameters, reportNode))
            .withToolProvider(toolProvider)
            .withOutageInstant(raoInput.getCrac().getOutageInstant())
            .build();

        OptimizationResult optResult = new SearchTree(searchTreeInput, searchTreeParameters, true).run(reportNode).join();
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
                                                                        boolean automatonsOnly,
                                                                        ReportNode reportNode) {
        Map<State, OptimizationResult> contingencyScenarioResults = new ConcurrentHashMap<>();
        // Create a new variant
        String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);
        // Create an automaton simulator
        AutomatonSimulator automatonSimulator = new AutomatonSimulator(crac, raoParameters, toolProvider, initialSensitivityOutput, prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), NUMBER_LOGGED_ELEMENTS_DURING_RAO, reportNode);
        // Go through all contingency scenarios
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, newVariant, raoParameters.getMultithreadingParameters().getContingencyScenariosInParallel(), true)) {
            AtomicInteger remainingScenarios = new AtomicInteger(stateTree.getContingencyScenarios().size());
            List<ForkJoinTask<ReportNode>> tasks = stateTree.getContingencyScenarios().stream().map(optimizedScenario ->
                networkPool.submit(() -> runScenario(crac, raoParameters, stateTree, toolProvider, automatonTreeParameters, curativeTreeParameters, initialSensitivityOutput, prePerimeterSensitivityOutput, automatonsOnly, optimizedScenario, networkPool, automatonSimulator, contingencyScenarioResults, remainingScenarios))
            ).toList();
            for (ForkJoinTask<ReportNode> task : tasks) {
                try {
                    reportNode.include(task.get());
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

    private ReportNode runScenario(Crac crac, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, TreeParameters automatonTreeParameters, TreeParameters curativeTreeParameters, PrePerimeterResult initialSensitivityOutput, PrePerimeterResult prePerimeterSensitivityOutput, boolean automatonsOnly, ContingencyScenario optimizedScenario, AbstractNetworkPool networkPool, AutomatonSimulator automatonSimulator, Map<State, OptimizationResult> contingencyScenarioResults, AtomicInteger remainingScenarios) throws InterruptedException {
        ReportNode rootReportNode = CastorAlgorithmReports.reportPostContingencyScenarioOptimization();

        ReportNode scenarioReportNode = CastorAlgorithmReports.reportPostContingencyScenarioOptimization(rootReportNode, optimizedScenario.getContingency().getId());
        Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks

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
            for (Perimeter curativePerimeter : optimizedScenario.getCurativePerimeters()) {
                State curativeState = curativePerimeter.getRaOptimisationState();
                if (previousPerimeterResult == null) {
                    previousPerimeterResult = getPreCurativePerimeterSensitivityAnalysis(crac, curativePerimeter, toolProvider).runBasedOnInitialResults(networkClone, raoInput.getCrac(), previousPerimeterResult, previousPerimeterResult, stateTree.getOperatorsNotSharingCras(), null, scenarioReportNode);
                }
                if (allPreviousPerimetersSucceded) {
                    OptimizationResult curativeResult = optimizeCurativePerimeter(curativePerimeter, crac, networkClone,
                        raoParameters, stateTree, toolProvider, curativeTreeParameters, initialSensitivityOutput, previousPerimeterResult, resultsPerPerimeter, scenarioReportNode);
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
        int remainingScenariosNumber = remainingScenarios.decrementAndGet();
        CastorAlgorithmReports.reportRemainingPostContingencyScenarios(scenarioReportNode, remainingScenariosNumber);
        networkPool.releaseUsedNetwork(networkClone);
        return rootReportNode;
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
                                                         ReportNode reportNode) {
        State curativeState = curativePerimeter.getRaOptimisationState();
        CastorAlgorithmReports.reportOptimizingCurativeState(reportNode, curativeState.getId());

        OptimizationPerimeter optPerimeter = CurativeOptimizationPerimeter.buildForStates(curativeState, curativePerimeter.getAllStates(), crac, network, raoParameters, prePerimeterSensitivityOutput, reportNode);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(curativeTreeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras(), raoInput.getCrac(), reportNode))
            .build();

        searchTreeParameters.decreaseRemedialActionUsageLimits(resultsPerPerimeter);

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialSensitivityOutput)
            .withPrePerimeterResult(prePerimeterSensitivityOutput)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialSensitivityOutput, prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, raoInput.getCrac(), stateTree.getOperatorsNotSharingCras(), raoParameters, reportNode))
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, false).run(reportNode).join();
        CastorAlgorithmReports.reportCurativeStateOptimized(reportNode, curativeState.getId());
        return result;
    }

    // ========================================
    // region Second preventive RAO
    // ========================================

    /**
     * This function decides if a 2nd preventive RAO should be run. It checks the user parameter first, then takes the
     * decision depending on the curative RAO results and the curative RAO stop criterion.
     */
    static boolean shouldRunSecondPreventiveRao(RaoParameters raoParameters, OptimizationResult firstPreventiveResult, Collection<OptimizationResult> curativeRaoResults, RaoResult postFirstRaoResult, java.time.Instant targetEndInstant, long estimatedPreventiveRaoTimeInSeconds, Instant lastCurativeInstant, ReportNode reportNode) {
        if (raoParameters.getSecondPreventiveRaoParameters().getExecutionCondition().equals(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED)) {
            return false;
        }
        if (!Objects.isNull(targetEndInstant) && ChronoUnit.SECONDS.between(java.time.Instant.now(), targetEndInstant) < estimatedPreventiveRaoTimeInSeconds) {
            CastorAlgorithmReports.reportNotEnoughTimeForSecondPreventive(reportNode, targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
            return false;
        }
        if (raoParameters.getSecondPreventiveRaoParameters().getExecutionCondition().equals(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE)
                && postFirstRaoResult.getCost(lastCurativeInstant) <= postFirstRaoResult.getCost(null)) {
            CastorAlgorithmReports.reportNoNeedSecondPreventiveBecauseCostHasNotIncreasedDuringRao(reportNode);
            // it is not necessary to compare initial & post-preventive costs since the preventive RAO cannot increase its own cost
            // only compare initial cost with the curative costs
            return false;
        }
        if (raoParameters.getObjectiveFunctionParameters().getPreventiveStopCriterion().equals(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE)
                && firstPreventiveResult.getCost() > 0) {
            // in case of curative optimization even if preventive unsecure (see parameter optimize-curative-if-preventive-unsecure)
            // we do not want to run a second preventive that would not be able to fix the situation, to save time
            CastorAlgorithmReports.reportNoNeedSecondPreventiveBecauseFirstPreventiveRaoUnsecure(reportNode);
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
                                                    PerimeterResult firstPreventiveResult,
                                                    Map<State, OptimizationResult> postContingencyResults,
                                                    ReportNode reportNode) {
        // Run 2nd preventive RAO
        SecondPreventiveRaoResult secondPreventiveRaoResult;
        try {
            secondPreventiveRaoResult = runSecondPreventiveRao(raoInput, parameters, stateTree, toolProvider, prePerimeterSensitivityAnalysis, initialOutput, firstPreventiveResult, postContingencyResults, reportNode);
            if (secondPreventiveRaoResult.postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                return new FailedRaoResultImpl();
            }
        } catch (OpenRaoException e) {
            CastorAlgorithmReports.reportCastorError(reportNode, e.getMessage());
            return new FailedRaoResultImpl();
        }

        // Run 2nd automaton simulation and update results
        ReportNode secondAutomatonReportNode = CastorAlgorithmReports.reportSecondAutomatonSimulation(reportNode);
        Map<State, OptimizationResult> newPostContingencyResults = optimizeContingencyScenarios(raoInput.getCrac(), raoParameters, stateTree, toolProvider, automatonTreeParameters, null, raoInput.getNetwork(), initialOutput, secondPreventiveRaoResult.postPraSensitivityAnalysisOutput, true, secondAutomatonReportNode);
        CastorAlgorithmReports.reportSecondAutomatonSimulationEnd(secondAutomatonReportNode);

        ReportNode finalResultMergingReportNode = CastorAlgorithmReports.reportResultsMergingPreventiveFirstAndSecondAndPostContingency(reportNode);
        // Always re-run curative sensitivity analysis (re-run is necessary in several specific cases)
        // -- Gather all post contingency remedial actions
        // ---- Curative remedial actions :
        // ------ appliedCras from secondPreventiveRaoResult
        AppliedRemedialActions appliedArasAndCras = secondPreventiveRaoResult.appliedArasAndCras.copyCurative();
        // ------ + curative range actions optimized during second preventive with global optimization
        if (raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) {
            for (Map.Entry<State, OptimizationResult> entry : postContingencyResults.entrySet()) {
                State state = entry.getKey();
                if (!state.getInstant().isCurative()) {
                    continue;
                }
                secondPreventiveRaoResult.perimeterResult.getActivatedRangeActions(state)
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
        PrePerimeterResult postCraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(raoInput.getNetwork(), raoInput.getCrac(), initialOutput, initialOutput, Collections.emptySet(), appliedArasAndCras, reportNode);
        if (postCraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            CastorAlgorithmReports.reportPostRaoSensitivityAnalysisFailed(finalResultMergingReportNode);
            return new FailedRaoResultImpl();
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
                newPostContingencyResults.put(state, new CurativeWithSecondPraoResult(state, entry.getValue(), secondPreventiveRaoResult.perimeterResult, secondPreventiveRaoResult.remedialActionsExcluded, postCraSensitivityAnalysisOutput));
            }
        }
        RaoLogger.logMostLimitingElementsResults(postCraSensitivityAnalysisOutput, parameters.getObjectiveFunctionParameters().getType(), NUMBER_LOGGED_ELEMENTS_END_RAO, finalResultMergingReportNode, TypedValue.INFO_SEVERITY);

        return new PreventiveAndCurativesRaoResultImpl(stateTree,
            initialOutput,
            firstPreventiveResult,
            secondPreventiveRaoResult.perimeterResult,
            secondPreventiveRaoResult.remedialActionsExcluded,
            secondPreventiveRaoResult.postPraSensitivityAnalysisOutput,
            newPostContingencyResults,
            postCraSensitivityAnalysisOutput,
            raoInput.getCrac());
    }

    private static class SecondPreventiveRaoResult {
        private final PerimeterResult perimeterResult;
        private final PrePerimeterResult postPraSensitivityAnalysisOutput;
        private final Set<RemedialAction<?>> remedialActionsExcluded;
        private final AppliedRemedialActions appliedArasAndCras;

        public SecondPreventiveRaoResult(PerimeterResult perimeterResult, PrePerimeterResult postPraSensitivityAnalysisOutput, Set<RemedialAction<?>> remedialActionsExcluded, AppliedRemedialActions appliedArasAndCras) {
            this.perimeterResult = perimeterResult;
            this.postPraSensitivityAnalysisOutput = postPraSensitivityAnalysisOutput;
            this.remedialActionsExcluded = remedialActionsExcluded;
            this.appliedArasAndCras = appliedArasAndCras;
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
                                                             Map<State, OptimizationResult> postContingencyResults, ReportNode reportNode) {
        ReportNode secondPreventiveReportNode = CastorAlgorithmReports.reportSecondPreventiveOptimization(reportNode);
        Network network = raoInput.getNetwork();
        // Go back to the initial state of the network, saved in the SECOND_PREVENTIVE_STATE variant
        network.getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);

        // Get the applied network actions for every contingency perimeter
        AppliedRemedialActions appliedArasAndCras = new AppliedRemedialActions();
        Crac crac = raoInput.getCrac();
        // TODO: see how to handle multiple curative instants here
        Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
        if (crac.hasAutoInstant()) {
            addAppliedNetworkActionsPostContingency(crac.getInstant(InstantKind.AUTO), appliedArasAndCras, postContingencyResults);
        }
        addAppliedNetworkActionsPostContingency(curativeInstant, appliedArasAndCras, postContingencyResults);
        // Get the applied range actions for every auto contingency perimeter
        if (crac.hasAutoInstant()) {
            addAppliedRangeActionsPostContingency(crac.getInstant(InstantKind.AUTO), appliedArasAndCras, postContingencyResults);
        }

        // Apply 1st preventive results for range actions that are both preventive and auto or curative. This way we are sure
        // that the optimal setpoints of the curative results stay coherent with their allowed range and close to
        // optimality in their perimeters. These range actions will be excluded from 2nd preventive RAO.
        Set<RemedialAction<?>> remedialActionsExcluded = new HashSet<>();
        if (!parameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) { // keep old behaviour
            remedialActionsExcluded = new HashSet<>(getRangeActionsExcludedFromSecondPreventive(crac));
            applyPreventiveResultsForAutoOrCurativeRangeActions(network, firstPreventiveResult, crac);
            addAppliedRangeActionsPostContingency(curativeInstant, appliedArasAndCras, postContingencyResults);
        }

        // Run a first sensitivity computation using initial network and applied CRAs
        // If any sensitivity computation fails, fail and fall back to 1st preventive result
        // TODO: can we / do we want to improve this behavior by excluding the failed contingencies?
        PrePerimeterResult sensiWithPostContingencyRemedialActions = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, initialOutput, stateTree.getOperatorsNotSharingCras(), appliedArasAndCras, reportNode);
        if (sensiWithPostContingencyRemedialActions.getSensitivityStatus() == FAILURE) {
            throw new OpenRaoException("Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed");
        }
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            new RangeActionActivationResultImpl(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(raoInput.getNetwork(), crac.getRangeActions())),
            sensiWithPostContingencyRemedialActions,
            parameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO, secondPreventiveReportNode);

        // Run second preventive RAO
        String newVariant = RandomizedString.getRandomizedString("SecondPreventive", raoInput.getNetwork().getVariantManager().getVariantIds(), 10);
        raoInput.getNetwork().getVariantManager().cloneVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT, newVariant, true);
        raoInput.getNetwork().getVariantManager().setWorkingVariant(newVariant);
        PerimeterResult secondPreventiveResult = optimizeSecondPreventivePerimeter(raoInput, parameters, stateTree, toolProvider, initialOutput, sensiWithPostContingencyRemedialActions, firstPreventiveResult.getActivatedNetworkActions(), appliedArasAndCras, secondPreventiveReportNode)
                .join().getPerimeterResult(crac.getPreventiveState());
        // Re-run sensitivity computation based on PRAs without CRAs, to access after PRA results
        PrePerimeterResult postPraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, initialOutput, stateTree.getOperatorsNotSharingCras(), null, reportNode);
        if (postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            CastorAlgorithmReports.reportPostSecondPreventiveSensitivityAnalysisFailed(secondPreventiveReportNode);
        }
        CastorAlgorithmReports.reportSecondPreventiveOptimizationEnd(secondPreventiveReportNode);
        return new SecondPreventiveRaoResult(secondPreventiveResult, postPraSensitivityAnalysisOutput, remedialActionsExcluded, appliedArasAndCras);
    }

    static void addAppliedNetworkActionsPostContingency(Instant instant, AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults) {
        postContingencyResults.forEach((state, optimizationResult) -> {
            if (state.getInstant().equals(instant)) {
                appliedRemedialActions.addAppliedNetworkActions(state, optimizationResult.getActivatedNetworkActions());
            }
        });
    }

    static void addAppliedRangeActionsPostContingency(Instant instant, AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults) {
        // Add all range actions that were activated.
        // Curative/ preventive duplicates are handled via exclusion from 2nd preventive
        postContingencyResults.forEach((state, optimizationResult) -> {
            if (state.getInstant().equals(instant)) {
                optimizationResult.getActivatedRangeActions(state).forEach(rangeAction -> appliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)));
            }
        });
    }

    private CompletableFuture<OneStateOnlyRaoResultImpl> optimizeSecondPreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, StateTree stateTree, ToolProvider toolProvider, PrePerimeterResult initialOutput, PrePerimeterResult prePerimeterResult, Set<NetworkAction> optimalNetworkActionsInFirstPreventiveRao, AppliedRemedialActions appliedCras, ReportNode reportNode) {

        OptimizationPerimeter optPerimeter;
        Crac crac = raoInput.getCrac();

        if (raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) {
            optPerimeter = GlobalOptimizationPerimeter.build(crac, raoInput.getNetwork(), raoParameters, prePerimeterResult, reportNode);
        } else {
            Set<RangeAction<?>> rangeActionsFor2p = new HashSet<>(crac.getRangeActions()); // TODO rangeActions are not used...
            removeRangeActionsExcludedFromSecondPreventive(rangeActionsFor2p, crac, reportNode);
            optPerimeter = PreventiveOptimizationPerimeter.buildWithAllCnecs(crac, rangeActionsFor2p, raoInput.getNetwork(), raoParameters, prePerimeterResult, reportNode);
        }

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(TreeParameters.buildForSecondPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras(), crac, reportNode))
            .build();

        // update RaUsageLimits with already applied RangeActions
        Set<RangeAction<?>> excludedRangeActions = new HashSet<>(getRangeActionsExcludedFromSecondPreventive(crac));
        if (!excludedRangeActions.isEmpty() && searchTreeParameters.getRaLimitationParameters().containsKey(crac.getPreventiveInstant())) {
            searchTreeParameters.setRaLimitationsForSecondPreventive(searchTreeParameters.getRaLimitationParameters().get(crac.getPreventiveInstant()), excludedRangeActions, crac.getPreventiveInstant());
        }

        if (raoParameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao()) {
            // Set the optimal set of network actions decided in 1st preventive RAO as a hint for 2nd preventive RAO
            searchTreeParameters.getNetworkActionParameters().addNetworkActionCombination(new NetworkActionCombination(optimalNetworkActionsInFirstPreventiveRao, true));
        }

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialOutput)
            .withPrePerimeterResult(prePerimeterResult)
            .withPreOptimizationAppliedNetworkActions(appliedCras) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialOutput, prePerimeterResult, prePerimeterResult, crac, new HashSet<>(), raoParameters, reportNode))
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, true).run(reportNode).join();

        // apply PRAs
        raoInput.getNetwork().getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        result.getActivatedRangeActions(crac.getPreventiveState()).forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), result.getOptimizedSetpoint(rangeAction, crac.getPreventiveState())));
        result.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(crac.getPreventiveState(), prePerimeterResult, result, optPerimeter.getFlowCnecs()));
    }

    /**
     * For second preventive optimization, we shouldn't re-optimize range actions that are also curative
     */
    static void removeRangeActionsExcludedFromSecondPreventive(Set<RangeAction<?>> rangeActions, Crac crac, ReportNode reportNode) {
        Set<RangeAction<?>> rangeActionsToRemove = new HashSet<>(rangeActions);
        rangeActionsToRemove.retainAll(getRangeActionsExcludedFromSecondPreventive(crac));
        rangeActionsToRemove.forEach(rangeAction ->
                CastorAlgorithmReports.reportRangeActionRemovedFromSecondCurative(reportNode, rangeAction.getId())
        );
        rangeActions.removeAll(rangeActionsToRemove);
    }

    /**
     * This method applies range action results on the network, for range actions that are auto or curative
     * It is used for second preventive optimization along with 1st preventive results in order to keep the result
     * of 1st preventive for range actions that are both preventive and auto or curative
     */
    static void applyPreventiveResultsForAutoOrCurativeRangeActions(Network network, PerimeterResult preventiveResult, Crac crac) {
        preventiveResult.getActivatedRangeActions(crac.getPreventiveState()).stream()
            .filter(rangeAction -> isRangeActionAutoOrCurative(rangeAction, crac))
            .forEach(rangeAction -> rangeAction.apply(network, preventiveResult.getOptimizedSetpoint(rangeAction, crac.getPreventiveState())));
    }

    /**
     * Returns the set of range actions that were excluded from the 2nd preventive RAO.
     * It consists of range actions that are both preventive and auto or curative, since they mustn't be re-optimized during 2nd preventive.
     */
    static Set<RangeAction<?>> getRangeActionsExcludedFromSecondPreventive(Crac crac) {
        return crac.getRangeActions().stream()
            .filter(rangeAction -> isRangeActionAutoOrCurative(rangeAction, crac))
            .collect(Collectors.toSet());
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
        Set<RangeAction<?>> rangeActionsForState = crac.getPotentiallyAvailableRangeActions(state);
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
