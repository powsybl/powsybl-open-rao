/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.powsybl.commons.report.ReportNode;
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
import com.powsybl.openrao.raoimpl.algorithms.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.raoimpl.algorithms.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.raoimpl.algorithms.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.raoimpl.commons.ToolProvider;
import com.powsybl.openrao.raoimpl.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.AbstractOptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.parameters.TreeParameters;
import com.powsybl.openrao.raoimpl.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.raoimpl.networkpool.AbstractNetworkPool;
import com.powsybl.openrao.raoimpl.reports.CastorReports;
import com.powsybl.openrao.raoimpl.result.api.NetworkActionsResult;
import com.powsybl.openrao.raoimpl.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.raoimpl.result.api.OptimizationResult;
import com.powsybl.openrao.raoimpl.result.api.PrePerimeterResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionActivationResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.raoimpl.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.raoimpl.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.PostPerimeterResult;
import com.powsybl.openrao.raoimpl.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.SkippedOptimizationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost;
import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;
import static com.powsybl.openrao.raoimpl.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;
import static com.powsybl.openrao.raoimpl.commons.RaoUtil.applyRemedialActions;

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

    public Map<State, PostPerimeterResult> optimizeContingencyScenarios(final Network network,
                                                                        final PrePerimeterResult prePerimeterSensitivityOutput,
                                                                        final boolean automatonsOnly,
                                                                        final ReportNode reportNode) {
        Map<State, PostPerimeterResult> contingencyScenarioResults = new ConcurrentHashMap<>();
        // Create a new variant
        String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);
        // Go through all contingency scenarios
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, newVariant, getAvailableCPUs(raoParameters), true)) {
            AtomicInteger remainingScenarios = new AtomicInteger(stateTree.getContingencyScenarios().size());
            List<ForkJoinTask<Object>> tasks = stateTree.getContingencyScenarios().stream().map(optimizedScenario -> {
                    final ReportNode scenarioOptimizationReportNode = CastorReports.reportOptimizingScenarioForContingency(reportNode, optimizedScenario.getContingency().getId());
                    AutomatonSimulator automatonSimulator = createAutomatonSimulator(prePerimeterSensitivityOutput, scenarioOptimizationReportNode);
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

    private AutomatonSimulator createAutomatonSimulator(PrePerimeterResult prePerimeterSensitivityOutput, ReportNode reportNode) {
        return new AutomatonSimulator(
            crac,
            raoParameters,
            toolProvider,
            initialSensitivityOutput,
            prePerimeterSensitivityOutput,
            stateTree.getOperatorsNotSharingCras(),
            NUMBER_LOGGED_ELEMENTS_DURING_RAO,
            reportNode
        );
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
        Set<State> scenarioStates = new HashSet<>(curativeStates);
        automatonState.ifPresent(scenarioStates::add);
        PrePerimeterResult preCurativeResult = prePerimeterSensitivityOutput;
        double sensitivityFailureOvercost = getSensitivityFailureOvercost(raoParameters);

        // If the contingency diverged during the initial sensitivity computation, keep the divergence:
        // no automatic action is simulated or curative action applied, and the scenario states are reported as failed.
        // The reasons for a load-flow divergence may be unrelated to RAO (e.g. a badly modeled voltage control,
        // or a weird starting point), and applying RAs on the case would hide the modeling problem (issue #1893).
        if (hasFailedInitialSensitivity(initialSensitivityOutput, scenarioStates)) {
            scenarioStates.forEach(state -> contingencyScenarioResults.put(state, generateSkippedPostPerimeterResult(state, sensitivityFailureOvercost)));
            TECHNICAL_LOGS.debug("Remaining post-contingency scenarios to optimize: {}", remainingScenarios.decrementAndGet());
            networkPool.releaseUsedNetwork(networkClone, false);
            return null;
        }

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

    /**
     * Returns true if the initial sensitivity computation diverged on at least one state of the scenario.
     */
    static boolean hasFailedInitialSensitivity(PrePerimeterResult initialSensitivityOutput, Set<State> scenarioStates) {
        return scenarioStates.stream()
            .anyMatch(state -> initialSensitivityOutput.getSensitivityStatus(state) == ComputationStatus.FAILURE);
    }

    private PostPerimeterResult generateSkippedPostPerimeterResult(State state, double sensitivityFailureOvercost) {
        OptimizationResult skippedOptimizationResult = new SkippedOptimizationResultImpl(state, sensitivityFailureOvercost);
        PrePerimeterResult prePerimeterResult = new PrePerimeterSensitivityResultImpl(skippedOptimizationResult, skippedOptimizationResult, null, skippedOptimizationResult);
        return new PostPerimeterResult(skippedOptimizationResult, prePerimeterResult);
    }

    private PostPerimeterResult getResultPostState(final State state,
                                                   final Network networkClone,
                                                   final PrePerimeterResult prePerimeterSensitivityOutput,
                                                   final OptimizationResult optimizationResult,
                                                   final ReportNode reportNode) {
        // if it's the last instant, no need to recompute things because the optimization result already contains all following states. (none)
        if (state.getInstant().equals(crac.getLastInstant())) {
            return new PostPerimeterResult(optimizationResult,
                new PrePerimeterSensitivityResultImpl(
                    optimizationResult,
                    optimizationResult,
                    RangeActionSetpointResultImpl.buildFromActivationOfRangeActionAtState(optimizationResult, state),
                    optimizationResult
                )
            );
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
}
