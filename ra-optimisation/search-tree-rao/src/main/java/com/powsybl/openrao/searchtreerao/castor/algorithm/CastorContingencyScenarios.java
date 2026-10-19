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
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.AbstractOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.networkpool.AbstractNetworkPool;
import com.powsybl.openrao.searchtreerao.reports.CastorReports;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.SkippedOptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
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

    private final Crac crac;
    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;
    private final StateTree stateTree;
    private final TreeParameters curativeTreeParameters;
    private final PrePerimeterResult initialSensitivityOutput;

    private ToolProvider workingToolProvider;
    private RaoParameters workingRaoParameters;
    private PrePerimeterResult workingInitialSensitivityOutput;

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
        this.workingToolProvider = toolProvider;
        this.workingRaoParameters = raoParameters;
    }

    public void setWorkingInitialSensitivityOutput(PrePerimeterResult workingInitialSensitivityOutput) {
        this.workingInitialSensitivityOutput = workingInitialSensitivityOutput;
    }

    public void setWorkingToolProvider(ToolProvider workingToolProvider) {
        this.workingToolProvider = workingToolProvider;
        if (workingToolProvider != null) {
            this.workingRaoParameters = workingToolProvider.getRaoParameters();
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

        Map<RangeAction<?>, Double> rangeActionSetpointMap = crac.getRangeActions(curativeState)
            .stream()
            .collect(Collectors.toMap(rangeAction -> rangeAction, prePerimeterSensitivityOutput::getSetpoint));
        RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(rangeActionSetpointMap);
        RangeActionActivationResult rangeActionsResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        RemedialActionActivationResult remedialActionActivationResult = new RemedialActionActivationResultImpl(rangeActionsResult, new NetworkActionsResultImpl(Map.of()));

        // the stop-criterion pre-check must be evaluated against base parameters: if it finds nothing to
        // optimize, its result is reported directly below with no recompute to follow (unlike the working search
        // below, which does get base-verified) - evaluating it with the workingRaoParameters would
        // silently report/compare margins in MW instead of A whenever optimize-in-dc is active.
        ObjectiveFunction acObjectiveFunction = ObjectiveFunction.build(
            flowCnecs,
            AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network),
            initialSensitivityOutput,
            prePerimeterSensitivityOutput,
            stateTree.getOperatorsNotSharingCras(),
            raoParameters,
            curativePerimeter.getAllStates()
        );
        ObjectiveFunctionResult objectiveFunctionResult = acObjectiveFunction.evaluate(prePerimeterSensitivityOutput, remedialActionActivationResult, reportNode);
        boolean stopCriterionReached = CastorSearchTreeHelper.isStopCriterionChecked(objectiveFunctionResult, curativeTreeParameters);
        if (stopCriterionReached) {
            NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Map.of());
            return new OptimizationResultImpl(objectiveFunctionResult, prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, networkActionsResult, rangeActionsResult);
        }

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, workingRaoParameters, network);
        ObjectiveFunction objectiveFunction = workingRaoParameters == raoParameters ? acObjectiveFunction : ObjectiveFunction.build(
            flowCnecs,
            loopFlowCnecs,
            initialSensitivityOutput,
            prePerimeterSensitivityOutput,
            stateTree.getOperatorsNotSharingCras(),
            workingRaoParameters,
            curativePerimeter.getAllStates()
        );

        OptimizationPerimeter optPerimeter = CurativeOptimizationPerimeter.buildForStates(
            curativeState,
            curativePerimeter.getAllStates(),
            crac,
            network,
            workingRaoParameters,
            prePerimeterSensitivityOutput,
            reportNode
        );

        SearchTreeParameters searchTreeParameters = buildSearchTreeParameters(workingRaoParameters, curativeTreeParameters, network, resultsPerPerimeter, prePerimeterResultPerPerimeter, reportNode);

        OptimizationResult result;
        if (workingToolProvider != toolProvider) {
            result = runDcSearch(curativePerimeter, network, prePerimeterSensitivityOutput, resultsPerPerimeter, prePerimeterResultPerPerimeter, reportNode,
                objectiveFunction, curativeState, flowCnecs, loopFlowCnecs, optPerimeter, searchTreeParameters);
        } else {
            result = CastorSearchTreeHelper.searchNetworkActionCombination(network, optPerimeter, objectiveFunction, initialSensitivityOutput, prePerimeterSensitivityOutput,
                new AppliedRemedialActions(), toolProvider, crac.getOutageInstant(), searchTreeParameters, false, reportNode);
        }

        CastorReports.reportCurativeStateOptimized(reportNode, curativeState.getId());
        return result;
    }

    OptimizationResult runDcSearch(final Perimeter curativePerimeter,
                                   final Network network,
                                   final PrePerimeterResult prePerimeterSensitivityOutput,
                                   final Map<State, OptimizationResult> resultsPerPerimeter,
                                   final Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter,
                                   final ReportNode reportNode,
                                   ObjectiveFunction objectiveFunction,
                                   State curativeState,
                                   Set<FlowCnec> flowCnecs,
                                   Set<FlowCnec> loopFlowCnecs,
                                   OptimizationPerimeter optPerimeter,
                                   SearchTreeParameters searchTreeParameters) {
        String newVariant = RandomizedString.getRandomizedString(CONTINGENCY_SCENARIO, network.getVariantManager().getVariantIds(), 10);
        String prevVariant = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().cloneVariant(prevVariant, newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);

        PrePerimeterResult searchInitialOutput = initialSensitivityOutput;
        PrePerimeterResult searchPrePerimeterOutput = prePerimeterSensitivityOutput;
        ObjectiveFunction searchObjectiveFunction = objectiveFunction;
        if (workingInitialSensitivityOutput != null) {
            Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(curativeState);
            curativePerimeter.getAllStates().forEach(s -> flowCnecsInSensi.addAll(crac.getFlowCnecs(s)));
            PrePerimeterResult dcPrePerimeterOutput = new PrePerimeterSensitivityAnalysis(crac, flowCnecsInSensi,
                    new HashSet<>(crac.getRangeActions(curativeState)), workingRaoParameters, workingToolProvider, true)
                    .runBasedOnInitialResults(network, workingInitialSensitivityOutput, stateTree.getOperatorsNotSharingCras(), null, reportNode);
            if (dcPrePerimeterOutput.getSensitivityStatus() != ComputationStatus.FAILURE) {
                searchInitialOutput = workingInitialSensitivityOutput;
                searchPrePerimeterOutput = dcPrePerimeterOutput;
                searchObjectiveFunction = ObjectiveFunction.build(flowCnecs, loopFlowCnecs, workingInitialSensitivityOutput,
                        dcPrePerimeterOutput, stateTree.getOperatorsNotSharingCras(), workingToolProvider.getRaoParameters(), curativePerimeter.getAllStates());
            }
        }

        OptimizationResult decidedResult = CastorSearchTreeHelper.searchNetworkActionCombination(network, optPerimeter, searchObjectiveFunction, searchInitialOutput, searchPrePerimeterOutput,
            new AppliedRemedialActions(), workingToolProvider, crac.getOutageInstant(), searchTreeParameters, false, reportNode);
        OptimizationResult result;
        // the search above ran on DC sensitivities for performance; recompute the decided RA combination in AC before reporting it
        network.getVariantManager().setWorkingVariant(prevVariant);

        // rebuild the optimization perimeter from AC parameters: remedial-action usability depends on the flow unit
        // (MW in DC, A in AC), so the DC-decided perimeter above cannot be reused for the AC recompute
        OptimizationPerimeter acOptPerimeter = CurativeOptimizationPerimeter.buildForStates(
            curativeState, curativePerimeter.getAllStates(), crac, network, toolProvider.getRaoParameters(), prePerimeterSensitivityOutput, reportNode);
        ObjectiveFunction baseObjectiveFunction = ObjectiveFunction.build(flowCnecs, loopFlowCnecs, initialSensitivityOutput,
                prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), toolProvider.getRaoParameters(),
                curativePerimeter.getAllStates());
        result = CastorSearchTreeHelper.reoptimizeNetworkActionCombination(network, acOptPerimeter, baseObjectiveFunction, initialSensitivityOutput, prePerimeterSensitivityOutput,
            new AppliedRemedialActions(), toolProvider, crac.getOutageInstant(), searchTreeParameters, decidedResult, false, reportNode);

        if (!CastorSearchTreeHelper.isStopCriterionChecked(result, curativeTreeParameters)) {
            // the DC search stopped believing this combination made the perimeter secure, but the AC recompute shows it isn't:
            // exhaust the DC search (ignore the stop criterion) to look for a combination that is actually secure in AC
            CastorReports.reportCurativeStateDcSecureButAcMarginViolation(reportNode, curativeState.getId(), decidedResult.getActivatedNetworkActions().size());
            TreeParameters exhaustiveTreeParameters = new TreeParameters(TreeParameters.StopCriterion.MIN_OBJECTIVE, 0.0,
                    curativeTreeParameters.maximumSearchDepth(), curativeTreeParameters.leavesInParallel(), curativeTreeParameters.raRangeShrinking());
            SearchTreeParameters exhaustiveSearchTreeParameters = buildSearchTreeParameters(workingToolProvider.getRaoParameters(), exhaustiveTreeParameters, network,
                resultsPerPerimeter, prePerimeterResultPerPerimeter, reportNode);

            network.getVariantManager().setWorkingVariant(newVariant);
            OptimizationResult exhaustedDecidedResult = CastorSearchTreeHelper.searchNetworkActionCombination(network, optPerimeter, searchObjectiveFunction,
                searchInitialOutput, searchPrePerimeterOutput, new AppliedRemedialActions(), workingToolProvider, crac.getOutageInstant(), exhaustiveSearchTreeParameters, false, reportNode);
            network.getVariantManager().setWorkingVariant(prevVariant);

            result = CastorSearchTreeHelper.reoptimizeNetworkActionCombination(network, acOptPerimeter, baseObjectiveFunction, initialSensitivityOutput, prePerimeterSensitivityOutput,
                new AppliedRemedialActions(), toolProvider, crac.getOutageInstant(), exhaustiveSearchTreeParameters, exhaustedDecidedResult, false, reportNode);

            if (!CastorSearchTreeHelper.isStopCriterionChecked(result, curativeTreeParameters)) {
                // DC's own greedy search rejected the network action combination that AC would actually need (DC judged it
                // as not improving, AC does not): fall back to a genuine AC search for this perimeter as a last resort.
                // This search needs its own AC-flavored parameters instead of reusing the DC-flavored exhaustiveSearchTreeParameters.
                CastorReports.reportCurativeStateExhaustiveDcSearchStillInsecureInAc(reportNode, curativeState.getId());
                SearchTreeParameters acExhaustiveSearchTreeParameters = buildSearchTreeParameters(toolProvider.getRaoParameters(), exhaustiveTreeParameters, network,
                    resultsPerPerimeter, prePerimeterResultPerPerimeter, reportNode);

                result = CastorSearchTreeHelper.searchNetworkActionCombination(network, acOptPerimeter, baseObjectiveFunction, initialSensitivityOutput, prePerimeterSensitivityOutput,
                    new AppliedRemedialActions(), toolProvider, crac.getOutageInstant(), acExhaustiveSearchTreeParameters, false, reportNode);
                if (!CastorSearchTreeHelper.isStopCriterionChecked(result, curativeTreeParameters)) {
                    CastorReports.reportCurativeStateNoSecureCombinationFoundInAc(reportNode, curativeState.getId(), result.getCost());
                }
            }

            network.getVariantManager().removeVariant(newVariant);
        }
        return result;
    }

    private SearchTreeParameters buildSearchTreeParameters(final RaoParameters currentRaoParameters,
                                                           final TreeParameters treeParameters,
                                                           final Network network,
                                                           final Map<State, OptimizationResult> resultsPerPerimeter,
                                                           final Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter,
                                                           final ReportNode reportNode) {
        SearchTreeParameters searchTreeParameters = CastorSearchTreeHelper.buildSearchTreeParameters(currentRaoParameters, treeParameters, crac, network,
            stateTree.getOperatorsNotSharingCras(), reportNode);
        searchTreeParameters.decreaseRemedialActionUsageLimits(resultsPerPerimeter, prePerimeterResultPerPerimeter);
        return searchTreeParameters;
    }
}
