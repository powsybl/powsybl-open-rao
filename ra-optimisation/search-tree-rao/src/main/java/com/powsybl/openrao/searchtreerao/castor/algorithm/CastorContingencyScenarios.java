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
    private final TemporalData<Crac> cracs;
    private final TemporalData<ToolProvider> toolProviders;
    private final TemporalData<StateTree> stateTrees;
    private final TemporalData<PrePerimeterResult> initialSensitivityOutputs;

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
        this.cracs = null;
        this.toolProviders = null;
        this.stateTrees = null;
        this.initialSensitivityOutputs = null;
    }

    public CastorContingencyScenarios(TemporalData<Crac> cracs,
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
        this.crac = null;
        this.toolProvider = null;
        this.stateTree = null;
        this.initialSensitivityOutput = null;
    }

    public Map<State, PostPerimeterResult> optimizeContingencyScenarios(final Network network,
                                                                        final PrePerimeterResult prePerimeterSensitivityOutput,
                                                                        final boolean automatonsOnly,
                                                                        final ReportNode reportNode) {
        Map<State, PostPerimeterResult> contingencyScenarioResults = new ConcurrentHashMap<>();
        // Create a new variant
        String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);
        // Create an automaton simulator
        AutomatonSimulator automatonSimulator = new AutomatonSimulator(
            crac,
            raoParameters,
            toolProvider,
            initialSensitivityOutput,
            prePerimeterSensitivityOutput,
            stateTree.getOperatorsNotSharingCras(),
            NUMBER_LOGGED_ELEMENTS_DURING_RAO,
            reportNode
        );
        // Go through all contingency scenarios
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, newVariant, getAvailableCPUs(raoParameters), true)) {
            AtomicInteger remainingScenarios = new AtomicInteger(stateTree.getContingencyScenarios().size());
            List<ForkJoinTask<Object>> tasks = stateTree.getContingencyScenarios().stream().map(optimizedScenario -> {
                    final ReportNode scenarioOptimizationReportNode = CastorReports.reportOptimizingScenarioForContingency(reportNode, optimizedScenario.getContingency().getId());
                    return networkPool.submit(() ->
                        runScenario(prePerimeterSensitivityOutput,
                            automatonsOnly,
                            optimizedScenario,
                            networkPool,
                            automatonSimulator,
                            contingencyScenarioResults,
                            remainingScenarios,
                            scenarioOptimizationReportNode
                        )
                    );
                }
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

    private Object runScenario(final PrePerimeterResult prePerimeterSensitivityOutput,
                               final boolean automatonsOnly,
                               final ContingencyScenario optimizedScenario,
                               final AbstractNetworkPool networkPool,
                               final AutomatonSimulator automatonSimulator,
                               final Map<State, PostPerimeterResult> contingencyScenarioResults,
                               final AtomicInteger remainingScenarios,
                               final ReportNode reportNode) throws InterruptedException {
        Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
        CastorReports.reportOptimizingScenarioPostContingency(reportNode, optimizedScenario.getContingency().getId());

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
            PostPerimeterResult postAutoResult = getResultPostState(automatonState.get(), networkClone, prePerimeterSensitivityOutput, automatonResult, reportNode);
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
            && prePerimeterSensitivityOutput.getSensitivityStatus(optimizedScenario.getCurativePerimeters().getFirst().getRaOptimisationState()) == ComputationStatus.FAILURE
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
                    previousPerimeterResult = getPreCurativePerimeterSensitivityAnalysis(curativePerimeter)
                        .runBasedOnInitialResults(
                            networkClone,
                            null,
                            stateTree.getOperatorsNotSharingCras(),
                            null,
                            reportNode
                        );
                }
                prePerimeterResultPerPerimeter.put(curativePerimeter.getRaOptimisationState(), previousPerimeterResult);
                if (allPreviousPerimetersSucceded) {
                    OptimizationResult curativeResult = optimizeCurativePerimeter(
                        curativePerimeter,
                        networkClone,
                        previousPerimeterResult,
                        resultsPerPerimeter,
                        prePerimeterResultPerPerimeter,
                        reportNode
                    );
                    allPreviousPerimetersSucceded = curativeResult.getSensitivityStatus() == DEFAULT;
                    applyRemedialActions(networkClone, curativeResult, curativeState);
                    //recompute sensi and objective function considering curative + all instants following curative (useful if multi curative)
                    PostPerimeterResult postCurativeResult = getResultPostState(curativeState, networkClone, previousPerimeterResult, curativeResult, reportNode);
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
        boolean anyRangeActionApplied = !postPerimeterResult.optimizationResult().getActivatedRangeActions(state).isEmpty();
        boolean anyNetworkActionApplied = !postPerimeterResult.optimizationResult().getActivatedNetworkActions().isEmpty();
        return anyRangeActionApplied || anyNetworkActionApplied;

    }

    private PostPerimeterResult generateSkippedPostPerimeterResult(State state, double sensitivityFailureOvercost) {
        OptimizationResult skippedOptimizationResult = new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), ComputationStatus.FAILURE, sensitivityFailureOvercost);
        PrePerimeterResult prePerimeterResult = new PrePerimeterSensitivityResultImpl(skippedOptimizationResult, skippedOptimizationResult, null, skippedOptimizationResult);
        return new PostPerimeterResult(skippedOptimizationResult, prePerimeterResult);
    }

    private PostPerimeterResult getResultPostState(final State state,
                                                   final Network networkClone,
                                                   final PrePerimeterResult prePerimeterSensitivityOutput,
                                                   final OptimizationResult optimizationResult,
                                                   final ReportNode reportNode) {
        return getResultPostState(state, networkClone, prePerimeterSensitivityOutput, optimizationResult,
            crac, toolProvider, initialSensitivityOutput, stateTree, reportNode);
    }

    private PostPerimeterResult getResultPostState(final State state,
                                                   final Network networkClone,
                                                   final PrePerimeterResult prePerimeterSensitivityOutput,
                                                   final OptimizationResult optimizationResult,
                                                   final Crac crac,
                                                   final ToolProvider toolProvider,
                                                   final PrePerimeterResult initialSensitivityOutput,
                                                   final StateTree stateTree,
                                                   final ReportNode reportNode) {
        // if it's the last instant, no need to recompute things because the optimization result already contains all following states. (none)
        if (state.getInstant().equals(crac.getLastInstant())) {
            return getLastStateResult(state, optimizationResult);
        }
        Set<State> statesToConsider = new HashSet<>();
        statesToConsider.add(state);
        crac.getStates(state.getContingency().orElseThrow()).stream()
            .filter(s -> s.getInstant().comesAfter(state.getInstant()))
            .forEach(statesToConsider::add);
        PostPerimeterSensitivityAnalysis postPerimeterSensitivityAnalysis = new PostPerimeterSensitivityAnalysis(crac, statesToConsider, raoParameters, toolProvider, false);

        return postPerimeterSensitivityAnalysis.runBasedOnInitialPreviousAndOptimizationResults(
            networkClone,
            initialSensitivityOutput,
            prePerimeterSensitivityOutput,
            stateTree.getOperatorsNotSharingCras(),
            optimizationResult,
            new AppliedRemedialActions(),
            reportNode);
    }

    private PostPerimeterResult getLastStateResult(State state, OptimizationResult optimizationResult) {
        return new PostPerimeterResult(optimizationResult,
            new PrePerimeterSensitivityResultImpl(optimizationResult, optimizationResult,
                RangeActionSetpointResultImpl.buildFromActivationOfRangeActionAtState(optimizationResult, state), optimizationResult));
    }

    private PrePerimeterSensitivityAnalysis getPreCurativePerimeterSensitivityAnalysis(Perimeter curativePerimeter) {
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(curativePerimeter.getRaOptimisationState());
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getRangeActions(curativePerimeter.getRaOptimisationState()));
        for (State curativeState : curativePerimeter.getAllStates()) {
            flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
        }
        return new PrePerimeterSensitivityAnalysis(crac, flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProvider, false);
    }

    private OptimizationResult optimizeCurativePerimeter(final Perimeter curativePerimeter,
                                                         final Network network,
                                                         final PrePerimeterResult prePerimeterSensitivityOutput,
                                                         final Map<State, OptimizationResult> resultsPerPerimeter,
                                                         final Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter,
                                                         final ReportNode reportNode) {
        State curativeState = curativePerimeter.getRaOptimisationState();
        CastorReports.reportOptimizingCurativeState(reportNode, curativeState.getId());

        Set<FlowCnec> flowCnecs = getFlowCnecsOfNonFailedStates(crac, curativePerimeter, prePerimeterSensitivityOutput);

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);
        Map<RangeAction<?>, Double> rangeActionSetpointMap = crac.getRangeActions(curativeState)
            .stream()
            .collect(Collectors.toMap(rangeAction -> rangeAction, prePerimeterSensitivityOutput::getSetpoint));
        RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(rangeActionSetpointMap);
        RangeActionActivationResult rangeActionsResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        RemedialActionActivationResult remedialActionActivationResult = new RemedialActionActivationResultImpl(rangeActionsResult, new NetworkActionsResultImpl(Map.of()));

        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
            flowCnecs,
            loopFlowCnecs,
            initialSensitivityOutput,
            prePerimeterSensitivityOutput,
            stateTree.getOperatorsNotSharingCras(),
            raoParameters,
            curativePerimeter.getAllStates()
        );
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(prePerimeterSensitivityOutput, remedialActionActivationResult, reportNode);
        boolean stopCriterionReached = isStopCriterionChecked(objectiveFunctionResult, curativeTreeParameters);
        if (stopCriterionReached) {
            NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of());
            return new OptimizationResultImpl(objectiveFunctionResult, prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, networkActionsResult, rangeActionsResult);
        }

        OptimizationPerimeter optPerimeter = CurativeOptimizationPerimeter.buildForStates(
            curativeState,
            curativePerimeter.getAllStates(),
            crac,
            network,
            raoParameters,
            prePerimeterSensitivityOutput,
            reportNode
        );

        SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(curativeTreeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()));

        if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters =
                raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                    ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                    : new LoadFlowAndSensitivityParameters(reportNode);
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

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, false, reportNode).run().join();
        CastorReports.reportCurativeStateOptimized(reportNode, curativeState.getId());
        return result;
    }

    /** Optimizes every post-contingency scenario across all the timestamps at once. */
    public TemporalData<Map<State, PostPerimeterResult>> optimizeContingencyScenarios(final TemporalData<Network> networks,
                                                                                      final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                                                                                      final boolean automatonsOnly,
                                                                                      final ReportNode reportNode) {
        Map<OffsetDateTime, Map<State, PostPerimeterResult>> contingencyScenarioResults = networks.getTimestamps().stream()
            .collect(Collectors.toMap(Function.identity(), timestamp -> new ConcurrentHashMap<>()));
        Map<String, Map<OffsetDateTime, ContingencyScenario>> scenariosByContingencyId = new LinkedHashMap<>();
        Map<OffsetDateTime, AbstractNetworkPool> timestampNetworkPoolMap = new HashMap<>();
        Map<OffsetDateTime, AutomatonSimulator> timestampAutomatonSimulatorMap = new HashMap<>();
        networks.getDataPerTimestamp().forEach((timestamp, network) -> {
            String newVariant = RandomizedString.getRandomizedString("TimeCoupledContingencyScenario", network.getVariantManager().getVariantIds(), 10);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
            network.getVariantManager().setWorkingVariant(newVariant);
            timestampNetworkPoolMap.put(timestamp, AbstractNetworkPool.create(network, newVariant, getAvailableCPUs(raoParameters), true));
            timestampAutomatonSimulatorMap.put(timestamp, createAutomatonSimulator(timestamp, prePerimeterSensitivityOutputs, reportNode));
            stateTrees.getData(timestamp).orElseThrow().getContingencyScenarios().forEach(
                scenario -> scenariosByContingencyId.computeIfAbsent(scenario.getContingency().getId(), id -> new HashMap<>()).put(timestamp, scenario));
        });
        TemporalData<AutomatonSimulator> automatonSimulators = new TemporalDataImpl<>(timestampAutomatonSimulatorMap);
        TemporalData<AbstractNetworkPool> networkPools = new TemporalDataImpl<>(timestampNetworkPoolMap);
        AbstractNetworkPool firstNetworkPool = networkPools.getData(networkPools.getTimestamps().getFirst()).orElseThrow();
        try {
            AtomicInteger remainingScenarios = new AtomicInteger(scenariosByContingencyId.size());
            List<ForkJoinTask<Object>> tasks = scenariosByContingencyId.entrySet().stream().map(entry -> {
                TemporalData<ContingencyScenario> optimizedScenarios = new TemporalDataImpl<>(entry.getValue());
                ReportNode scenarioReportNode = CastorReports.reportOptimizingScenarioForContingency(reportNode, entry.getKey());
                return firstNetworkPool.submit(() -> runScenario(prePerimeterSensitivityOutputs, automatonsOnly, optimizedScenarios,
                    networkPools, automatonSimulators, contingencyScenarioResults, remainingScenarios, scenarioReportNode));
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

    private AutomatonSimulator createAutomatonSimulator(OffsetDateTime timestamp,
                                                        TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                                                        ReportNode reportNode) {
        return new AutomatonSimulator(cracs.getData(timestamp).orElseThrow(), raoParameters,
            toolProviders.getData(timestamp).orElseThrow(), initialSensitivityOutputs.getData(timestamp).orElseThrow(),
            prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow(), stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras(),
            NUMBER_LOGGED_ELEMENTS_DURING_RAO, reportNode);
    }

    private Object runScenario(final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                               final boolean automatonsOnly,
                               final TemporalData<ContingencyScenario> optimizedScenarios,
                               final TemporalData<AbstractNetworkPool> networkPools,
                               final TemporalData<AutomatonSimulator> automatonSimulators,
                               final Map<OffsetDateTime, Map<State, PostPerimeterResult>> contingencyScenarioResultsPerTimestamp,
                               final AtomicInteger remainingScenarios,
                               final ReportNode reportNode) throws InterruptedException {
        String contingencyId = optimizedScenarios.getDataPerTimestamp().values().iterator().next().getContingency().getId();
        Map<OffsetDateTime, Network> timestampNetworkCloneMap = new HashMap<>();
        try {
            for (OffsetDateTime timestamp : optimizedScenarios.getTimestamps()) {
                timestampNetworkCloneMap.put(timestamp, networkPools.getData(timestamp).orElseThrow().getAvailableNetwork());
            }
            TemporalData<Network> networkClones = new TemporalDataImpl<>(timestampNetworkCloneMap);
            CastorReports.reportOptimizingScenarioPostContingency(reportNode, contingencyId);
            TemporalData<Set<State>> curativeStates = optimizedScenarios.map(
                scenario -> scenario.getCurativePerimeters().stream().flatMap(perimeter -> perimeter.getAllStates().stream()).collect(Collectors.toSet()));
            Map<OffsetDateTime, PrePerimeterResult> preCurativeResultPerTimestamp = new HashMap<>();
            optimizedScenarios.getTimestamps().forEach(timestamp -> preCurativeResultPerTimestamp.put(timestamp, prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow()));
            Set<OffsetDateTime> autoStateSensiFailed = new HashSet<>();
            optimizedScenarios.getDataPerTimestamp().forEach((timestamp, optimizedScenario) -> {
                Optional<State> automatonState = optimizedScenario.getAutomatonState();
                if (automatonState.isPresent()) {
                    Network networkClone = networkClones.getData(timestamp).orElseThrow();
                    AutomatonPerimeterResultImpl automatonResult = automatonSimulators.getData(timestamp).orElseThrow()
                        .simulateAutomatonState(automatonState.get(), curativeStates.getData(timestamp).orElseThrow(), networkClone);
                    PostPerimeterResult postAutoResult = getResultPostState(automatonState.get(), networkClone,
                        prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow(), automatonResult, timestamp, null, reportNode);
                    contingencyScenarioResultsPerTimestamp.get(timestamp).put(automatonState.get(), postAutoResult);
                    if (automatonResult.getComputationStatus() == ComputationStatus.FAILURE) {
                        autoStateSensiFailed.add(timestamp);
                    } else {
                        preCurativeResultPerTimestamp.put(timestamp, automatonResult.getPostAutomatonSensitivityAnalysisOutput());
                    }
                }
            });
            boolean anyTimestampSensiFailed = optimizedScenarios.getDataPerTimestamp().entrySet().stream().anyMatch(entry -> {
                OffsetDateTime timestamp = entry.getKey();
                ContingencyScenario optimizedScenario = entry.getValue();
                Optional<State> automatonState = optimizedScenario.getAutomatonState();
                return !automatonsOnly && automatonState.isEmpty() && !optimizedScenario.getCurativePerimeters().isEmpty()
                    && prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow()
                        .getSensitivityStatus(optimizedScenario.getCurativePerimeters().getFirst().getRaOptimisationState()) == ComputationStatus.FAILURE
                    || automatonState.isPresent() && autoStateSensiFailed.contains(timestamp);
            });
            if (anyTimestampSensiFailed) {
                double sensitivityFailureOvercost = getSensitivityFailureOvercost(raoParameters);
                curativeStates.getDataPerTimestamp().forEach((timestamp, states) -> states.forEach(
                    state -> contingencyScenarioResultsPerTimestamp.get(timestamp).put(state, generateSkippedPostPerimeterResult(state, sensitivityFailureOvercost))));
            } else if (!automatonsOnly && optimizedScenarios.getDataPerTimestamp().values().stream().anyMatch(scenario -> !scenario.getCurativePerimeters().isEmpty())) {
                // Coupled optimization supports one curative perimeter per timestamp.
                TemporalData<Perimeter> curativePerimeters = optimizedScenarios.map(scenario -> scenario.getCurativePerimeters().getFirst());
                Map<OffsetDateTime, PrePerimeterResult> previousPerimeterResults = new HashMap<>();
                Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter = new HashMap<>();
                curativePerimeters.getDataPerTimestamp().forEach((timestamp, perimeter) -> {
                    PrePerimeterResult previousPerimeterResult = preCurativeResultPerTimestamp.get(timestamp);
                    previousPerimeterResults.put(timestamp, previousPerimeterResult);
                    prePerimeterResultPerPerimeter.put(perimeter.getRaOptimisationState(), previousPerimeterResult);
                });
                CurativeOptimizationResult curativeOptimizationResult = optimizeCurativePerimeter(curativePerimeters, networkClones,
                    new TemporalDataImpl<>(previousPerimeterResults), Map.of(), prePerimeterResultPerPerimeter, reportNode);
                OptimizationResult curativeResult = curativeOptimizationResult.optimizationResult();
                TemporalData<ObjectiveFunction> curativeObjectiveFunctionsPerTimestamp = curativeOptimizationResult.objectiveFunctions();
                curativePerimeters.getDataPerTimestamp().forEach((timestamp, perimeter) -> {
                    State curativeState = perimeter.getRaOptimisationState();
                    Network networkClone = networkClones.getData(timestamp).orElseThrow();
                    applyRemedialActions(networkClone, curativeResult, curativeState);
                    PostPerimeterResult postCurativeResult = getResultPostState(curativeState, networkClone, previousPerimeterResults.get(timestamp),
                        curativeResult, timestamp, curativeObjectiveFunctionsPerTimestamp.getData(timestamp).orElseThrow(), reportNode);
                    contingencyScenarioResultsPerTimestamp.get(timestamp).put(curativeState, postCurativeResult);
                });
            }
        } finally {
            TECHNICAL_LOGS.debug("Remaining post-contingency scenarios to optimize: {}", remainingScenarios.decrementAndGet());
            for (Map.Entry<OffsetDateTime, Network> entry : timestampNetworkCloneMap.entrySet()) {
                boolean actionWasApplied = contingencyScenarioResultsPerTimestamp.get(entry.getKey()).entrySet().stream()
                    .filter(stateAndResult -> stateAndResult.getKey().getContingency().orElseThrow().getId().equals(contingencyId))
                    .anyMatch(this::isAnyActionApplied);
                networkPools.getData(entry.getKey()).orElseThrow().releaseUsedNetwork(entry.getValue(), actionWasApplied);
            }
        }
        return null;
    }

    private PostPerimeterResult getResultPostState(final State state, final Network networkClone,
                                                   final PrePerimeterResult prePerimeterSensitivityOutput,
                                                   final OptimizationResult optimizationResult, final OffsetDateTime timestamp,
                                                   final ObjectiveFunction objectiveFunction, final ReportNode reportNode) {
        Crac timestampCrac = cracs.getData(timestamp).orElseThrow();
        if (state.getInstant().equals(timestampCrac.getLastInstant())) {
            OptimizationResult optimizationResultForTimestamp = getOptimizationResult(optimizationResult, state, timestamp, objectiveFunction, reportNode);
            return getLastStateResult(state, optimizationResultForTimestamp);
        }
        OptimizationResult optimizationResultForTimestamp = getOptimizationResult(optimizationResult, state, timestamp, null, reportNode);
        return getResultPostState(state, networkClone, prePerimeterSensitivityOutput, optimizationResultForTimestamp,
            timestampCrac, toolProviders.getData(timestamp).orElseThrow(), initialSensitivityOutputs.getData(timestamp).orElseThrow(),
            stateTrees.getData(timestamp).orElseThrow(), reportNode);
    }

    OptimizationResult getOptimizationResult(OptimizationResult optimizationResult, State state, OffsetDateTime timestamp,
                                             ObjectiveFunction perTimestampObjectiveFunction, ReportNode reportNode) {
        if (optimizationResult instanceof Leaf leaf) {
            RangeActionActivationResult activationForTimestamp = leaf.getAllRangeActionActivationResults().getData(timestamp).orElseThrow();
            NetworkActionsResult networkActionsForState = new NetworkActionsResultImpl(Map.of(state, getSameNetworkActionsOfTimestamp(leaf.getActivatedNetworkActions(), timestamp)));
            ObjectiveFunctionResult objectiveFunctionResultForTimestamp = leaf;
            if (perTimestampObjectiveFunction != null) {
                RemedialActionActivationResult activation = new RemedialActionActivationResultImpl(activationForTimestamp, networkActionsForState);
                objectiveFunctionResultForTimestamp = perTimestampObjectiveFunction.evaluate(leaf, activation, reportNode);
            }
            return new OptimizationResultImpl(objectiveFunctionResultForTimestamp, leaf, leaf, networkActionsForState, activationForTimestamp);
        }
        return optimizationResult;
    }

    private CurativeOptimizationResult optimizeCurativePerimeter(final TemporalData<Perimeter> curativePerimeters,
                                                                 final TemporalData<Network> networks,
                                                                 final TemporalData<PrePerimeterResult> prePerimeterSensitivityOutputs,
                                                                 final Map<State, OptimizationResult> resultsPerPerimeter,
                                                                 final Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter,
                                                                 final ReportNode reportNode) {
        Set<FlowCnec> allFlowCnecs = new HashSet<>();
        Set<FlowCnec> allLoopFlowCnecs = new HashSet<>();
        Set<State> allOptimizationStates = new HashSet<>();
        Set<String> allOperatorsNotSharingCras = new HashSet<>();
        Map<OffsetDateTime, OptimizationPerimeter> timestampOptimizationPerimeterMap = new HashMap<>();
        Map<OffsetDateTime, FlowResult> timestampInitialFlowResultMap = new HashMap<>();
        Map<OffsetDateTime, AppliedRemedialActions> timestampPreOptimAppliedRaMap = new HashMap<>();
        Map<OffsetDateTime, Instant> timestampOutageInstantsMap = new HashMap<>();
        Map<OffsetDateTime, ToolProvider> timestampToolProvidersMap = new HashMap<>();
        AtomicBoolean anyHvdcAcEmulation = new AtomicBoolean(false);
        Map<OffsetDateTime, ObjectiveFunction> timestampObjectiveFunctionMap = new HashMap<>();
        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
            Crac timestampCrac = cracs.getData(timestamp).orElseThrow();
            PrePerimeterResult prePerimeterSensitivityOutput = prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow();
            Set<FlowCnec> flowCnecs = getFlowCnecsOfNonFailedStates(timestampCrac, curativePerimeter, prePerimeterSensitivityOutput);
            Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, networks.getData(timestamp).orElseThrow());
            Set<String> operatorsNotSharingCras = stateTrees.getData(timestamp).orElseThrow().getOperatorsNotSharingCras();
            allFlowCnecs.addAll(flowCnecs);
            allLoopFlowCnecs.addAll(loopFlowCnecs);
            allOptimizationStates.addAll(curativePerimeter.getAllStates());
            allOperatorsNotSharingCras.addAll(operatorsNotSharingCras);
            timestampObjectiveFunctionMap.put(timestamp, ObjectiveFunction.build(flowCnecs, loopFlowCnecs,
                initialSensitivityOutputs.getData(timestamp).orElseThrow(), prePerimeterSensitivityOutput,
                operatorsNotSharingCras, raoParameters, curativePerimeter.getAllStates()));
        });
        curativePerimeters.getDataPerTimestamp().forEach((timestamp, curativePerimeter) -> {
            State curativeState = curativePerimeter.getRaOptimisationState();
            Crac timestampCrac = cracs.getData(timestamp).orElseThrow();
            Network network = networks.getData(timestamp).orElseThrow();
            CastorReports.reportOptimizingCurativeState(reportNode, curativeState.getId());
            timestampOptimizationPerimeterMap.put(timestamp, CurativeOptimizationPerimeter.buildForStates(curativeState, curativePerimeter.getAllStates(),
                timestampCrac, network, raoParameters, prePerimeterSensitivityOutputs.getData(timestamp).orElseThrow(), reportNode));
            timestampInitialFlowResultMap.put(timestamp, initialSensitivityOutputs.getData(timestamp).orElseThrow());
            timestampPreOptimAppliedRaMap.put(timestamp, new AppliedRemedialActions());
            timestampOutageInstantsMap.put(timestamp, timestampCrac.getOutageInstant());
            timestampToolProvidersMap.put(timestamp, toolProviders.getData(timestamp).orElseThrow());
            if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(timestampCrac.getHvdcRangeActions(), network).isEmpty()) {
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
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(allFlowCnecs, allLoopFlowCnecs, new GlobalFlowResult(initialSensitivityOutputs),
            globalPrePerimeterFlowResult, allOperatorsNotSharingCras, raoParameters, allOptimizationStates);
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(globalPrePerimeterFlowResult, remedialActionActivationResult, reportNode);
        boolean isStopCriterionReached = isStopCriterionChecked(objectiveFunctionResult, curativeTreeParameters);
        boolean secureAtEveryTimestamp = objectiveFunctionResult.getMostLimitingElements(1).stream()
            .allMatch(cnec -> globalPrePerimeterFlowResult.getMargin(cnec, Unit.MEGAWATT) >= 0);
        if (isStopCriterionReached && secureAtEveryTimestamp) {
            NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of());
            GlobalSensitivityResult globalPrePerimeterSensitivityResult = new GlobalSensitivityResult(prePerimeterSensitivityOutputs.map(SensitivityResult.class::cast));
            OptimizationResult noActionResult = new OptimizationResultImpl(objectiveFunctionResult, globalPrePerimeterFlowResult,
                globalPrePerimeterSensitivityResult, networkActionsResult, rangeActionsResult);
            return new CurativeOptimizationResult(noActionResult, new TemporalDataImpl<>(timestampObjectiveFunctionMap));
        }
        Crac referenceCrac = cracs.getData(cracs.getTimestamps().getFirst()).orElseThrow();
        SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, referenceCrac)
            .withTreeParameters(curativeTreeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), allOperatorsNotSharingCras))
            .withGlobalRemedialActionLimitationParameters(mergeRaUsageLimits(cracs));
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

    public static Map<Instant, RaUsageLimits> mergeRaUsageLimits(TemporalData<Crac> cracs) {
        Map<Instant, RaUsageLimits> instantRaUsageLimitsMap = new HashMap<>();
        cracs.getDataPerTimestamp().values().forEach(crac -> instantRaUsageLimitsMap.putAll(crac.getRaUsageLimitsPerInstant()));
        return instantRaUsageLimitsMap;
    }

    Set<NetworkAction> getSameNetworkActionsOfTimestamp(Set<NetworkAction> appliedNetworkActions, OffsetDateTime timestamp) {
        Crac timestampCrac = cracs.getData(timestamp).orElseThrow();
        return appliedNetworkActions.stream().map(networkAction -> timestampCrac.getNetworkAction(networkAction.getId()))
            .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<FlowCnec> getFlowCnecsOfNonFailedStates(Crac crac, Perimeter curativePerimeter, PrePerimeterResult prePerimeterSensitivityOutput) {
        Set<State> nonFailedStates = curativePerimeter.getAllStates().stream()
            .filter(state -> !prePerimeterSensitivityOutput.getSensitivityStatus(state).equals(ComputationStatus.FAILURE)).collect(Collectors.toSet());
        return crac.getFlowCnecs().stream().filter(flowCnec -> nonFailedStates.contains(flowCnec.getState())).collect(Collectors.toSet());
    }

    private record CurativeOptimizationResult(OptimizationResult optimizationResult, TemporalData<ObjectiveFunction> objectiveFunctions) {
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
