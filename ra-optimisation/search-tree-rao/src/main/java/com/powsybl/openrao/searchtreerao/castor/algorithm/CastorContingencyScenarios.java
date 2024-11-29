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
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost;
import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getContingencyScenariosInParallel;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyRemedialActions;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CastorContingencyScenarios {

    private static final String CONTINGENCY_SCENARIO = "ContingencyScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;

    private final Crac crac;
    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;
    private final StateTree stateTree;
    private final TreeParameters automatonTreeParameters;
    private final TreeParameters curativeTreeParameters;
    private final PrePerimeterResult initialSensitivityOutput;

    public CastorContingencyScenarios(Crac crac,
                                      RaoParameters raoParameters,
                                      ToolProvider toolProvider,
                                      StateTree stateTree,
                                      TreeParameters automatonTreeParameters,
                                      TreeParameters curativeTreeParameters,
                                      PrePerimeterResult initialSensitivityOutput) {
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
        this.stateTree = stateTree;
        this.automatonTreeParameters = automatonTreeParameters;
        this.curativeTreeParameters = curativeTreeParameters;
        this.initialSensitivityOutput = initialSensitivityOutput;
    }

    public Map<State, OptimizationResult> optimizeContingencyScenarios(Network network,
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
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, newVariant, getContingencyScenariosInParallel(raoParameters), true)) {
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

    private Object runScenario(PrePerimeterResult prePerimeterSensitivityOutput, boolean automatonsOnly, ContingencyScenario optimizedScenario, AbstractNetworkPool networkPool, AutomatonSimulator automatonSimulator, Map<State, OptimizationResult> contingencyScenarioResults, AtomicInteger remainingScenarios) throws InterruptedException {
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
                    previousPerimeterResult = getPreCurativePerimeterSensitivityAnalysis(curativePerimeter).runBasedOnInitialResults(networkClone, crac, null, stateTree.getOperatorsNotSharingCras(), null);
                }
                prePerimeterResultPerPerimeter.put(curativePerimeter.getRaOptimisationState(), previousPerimeterResult);
                if (allPreviousPerimetersSucceded) {
                    OptimizationResult curativeResult = optimizeCurativePerimeter(curativePerimeter, networkClone, previousPerimeterResult, resultsPerPerimeter, prePerimeterResultPerPerimeter);
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

    private PrePerimeterSensitivityAnalysis getPreCurativePerimeterSensitivityAnalysis(Perimeter curativePerimeter) {
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(curativePerimeter.getRaOptimisationState());
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getPotentiallyAvailableRangeActions(curativePerimeter.getRaOptimisationState()));
        for (State curativeState : curativePerimeter.getAllStates()) {
            flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
        }
        return new PrePerimeterSensitivityAnalysis(flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProvider);
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
}
