/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.timecoupledsearchtreerao.castor.algorithm;

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
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.ToolProvider;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.optimizationperimeters.AbstractOptimizationPerimeter;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.timecoupledsearchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.timecoupledsearchtreerao.marmot.results.GlobalSensitivityResult;
import com.powsybl.openrao.timecoupledsearchtreerao.reports.CastorReports;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.FlowResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.SkippedOptimizationResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.algorithms.TimeCoupledLeaf;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.algorithms.TimeCoupledSearchTree;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.inputs.TimeCoupledSearchTreeInput;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.util.AbstractNetworkPool;

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
import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost;
import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;
import static com.powsybl.openrao.timecoupledsearchtreerao.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;
import static com.powsybl.openrao.timecoupledsearchtreerao.commons.RaoUtil.applyRemedialActions;

/**
 * Multi-timestamp version of CastorContingencyScenarios.
 * In time coupled :
 * optimizes every post-contingency scenario across all timestamps at once.
 * <li> inputs are all temporal data
 * <li> the contingency scenarios of all the timestamps are grouped by contingency id -> one scenario = same contingency on every timestamp.
 * <li> one network pool is created per timestamp
 * <li> automatons are simulated independently per timestamp
 * <li> a timestamp whose sensitivity failed is skipped without affecting the other timestamps
 * <li> curative perimeters are optimized through one global search tree : the topological actions are shared and the range action setpoints are synchronized in the mip
 *
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

    /**
     * optimizes every post-contingency scenario across all timestamps at once. The contingency scenarios are grouped by
     * contingency id and run in parallel on the first timestamp's pool threads.
     */
    public TemporalData<Map<State, PostPerimeterResult>> optimizeContingencyScenarios(final TemporalData<Network> networks,
                                                                                      final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                                                                                      final boolean automatonsOnly,
                                                                                      final ReportNode reportNode) {
        // we keep every timestamp's optimization result in a "timestamp : (state : postPerimeterResult)" map
        Map<OffsetDateTime, Map<State, PostPerimeterResult>> contingencyScenarioResults = networks.getTimestamps().stream().collect(Collectors.toMap(Function.identity(), timestamp -> new ConcurrentHashMap<>()));
        int parallelism = getAvailableCPUs(raoParameters);
        Map<OffsetDateTime, AbstractNetworkPool> poolsPerTimestamp = new HashMap<>();
        Map<OffsetDateTime, AutomatonSimulator> automatonSimulatorPerTimestamp = new HashMap<>();
        Map<String, Map<OffsetDateTime, ContingencyScenario>> scenariosByContingencyId = new LinkedHashMap<>();
        // build the 3 per timestamp structures (network pools, automaton simulators, contingency groupings) at once
        networks.getDataPerTimestamp().forEach((timestamp, network) -> {
            String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
            network.getVariantManager().setWorkingVariant(newVariant);
            poolsPerTimestamp.put(timestamp, AbstractNetworkPool.create(network, newVariant, parallelism, true));

            automatonSimulatorPerTimestamp.put(timestamp, new AutomatonSimulator(
                    cracs.getData(timestamp).orElseThrow(),
                    raoParameters,
                    toolProviders.getData(timestamp).orElseThrow(),
                    initialSensitivityOutputs.getData(timestamp).orElseThrow(),
                    prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow(),
                    stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras(),
                    NUMBER_LOGGED_ELEMENTS_DURING_RAO,
                    reportNode
            ));
            // "contingency id : (timestamp : contingency object)" to group the common contingencies across timestamps
            stateTrees.getData(timestamp).orElseThrow().getContingencyScenarios().forEach(scenario -> scenariosByContingencyId.computeIfAbsent(scenario.getContingency().getId(), id -> new HashMap<>()).put(timestamp, scenario));
        });
        TemporalData<AutomatonSimulator> automatonSimulators = new TemporalDataImpl<>(automatonSimulatorPerTimestamp);

        // 1 network pool per timestamp
        TemporalData<AbstractNetworkPool> networkPools = new TemporalDataImpl<>(poolsPerTimestamp);
        // the first timestamp's pool only provides the threads
        // every pool, including the first, provides one network clone per running task
        AbstractNetworkPool submitPool = networkPools.getDataPerTimestamp().values().iterator().next();
        try {
            AtomicInteger remainingScenarios = new AtomicInteger(scenariosByContingencyId.size());
            List<ForkJoinTask<Object>> tasks = scenariosByContingencyId.entrySet().stream().map(contingencyScenarioPerTimestamp -> {
                String contingencyId = contingencyScenarioPerTimestamp.getKey();
                TemporalData<ContingencyScenario> optimizedScenarios = new TemporalDataImpl<>(contingencyScenarioPerTimestamp.getValue());
                final ReportNode scenarioOptimizationReportNode = CastorReports.reportOptimizingScenarioForContingency(reportNode, contingencyId);
                return submitPool.submit(() -> runScenario(prePerimeterSensitivityOutputs, automatonsOnly, optimizedScenarios, networkPools, automatonSimulators, contingencyScenarioResults, remainingScenarios, scenarioOptimizationReportNode));
            }
            ).toList();
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

    private Object runScenario(final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                               final boolean automatonsOnly,
                               final TemporalData<ContingencyScenario> optimizedScenarios,
                               final TemporalData<AbstractNetworkPool> networkPools,
                               final TemporalData<AutomatonSimulator> automatonSimulators,
                               final Map<OffsetDateTime, Map<State, PostPerimeterResult>> contingencyScenarioResultsPerTs,
                               final AtomicInteger remainingScenarios,
                               final ReportNode reportNode) throws InterruptedException {
        // the grouping guarantees that every timestamp's scenario carries the same contingency id
        String contingencyId = optimizedScenarios.getDataPerTimestamp().values().iterator().next().getContingency().getId();
        Map<OffsetDateTime, Network> networkClonePerTimestamp = new HashMap<>();
        try {
            // pick one clone in every timestamp's pool, this is where the threads actually wait for available networks
            for (OffsetDateTime timestamp : optimizedScenarios.getTimestamps()) {
                networkClonePerTimestamp.put(timestamp, networkPools.getData(timestamp).orElseThrow().getAvailableNetwork());
            }
            TemporalData<Network> networkClones = new TemporalDataImpl<>(networkClonePerTimestamp);
            CastorReports.reportOptimizingScenarioPostContingency(reportNode, contingencyId);

            // Init variables
            Map<OffsetDateTime, PrePerimeterResult> preCurativeResultPerTimestamp = new HashMap<>();
            optimizedScenarios.getTimestamps().forEach(timestamp -> preCurativeResultPerTimestamp.put(timestamp, prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow()));
            TemporalData<Set<State>> curativeStates = optimizedScenarios.map(scenario -> scenario.getCurativePerimeters().stream()
                    .flatMap(perimeter -> perimeter.getAllStates().stream())
                    .collect(Collectors.toSet()));
            double sensitivityFailureOvercost = getSensitivityFailureOvercost(raoParameters);

            // Simulate automaton instants independently on every timestamp
            Set<OffsetDateTime> autoStateSensiFailed = new HashSet<>();
            optimizedScenarios.getDataPerTimestamp().forEach((timestamp, optimizedScenario) -> {
                Optional<State> automatonState = optimizedScenario.getAutomatonState();
                if (automatonState.isPresent()) {
                    Network networkClone = networkClones.getData(timestamp).orElseThrow();
                    AutomatonPerimeterResultImpl automatonResult = automatonSimulators.getData(timestamp).orElseThrow()
                            .simulateAutomatonState(automatonState.get(), curativeStates.getData(timestamp).orElseThrow(), networkClone);
                    // recompute sensi and objective function considering auto + all instants following auto
                    PostPerimeterResult postAutoResult = getResultPostState(automatonState.get(), networkClone, prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow(), automatonResult, timestamp, reportNode);
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
            // a failed timestamp is skipped while the other timestamps still get optimized
            Set<OffsetDateTime> activeTimestamps = new HashSet<>(optimizedScenarios.getTimestamps());
            optimizedScenarios.getDataPerTimestamp().forEach((timestamp, optimizedScenario) -> {
                Optional<State> automatonState = optimizedScenario.getAutomatonState();
                if (!automatonsOnly
                        && automatonState.isEmpty()
                        && !optimizedScenario.getCurativePerimeters().isEmpty()
                        && prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow().getSensitivityStatus(optimizedScenario.getCurativePerimeters().getFirst().getRaOptimisationState()) == ComputationStatus.FAILURE
                        || automatonState.isPresent()
                        && autoStateSensiFailed.contains(timestamp)
                ) {
                    activeTimestamps.remove(timestamp);
                    curativeStates.getData(timestamp).orElseThrow().forEach(curativeState -> contingencyScenarioResultsPerTs.get(timestamp).put(curativeState, generateSkippedPostPerimeterResult(curativeState, sensitivityFailureOvercost)));
                }
            });
            if (!automatonsOnly && !activeTimestamps.isEmpty()) {
                // Optimize curative perimeters by one global search tree
                Set<Integer> curativePerimeterCounts = activeTimestamps.stream().map(timestamp ->
                        optimizedScenarios.getData(timestamp).orElseThrow().getCurativePerimeters().size()).collect(Collectors.toSet());
                if (curativePerimeterCounts.size() > 1) {
                    throw new OpenRaoException("number of curative instants is different across timestamps for contingency " + contingencyId + " (found: " + curativePerimeterCounts + ")");
                }
                boolean allPreviousPerimetersSucceded = true;
                Map<OffsetDateTime, PrePerimeterResult> previousPerimeterResultPerTs = new HashMap<>();
                activeTimestamps.forEach(timestamp -> previousPerimeterResultPerTs.put(timestamp, preCurativeResultPerTimestamp.get(timestamp)));
                Map<State, OptimizationResult> resultsPerPerimeter = new HashMap<>();
                Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter = new HashMap<>();
                TemporalData<Network> activeNetworkClones = filterTemporalData(networkClones, activeTimestamps);
                for (int perimeterIndex = 0; perimeterIndex < curativePerimeterCounts.iterator().next(); perimeterIndex++) {
                    final int index = perimeterIndex;
                    TemporalData<Perimeter> curativePerimeters = new TemporalDataImpl<>(activeTimestamps.stream().collect(Collectors.toMap(Function.identity(),
                            timestamp -> optimizedScenarios.getData(timestamp).orElseThrow().getCurativePerimeters().get(index))));
                    curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
                        if (previousPerimeterResultPerTs.get(timestamp) == null) {
                            previousPerimeterResultPerTs.put(timestamp, getPreCurativePerimeterSensitivityAnalysis(curativePerimeter, timestamp)
                                    .runBasedOnInitialResults(activeNetworkClones.getData(timestamp).orElseThrow(), null, stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras(), null, reportNode));
                        }
                        prePerimeterResultPerPerimeter.put(curativePerimeter.getRaOptimisationState(), previousPerimeterResultPerTs.get(timestamp));
                    });
                    if (allPreviousPerimetersSucceded) {
                        OptimizationResult curativeResult = optimizeCurativePerimeter(
                                curativePerimeters,
                                activeNetworkClones,
                                filterMapToTemporalData(previousPerimeterResultPerTs, curativePerimeters.getDataPerTimestamp().keySet()),
                                resultsPerPerimeter,
                                prePerimeterResultPerPerimeter,
                                reportNode
                        );
                        // a single failure fails every timestamp
                        allPreviousPerimetersSucceded = curativeResult.getSensitivityStatus() == DEFAULT;
                        final boolean perimeterSucceeded = allPreviousPerimetersSucceded;
                        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
                            State curativeState = curativePerimeter.getRaOptimisationState();
                            Network networkClone = activeNetworkClones.getData(timestamp).orElseThrow();
                            applyRemedialActions(networkClone, curativeResult, curativeState);
                            // recompute sensi and objective function considering curative + all instants following curative (useful if multi curative)
                            PostPerimeterResult postCurativeResult = getResultPostState(curativeState, networkClone, previousPerimeterResultPerTs.get(timestamp), curativeResult, timestamp, reportNode);
                            contingencyScenarioResultsPerTs.get(timestamp).put(curativeState, postCurativeResult);
                            previousPerimeterResultPerTs.put(timestamp, null);
                            if (perimeterSucceeded) {
                                resultsPerPerimeter.put(curativeState, curativeResult);
                            }
                        });
                    } else {
                        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
                            State curativeState = curativePerimeter.getRaOptimisationState();
                            contingencyScenarioResultsPerTs.get(timestamp).put(curativeState, generateSkippedPostPerimeterResult(curativeState, sensitivityFailureOvercost));
                        });
                    }
                }
            }
        } finally {
            TECHNICAL_LOGS.debug("Remaining post-contingency scenarios to optimize: {}", remainingScenarios.decrementAndGet());
            // release every clone
            for (Map.Entry<OffsetDateTime, Network> entry : networkClonePerTimestamp.entrySet()) {
                // contingencies are compared by id
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

    /**
     * Recomputes the sensitivities and the objective function after the given state,
     * considering this state + all the instants following it per timestamp.
     * */
    private PostPerimeterResult getResultPostState(final State state,
                                                   final Network networkClone,
                                                   final PrePerimeterResult prePerimeterSensitivityOutput,
                                                   final OptimizationResult optimizationResult,
                                                   final OffsetDateTime timestamp,
                                                   final ReportNode reportNode) {
        Crac crac = cracs.getData(timestamp).orElseThrow();
        // if it's the last instant, no need to recompute things because the optimization result already contains all following states. (none)
        if (state.getInstant().equals(crac.getLastInstant())) {
            RangeActionActivationResult raActivationForState;
            // for a time coupled leaf the setpoints of this timestamp must be extracted from its per-timestamp activation results
            if (optimizationResult instanceof TimeCoupledLeaf timeCoupledLeaf) {
                raActivationForState = timeCoupledLeaf.getRangeActionActivationResults().getData(timestamp).orElseThrow();
            } else {
                raActivationForState = optimizationResult;
            }
            return new PostPerimeterResult(optimizationResult,
                new PrePerimeterSensitivityResultImpl(
                    optimizationResult,
                    optimizationResult,
                    RangeActionSetpointResultImpl.buildFromActivationOfRangeActionAtState(raActivationForState, state),
                    optimizationResult
                )
            );
        }
        Set<State> statesToConsider = new HashSet<>();
        statesToConsider.add(state);
        crac.getStates(state.getContingency().orElseThrow()).stream()
            .filter(s -> s.getInstant().comesAfter(state.getInstant()))
            .forEach(statesToConsider::add);
        PostPerimeterSensitivityAnalysis postPerimeterSensitivityAnalysis = new PostPerimeterSensitivityAnalysis(crac, statesToConsider, raoParameters, toolProviders.getData(timestamp).orElseThrow(), false);

        return postPerimeterSensitivityAnalysis.runBasedOnInitialPreviousAndOptimizationResults(
            networkClone,
            initialSensitivityOutputs.getData(timestamp).orElseThrow(),
            prePerimeterSensitivityOutput,
            stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras(),
            getOptimizationResult(optimizationResult, state, timestamp),
            new AppliedRemedialActions(),
            reportNode);
    }

    /** get optimization result for one timestamp */
    static OptimizationResult getOptimizationResult(OptimizationResult optimizationResult, State state, OffsetDateTime timestamp) {
        if (optimizationResult instanceof TimeCoupledLeaf timeCoupledLeaf) {
            RangeActionActivationResult activationForTimestamp = timeCoupledLeaf.getRangeActionActivationResults().getData(timestamp).orElseThrow();
            // the shared decision is declared at this timestamp's state only
            NetworkActionsResult networkActionsForState = new NetworkActionsResultImpl(Map.of(state, timeCoupledLeaf.getActivatedNetworkActions()));
            return new OptimizationResultImpl(timeCoupledLeaf, timeCoupledLeaf, timeCoupledLeaf, networkActionsForState, activationForTimestamp);
        }
        return optimizationResult;
    }

    private PrePerimeterSensitivityAnalysis getPreCurativePerimeterSensitivityAnalysis(Perimeter curativePerimeter, OffsetDateTime timestamp) {
        Crac crac = cracs.getData(timestamp).orElseThrow();
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(curativePerimeter.getRaOptimisationState());
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getRangeActions(curativePerimeter.getRaOptimisationState()));
        curativePerimeter.getAllStates().forEach(curativeState -> flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState)));
        return new PrePerimeterSensitivityAnalysis(crac, flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProviders.getData(timestamp).orElseThrow(), false);
    }

    private OptimizationResult optimizeCurativePerimeter(final TemporalData<Perimeter> curativePerimeters,
                                                         final TemporalData<Network> networks,
                                                         final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                                                         final Map<State, OptimizationResult> resultsPerPerimeter,
                                                         final Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter,
                                                         final ReportNode reportNode) {
        // flowCnecs, loopFlowCnecs, states, operators, filtered states per timestamps union
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        Set<FlowCnec> allLoopFlowCnecs = new HashSet<>();
        Set<State> allOptimisationStates = new HashSet<>();
        Set<String> allOperatorsNotSharingCras = new HashSet<>();
        // collect the optimization perimeters per timestamp
        Map<OffsetDateTime, OptimizationPerimeter> optPerimetersPerTs = new HashMap<>();
        Map<OffsetDateTime, FlowResult> initialFlowResultsPerTs = new HashMap<>();
        Map<OffsetDateTime, AppliedRemedialActions> preOptimAppliedRaPerTs = new HashMap<>();
        Map<OffsetDateTime, Instant> outageInstantsPerTs = new HashMap<>();
        Map<OffsetDateTime, ToolProvider> toolProvidersPerTs = new HashMap<>();

        AtomicBoolean anyHvdcAcEmulation = new AtomicBoolean(false);

        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
            State curativeState = curativePerimeter.getRaOptimisationState();
            Crac crac = cracs.getData(timestamp).orElseThrow();
            Network network = networks.getData(timestamp).orElseThrow();
            PrePerimeterResult prePerimeterSensitivityOutput = prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow();

            CastorReports.reportOptimizingCurativeState(reportNode, curativeState.getId());

            Set<State> filteredStates = curativePerimeter.getAllStates().stream()
                    .filter(state -> !prePerimeterSensitivityOutput.getSensitivityStatus(state).equals(ComputationStatus.FAILURE))
                    .collect(Collectors.toSet());

            Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream().filter(flowCnec -> filteredStates.contains(flowCnec.getState())).collect(Collectors.toSet());

            Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

            allFlowCnecs.addAll(flowCnecs);
            allLoopFlowCnecs.addAll(loopFlowCnecs);
            allOptimisationStates.addAll(curativePerimeter.getAllStates());
            allOperatorsNotSharingCras.addAll(stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras());

            OptimizationPerimeter optPerimeter = CurativeOptimizationPerimeter.buildForStates(curativeState, curativePerimeter.getAllStates(),
                    crac, network, raoParameters, prePerimeterSensitivityOutput, reportNode);
            optPerimetersPerTs.put(timestamp, optPerimeter);
            initialFlowResultsPerTs.put(timestamp, initialSensitivityOutputs.getData(timestamp).orElseThrow());
            preOptimAppliedRaPerTs.put(timestamp, new AppliedRemedialActions());
            outageInstantsPerTs.put(timestamp, crac.getOutageInstant());
            toolProvidersPerTs.put(timestamp, toolProviders.getData(timestamp).orElseThrow());

            if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
                anyHvdcAcEmulation.set(true);
            }
        });

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

        // evaluate the global objective
        Map<RangeAction<?>, Double> rangeActionSetpointMap = new HashMap<>();
        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
            PrePerimeterResult prePerimeterSensitivityOutput = prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow();
            cracs.getData(timestamp).orElseThrow().getRangeActions(curativePerimeter.getRaOptimisationState())
                    .forEach(ra -> rangeActionSetpointMap.put(ra, prePerimeterSensitivityOutput.getSetpoint(ra)));
        });
        RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(rangeActionSetpointMap);
        RangeActionActivationResult rangeActionsResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        RemedialActionActivationResult remedialActionActivationResult = new RemedialActionActivationResultImpl(rangeActionsResult, new NetworkActionsResultImpl(Map.of()));
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(globalPrePerimeterFlowResult, remedialActionActivationResult, reportNode);
        boolean secureAtEveryTimestamp = objectiveFunctionResult.getMostLimitingElements(1).stream().allMatch(cnec -> globalPrePerimeterFlowResult.getMargin(cnec, Unit.MEGAWATT) >= 0);
        if (isStopCriterionChecked(objectiveFunctionResult, curativeTreeParameters) && secureAtEveryTimestamp) {
            NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of());
            GlobalSensitivityResult globalPrePerimeterSensitivityResult = new GlobalSensitivityResult(prePerimeterSensitivityOutputs.map(SensitivityResult.class::cast));
            return new OptimizationResultImpl(objectiveFunctionResult, globalPrePerimeterFlowResult, globalPrePerimeterSensitivityResult, networkActionsResult, rangeActionsResult);
        }

        Crac referenceCrac = cracs.getDataPerTimestamp().values().iterator().next();
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

        TimeCoupledSearchTreeInput searchTreeInput = TimeCoupledSearchTreeInput.create()
            .withNetworks(networks)
            .withOptimizationPerimeters(new TemporalDataImpl<>(optPerimetersPerTs))
            .withInitialFlowResults(new TemporalDataImpl<>(initialFlowResultsPerTs))
            .withPrePerimeterResults(prePerimeterSensitivityOutputs)
            .withPreOptimizationAppliedNetworkActions(new TemporalDataImpl<>(preOptimAppliedRaPerTs))
            .withObjectiveFunction(objectiveFunction)
            .withToolProviders(new TemporalDataImpl<>(toolProvidersPerTs))
            .withOutageInstants(new TemporalDataImpl<>(outageInstantsPerTs))
            .build();

        OptimizationResult result = new TimeCoupledSearchTree(searchTreeInput, searchTreeParameters, false, reportNode).run().join();
        curativePerimeters.getDataPerTimestamp().values().forEach(perimeter ->
                CastorReports.reportCurativeStateOptimized(reportNode, perimeter.getRaOptimisationState().getId()));
        return result;
    }

    /**
     * merges the RaUsageLimits of every timestamp's crac, they become shared supposedly
     */
    static Map<Instant, RaUsageLimits> mergeRaUsageLimitsAcrossTimestamps(TemporalData<Crac> cracs) {
        Map<String, Instant> instantPerId = new HashMap<>();
        Map<String, RaUsageLimits> limitsPerInstantId = new HashMap<>();
        cracs.getTimestamps().forEach(timestamp ->
                cracs.getData(timestamp).orElseThrow().getRaUsageLimitsPerInstant().forEach((instant, raUsageLimits) -> {
                    instantPerId.putIfAbsent(instant.getId(), instant);
                    limitsPerInstantId.putIfAbsent(instant.getId(), raUsageLimits);
                }));
        return instantPerId.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, entry -> limitsPerInstantId.get(entry.getKey())));
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

    private static <T> TemporalData<T> filterTemporalData(TemporalData<T> source, Set<OffsetDateTime> keepers) {
        return new TemporalDataImpl<>(keepers.stream().collect(Collectors.toMap(Function.identity(), timestamp -> source.getData(timestamp).orElseThrow())));
    }

    private static <T> TemporalData<T> filterMapToTemporalData(Map<OffsetDateTime, T> source, Set<OffsetDateTime> keepers) {
        return new TemporalDataImpl<>(keepers.stream().collect(Collectors.toMap(Function.identity(), timestamp -> Objects.requireNonNull(source.get(timestamp), () -> "No data for timestamp " + timestamp))));
    }
}
