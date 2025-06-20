/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OneStateOnlyRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.SkippedOptimizationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost;
import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
public class RemedialActionsApplier {

    private static final String CONTINGENCY_SCENARIO = "ContingencyScenario";

    private final Crac crac;
    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;
    private final StateTree stateTree;
    private final RaoResult raoResult;

    public RemedialActionsApplier(final Crac crac,
                                  final RaoParameters raoParameters,
                                  final ToolProvider toolProvider,
                                  final StateTree stateTree,
                                  final RaoResult raoResult) {
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
        this.stateTree = stateTree;
        this.raoResult = raoResult;
    }

    public Map<State, OptimizationResult> optimizeContingencyScenarios(final Network network,
                                                                       final PrePerimeterResult prePerimeterSensitivityOutput,
                                                                       final Map<State, Map<RangeAction<?>, Double>> pstRaMap) {
        final Map<State, OptimizationResult> contingencyScenarioResults = new ConcurrentHashMap<>();
        // Create a new variant
        final String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);
        // Go through all contingency scenarios
        try (final AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, newVariant, getAvailableCPUs(raoParameters), true)) {
            final List<ForkJoinTask<Object>> tasks = stateTree.getContingencyScenarios().stream().map(optimizedScenario ->
                    networkPool.submit(() -> runScenario(prePerimeterSensitivityOutput, optimizedScenario,
                            networkPool, contingencyScenarioResults, pstRaMap))
            ).toList();
            for (final ForkJoinTask<Object> task : tasks) {
                try {
                    task.get();
                } catch (final ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return contingencyScenarioResults;
    }

    private Object runScenario(final PrePerimeterResult prePerimeterSensitivityOutput,
                               final ContingencyScenario optimizedScenario,
                               final AbstractNetworkPool networkPool,
                               final Map<State, OptimizationResult> contingencyScenarioResults,
                               final Map<State, Map<RangeAction<?>, Double>> pstRaMap) throws InterruptedException {
        final Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks

        // Init variables
        final Optional<State> automatonState = optimizedScenario.getAutomatonState();
        final Set<State> curativeStates = new HashSet<>();
        optimizedScenario.getCurativePerimeters().forEach(perimeter -> curativeStates.addAll(perimeter.getAllStates()));
        final double sensitivityFailureOvercost = getSensitivityFailureOvercost(raoParameters);

        // Simulate automaton instant
        // Do not simulate curative instant if last sensitivity analysis failed
        // -- if there was no automaton state, check prePerimeterSensitivityOutput sensi status
        // -- or if there was an automaton state that failed
        if (automatonState.isEmpty()
            && !optimizedScenario.getCurativePerimeters().isEmpty()
            && prePerimeterSensitivityOutput.getSensitivityStatus(optimizedScenario.getCurativePerimeters().get(0).getRaOptimisationState()) == ComputationStatus.FAILURE) {
            curativeStates.forEach(curativeState -> contingencyScenarioResults.put(curativeState, new SkippedOptimizationResultImpl(curativeState, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE, sensitivityFailureOvercost)));
        } else {
            boolean allPreviousPerimetersSucceded = true;
            PrePerimeterResult previousPerimeterResult = prePerimeterSensitivityOutput;
            // Optimize curative perimeters
            for (final Perimeter curativePerimeter : optimizedScenario.getCurativePerimeters()) {
                final State curativeState = curativePerimeter.getRaOptimisationState();
                if (previousPerimeterResult == null) {
                    previousPerimeterResult = getPreCurativePerimeterSensitivityAnalysis(curativePerimeter).runBasedOnInitialResults(networkClone, crac, null, stateTree.getOperatorsNotSharingCras(), null);
                }
                final Set<FlowCnec> flowCnecs = getFlowCnecsOfPerimeter(curativePerimeter, crac);
                final OptimizationResult optimizationResult = applyOptimizedRemedialActions(previousPerimeterResult, flowCnecs, curativeState, networkClone, raoResult, toolProvider, raoParameters, crac, pstRaMap.getOrDefault(curativeState, Map.of())).getOptimizationResult(curativeState);
                if (allPreviousPerimetersSucceded) {
                    allPreviousPerimetersSucceded = optimizationResult.getSensitivityStatus() == DEFAULT;
                    contingencyScenarioResults.put(curativeState, optimizationResult);
                    previousPerimeterResult = null;
                } else {
                    contingencyScenarioResults.put(curativeState, new SkippedOptimizationResultImpl(curativeState, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE, sensitivityFailureOvercost));
                }
            }
        }
        networkPool.releaseUsedNetwork(networkClone);
        return null;
    }

    private PrePerimeterSensitivityAnalysis getPreCurativePerimeterSensitivityAnalysis(final Perimeter curativePerimeter) {
        final Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(curativePerimeter.getRaOptimisationState());
        final Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getPotentiallyAvailableRangeActions(curativePerimeter.getRaOptimisationState()));
        for (final State curativeState : curativePerimeter.getAllStates()) {
            flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
        }
        return new PrePerimeterSensitivityAnalysis(flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProvider);
    }

    static OneStateOnlyRaoResultImpl applyOptimizedRemedialActions(final PrePerimeterResult prePerimeterResult,
                                                                   final Set<FlowCnec> perimeterFlowCnecs,
                                                                   final State state,
                                                                   final Network network,
                                                                   final RaoResult raoResult,
                                                                   final ToolProvider toolProvider,
                                                                   final RaoParameters raoParameters,
                                                                   final Crac crac,
                                                                   final Map<RangeAction<?>, Double> forcedSetPoints) {
        final NetworkActionsResultImpl networkActionsResult = new NetworkActionsResultImpl(raoResult.getActivatedNetworkActionsDuringState(state));
        networkActionsResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));

        final RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(prePerimeterResult);
        raoResult.getActivatedRangeActionsDuringState(state)
                .forEach(action -> rangeActionActivationResult.putResult(action, state, raoResult.getOptimizedSetPointOnState(state, action)));
        forcedSetPoints.forEach((rangeAction, setPoint) -> rangeActionActivationResult.putResult(rangeAction, state, setPoint));
        rangeActionActivationResult.getOptimizedSetpointsOnState(state).forEach((rangeAction, setPoint) -> rangeAction.apply(network, setPoint));

        final AppliedRemedialActions curativeRAs = new AppliedRemedialActions();
        if (state.getInstant().isCurative()) {
            curativeRAs.addAppliedNetworkActions(state, networkActionsResult.getActivatedNetworkActions());
            curativeRAs.addAppliedRangeActions(state, rangeActionActivationResult.getOptimizedSetpointsOnState(state));
        }

        final PrePerimeterResult postRAperimeterResult = new PrePerimeterSensitivityAnalysis(perimeterFlowCnecs, crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.FORCED), raoParameters, toolProvider)
                .runBasedOnInitialResults(network, crac, prePerimeterResult, Set.of(), curativeRAs);
        final OptimizationResultImpl optimizationResult = new OptimizationResultImpl(postRAperimeterResult, postRAperimeterResult, postRAperimeterResult, networkActionsResult, rangeActionActivationResult);
        return new OneStateOnlyRaoResultImpl(state, prePerimeterResult, optimizationResult, perimeterFlowCnecs);
    }

    static Set<FlowCnec> getFlowCnecsOfPerimeter(final Perimeter perimeter,
                                                 final Crac crac) {
        return perimeter
                .getAllStates()
                .stream()
                .map(crac::getFlowCnecs)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
