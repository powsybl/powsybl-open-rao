/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost;
import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyRemedialActions;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CastorContingencyScenarios {

    private static final String CONTINGENCY_SCENARIO = "ContingencyScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final double COST_EPSILON = 1e-6;

    private final Crac crac;
    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;
    private final StateTree stateTree;
    private final TreeParameters curativeTreeParameters;
    private final PrePerimeterResult initialSensitivityOutput;

    public CastorContingencyScenarios(Crac crac,
                                      RaoParameters raoParameters,
                                      ToolProvider toolProvider,
                                      StateTree stateTree,
                                      TreeParameters curativeTreeParameters,
                                      PrePerimeterResult initialSensitivityOutput) {
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
        this.stateTree = stateTree;
        this.curativeTreeParameters = curativeTreeParameters;
        this.initialSensitivityOutput = initialSensitivityOutput;
    }

    public Map<State, PostPerimeterResult> optimizeContingencyScenarios(Network network,
                                                                       PrePerimeterResult prePerimeterSensitivityOutput,
                                                                       boolean automatonsOnly) {
        Map<State, PostPerimeterResult> contingencyScenarioResults = new ConcurrentHashMap<>();
        // Create a new variant
        String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);
        // Create an automaton simulator
        AutomatonSimulator automatonSimulator = new AutomatonSimulator(crac, raoParameters, toolProvider, initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
        // Go through all contingency scenarios
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, newVariant, getAvailableCPUs(raoParameters), true)) {
            AtomicInteger remainingScenarios = new AtomicInteger(stateTree.getContingencyScenarios().size());
            List<ForkJoinTask<Object>> tasks = stateTree.getContingencyScenarios().stream().map(optimizedScenario ->
                networkPool.submit(() -> runScenario(prePerimeterSensitivityOutput, automatonsOnly, optimizedScenario, networkPool, automatonSimulator, contingencyScenarioResults, remainingScenarios))
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

    private Object runScenario(PrePerimeterResult prePerimeterSensitivityOutput, boolean automatonsOnly, ContingencyScenario optimizedScenario, AbstractNetworkPool networkPool, AutomatonSimulator automatonSimulator, Map<State, PostPerimeterResult> contingencyScenarioResults, AtomicInteger remainingScenarios) throws InterruptedException {
        Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
        TECHNICAL_LOGS.info("Optimizing scenario post-contingency {}.", optimizedScenario.getContingency().getId());

        // Init variables
        Optional<State> automatonState = optimizedScenario.getAutomatonState();
        Set<State> curativeStates = new HashSet<>();
        optimizedScenario.getCurativePerimeters().forEach(perimeter -> curativeStates.addAll(perimeter.getAllStates()));
        PrePerimeterResult preCurativeResult = prePerimeterSensitivityOutput;
        double sensitivityFailureOvercost = getSensitivityFailureOvercost(raoParameters);

        // Simulate automaton instant
        boolean autoStateSensiFailed = false;
        if (automatonState.isPresent()) {
            AutomatonPerimeterResultImpl automatonResult = automatonSimulator.simulateAutomatonState(automatonState.get(), curativeStates, networkClone);
            //recompute sensi and objective function considering auto + all instants following auto
            PostPerimeterResult postAutoResult = getResultPostState(automatonState.get(), networkClone, prePerimeterSensitivityOutput, automatonResult);
            contingencyScenarioResults.put(automatonState.get(), postAutoResult);
            if (automatonResult.getComputationStatus() == ComputationStatus.FAILURE) {
                autoStateSensiFailed = true;
            } else {
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
            curativeStates.forEach(curativeState -> contingencyScenarioResults.put(curativeState, generateSkippedPostPerimeterResult(curativeState, sensitivityFailureOvercost)));
        } else if (!automatonsOnly) {
            boolean allPreviousPerimetersSucceded = true;
            PrePerimeterResult previousPerimeterResult = preCurativeResult;
            // Optimize curative perimeters
            Map<State, OptimizationResult> resultsPerPerimeter = new HashMap<>();
            Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter = new HashMap<>();
            for (Perimeter curativePerimeter : optimizedScenario.getCurativePerimeters()) {
                State curativeState = curativePerimeter.getRaOptimisationState();
                if (previousPerimeterResult == null) {
                    previousPerimeterResult = getPreCurativePerimeterSensitivityAnalysis(curativePerimeter).runBasedOnInitialResults(networkClone, null, stateTree.getOperatorsNotSharingCras(), null);
                }
                prePerimeterResultPerPerimeter.put(curativePerimeter.getRaOptimisationState(), previousPerimeterResult);
                if (allPreviousPerimetersSucceded) {
                    OptimizationResult curativeResult = optimizeCurativePerimeter(curativePerimeter, networkClone, previousPerimeterResult, resultsPerPerimeter, prePerimeterResultPerPerimeter);
                    allPreviousPerimetersSucceded = curativeResult.getSensitivityStatus() == DEFAULT;
                    applyRemedialActions(networkClone, curativeResult, curativeState);
                    //recompute sensi and objective function considering curative + all instants following curative (useful if multi curative)
                    PostPerimeterResult postCurativeResult = getResultPostState(curativeState, networkClone, previousPerimeterResult, curativeResult);
                    contingencyScenarioResults.put(curativeState, postCurativeResult);
                    previousPerimeterResult = null;
                    if (allPreviousPerimetersSucceded) {
                        resultsPerPerimeter.put(curativePerimeter.getRaOptimisationState(), curativeResult);
                    }
                } else {
                    contingencyScenarioResults.put(curativeState, generateSkippedPostPerimeterResult(curativeState, sensitivityFailureOvercost));
                }
            }
        }
        TECHNICAL_LOGS.debug("Remaining post-contingency scenarios to optimize: {}", remainingScenarios.decrementAndGet());
        boolean actionWasApplied = contingencyScenarioResults.entrySet().stream()
            .filter(stateAndResult -> stateAndResult.getKey().getContingency().orElseThrow().equals(optimizedScenario.getContingency()))
            .anyMatch(this::isAnyActionApplied);
        networkPool.releaseUsedNetwork(networkClone, actionWasApplied);
        return null;
    }

    private boolean isAnyActionApplied(Map.Entry<State, PostPerimeterResult> stateAndResult) {
        State state = stateAndResult.getKey();
        PostPerimeterResult postPerimeterResult = stateAndResult.getValue();
        boolean anyRangeActionApplied = !postPerimeterResult.getOptimizationResult().getActivatedRangeActions(state).isEmpty();
        boolean anyNetworkActionApplied = !postPerimeterResult.getOptimizationResult().getActivatedNetworkActions().isEmpty();
        return anyRangeActionApplied || anyNetworkActionApplied;

    }

    private PostPerimeterResult generateSkippedPostPerimeterResult(State state, double sensitivityFailureOvercost) {
        OptimizationResult skippedOptimizationResult = new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE, sensitivityFailureOvercost);
        PrePerimeterResult prePerimeterResult = new PrePerimeterSensitivityResultImpl(skippedOptimizationResult, skippedOptimizationResult, null, skippedOptimizationResult);
        return new PostPerimeterResult(skippedOptimizationResult, prePerimeterResult);
    }

    private PostPerimeterResult getResultPostState(State state, Network networkClone, PrePerimeterResult prePerimeterSensitivityOutput, OptimizationResult optimizationResult) {
        // if it's the last instant, no need to recompute things because the optimization result already contains all following states. (none)
        if (state.getInstant().equals(crac.getLastInstant())) {
            return new PostPerimeterResult(optimizationResult,
                new PrePerimeterSensitivityResultImpl(optimizationResult, optimizationResult, RangeActionSetpointResultImpl.buildFromActivationOfRangeActionAtState(optimizationResult, state), optimizationResult));
        }
        Set<State> statesToConsider = new HashSet<>();
        statesToConsider.add(state);
        crac.getStates(state.getContingency().orElseThrow()).stream()
            .filter(s -> s.getInstant().comesAfter(state.getInstant()))
            .forEach(statesToConsider::add);
        PostPerimeterSensitivityAnalysis postPerimeterSensitivityAnalysis = new PostPerimeterSensitivityAnalysis(crac, statesToConsider, raoParameters, toolProvider);
        try {
            return postPerimeterSensitivityAnalysis.runBasedOnInitialPreviousAndOptimizationResults(
                networkClone,
                initialSensitivityOutput,
                CompletableFuture.completedFuture(prePerimeterSensitivityOutput),
                stateTree.getOperatorsNotSharingCras(),
                optimizationResult,
                new AppliedRemedialActions()).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new OpenRaoException(String.format("Error while running sensi after state %s", state.getId()), e);
        }
    }

    private PrePerimeterSensitivityAnalysis getPreCurativePerimeterSensitivityAnalysis(Perimeter curativePerimeter) {
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(curativePerimeter.getRaOptimisationState());
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getRangeActions(curativePerimeter.getRaOptimisationState()));
        for (State curativeState : curativePerimeter.getAllStates()) {
            flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
        }
        return new PrePerimeterSensitivityAnalysis(crac, flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProvider);
    }

    private OptimizationResult optimizeCurativePerimeter(Perimeter curativePerimeter,
                                                         Network network,
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
        Map<RangeAction<?>, Double> rangeActionSetpointMap = crac.getRangeActions(curativeState)
            .stream()
            .collect(Collectors.toMap(rangeAction -> rangeAction, prePerimeterSensitivityOutput::getSetpoint));
        RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(rangeActionSetpointMap);
        RangeActionActivationResult rangeActionsResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        RemedialActionActivationResult remedialActionActivationResult = new RemedialActionActivationResultImpl(rangeActionsResult, new NetworkActionsResultImpl(Map.of()));

        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(flowCnecs, loopFlowCnecs, initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), raoParameters, curativePerimeter.getAllStates());
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(prePerimeterSensitivityOutput, remedialActionActivationResult);
        boolean stopCriterionReached = isStopCriterionChecked(objectiveFunctionResult, curativeTreeParameters);
        if (stopCriterionReached) {
            NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of());
            return new OptimizationResultImpl(objectiveFunctionResult, prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, networkActionsResult, rangeActionsResult);
        }

        OptimizationPerimeter optPerimeter = CurativeOptimizationPerimeter.buildForStates(curativeState, curativePerimeter.getAllStates(), crac, network, raoParameters, prePerimeterSensitivityOutput);

        SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(curativeTreeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()));

        if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters =
                raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                    ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                    : new LoadFlowAndSensitivityParameters();
            searchTreeParametersBuilder.withLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);
        }

        SearchTreeParameters searchTreeParameters = searchTreeParametersBuilder.build();

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
        if (result.getVirtualCost() > COST_EPSILON) {
            return false;
        }
        if (result.getFunctionalCost() < -Double.MAX_VALUE / 2 && result.getVirtualCost() < COST_EPSILON) {
            return true;
        }

        if (treeParameters.stopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
            return false;
        } else if (treeParameters.stopCriterion().equals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE)) {
            return result.getCost() < treeParameters.targetObjectiveValue() + COST_EPSILON;
        } else {
            throw new OpenRaoException("Unexpected stop criterion: " + treeParameters.stopCriterion());
        }
    }
}
