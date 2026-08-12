/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.AbstractOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalSensitivityResult;
import com.powsybl.openrao.searchtreerao.networkpool.AbstractNetworkPool;
import com.powsybl.openrao.searchtreerao.reports.CastorReports;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.SkippedOptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.Leaf;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
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
public class TimeCoupledCastorContingencyScenarios {

    private static final String CONTINGENCY_SCENARIO = "TimeCoupledContingencyScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final double COST_EPSILON = 1e-6;

    private final TemporalData<Crac> cracs;
    private final RaoParameters raoParameters;
    private final TemporalData<ToolProvider> toolProviders;
    private final TemporalData<StateTree> stateTrees;
    private final TreeParameters curativeTreeParameters;
    private final TemporalData<PrePerimeterResult> initialSensitivityOutputs;

    public TimeCoupledCastorContingencyScenarios(TemporalData<Crac> cracs,
                                                 RaoParameters raoParameters,
                                                 TemporalData<ToolProvider> toolProviders,
                                                 TemporalData<StateTree> stateTrees,
                                                 TreeParameters curativeTreeParameters,
                                                 TemporalData<PrePerimeterResult> initialSensitivityOutputs) {
        this.cracs = cracs;
        this.raoParameters = raoParameters;
        this.toolProviders = toolProviders;
        this.stateTrees = stateTrees;
        this.curativeTreeParameters = curativeTreeParameters;
        this.initialSensitivityOutputs = initialSensitivityOutputs;
    }

