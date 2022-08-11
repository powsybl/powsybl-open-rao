/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
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
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import org.apache.commons.lang3.tuple.Pair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
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
    private static final double DOUBLE_NON_NULL = 1e-12;
    private static final int MAX_NUMBER_OF_SENSI_IN_AUTO_SETPOINT_SHIFT = 10;

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
            initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork());
            RaoLogger.logSensitivityAnalysisResults("Initial sensitivity analysis: ",
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
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

        PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, Collections.emptySet(), null);
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after preventive remedial actions: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
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
        Map<State, OptimizationResult> postContingencyResults = optimizeContingencyScenarios(raoInput.getCrac(), raoParameters, stateTree, toolProvider, curativeTreeParameters, network, initialOutput, preCurativeSensitivityAnalysisOutput);
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");

        // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----

        if (shouldRunSecondPreventiveRao(raoParameters, initialOutput, preventiveResult, postContingencyResults, targetEndInstant, preventiveRaoTime)) {
            mergedRaoResults = runSecondPreventiveRao(raoInput, raoParameters, stateTree, toolProvider, prePerimeterSensitivityAnalysis, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, postContingencyResults);
        } else {
            BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, postContingencyResults);
            // log results
            RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, stateTree.getBasecaseScenario(), preventiveResult, stateTree.getContingencyScenarios(), postContingencyResults, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        }

        return postCheckResults(mergedRaoResults, initialOutput, raoParameters);
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
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), optPerimeter.getFlowCnecs()))
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(initialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResult, initialResult, Collections.emptySet(), raoParameters))
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
                                                                        PrePerimeterResult prePerimeterSensitivityOutput) {
        Map<State, OptimizationResult> contingencyScenarioResults = new ConcurrentHashMap<>();
        // Create a new variant
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        network.getVariantManager().cloneVariant(PREVENTIVE_SCENARIO, CONTINGENCY_SCENARIO);
        network.getVariantManager().setWorkingVariant(CONTINGENCY_SCENARIO);
        // Go through all contingency scenarios
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, CONTINGENCY_SCENARIO, raoParameters.getPerimetersInParallel())) {
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
                            AutomatonPerimeterResultImpl automatonResult = simulateAutomatonState(automatonState.get(), curativeState, crac, networkClone, raoParameters, toolProvider, initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras());
                            contingencyScenarioResults.put(automatonState.get(), automatonResult);
                            preCurativeResult = automatonResult.getPostAutomatonSensitivityAnalysisOutput();
                        }

                        // Optimize curative instant
                        OptimizationResult curativeResult = optimizeCurativeState(curativeState, crac, networkClone,
                            raoParameters, stateTree, toolProvider, curativeTreeParameters, initialSensitivityOutput, preCurativeResult);
                        contingencyScenarioResults.put(curativeState, curativeResult);
                    } catch (Exception e) {
                        BUSINESS_LOGS.error("Scenario post-contingency {} could not be optimized.", optimizedScenario.getContingency().getId(), e);
                    }
                    TECHNICAL_LOGS.info("Remaining post-contingency scenarios to optimize: {}", remainingScenarios.decrementAndGet());
                    contingencyCountDownLatch.countDown();
                    try {
                        networkPool.releaseUsedNetwork(networkClone);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new FaraoException(ex);
                    }
                })
            );
            boolean success = contingencyCountDownLatch.await(24, TimeUnit.HOURS);
            if (!success) {
                throw new FaraoException("At least one post-contingency state could not be optimized within the given time (24 hours). This should not happen.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return contingencyScenarioResults;
    }

    // ========================================
    // region simulation at AUTO instant
    // ========================================
    /**
     * This function simulates automatons at AUTO instant. First, it simulates topological automatons, then range actions
     * by order of speed.
     * Returns an AutomatonPerimeterResult
     */
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
        if (!crac.getNetworkActions(automatonState, UsageMethod.AVAILABLE).isEmpty()) {
            BUSINESS_WARNS.warn("CRAC has network action automatons with usage method AVAILABLE. These are not supported.");
        }
        TECHNICAL_LOGS.info("Initial situation:");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, prePerimeterSensitivityOutput, Set.of(automatonState, curativeState), raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // I) Simulate topological automatons
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>();
        rangeActionsInSensi.addAll(crac.getRangeActions(automatonState, UsageMethod.FORCED, UsageMethod.TO_BE_EVALUATED));
        rangeActionsInSensi.addAll(crac.getRangeActions(curativeState, UsageMethod.AVAILABLE, UsageMethod.FORCED, UsageMethod.TO_BE_EVALUATED));
        PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac.getFlowCnecs(),
            rangeActionsInSensi,
            raoParameters,
            toolProvider);
        Pair<PrePerimeterResult, Set<NetworkAction>> topoSimulationResult = simulateTopologicalAutomatons(automatonState,
            crac,
            network,
            raoParameters,
            initialFlowResult,
            prePerimeterSensitivityOutput,
            preAutoPerimeterSensitivityAnalysis,
            operatorsNotSharingCras);
        PrePerimeterResult postAutoResult = topoSimulationResult.getLeft();
        Set<NetworkAction> appliedNetworkActions = topoSimulationResult.getRight();

        // II) Simulate range actions
        // -- Create groups of aligned range actions
        List<List<RangeAction<?>>> rangeActionsOnAutomatonState = buildRangeActionsGroupsOrderedBySpeed(postAutoResult, automatonState, crac, network);
        // -- Build AutomatonPerimeterResultImpl objects
        Map<RangeAction<?>, Double> rangeActionsWithSetpoint = new HashMap<>();
        rangeActionsOnAutomatonState.stream().flatMap(List::stream).forEach(rangeAction -> rangeActionsWithSetpoint.put(rangeAction, rangeAction.getCurrentSetpoint(network)));
        Set<RangeAction<?>> activatedRangeActions = new HashSet<>();

        if (rangeActionsOnAutomatonState.isEmpty()) {
            TECHNICAL_LOGS.info("Automaton state {} has been optimized (no automaton range actions available).", automatonState.getId());
            return new AutomatonPerimeterResultImpl(postAutoResult, appliedNetworkActions, activatedRangeActions, rangeActionsWithSetpoint, automatonState);
        }

        // -- Optimize range-action automatons
        Unit marginUnit = raoParameters.getObjectiveFunction().getUnit();
        for (List<RangeAction<?>> alignedRa : rangeActionsOnAutomatonState) {
            RangeAction<?> availableRa = alignedRa.get(0);
            // Disable AC emulation for HVDC lines
            if (alignedRa.stream().allMatch(HvdcRangeAction.class::isInstance)) {
                postAutoResult = disableACEmulation(alignedRa, network,
                    preAutoPerimeterSensitivityAnalysis, initialFlowResult, postAutoResult, operatorsNotSharingCras);
            }
            // Define flowCnecs depending on UsageMethod
            Set<FlowCnec> flowCnecs = gatherFlowCnecs(availableRa, automatonState, crac, network);
            // Shift
            Pair<PrePerimeterResult,  Map<RangeAction<?>, Double>> postShiftResult = shiftRangeActionsUntilFlowCnecsSecure(alignedRa,
                flowCnecs,
                network,
                preAutoPerimeterSensitivityAnalysis,
                initialFlowResult,
                postAutoResult,
                marginUnit,
                operatorsNotSharingCras);
            postAutoResult = postShiftResult.getLeft();
            activatedRangeActions.addAll(postShiftResult.getRight().keySet());
            rangeActionsWithSetpoint.putAll(postShiftResult.getRight());
        }

        PrePerimeterResult postAutomatonSensitivityAnalysisOutput = runPreCurativeSensitivityComputation(automatonState,
            curativeState,
            crac,
            network,
            raoParameters,
            toolProvider,
            initialFlowResult,
            operatorsNotSharingCras);

        // Build and return optimization result
        AutomatonPerimeterResultImpl automatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(postAutomatonSensitivityAnalysisOutput, appliedNetworkActions, activatedRangeActions, rangeActionsWithSetpoint, automatonState);
        TECHNICAL_LOGS.info("Automaton state {} has been optimized.", automatonState.getId());
        RaoLogger.logOptimizationSummary(BUSINESS_LOGS, automatonState, automatonPerimeterResultImpl.getActivatedNetworkActions().size(), automatonPerimeterResultImpl.getActivatedRangeActions(automatonState).size(), null, null, automatonPerimeterResultImpl);
        return automatonPerimeterResultImpl;
    }

    /**
     * This function simulates topological automatons.
     * Returns a pair of :
     * -- a PrePerimeterResult : a new sensi analysis is run after having applied the topological automatons,
     * -- and the set of applied network actions.
     */
    private Pair<PrePerimeterResult, Set<NetworkAction>> simulateTopologicalAutomatons(State automatonState,
                                                                                       Crac crac,
                                                                                       Network network,
                                                                                       RaoParameters raoParameters,
                                                                                       FlowResult initialFlowResult,
                                                                                       PrePerimeterResult prePerimeterSensitivityOutput,
                                                                                       PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis,
                                                                                       Set<String> operatorsNotSharingCras) {
        // -- Apply network actions
        // -- First get forced network actions
        Set<NetworkAction> appliedNetworkActions = crac.getNetworkActions(automatonState, UsageMethod.FORCED);
        // -- Then add those with an OnFlowConstraint usage rule if their constraint is verified
        crac.getNetworkActions(automatonState, UsageMethod.TO_BE_EVALUATED).stream()
            .filter(na -> RaoUtil.isRemedialActionAvailable(na, automatonState, prePerimeterSensitivityOutput, crac.getFlowCnecs(), network))
            .forEach(appliedNetworkActions::add);

        if (appliedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("Topological automaton state {} has been skipped as no topological automatons were activated.", automatonState.getId());
            return Pair.of(prePerimeterSensitivityOutput, appliedNetworkActions);
        }

        // -- Apply
        appliedNetworkActions.forEach(na -> {
            TECHNICAL_LOGS.debug("Activating automaton {} - {}.", na.getId(), na.getName());
            na.apply(network);
        });

        // -- Sensi must be run to evaluate available auto range actions
        // -- If network actions have been applied, run sensi :
        PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput = prePerimeterSensitivityOutput;
        if (!appliedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("Running sensi post application of auto network actions for automaton state {}.", automatonState.getId());
            automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
        }

        return Pair.of(automatonRangeActionOptimizationSensitivityAnalysisOutput, appliedNetworkActions);
    }

    /**
     * This function gathers the flow cnecs to be considered while shifting range actions,
     * depending on the range action availableRa's UsageMethod.
     */
    static Set<FlowCnec> gatherFlowCnecs(RangeAction<?> availableRa,
                                         State automatonState,
                                         Crac crac,
                                         Network network) {
        // UsageMethod is either FORCED or TO_BE_EVALUATED
        if (availableRa.getUsageMethod(automatonState).equals(UsageMethod.FORCED)) {
            return crac.getFlowCnecs().stream()
                .filter(flowCnec -> flowCnec.getState().equals(automatonState))
                .collect(Collectors.toSet());
        } else if (availableRa.getUsageMethod(automatonState).equals(UsageMethod.TO_BE_EVALUATED)) {
            // Get flowcnecs constrained by OnFlowConstraint
            Set<FlowCnec> flowCnecs = availableRa.getUsageRules().stream()
                .filter(OnFlowConstraint.class::isInstance)
                .map(OnFlowConstraint.class::cast)
                .map(OnFlowConstraint::getFlowCnec)
                .filter(flowCnec -> flowCnec.getState().equals(automatonState))
                .collect(Collectors.toSet());
            // Get all cnecs in country if availableRa is available on a OnFlowConstraintInCountry usage rule
            Set<Country> countries = availableRa.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance)
                .map(OnFlowConstraintInCountry.class::cast)
                .map(OnFlowConstraintInCountry::getCountry)
                .collect(Collectors.toSet());
            flowCnecs.addAll(crac.getFlowCnecs().stream()
                .filter(flowCnec -> flowCnec.getState().equals(automatonState))
                .filter(flowCnec -> countries.stream().anyMatch(country -> RaoUtil.isCnecInCountry(flowCnec, country, network)))
                .collect(Collectors.toSet()));
            return flowCnecs;
        } else {
            throw new FaraoException(String.format("Range action %s has usage method %s although FORCED or TO_BE_EVALUATED were expected.", availableRa, availableRa.getUsageMethod(automatonState)));
        }
    }

    /**
     * This function sorts groups of aligned range actions by speed.
     */
    private List<List<RangeAction<?>>> buildRangeActionsGroupsOrderedBySpeed(PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput,
                                                                             State automatonState,
                                                                             Crac crac,
                                                                             Network network) {
        // 1) Get available range actions
        // -- First get forced range actions
        Set<RangeAction<?>> availableRangeActions = crac.getRangeActions(automatonState, UsageMethod.FORCED);
        // -- Then add those with an OnFlowConstraint or OnFlowConstraintInCountry usage rule if their constraint is verified
        PrePerimeterResult finalAutomatonRangeActionOptimizationSensitivityAnalysisOutput = automatonRangeActionOptimizationSensitivityAnalysisOutput;
        crac.getRangeActions(automatonState, UsageMethod.TO_BE_EVALUATED).stream()
            .filter(na -> RaoUtil.isRemedialActionAvailable(na, automatonState, finalAutomatonRangeActionOptimizationSensitivityAnalysisOutput, crac.getFlowCnecs(), network))
            .forEach(availableRangeActions::add);

        // 2) Sort range actions
        // -- Check that speed is defined
        availableRangeActions.forEach(rangeAction -> {
            if (rangeAction.getSpeed().isEmpty()) {
                BUSINESS_WARNS.warn("Range action {} will not be considered in RAO as no speed is defined", rangeAction.getId());
            }
        });
        // -- Sort RAs from fastest to slowest
        List<RangeAction<?>> rangeActionsOrderedBySpeed = availableRangeActions.stream()
            .filter(rangeAction -> rangeAction.getSpeed().isPresent())
            .sorted(Comparator.comparing(ra -> ra.getSpeed().get()))
            .collect(Collectors.toList());

        // 3) Gather aligned range actions : they will be simulated simultaneously in one shot
        // -- Create groups of aligned range actions
        List<List<RangeAction<?>>> rangeActionsOnAutomatonState = new ArrayList<>();
        for (RangeAction<?> availableRangeAction : rangeActionsOrderedBySpeed) {
            // Look for aligned range actions in all range actions : they have the same groupId and the same usageMethod
            Optional<String> groupId = availableRangeAction.getGroupId();
            List<RangeAction<?>> alignedRa;
            if (groupId.isPresent()) {
                alignedRa = crac.getRangeActions().stream()
                    .filter(rangeAction -> groupId.get().equals(rangeAction.getGroupId().orElse(null)))
                    .collect(Collectors.toList());
            } else {
                alignedRa = List.of(availableRangeAction);
            }
            if (!checkAlignedRangeActions(availableRangeAction.getId(), automatonState, alignedRa, rangeActionsOrderedBySpeed)) {
                continue;
            }
            rangeActionsOnAutomatonState.add(alignedRa);
        }
        return rangeActionsOnAutomatonState;
    }

    /**
     * This function checks that the group of aligned range actions :
     * - contains same type range actions (PST, HVDC, or other) : all-or-none principle
     * - contains range actions that share the same usage rule
     * - contains range actions that are all available at AUTO instant.
     * Returns true if checks are valid.
     */
    public static boolean checkAlignedRangeActions(String availableRangeActionId, State automatonState, List<RangeAction<?>> alignedRa, List<RangeAction<?>> rangeActionsOrderedBySpeed) {
        // Check types
        checkAlignedRangeActionsType(alignedRa);

        // Ignore aligned range actions when one element of the group has a different usage method than the others
        if (alignedRa.stream().map(rangeAction -> rangeAction.getUsageMethod(automatonState)).collect(Collectors.toSet()).size() > 1) {
            BUSINESS_WARNS.warn("Range action {} belongs to a group of aligned range actions with different usage methods; they are not simulated", availableRangeActionId);
            return false;
        }
        // Ignore aligned range actions when one element of the group is not available at AUTO instant
        if (!rangeActionsOrderedBySpeed.containsAll(alignedRa)) {
            BUSINESS_WARNS.warn("Range action {} belongs to a group of aligned range actions not all available at AUTO instant; they are not simulated", availableRangeActionId);
            return false;
        }
        return true;
    }

    /**
     * Checks if all range actions in the list are of the same type (PstRangeAction / InjectionRangeAction / JHvdcRangeAction)
     */
    public static void checkAlignedRangeActionsType(List<RangeAction<?>> alignedRangeActions) {
        Set<RangeAction<?>> pstInAlignedRangeActions = alignedRangeActions.stream()
            .filter(PstRangeAction.class::isInstance).collect(Collectors.toSet());
        Set<RangeAction<?>> hvdcInAlignedRangeActions = alignedRangeActions.stream()
            .filter(HvdcRangeAction.class::isInstance).collect(Collectors.toSet());

        if (!pstInAlignedRangeActions.isEmpty() && pstInAlignedRangeActions.size() < alignedRangeActions.size()) {
            throw new FaraoException(String.format("Range action %s is in a group of aligned range actions containing PST and non PST range actions.", alignedRangeActions.get(0).getName()));
        }
        if (!hvdcInAlignedRangeActions.isEmpty() && hvdcInAlignedRangeActions.size() < alignedRangeActions.size()) {
            throw new FaraoException(String.format("Range action %s is in a group of aligned range actions containing HVDC and non HVDC range actions.", alignedRangeActions.get(0).getName()));
        }
    }

    /**
     * This functions runs a sensi when the remedial actions simulation process is over.
     * The sensi analysis is run on curative range actions, to be used at curative instant.
     * This function returns a prePerimeterResult that will be used to build an AutomatonPerimeterResult.
     */
    private PrePerimeterResult runPreCurativeSensitivityComputation(State automatonState,
                                                                    State curativeState,
                                                                    Crac crac,
                                                                    Network network,
                                                                    RaoParameters raoParameters,
                                                                    ToolProvider toolProvider,
                                                                    FlowResult initialFlowResult,
                                                                    Set<String> operatorsNotSharingCras) {
        // -- Run sensitivity computation before running curative RAO later
        // -- Get curative range actions
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
        TECHNICAL_LOGS.info("Running pre curative sensi after auto state {}.", automatonState.getId());
        PrePerimeterResult postAutomatonSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, postAutomatonSensitivityAnalysisOutput, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
        return postAutomatonSensitivityAnalysisOutput;
    }

    /**
     * This function disables AC emulation if alignedRA are HVDC range actions enabled in AC.
     * It runs a sensi when AC emulations have been disabled.
     */
    private PrePerimeterResult disableACEmulation(List<RangeAction<?>> alignedRa,
                                                  Network network,
                                                  PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis,
                                                  FlowResult initialFlowResult,
                                                  PrePerimeterResult prePerimeterSensitivityOutput,
                                                  Set<String> operatorsNotSharingCras) {
        boolean runSensi = false;
        for (RangeAction<?> alignedAvailableRa : alignedRa) {
            if (alignedAvailableRa instanceof HvdcRangeAction) {
                // Disable AC emulation
                try {
                    if (network.getHvdcLine(((HvdcRangeAction) alignedAvailableRa).getNetworkElement().getId())
                        .getExtension(HvdcAngleDroopActivePowerControl.class)
                        .isEnabled()) {
                        network.getHvdcLine(((HvdcRangeAction) alignedAvailableRa).getNetworkElement().getId()).getExtension(HvdcAngleDroopActivePowerControl.class).setEnabled(false);
                        runSensi = true;
                    }
                } catch (Exception e) {
                    throw new FaraoException(String.format("Hvdc range action %s is ill defined in network with error %s", alignedAvailableRa, e));
                }
            }
        }
        if (runSensi) {
            TECHNICAL_LOGS.info("Running sensi after disabling AC emulation.");
            PrePerimeterResult result =  preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, result, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
            return result;
        } else {
            return prePerimeterSensitivityOutput;
        }
    }

    /**
     * This function shifts alignedRangeAction setpoints until :
     * -- no cnecs with a negative margin remain
     * -- OR setpoints have been shifted as far as possible in one direction
     * -- OR the direction in which the shift is performed switches
     * -- OR too many iterations have been performed
     * After every setpoint shift, a new sensi analysis is performed.
     * This function returns a pair of a prePerimeterResult, and a map of activated range actions during the shift, with their
     * newly computed setpoints, both used to compute an AutomatonPerimeterResult.
     */
    private Pair<PrePerimeterResult,  Map<RangeAction<?>, Double>> shiftRangeActionsUntilFlowCnecsSecure(List<RangeAction<?>> alignedRangeActions,
                                                                                                         Set<FlowCnec> flowCnecs,
                                                                                                         Network network,
                                                                                                         PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis,
                                                                                                         FlowResult initialFlowResult,
                                                                                                         PrePerimeterResult prePerimeterSensitivityOutput,
                                                                                                         Unit marginUnit,
                                                                                                         Set<String> operatorsNotSharingCras) {
        Set<FlowCnec> flowCnecsToBeExcluded = new HashSet<>();
        PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput = prePerimeterSensitivityOutput;
        Map<RangeAction<?>, Double> activatedRangeActionsWithSetpoint = new HashMap<>();
        List<FlowCnec> flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);
        RangeAction<?> firstRangeAction = alignedRangeActions.get(0);

        // -- Define setpoint bounds
        // Aligned range actions have the same setpoint :
        double initialSetpoint = firstRangeAction.getCurrentSetpoint(network);
        double minSetpointInAlignedRa = firstRangeAction.getMinAdmissibleSetpoint(initialSetpoint);
        double maxSetpointInAlignedRa = firstRangeAction.getMaxAdmissibleSetpoint(initialSetpoint);
        for (RangeAction<?> rangeAction : alignedRangeActions) {
            minSetpointInAlignedRa = Math.max(minSetpointInAlignedRa, rangeAction.getMinAdmissibleSetpoint(initialSetpoint));
            maxSetpointInAlignedRa = Math.min(maxSetpointInAlignedRa, rangeAction.getMaxAdmissibleSetpoint(initialSetpoint));
        }

        boolean firstIteration = true;
        int numberOfIterations = 0; // security measure
        double direction = 0;
        while (!flowCnecsWithNegativeMargin.isEmpty()) {
            numberOfIterations++;
            FlowCnec toBeShiftedCnec = flowCnecsWithNegativeMargin.get(0);
            double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(toBeShiftedCnec, Side.LEFT, marginUnit, MEGAWATT);
            double cnecMargin = unitConversionCoefficient * automatonRangeActionOptimizationSensitivityAnalysisOutput.getMargin(toBeShiftedCnec, marginUnit);
            // Aligned range actions have the same setpoint :
            double currentSetpoint = firstRangeAction.getCurrentSetpoint(network);
            double sensitivityValue = 0;
            for (RangeAction<?> rangeAction : alignedRangeActions) {
                sensitivityValue += automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityValue(toBeShiftedCnec, rangeAction, MEGAWATT);
            }
            // if sensi is null, move on to next cnec with negative margin
            if (Math.abs(sensitivityValue) < DOUBLE_NON_NULL) {
                flowCnecsToBeExcluded.add(toBeShiftedCnec);
                flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);
                continue;
            }

            double optimalSetpoint = computeOptimalSetpoint(currentSetpoint, cnecMargin, sensitivityValue, alignedRangeActions, minSetpointInAlignedRa, maxSetpointInAlignedRa);

            // On first iteration, define direction
            if (firstIteration) {
                direction = Math.signum(optimalSetpoint - currentSetpoint);
                firstIteration = false;
            }
            // Compare direction with previous shift
            // If direction == 0, then the RA is at one of its bounds
            if (direction == 0 || (!firstIteration && direction != Math.signum(optimalSetpoint - currentSetpoint)) || numberOfIterations > MAX_NUMBER_OF_SENSI_IN_AUTO_SETPOINT_SHIFT) {
                return Pair.of(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint);
            }

            for (RangeAction<?> rangeAction : alignedRangeActions) {
                rangeAction.apply(network, optimalSetpoint);
                activatedRangeActionsWithSetpoint.put(rangeAction, optimalSetpoint);
            }
            TECHNICAL_LOGS.debug("Shifting setpoint from {number, #.##} to {number, #.##} on range action(s) {} to improve margin on cnec {}} (initial margin : {number, #.##} MW).", initialSetpoint, optimalSetpoint, alignedRangeActions.stream().map(Identifiable::getId).collect(Collectors.joining(" ,")), toBeShiftedCnec.getId(), cnecMargin);
            automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, raoParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
            flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);
        }
        return Pair.of(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint);
    }

    /**
     * This function builds a list of cnecs with negative margin, except cnecs in cnecsToBeExcluded.
     * N.B : margin is retrieved in MEGAWATT as only the sign matters.
     * Returns a sorted list of FlowCnecs with negative margin.
     */
    private List<FlowCnec> getCnecsWithNegativeMarginWithoutExcludedCnecs(Set<FlowCnec> cnecs,
                                                                          Set<FlowCnec> cnecsToBeExcluded,
                                                                          PrePerimeterResult prePerimeterSensitivityOutput) {
        List<FlowCnec> flowCnecsWithNegativeMargin = cnecs.stream()
            .filter(flowCnec -> prePerimeterSensitivityOutput.getMargin(flowCnec, MEGAWATT) <= 0)
            .sorted(Comparator.comparing(flowCnec -> prePerimeterSensitivityOutput.getMargin(flowCnec, MEGAWATT)))
            .collect(Collectors.toList());
        flowCnecsWithNegativeMargin.removeAll(cnecsToBeExcluded);
        return flowCnecsWithNegativeMargin;
    }

    /**
     * This function computes the optimal setpoint to bring cnecMargin over 0.
     * Returns optimal setpoint.
     */
    private double computeOptimalSetpoint(double currentSetpoint, double cnecMargin, double sensitivityValue, List<RangeAction<?>> alignedRangeActions, double minSetpointInAlignedRa, double maxSetpointInAlignedRa) {
        double optimalSetpoint = currentSetpoint + Math.min(cnecMargin, 0) / sensitivityValue;
        // Compare setpoint to min and max
        if (optimalSetpoint > maxSetpointInAlignedRa) {
            optimalSetpoint = maxSetpointInAlignedRa;
        }
        if (optimalSetpoint < minSetpointInAlignedRa) {
            optimalSetpoint = minSetpointInAlignedRa;
        }

        for (RangeAction<?> rangeAction : alignedRangeActions) {
            if (rangeAction instanceof PstRangeAction) {
                optimalSetpoint = roundUpAngleToTapWrtInitialSetpoint((PstRangeAction) rangeAction, optimalSetpoint, currentSetpoint);
                return optimalSetpoint;
            }
        }
        return optimalSetpoint;
    }

    /**
     * This function converts angleToBeRounded in the angle corresponding to the first tap
     * after angleToBeRounded in the direction opposite of initialAngle.
     */
    public static Double roundUpAngleToTapWrtInitialSetpoint(PstRangeAction rangeAction, double angleToBeRounded, double initialAngle) {
        double direction = Math.signum(angleToBeRounded - initialAngle);
        if (direction > 0) {
            Optional<Double> roundedAngle = rangeAction.getTapToAngleConversionMap().values().stream().filter(angle -> angle >= angleToBeRounded).min(Double::compareTo);
            if (roundedAngle.isPresent()) {
                return roundedAngle.get();
            }
        } else if (direction < 0) {
            Optional<Double> roundedAngle = rangeAction.getTapToAngleConversionMap().values().stream().filter(angle -> angle <= angleToBeRounded).max(Double::compareTo);
            if (roundedAngle.isPresent()) {
                return roundedAngle.get();
            }
        }
        // else, min or max was not found or angleToBeRounded = initialAngle. Return closest tap :
        return rangeAction.getTapToAngleConversionMap().get(rangeAction.convertAngleToTap(angleToBeRounded));
    }

    // ========================================
    // endregion
    // ========================================

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
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), optPerimeter.getFlowCnecs()))
            .build();

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialSensitivityOutput)
            .withPrePerimeterResult(prePerimeterSensitivityOutput)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), raoParameters))
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

        // Get the applied network actions for every contingency perimeter
        AppliedRemedialActions appliedCras = getAppliedNetworkActionsPostContingency(postContingencyResults);

        // Apply 1st preventive results for range actions that are both preventive and curative. This way we are sure
        // that the optimal setpoints of the curative results stay coherent with their allowed range and close to
        // optimality in their perimeters. These range actions will be excluded from 2nd preventive RAO.
        Set<RemedialAction<?>> remedialActionsExcluded = new HashSet<>();
        if (!parameters.getExtension(SearchTreeRaoParameters.class).isGlobalOptimizationInSecondPreventive()) { // keep old behaviour
            remedialActionsExcluded = new HashSet<>(getRangeActionsExcludedFromSecondPreventive(raoInput.getCrac()));
            applyPreventiveResultsForCurativeRangeActions(network, firstPreventiveResult, raoInput.getCrac());
            addAppliedRangeActionsPostContingency(appliedCras, postContingencyResults, preCurativeSensitivityAnalysisOutput);
        }

        // Run a first sensitivity computation using initial network and applied CRAs
        PrePerimeterResult sensiWithPostContingencyRemedialActions = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, stateTree.getOperatorsNotSharingCras(), appliedCras);
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            sensiWithPostContingencyRemedialActions,
            parameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // Run second preventive RAO
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        PerimeterResult secondPreventiveResult = optimizeSecondPreventivePerimeter(raoInput, parameters, stateTree, toolProvider, initialOutput, sensiWithPostContingencyRemedialActions, firstPreventiveResult.getActivatedNetworkActions(), appliedCras)
            .join().getPerimeterResult(OptimizationState.AFTER_CRA, raoInput.getCrac().getPreventiveState());
        // Re-run sensitivity computation based on PRAs without CRAs, to access OptimizationState.AFTER_PRA results
        PrePerimeterResult updatedPreCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, stateTree.getOperatorsNotSharingCras(), null);
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");

        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        // log results
        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, secondPreventiveResult, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_RAO);

        return new SecondPreventiveAndCurativesRaoResultImpl(initialOutput, raoInput.getCrac().getPreventiveState(), firstPreventiveResult, secondPreventiveResult, updatedPreCurativeSensitivityAnalysisOutput, postContingencyResults, remedialActionsExcluded);
    }

    static AppliedRemedialActions getAppliedNetworkActionsPostContingency(Map<State, OptimizationResult> postContingencyResults) {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        postContingencyResults.forEach((state, optimizationResult) -> appliedRemedialActions.addAppliedNetworkActions(state, optimizationResult.getActivatedNetworkActions()));
        return appliedRemedialActions;
    }

    static void addAppliedRangeActionsPostContingency(AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults, PrePerimeterResult preCurativeResults) {
        // Add all range actions that were activated in curative, even if they are also preventive (they will be excluded from 2nd preventive)
        postContingencyResults.forEach((state, optimizationResult) ->
            (new PerimeterResultImpl(preCurativeResults, optimizationResult)).getActivatedRangeActions(state)
                .forEach(rangeAction -> appliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)))
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
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters, stateTree.getOperatorsNotSharingCras(), optPerimeter.getFlowCnecs()))
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
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialOutput, prePerimeterResult, new HashSet<>(), raoParameters))
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