    /** Optimizes every post-contingency scenario across all the timestamps at once.*/
    public TemporalData<Map<State, PostPerimeterResult>> optimizeContingencyScenarios(final TemporalData<Network> networks,
                                                                                      final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                                                                                      final boolean automatonsOnly,
                                                                                      final ReportNode reportNode) {
        // we keep every timestamp's optimization result in a "timestamp : (state : postPerimeterResult)" map
        Map<OffsetDateTime, Map<State, PostPerimeterResult>> contingencyScenarioResults = networks.getTimestamps().stream()
                .collect(Collectors.toMap(Function.identity(), timestamp -> new ConcurrentHashMap<>()));
        // contingencyID : (timestamp : contingencyScenario)) map to group the contingency scenarios by their IDs
        Map<String, Map<OffsetDateTime, ContingencyScenario>> scenariosByContingencyId = new LinkedHashMap<>();
        Map<OffsetDateTime, AbstractNetworkPool> timestampNetworkPoolMap = new HashMap<>();
        Map<OffsetDateTime, AutomatonSimulator> timestampAutomatonSimulatorMap = new HashMap<>();

        // fill the 3 per timestamp maps (network pools, automaton simulators, contingencies) at once
        networks.getDataPerTimestamp().forEach((timestamp, network) -> {
            // Create a new variant
            String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
            network.getVariantManager().setWorkingVariant(newVariant);
            timestampNetworkPoolMap.put(timestamp, AbstractNetworkPool.create(network, newVariant, getAvailableCPUs(raoParameters), true));

            // Create an automaton simulator
            timestampAutomatonSimulatorMap.put(timestamp, createAutomatonSimulator(timestamp, prePerimeterSensitivityOutputs, reportNode));

            // Group contingency scenarios by their IDs
            stateTrees.getData(timestamp).orElseThrow().getContingencyScenarios().forEach(
                    contingencyScenario -> scenariosByContingencyId.computeIfAbsent(contingencyScenario.getContingency().getId(), id -> new HashMap<>()).put(timestamp, contingencyScenario)
            );
        });

        TemporalData<AutomatonSimulator> automatonSimulators = new TemporalDataImpl<>(timestampAutomatonSimulatorMap);
        TemporalData<AbstractNetworkPool> networkPools = new TemporalDataImpl<>(timestampNetworkPoolMap); // 1 network pool per timestamp
        AbstractNetworkPool firstNetworkPool = networkPools.getData(networkPools.getTimestamps().getFirst()).orElseThrow(); // the first timestamp's pool provides the threads
        try {
            AtomicInteger remainingScenarios = new AtomicInteger(scenariosByContingencyId.size());
            // Go through all contingency scenarios
            List<ForkJoinTask<Object>> tasks = scenariosByContingencyId.entrySet().stream().map(contingencyScenarioPerTimestamp -> {
                String contingencyId = contingencyScenarioPerTimestamp.getKey();
                TemporalData<ContingencyScenario> optimizedScenarios = new TemporalDataImpl<>(contingencyScenarioPerTimestamp.getValue());
                final ReportNode scenarioOptimizationReportNode = CastorReports.reportOptimizingScenarioForContingency(reportNode, contingencyId);
                return firstNetworkPool.submit(() ->
                        runScenario(prePerimeterSensitivityOutputs,
                            automatonsOnly,
                            optimizedScenarios,
                            networkPools,
                            automatonSimulators,
                            contingencyScenarioResults,
                            remainingScenarios,
                            scenarioOptimizationReportNode
                        )
                );
            }).toList();
            for (ForkJoinTask<Object> task : tasks) {
                try {
                    task.get();
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            for (AbstractNetworkPool networkPool : networkPools.getDataPerTimestamp().values()) {
                networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
            }
        } catch (InterruptedException e) {
            TECHNICAL_LOGS.warn("A computation thread was interrupted");
            Thread.currentThread().interrupt();
        } finally {
            networkPools.getDataPerTimestamp().values().forEach(AbstractNetworkPool::close);
        }
        return new TemporalDataImpl<>(contingencyScenarioResults);
    }

    /** creates one timestamp's automaton simulator */
    private AutomatonSimulator createAutomatonSimulator(OffsetDateTime timestamp, TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs, ReportNode reportNode) {
        return new AutomatonSimulator(
                cracs.getData(timestamp).orElseThrow(),
                raoParameters,
                toolProviders.getData(timestamp).orElseThrow(),
                initialSensitivityOutputs.getData(timestamp).orElseThrow(),
                prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow(),
                stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras(),
                NUMBER_LOGGED_ELEMENTS_DURING_RAO,
                reportNode
        );
    }

    private Object runScenario(final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                               final boolean automatonsOnly,
                               final TemporalData<ContingencyScenario> optimizedScenarios,
                               final TemporalData<AbstractNetworkPool> networkPools,
                               final TemporalData<AutomatonSimulator> automatonSimulators,
                               final Map<OffsetDateTime, Map<State, PostPerimeterResult>> contingencyScenarioResultsPerTs,
                               final AtomicInteger remainingScenarios,
                               final ReportNode reportNode) throws InterruptedException {

        String contingencyId = optimizedScenarios.getDataPerTimestamp().values().iterator().next().getContingency().getId();
        Map<OffsetDateTime, Network> timestampNetworkCloneMap = new HashMap<>();
        try {
            // pick one clone in every timestamp's pool
            for (OffsetDateTime timestamp : optimizedScenarios.getTimestamps()) {
                // this is where the threads actually wait for available networks
                timestampNetworkCloneMap.put(timestamp, networkPools.getData(timestamp).orElseThrow().getAvailableNetwork());
            }
            TemporalData<Network> networkClones = new TemporalDataImpl<>(timestampNetworkCloneMap);
            CastorReports.reportOptimizingScenarioPostContingency(reportNode, contingencyId);

            // Init variables
            TemporalData<Set<State>> curativeStates = optimizedScenarios.map(
                    scenario -> scenario.getCurativePerimeters().stream().flatMap(
                                    perimeter -> perimeter.getAllStates().stream())
                            .collect(Collectors.toSet())
            );
            Map<OffsetDateTime, PrePerimeterResult> preCurativeResultPerTimestamp = new HashMap<>();
            optimizedScenarios.getTimestamps().forEach(
                    timestamp -> preCurativeResultPerTimestamp.put(timestamp, prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow())
            );
            // set of the timestamps for which sensi failed
            Set<OffsetDateTime> autoStateSensiFailed = new HashSet<>();
            optimizedScenarios.getDataPerTimestamp().forEach(
                    // Simulate automaton instants independently on every timestamp
                    (timestamp, optimizedScenario) -> {
                        Optional<State> automatonState = optimizedScenario.getAutomatonState();
                        if (automatonState.isPresent()) {
                            Network networkClone = networkClones.getData(timestamp).orElseThrow();
                            AutomatonPerimeterResultImpl automatonResult = automatonSimulators.getData(timestamp).orElseThrow()
                                    .simulateAutomatonState(automatonState.get(), curativeStates.getData(timestamp).orElseThrow(), networkClone);
                            // recompute sensi and objective function considering auto + all instants following auto
                            PostPerimeterResult postAutoResult = getResultPostState(automatonState.get(), networkClone,
                                    prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow(), automatonResult, timestamp, null, reportNode);
                            contingencyScenarioResultsPerTs.get(timestamp).put(automatonState.get(), postAutoResult);
                            if (automatonResult.getComputationStatus() == ComputationStatus.FAILURE) {
                                autoStateSensiFailed.add(timestamp);
                            } else {
                                preCurativeResultPerTimestamp.put(timestamp, automatonResult.getPostAutomatonSensitivityAnalysisOutput());
                            }
                        }
                    });

            // Do not simulate curative instant if last sensitivity analysis failed
            // -- if there was no automaton state, check prePerimeterSensitivityOutput sensi status
            // -- or if there was an automaton state that failed
            // the coupled optimization needs every timestamp: one failed timestamp skips the whole scenario, on every timestamp
            boolean anyTimestampSensiFailed = optimizedScenarios.getDataPerTimestamp().entrySet().stream().anyMatch(entry -> {
                OffsetDateTime timestamp = entry.getKey();
                ContingencyScenario optimizedScenario = entry.getValue();
                Optional<State> automatonState = optimizedScenario.getAutomatonState();
                return !automatonsOnly
                        && automatonState.isEmpty()
                        && !optimizedScenario.getCurativePerimeters().isEmpty()
                        && prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow()
                        .getSensitivityStatus(optimizedScenario.getCurativePerimeters().getFirst().getRaOptimisationState()) == ComputationStatus.FAILURE
                        || automatonState.isPresent()
                        && autoStateSensiFailed.contains(timestamp);
            });
            if (anyTimestampSensiFailed) {
                double sensitivityFailureOvercost = getSensitivityFailureOvercost(raoParameters);
                curativeStates.getDataPerTimestamp().forEach((timestamp, timestampCurativeStates) -> timestampCurativeStates.forEach(
                        curativeState -> contingencyScenarioResultsPerTs.get(timestamp).put(curativeState, generateSkippedPostPerimeterResult(curativeState, sensitivityFailureOvercost))
                ));
            } else if (!automatonsOnly) {
                // optimize the curative perimeters with one global search tree
                if (optimizedScenarios.getDataPerTimestamp().values().stream().anyMatch(scenario -> !scenario.getCurativePerimeters().isEmpty())) {

                    TemporalData<Perimeter> curativePerimeters = optimizedScenarios.map(scenario -> scenario.getCurativePerimeters().getFirst());
                    Map<OffsetDateTime, PrePerimeterResult> previousPerimeterResults = new HashMap<>();
                    Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter = new HashMap<>();

                    curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
                        PrePerimeterResult previousPerimeterResult = preCurativeResultPerTimestamp.get(timestamp);
                        if (previousPerimeterResult == null) {
                            previousPerimeterResult = getPreCurativePerimeterSensitivityAnalysis(curativePerimeter, timestamp)
                                    .runBasedOnInitialResults(
                                            networkClones.getData(timestamp).orElseThrow(),
                                            null,
                                            stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras(),
                                            null,
                                            reportNode
                                    );
                        }
                        previousPerimeterResults.put(timestamp, previousPerimeterResult);
                        prePerimeterResultPerPerimeter.put(curativePerimeter.getRaOptimisationState(), previousPerimeterResult);
                    });

                    CurativeOptimizationResult curativeOptimizationResult = optimizeCurativePerimeter(
                            curativePerimeters,
                            networkClones,
                            new TemporalDataImpl<>(previousPerimeterResults),
                            Map.of(),
                            prePerimeterResultPerPerimeter,
                            reportNode
                    );
                    OptimizationResult curativeResult = curativeOptimizationResult.optimizationResult();

                    TemporalData<ObjectiveFunction> curativeObjectiveFunctionsPerTimestamp = curativeOptimizationResult.objectiveFunctionsPerTimestamp();

                    curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
                        State curativeState = curativePerimeter.getRaOptimisationState();
                        Network networkClone = networkClones.getData(timestamp).orElseThrow();
                        applyRemedialActions(networkClone, curativeResult, curativeState);
                        //recompute sensi and objective function considering curative + all instants following curative (useful if multi curative)
                        PostPerimeterResult postCurativeResult = getResultPostState(curativeState, networkClone, previousPerimeterResults.get(timestamp), curativeResult, timestamp,
                                curativeObjectiveFunctionsPerTimestamp.getData(timestamp).orElseThrow(), reportNode);
                        contingencyScenarioResultsPerTs.get(timestamp).put(curativeState, postCurativeResult);
                    });
                }
            }
        } finally {
            TECHNICAL_LOGS.debug("Remaining post-contingency scenarios to optimize: {}", remainingScenarios.decrementAndGet());
            for (Map.Entry<OffsetDateTime, Network> entry : timestampNetworkCloneMap.entrySet()) {
                // compare contingencies by id
                boolean actionWasApplied = contingencyScenarioResultsPerTs.get(entry.getKey()).entrySet().stream()
                        .filter(stateAndResult -> stateAndResult.getKey().getContingency().orElseThrow().getId().equals(contingencyId))
                        .anyMatch(this::isAnyActionApplied);
                networkPools.getData(entry.getKey()).orElseThrow().releaseUsedNetwork(entry.getValue(), actionWasApplied);
            }
        }
        return null;
    }

    private boolean isAnyActionApplied(Map.Entry<State, PostPerimeterResult> stateAndResult) {
        State state = stateAndResult.getKey();
        PostPerimeterResult postPerimeterResult = stateAndResult.getValue();
        boolean anyRangeActionApplied = !postPerimeterResult.optimizationResult().getActivatedRangeActions(state).isEmpty();
        boolean anyNetworkActionApplied = !postPerimeterResult.optimizationResult().getActivatedNetworkActions().isEmpty();
        return anyRangeActionApplied || anyNetworkActionApplied;
    }

    private PostPerimeterResult generateSkippedPostPerimeterResult(State state, double sensitivityFailureOvercost) {
        OptimizationResult skippedOptimizationResult = new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE, sensitivityFailureOvercost);
        PrePerimeterResult prePerimeterResult = new PrePerimeterSensitivityResultImpl(skippedOptimizationResult, skippedOptimizationResult, null, skippedOptimizationResult);
        return new PostPerimeterResult(skippedOptimizationResult, prePerimeterResult);
    }

    /** Recomputes the sensitivities and the objective function after the given state, considering this state + all the instants following it per timestamp. */
    private PostPerimeterResult getResultPostState(final State state,
                                                   final Network networkClone,
                                                   final PrePerimeterResult prePerimeterSensitivityOutput,
                                                   final OptimizationResult optimizationResult,
                                                   final OffsetDateTime timestamp,
                                                   final ObjectiveFunction perTimestampObjectiveFunction,
                                                   final ReportNode reportNode) {
        Crac crac = cracs.getData(timestamp).orElseThrow();
        // if it's the last instant, no need to recompute things because the optimization result already contains all following states. (none)
        if (state.getInstant().equals(crac.getLastInstant())) {
            OptimizationResult optimizationResultForTimestamp = getOptimizationResult(optimizationResult, state, timestamp, perTimestampObjectiveFunction, reportNode);
            RangeActionActivationResult raActivationForState;
            if (optimizationResult instanceof Leaf leaf) {
                raActivationForState = leaf.getAllRangeActionActivationResults().getData(timestamp).orElseThrow();
            } else {
                raActivationForState = optimizationResult;
            }
            return new PostPerimeterResult(optimizationResultForTimestamp,
                new PrePerimeterSensitivityResultImpl(
                    optimizationResultForTimestamp,
                    optimizationResultForTimestamp,
                    RangeActionSetpointResultImpl.buildFromActivationOfRangeActionAtState(raActivationForState, state),
                    optimizationResultForTimestamp
                )
            );
        }
        Set<State> statesToConsider = new HashSet<>();
        statesToConsider.add(state);
        crac.getStates(state.getContingency().orElseThrow()).stream()
            .filter(s -> s.getInstant().comesAfter(state.getInstant()))
            .forEach(statesToConsider::add);
        PostPerimeterSensitivityAnalysis postPerimeterSensitivityAnalysis = new PostPerimeterSensitivityAnalysis(crac, statesToConsider, raoParameters,
                toolProviders.getData(timestamp).orElseThrow(), false);

        return postPerimeterSensitivityAnalysis.runBasedOnInitialPreviousAndOptimizationResults(
            networkClone,
            initialSensitivityOutputs.getData(timestamp).orElseThrow(),
            prePerimeterSensitivityOutput,
            stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras(),
            getOptimizationResult(optimizationResult, state, timestamp, null, reportNode),
            new AppliedRemedialActions(),
            reportNode);
    }

    /** Get the optimization result of one timestamp. */
    OptimizationResult getOptimizationResult(OptimizationResult optimizationResult, State state, OffsetDateTime timestamp, ObjectiveFunction perTimestampObjectiveFunction, ReportNode reportNode) {
        if (optimizationResult instanceof Leaf leaf) {
            RangeActionActivationResult activationForTimestamp = leaf.getAllRangeActionActivationResults().getData(timestamp).orElseThrow();
            // the shared decision is declared at this timestamp's state only
            NetworkActionsResult networkActionsForState = new NetworkActionsResultImpl(Map.of(state, getNetworkActionsOfTimestamp(leaf.getActivatedNetworkActions(), timestamp)));
            ObjectiveFunctionResult objectiveFunctionResultForTimestamp = leaf;
            if (perTimestampObjectiveFunction != null) {
                RemedialActionActivationResult remedialActionActivationResultForTimestamp = new RemedialActionActivationResultImpl(activationForTimestamp, networkActionsForState);
                objectiveFunctionResultForTimestamp = perTimestampObjectiveFunction.evaluate(leaf, remedialActionActivationResultForTimestamp, reportNode);
            }
            return new OptimizationResultImpl(objectiveFunctionResultForTimestamp, leaf, leaf, networkActionsForState, activationForTimestamp);
        }
        return optimizationResult;
    }

    private Set<NetworkAction> getNetworkActionsOfTimestamp(Set<NetworkAction> appliedNetworkActions, OffsetDateTime timestamp) {
        Crac crac = cracs.getData(timestamp).orElseThrow();
        return appliedNetworkActions.stream()
            .map(networkAction -> crac.getNetworkAction(networkAction.getId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private PrePerimeterSensitivityAnalysis getPreCurativePerimeterSensitivityAnalysis(Perimeter curativePerimeter, OffsetDateTime timestamp) {
        Crac crac = cracs.getData(timestamp).orElseThrow();
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(curativePerimeter.getRaOptimisationState());
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getRangeActions(curativePerimeter.getRaOptimisationState()));
        for (State curativeState : curativePerimeter.getAllStates()) {
            flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
        }
        return new PrePerimeterSensitivityAnalysis(crac, flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProviders.getData(timestamp).orElseThrow(), false);
    }

    private CurativeOptimizationResult optimizeCurativePerimeter(final TemporalData<Perimeter> curativePerimeters,
                                                                 final TemporalData<Network> networks,
                                                                 final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                                                                 final Map<State, OptimizationResult> resultsPerPerimeter,
                                                                 final Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter,
                                                                 final ReportNode reportNode) {
        // TODO : multi-curative is not supported
        // flowCnecs, loopFlowCnecs, states, operators, filtered states per timestamps union
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        Set<FlowCnec> allLoopFlowCnecs = new HashSet<>();
        Set<State> allOptimisationStates = new HashSet<>();
        Set<String> allOperatorsNotSharingCras = new HashSet<>();
        // collect the optimization perimeters per timestamp
        Map<OffsetDateTime, OptimizationPerimeter> timestampOptimizationPerimeterMap = new HashMap<>();
        Map<OffsetDateTime, FlowResult> timestampInitialFlowResultMap = new HashMap<>();
        Map<OffsetDateTime, AppliedRemedialActions> timestampPreOptimAppliedRaMap = new HashMap<>();
        Map<OffsetDateTime, Instant> timestampOutageInstantsMap = new HashMap<>();
        Map<OffsetDateTime, ToolProvider> timestampToolProvidersMap = new HashMap<>();

        AtomicBoolean anyHvdcAcEmulation = new AtomicBoolean(false);

        Map<OffsetDateTime, ObjectiveFunction> timestampObjectiveFunctionMap = new HashMap<>();
        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
            Crac crac = cracs.getData(timestamp).orElseThrow();
            PrePerimeterResult prePerimeterSensitivityOutput = prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow();
            Set<FlowCnec> flowCnecs = getFlowCnecsOfNonFailedStates(crac, curativePerimeter, prePerimeterSensitivityOutput);
            Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, networks.getData(timestamp).orElseThrow());
            Set<String> operatorsNotSharingCras = stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras();
            allFlowCnecs.addAll(flowCnecs);
            allLoopFlowCnecs.addAll(loopFlowCnecs);
            allOptimisationStates.addAll(curativePerimeter.getAllStates());
            allOperatorsNotSharingCras.addAll(operatorsNotSharingCras);
            timestampObjectiveFunctionMap.put(timestamp, ObjectiveFunction.build(
                    flowCnecs,
                    loopFlowCnecs,
                    initialSensitivityOutputs.getData(timestamp).orElseThrow(),
                    prePerimeterSensitivityOutput,
                    operatorsNotSharingCras,
                    raoParameters,
                    curativePerimeter.getAllStates()
            ));
        });

        // build every timestamp's search tree input
        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
            State curativeState = curativePerimeter.getRaOptimisationState();
            Crac crac = cracs.getData(timestamp).orElseThrow();
            Network network = networks.getData(timestamp).orElseThrow();
            CastorReports.reportOptimizingCurativeState(reportNode, curativeState.getId());
            timestampOptimizationPerimeterMap.put(timestamp, CurativeOptimizationPerimeter.buildForStates(curativeState, curativePerimeter.getAllStates(),
                    crac, network, raoParameters, prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow(), reportNode));
            timestampInitialFlowResultMap.put(timestamp, initialSensitivityOutputs.getData(timestamp).orElseThrow());
            timestampPreOptimAppliedRaMap.put(timestamp, new AppliedRemedialActions());
            timestampOutageInstantsMap.put(timestamp, crac.getOutageInstant());
            timestampToolProvidersMap.put(timestamp, toolProviders.getData(timestamp).orElseThrow());
            if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
                anyHvdcAcEmulation.set(true);
            }
        });

        Map<RangeAction<?>, Double> rangeActionSetpointMap = new HashMap<>();
        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
            PrePerimeterResult prePerimeterSensitivityOutput = prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow();
            cracs.getData(timestamp).orElseThrow().getRangeActions(curativePerimeter.getRaOptimisationState())
                    .forEach(ra -> rangeActionSetpointMap.put(ra, prePerimeterSensitivityOutput.getSetpoint(ra)));
        });
        RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(rangeActionSetpointMap);
        RangeActionActivationResult rangeActionsResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        RemedialActionActivationResult remedialActionActivationResult = new RemedialActionActivationResultImpl(rangeActionsResult, new NetworkActionsResultImpl(Map.of()));

        GlobalFlowResult globalPrePerimeterFlowResult = new GlobalFlowResult(prePerimeterSensitivityOutputs);
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
            allFlowCnecs,
            allLoopFlowCnecs,
            new GlobalFlowResult(initialSensitivityOutputs),
            globalPrePerimeterFlowResult,
            allOperatorsNotSharingCras,
            raoParameters,
            allOptimisationStates
        );
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(globalPrePerimeterFlowResult, remedialActionActivationResult, reportNode);
        // TODO: since the global cost is a sum, it being satisfied does not mean that every timestamp is secure ?
        //  The check is done here to avoid skipping the optimization of a scenario where one timestamp is still overloaded.
        boolean stopCriterionReached = isStopCriterionChecked(objectiveFunctionResult, curativeTreeParameters);
        boolean secureAtEveryTimestamp = objectiveFunctionResult.getMostLimitingElements(1).stream().allMatch(cnec -> globalPrePerimeterFlowResult.getMargin(cnec, Unit.MEGAWATT) >= 0);
        if (stopCriterionReached && secureAtEveryTimestamp) {
            NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of());
            GlobalSensitivityResult globalPrePerimeterSensitivityResult = new GlobalSensitivityResult(prePerimeterSensitivityOutputs.map(SensitivityResult.class::cast));
            OptimizationResult noActionResult = new OptimizationResultImpl(objectiveFunctionResult, globalPrePerimeterFlowResult,
                    globalPrePerimeterSensitivityResult, networkActionsResult, rangeActionsResult
            );
            return new CurativeOptimizationResult(noActionResult, new TemporalDataImpl<>(timestampObjectiveFunctionMap));
        }

        Crac referenceCrac = cracs.getData(cracs.getTimestamps().getFirst()).orElseThrow();
        SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, referenceCrac)
            .withTreeParameters(curativeTreeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), allOperatorsNotSharingCras))
            .withGlobalRemedialActionLimitationParameters(mergeRaUsageLimitsAcrossTimestamps(cracs));

        if (anyHvdcAcEmulation.get()) {
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                    ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                    : new LoadFlowAndSensitivityParameters(reportNode);
            searchTreeParametersBuilder.withLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);
        }

        SearchTreeParameters searchTreeParameters = searchTreeParametersBuilder.build();

        searchTreeParameters.decreaseRemedialActionUsageLimits(resultsPerPerimeter, prePerimeterResultPerPerimeter);

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withAllNetworks(networks)
            .withAllOptimizationPerimeters(new TemporalDataImpl<>(timestampOptimizationPerimeterMap))
            .withAllInitialFlowResults(new TemporalDataImpl<>(timestampInitialFlowResultMap))
            .withAllPrePerimeterResults(prePerimeterSensitivityOutputs)
            .withAllPreOptimizationAppliedNetworkActions(new TemporalDataImpl<>(timestampPreOptimAppliedRaMap))
            .withObjectiveFunction(objectiveFunction)
            .withAllToolProviders(new TemporalDataImpl<>(timestampToolProvidersMap))
            .withAllOutageInstants(new TemporalDataImpl<>(timestampOutageInstantsMap))
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, false, reportNode).run().join();
        curativePerimeters.getDataPerTimestamp().values().forEach(perimeter ->
            CastorReports.reportCurativeStateOptimized(reportNode, perimeter.getRaOptimisationState().getId()));
        return new CurativeOptimizationResult(result, new TemporalDataImpl<>(timestampObjectiveFunctionMap));
    }

    /** Gathers the flow CNECs of the curative perimeter's states, ignoring the states whose sensitivity analysis failed */
    private Set<FlowCnec> getFlowCnecsOfNonFailedStates(Crac crac, Perimeter curativePerimeter, PrePerimeterResult prePerimeterSensitivityOutput) {
        Set<State> nonFailedStates = curativePerimeter.getAllStates().stream()
                .filter(state -> !prePerimeterSensitivityOutput.getSensitivityStatus(state).equals(ComputationStatus.FAILURE))
                .collect(Collectors.toSet());
        return crac.getFlowCnecs().stream().filter(flowCnec -> nonFailedStates.contains(flowCnec.getState())).collect(Collectors.toSet());
    }

    /**
     * Gathers the RaUsageLimits of every timestamp's CRAC in a single map, so that each timestamp's states resolve
     * their limits with plain map lookups. The limits are assumed identical between the timestamps and are not checked.
     */
    public static Map<Instant, RaUsageLimits> mergeRaUsageLimitsAcrossTimestamps(TemporalData<Crac> cracs) {
        Map<Instant, RaUsageLimits> raUsageLimitsPerInstant = new HashMap<>();
        cracs.getDataPerTimestamp().values().forEach(crac -> raUsageLimitsPerInstant.putAll(crac.getRaUsageLimitsPerInstant()));
        return raUsageLimitsPerInstant;
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

    /** Result of the coupled curative search tree, to report a timestamp's own cost instead of the coupled global one */
    private record CurativeOptimizationResult(OptimizationResult optimizationResult, TemporalData<ObjectiveFunction> objectiveFunctionsPerTimestamp) {
    }
}
