/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.computation.ComputationManager;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunctionResultImpl;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.AutoOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyRemedialActions;

/**
 * Automaton simulator
 * Simulates the behavior of topological and range automatons
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class AutomatonSimulator {
    private static final double DOUBLE_NON_NULL = 1e-12;
    private static final int MAX_NUMBER_OF_SENSI_IN_AUTO_SETPOINT_SHIFT = 10;
    public static final double SENSI_UNDER_ESTIMATOR_MIN = 0.5;
    private static final double SENSI_UNDER_ESTIMATOR_DECREMENT = 0.15;

    private final Crac crac;
    private final RaoParameters raoParameters;
    private final Unit flowUnit;
    private final ToolProvider toolProvider;
    private final FlowResult initialFlowResult;
    private final PrePerimeterResult prePerimeterSensitivityOutput;
    private final Set<String> operatorsNotSharingCras;
    private final int numberLoggedElementsDuringRao;

    public AutomatonSimulator(Crac crac, RaoParameters raoParameters, ToolProvider toolProvider, FlowResult initialFlowResult, PrePerimeterResult prePerimeterSensitivityOutput, Set<String> operatorsNotSharingCras, int numberLoggedElementsDuringRao) {
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.flowUnit = raoParameters.getObjectiveFunctionParameters().getUnit();
        this.toolProvider = toolProvider;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterSensitivityOutput = prePerimeterSensitivityOutput;
        this.operatorsNotSharingCras = operatorsNotSharingCras;
        this.numberLoggedElementsDuringRao = numberLoggedElementsDuringRao;
    }

    /**
     * This function simulates automatons at AUTO instant. First, it simulates topological automatons,
     * then range actions by order of speed. TODO Network actions by speed  is not implemented yet
     * Returns an AutomatonPerimeterResult
     */
    AutomatonPerimeterResultImpl simulateAutomatonState(State automatonState, Set<State> curativeStates, Network network, StateTree stateTree, TreeParameters automatonTreeParameters) {
        TECHNICAL_LOGS.info("Optimizing automaton state {}.", automatonState.getId());
        TECHNICAL_LOGS.info("Initial situation:");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, prePerimeterSensitivityOutput, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);

        PrePerimeterSensitivityAnalysis preAutoPstOptimizationSensitivityAnalysis = getPreAutoPerimeterSensitivityAnalysis(automatonState, curativeStates);

        // Sensitivity analysis failed :
        if (prePerimeterSensitivityOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
            return createFailedAutomatonPerimeterResult(automatonState, prePerimeterSensitivityOutput, new HashSet<>(), "before");
        }

        // I) Simulate FORCED topological automatons
        TopoAutomatonSimulationResult topoSimulationResult = simulateTopologicalAutomatons(automatonState, network, preAutoPstOptimizationSensitivityAnalysis);

        // Sensitivity analysis failed :
        if (topoSimulationResult.perimeterResult().getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
            return createFailedAutomatonPerimeterResult(automatonState, topoSimulationResult.perimeterResult(), topoSimulationResult.activatedNetworkActions(), "during");
        }

        // II) Run auto search tree on AVAILABLE topological automatons
        OptimizationPerimeter autoOptimizationPerimeter = AutoOptimizationPerimeter.build(automatonState, crac, network, raoParameters, topoSimulationResult.perimeterResult());
        OptimizationResult autoSearchTreeResult = null;

        PrePerimeterResult postAutoSearchTreeResult = topoSimulationResult.perimeterResult();

        if (!autoOptimizationPerimeter.getNetworkActions().isEmpty()) {
            autoSearchTreeResult = runAutoSearchTree(automatonState, network, stateTree, automatonTreeParameters, topoSimulationResult.perimeterResult(), autoOptimizationPerimeter, topoSimulationResult.activatedNetworkActions());
            if (autoSearchTreeResult.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return createFailedAutomatonPerimeterResult(automatonState, topoSimulationResult.perimeterResult(), autoSearchTreeResult.getActivatedNetworkActions(), "during");
            }
            applyRemedialActions(network, autoSearchTreeResult, automatonState);

            postAutoSearchTreeResult = preAutoPstOptimizationSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, operatorsNotSharingCras, null);
        }

        // III) Simulate range actions
        RangeAutomatonSimulationResult rangeAutomatonSimulationResult = simulateRangeAutomatons(automatonState, curativeStates, network, preAutoPstOptimizationSensitivityAnalysis, postAutoSearchTreeResult);

        // Sensitivity analysis failed :
        if (rangeAutomatonSimulationResult.perimeterResult().getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
            AutomatonPerimeterResultImpl failedAutomatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(
                rangeAutomatonSimulationResult.perimeterResult(),
                topoSimulationResult.activatedNetworkActions(),
                autoSearchTreeResult == null ? new HashSet<>() : new HashSet<>(autoSearchTreeResult.getActivatedNetworkActions()),
                rangeAutomatonSimulationResult.activatedRangeActions(),
                rangeAutomatonSimulationResult.rangeActionsWithSetpoint(),
                automatonState);
            TECHNICAL_LOGS.info("Automaton state {} has failed during sensitivity computation during range automaton simulation.", automatonState.getId());
            RaoLogger.logFailedOptimizationSummary(BUSINESS_LOGS, automatonState, failedAutomatonPerimeterResultImpl.getActivatedNetworkActions(), getRangeActionsAndTheirTapsAppliedOnState(failedAutomatonPerimeterResultImpl, automatonState));
            return failedAutomatonPerimeterResultImpl;
        }
        // Build and return optimization result
        PrePerimeterResult prePerimeterResultForOptimizedState = buildPrePerimeterResultForOptimizedState(rangeAutomatonSimulationResult.perimeterResult(), automatonState);
        Map<RangeAction<?>, Double> rangeActionsWithSetpoint = rangeAutomatonSimulationResult.rangeActionsWithSetpoint();
        prePerimeterResultForOptimizedState.getRangeActionSetpointResult().getRangeActions().forEach(ra -> rangeActionsWithSetpoint.putIfAbsent(ra, prePerimeterResultForOptimizedState.getSetpoint(ra)));
        AutomatonPerimeterResultImpl automatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(
            prePerimeterResultForOptimizedState,
            topoSimulationResult.activatedNetworkActions(),
            autoSearchTreeResult == null ? new HashSet<>() : new HashSet<>(autoSearchTreeResult.getActivatedNetworkActions()),
            rangeAutomatonSimulationResult.activatedRangeActions(),
            rangeActionsWithSetpoint,
            automatonState);
        TECHNICAL_LOGS.info("Automaton state {} has been optimized.", automatonState.getId());
        RaoLogger.logOptimizationSummary(BUSINESS_LOGS, automatonState, automatonPerimeterResultImpl.getActivatedNetworkActions(), getRangeActionsAndTheirTapsAppliedOnState(automatonPerimeterResultImpl, automatonState), null, automatonPerimeterResultImpl);
        return automatonPerimeterResultImpl;
    }

    private OptimizationResult runAutoSearchTree(State automatonState, Network network, StateTree stateTree, TreeParameters automatonTreeParameters, PrePerimeterResult initialSensitivityOutput, OptimizationPerimeter autoOptimizationPerimeter, Set<NetworkAction> forcedNetworkActions) {
        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(automatonTreeParameters)
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()))
            .build();

        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkActions(automatonState, forcedNetworkActions);

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(autoOptimizationPerimeter)
            .withInitialFlowResult(prePerimeterSensitivityOutput)
            .withPrePerimeterResult(initialSensitivityOutput)
            .withPreOptimizationAppliedNetworkActions(appliedRemedialActions)
            .withObjectiveFunction(ObjectiveFunction.create().build(autoOptimizationPerimeter.getFlowCnecs(), autoOptimizationPerimeter.getLoopFlowCnecs(), initialSensitivityOutput, prePerimeterSensitivityOutput, stateTree.getOperatorsNotSharingCras(), raoParameters))
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        return new SearchTree(searchTreeInput, searchTreeParameters, false).run().join();
    }

    private PrePerimeterSensitivityAnalysis getPreAutoPerimeterSensitivityAnalysis(State automatonState, Set<State> curativeStates) {
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(automatonState);
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getRangeActions(automatonState, UsageMethod.FORCED));
        for (State curativeState : curativeStates) {
            flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
            rangeActionsInSensi.addAll(crac.getRangeActions(curativeState, UsageMethod.AVAILABLE));
        }
        return new PrePerimeterSensitivityAnalysis(flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProvider);
    }

    public static Map<RangeAction<?>, Double> getRangeActionsAndTheirTapsAppliedOnState(OptimizationResult optimizationResult, State state) {
        Set<RangeAction<?>> setActivatedRangeActions = optimizationResult.getActivatedRangeActions(state);
        Map<RangeAction<?>, Double> allRangeActions = new HashMap<>();
        setActivatedRangeActions.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).forEach(pstRangeAction -> allRangeActions.put(pstRangeAction, (double) optimizationResult.getOptimizedTap(pstRangeAction, state)));
        setActivatedRangeActions.stream().filter(ra -> !(ra instanceof PstRangeAction)).forEach(rangeAction -> allRangeActions.put(rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)));
        return allRangeActions;
    }

    AutomatonPerimeterResultImpl createFailedAutomatonPerimeterResult(State autoState, PrePerimeterResult result, Set<NetworkAction> activatedNetworkActions, String defineMoment) {
        AutomatonPerimeterResultImpl failedAutomatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(
            result,
            activatedNetworkActions,
            new HashSet<>(),
            new HashSet<>(),
            new HashMap<>(),
            autoState);
        TECHNICAL_LOGS.info("Automaton state {} has failed during sensitivity computation {} topological automaton simulation.", autoState.getId(), defineMoment);
        RaoLogger.logFailedOptimizationSummary(BUSINESS_LOGS, autoState, failedAutomatonPerimeterResultImpl.getActivatedNetworkActions(), getRangeActionsAndTheirTapsAppliedOnState(failedAutomatonPerimeterResultImpl, autoState));
        return failedAutomatonPerimeterResultImpl;
    }

    /**
     * Utility class to hold the results of topo actions simulation
     */
    record TopoAutomatonSimulationResult(PrePerimeterResult perimeterResult,
                                         Set<NetworkAction> activatedNetworkActions) {
    }

    /**
     * This function simulates topological automatons.
     * Returns a pair of :
     * -- a PrePerimeterResult : a new sensitivity analysis is run after having applied the topological automatons,
     * -- and the set of applied network actions.
     */
    TopoAutomatonSimulationResult simulateTopologicalAutomatons(State automatonState, Network network, PrePerimeterSensitivityAnalysis preAutoPstOptimizationSensitivityAnalysis) {
        // -- Apply network actions
        // -- First get forced network actions
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(automatonState);
        Set<NetworkAction> appliedNetworkActions = new HashSet<>();
        crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionForced(ra, automatonState, prePerimeterSensitivityOutput, flowCnecs, network, raoParameters))
            .forEach(networkAction -> {
                if (networkAction.hasImpactOnNetwork(network)) {
                    appliedNetworkActions.add(networkAction);
                } else {
                    TECHNICAL_LOGS.info("Automaton {} - {} has been skipped as it has no impact on network.", networkAction.getId(), networkAction.getName());
                }
            });

        if (appliedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("Topological automaton state {} has been skipped as no topological automatons were activated.", automatonState.getId());
            return new TopoAutomatonSimulationResult(prePerimeterSensitivityOutput, appliedNetworkActions);
        }

        // -- Apply
        appliedNetworkActions.forEach(na -> {
            TECHNICAL_LOGS.debug("Activating automaton {} - {}.", na.getId(), na.getName());
            na.apply(network);
        });

        // -- Sensitivity analysis must be run to evaluate available auto range actions
        // -- If network actions have been applied, run sensitivity :
        PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput = prePerimeterSensitivityOutput;
        if (!appliedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("Running sensitivity analysis post application of auto network actions for automaton state {}.", automatonState.getId());
            automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPstOptimizationSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, operatorsNotSharingCras, null);
            if (automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new TopoAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, appliedNetworkActions);
            }
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);
        }

        return new TopoAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, appliedNetworkActions);
    }

    /**
     * Utility class to hold the results of auto range actions simulation
     */
    record RangeAutomatonSimulationResult(PrePerimeterResult perimeterResult, Set<RangeAction<?>> activatedRangeActions,
                                          Map<RangeAction<?>, Double> rangeActionsWithSetpoint) {
    }

    RangeAutomatonSimulationResult simulateRangeAutomatons(State automatonState, Set<State> curativeStates, Network network, PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis, PrePerimeterResult postAutoTopoResult) {
        PrePerimeterResult finalPostAutoResult = postAutoTopoResult;
        // -- Create groups of aligned range actions
        List<List<RangeAction<?>>> rangeActionsOnAutomatonState = buildRangeActionsGroupsOrderedBySpeed(finalPostAutoResult, automatonState, network);
        // -- Build AutomatonPerimeterResultImpl objects
        // -- rangeActionsWithSetpoint contains all available automaton range actions
        Map<RangeAction<?>, Double> rangeActionsWithSetpoint = new HashMap<>();
        rangeActionsOnAutomatonState.stream().flatMap(List::stream).forEach(rangeAction -> rangeActionsWithSetpoint.put(rangeAction, rangeAction.getCurrentSetpoint(network)));
        Set<RangeAction<?>> activatedRangeActions = new HashSet<>();

        if (rangeActionsOnAutomatonState.isEmpty()) {
            TECHNICAL_LOGS.info("Automaton state {} has been optimized (no automaton range actions available).", automatonState.getId());
            return new RangeAutomatonSimulationResult(finalPostAutoResult, activatedRangeActions, rangeActionsWithSetpoint);
        }

        // -- Optimize range-action automatons
        for (List<RangeAction<?>> alignedRa : rangeActionsOnAutomatonState) {
            RangeAction<?> availableRa = alignedRa.get(0);
            // Define flowCnecs depending on UsageMethod
            Set<FlowCnec> flowCnecs = gatherFlowCnecsForAutoRangeAction(availableRa, automatonState, network);
            // Shift
            RangeAutomatonSimulationResult postShiftResult = shiftRangeActionsUntilFlowCnecsSecure(
                alignedRa,
                flowCnecs,
                network,
                preAutoPerimeterSensitivityAnalysis,
                finalPostAutoResult,
                automatonState);
            finalPostAutoResult = postShiftResult.perimeterResult();
            activatedRangeActions.addAll(postShiftResult.activatedRangeActions());
            rangeActionsWithSetpoint.putAll(postShiftResult.rangeActionsWithSetpoint());
            if (finalPostAutoResult.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new RangeAutomatonSimulationResult(finalPostAutoResult, activatedRangeActions, rangeActionsWithSetpoint);
            }
        }

        if (!activatedRangeActions.isEmpty()) {
            finalPostAutoResult = runPreCurativeSensitivityComputation(automatonState, curativeStates, network);
            if (finalPostAutoResult.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new RangeAutomatonSimulationResult(finalPostAutoResult, activatedRangeActions, rangeActionsWithSetpoint);
            }
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, finalPostAutoResult, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);
        }
        return new RangeAutomatonSimulationResult(finalPostAutoResult, activatedRangeActions, rangeActionsWithSetpoint);
    }

    /**
     * This function gathers the flow cnecs to be considered while shifting range actions,
     * depending on the range action availableRa's UsageMethod.
     */
    Set<FlowCnec> gatherFlowCnecsForAutoRangeAction(RangeAction<?> availableRa,
                                                    State automatonState,
                                                    Network network) {
        // UsageMethod should be FORCED
        if (availableRa.getUsageMethod(automatonState).equals(UsageMethod.FORCED)) {
            if (availableRa.getUsageRules().stream().filter(usageRule -> usageRule instanceof OnInstant || usageRule instanceof OnContingencyState)
                .anyMatch(usageRule -> usageRule.getUsageMethod(automatonState).equals(UsageMethod.FORCED))) {
                return crac.getFlowCnecs(automatonState);
            } else {
                return availableRa.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(automatonState), network, automatonState);
            }
        } else {
            throw new OpenRaoException(String.format("Range action %s has usage method %s although FORCED was expected.", availableRa, availableRa.getUsageMethod(automatonState)));
        }
    }

    /**
     * This function sorts groups of aligned range actions by speed.
     */
    List<List<RangeAction<?>>> buildRangeActionsGroupsOrderedBySpeed(PrePerimeterResult rangeActionSensitivity, State automatonState, Network network) {
        // 1) Get available range actions
        // -- First get forced range actions
        Set<RangeAction<?>> availableRangeActions = crac.getRangeActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionForced(ra, automatonState, rangeActionSensitivity, crac.getFlowCnecs(), network, raoParameters))
            .collect(Collectors.toSet());

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
            .toList();

        // 3) Gather aligned range actions : they will be simulated simultaneously in one shot
        // -- Create groups of aligned range actions
        List<List<RangeAction<?>>> rangeActionsOnAutomatonState = new ArrayList<>();
        for (RangeAction<?> availableRangeAction : rangeActionsOrderedBySpeed) {
            if (rangeActionsOnAutomatonState.stream().anyMatch(l -> l.contains(availableRangeAction))) {
                continue;
            }
            // Look for aligned range actions in all range actions : they have the same groupId and should both be available
            Optional<String> groupId = availableRangeAction.getGroupId();
            List<RangeAction<?>> alignedRa;
            if (groupId.isPresent()) {
                alignedRa = crac.getRangeActions().stream()
                    .filter(rangeAction -> groupId.get().equals(rangeAction.getGroupId().orElse(null)))
                    .sorted(Comparator.comparing(RangeAction::getId))
                    .toList();
            } else {
                alignedRa = List.of(availableRangeAction);
            }
            if (!checkAlignedRangeActions(alignedRa, rangeActionsOrderedBySpeed)) {
                continue;
            }
            rangeActionsOnAutomatonState.add(alignedRa);
        }
        return rangeActionsOnAutomatonState;
    }

    /**
     * This function checks that the group of aligned range actions :
     * - contains same type range actions (PST, HVDC, or other) : all-or-none principle
     * - contains range actions that are all available at AUTO instant.
     * Returns true if checks are valid.
     */
    static boolean checkAlignedRangeActions(List<RangeAction<?>> alignedRa, List<RangeAction<?>> rangeActionsOrderedBySpeed) {
        if (alignedRa.size() == 1) {
            // nothing to check
            return true;
        }
        // Ignore aligned range actions with heterogeneous types
        if (alignedRa.stream().map(Object::getClass).distinct().count() > 1) {
            BUSINESS_WARNS.warn("Range action group {} contains range actions of different types; they are not simulated", alignedRa.get(0).getGroupId().orElseThrow());
            return false;
        }
        // Ignore aligned range actions when one element of the group is not available at AUTO instant
        if (alignedRa.stream().anyMatch(aRa -> !rangeActionsOrderedBySpeed.contains(aRa))) {
            BUSINESS_WARNS.warn("Range action group {} contains range actions not all available at AUTO instant; they are not simulated", alignedRa.get(0).getGroupId().orElseThrow());
            return false;
        }
        return true;
    }

    /**
     * This functions runs a sensitivity analysis when the remedial actions simulation process is over.
     * The sensitivity analysis is run on curative range actions, to be used at curative instant.
     * This function returns a prePerimeterResult that will be used to build an AutomatonPerimeterResult.
     */
    private PrePerimeterResult runPreCurativeSensitivityComputation(State automatonState, Set<State> curativeStates, Network network) {
        // -- Run sensitivity computation before running curative RAO later
        // -- Get curative range actions
        Set<RangeAction<?>> curativeRangeActions = new HashSet<>();
        // Get cnecs
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(automatonState);
        for (State curativeState : curativeStates) {
            curativeRangeActions.addAll(crac.getRangeActions(curativeState, UsageMethod.AVAILABLE));
            flowCnecs.addAll(crac.getFlowCnecs(curativeState));
        }
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            flowCnecs,
            curativeRangeActions,
            raoParameters,
            toolProvider);

        // Run computation
        TECHNICAL_LOGS.info("Running pre curative sensitivity analysis after auto state {}.", automatonState.getId());
        return prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, operatorsNotSharingCras, null);
    }

    /**
     * This function disables HvdcAngleDroopActivePowerControl if alignedRA contains HVDC range actions with this control
     * enabled. It sets the active power set-point of the HVDCs to the one computed by the control prior to deactivation.
     * It finally runs a sensitivity analysis after this control has been disabled.
     * It returns the sensitivity analysis result and the HVDC active power set-points that have been set.
     */
    Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> disableHvdcAngleDroopActivePowerControl(List<RangeAction<?>> alignedRa,
                                                                                                   Network network,
                                                                                                   PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis,
                                                                                                   PrePerimeterResult prePerimeterSensitivityOutput,
                                                                                                   State automatonState) {
        Set<HvdcRangeAction> hvdcRasWithControl = alignedRa.stream()
            .filter(HvdcRangeAction.class::isInstance)
            .map(HvdcRangeAction.class::cast)
            .filter(hvdcRa -> hvdcRa.isAngleDroopActivePowerControlEnabled(network))
            .collect(Collectors.toSet());

        if (hvdcRasWithControl.isEmpty()) {
            return Pair.of(prePerimeterSensitivityOutput, new HashMap<>());
        }

        TECHNICAL_LOGS.debug("Running load-flow computation to access HvdcAngleDroopActivePowerControl set-point values.");
        Map<String, Double> controls = computeHvdcAngleDroopActivePowerControlValues(network, automatonState, raoParameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider(), raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters());

        // Next, disable AngleDroopActivePowerControl on HVDCs and set their active power set-points to the value
        // previously computed by the AngleDroopActivePowerControl.
        // This makes sure that the future sensitivity computations will converge.
        Map<HvdcRangeAction, Double> activePowerSetpoints = new HashMap<>();
        hvdcRasWithControl.forEach(hvdcRa -> {
            String hvdcLineId = hvdcRa.getNetworkElement().getId();
            double activePowerSetpoint = controls.get(hvdcLineId);
            if (activePowerSetpoint >= hvdcRa.getMinAdmissibleSetpoint(activePowerSetpoint)
                && activePowerSetpoint <= hvdcRa.getMaxAdmissibleSetpoint(activePowerSetpoint)
            ) {
                activePowerSetpoints.put(hvdcRa, activePowerSetpoint);
                disableHvdcAngleDroopActivePowerControl(hvdcLineId, network, activePowerSetpoint);
            } else {
                BUSINESS_LOGS.info(String.format("HVDC range action %s could not be activated because its initial set-point (%.1f) does not fall within its allowed range (%.1f - %.1f)",
                    hvdcRa.getId(), activePowerSetpoint, hvdcRa.getMinAdmissibleSetpoint(activePowerSetpoint), hvdcRa.getMaxAdmissibleSetpoint(activePowerSetpoint)));
            }
        });

        if (activePowerSetpoints.isEmpty()) {
            // Nothing has changed
            return Pair.of(prePerimeterSensitivityOutput, new HashMap<>());
        }

        // Finally, run a sensitivity analysis to get sensitivity values in DC set-point mode if needed
        TECHNICAL_LOGS.info("Running sensitivity analysis after disabling AngleDroopActivePowerControl on HVDC RAs.");
        PrePerimeterResult result = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, operatorsNotSharingCras, null);
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, result, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);

        return Pair.of(result, activePowerSetpoints);
    }

    private static Map<String, Double> computeHvdcAngleDroopActivePowerControlValues(Network network, State state, String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        // Create a temporary variant to apply contingency and compute load-flow on
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String tmpVariant = RandomizedString.getRandomizedString("HVDC_LF", network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(initialVariantId, tmpVariant);
        network.getVariantManager().setWorkingVariant(tmpVariant);

        // Apply contingency and compute load-flow
        if (state.getContingency().isPresent()) {
            Contingency contingency = state.getContingency().orElseThrow();
            if (!contingency.isValid(network)) {
                throw new OpenRaoException("Unable to apply contingency " + contingency.getId());
            }
            contingency.toModification().apply(network, (ComputationManager) null);
        }
        LoadFlow.find(loadFlowProvider).run(network, loadFlowParameters);

        // Compute HvdcAngleDroopActivePowerControl values of HVDC lines
        Map<String, Double> controls = network.getHvdcLineStream()
            .filter(hvdcLine -> hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class) != null)
            .collect(Collectors.toMap(com.powsybl.iidm.network.Identifiable::getId, AutomatonSimulator::computeHvdcAngleDroopActivePowerControlValue));

        // Reset working variant
        network.getVariantManager().setWorkingVariant(initialVariantId);
        network.getVariantManager().removeVariant(tmpVariant);

        return controls;
    }

    /**
     * Compute setpoint set by AngleDroopActivePowerControl = p0 + droop * angle difference
     * NB: p0 and angle difference are always in 1->2 direction
     *
     * @param hvdcLine: HVDC line object
     * @return the setpoint computed by the HvdcAngleDroopActivePowerControl
     */
    private static double computeHvdcAngleDroopActivePowerControlValue(HvdcLine hvdcLine) {
        double phi1 = hvdcLine.getConverterStation1().getTerminal().getBusView().getBus().getAngle();
        double phi2 = hvdcLine.getConverterStation2().getTerminal().getBusView().getBus().getAngle();
        double p0 = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class).getP0();
        double droop = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class).getDroop();
        return p0 + droop * (phi1 - phi2);
    }

    /**
     * Disables the HvdcAngleDroopActivePowerControl on an HVDC line and sets its active power set-point
     *
     * @param hvdcLineId:          ID of the HVDC line
     * @param network:             network to modify the HVDC line in
     * @param activePowerSetpoint: active power set-point to set on the HVDC line
     */
    private static void disableHvdcAngleDroopActivePowerControl(String hvdcLineId, Network network, double activePowerSetpoint) {
        HvdcLine hvdcLine = network.getHvdcLine(hvdcLineId);
        TECHNICAL_LOGS.debug("Disabling HvdcAngleDroopActivePowerControl on HVDC line {} and setting its set-point to {}", hvdcLine.getId(), activePowerSetpoint);
        hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class).setEnabled(false);
        hvdcLine.setConvertersMode(activePowerSetpoint > 0 ? HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER : HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        hvdcLine.setActivePowerSetpoint(Math.abs(activePowerSetpoint));
    }

    /**
     * This function shifts alignedRangeAction setpoints until :
     * -- no cnecs with a negative margin remain
     * -- OR setpoints have been shifted as far as possible in one direction
     * -- OR the direction in which the shift is performed switches
     * -- OR too many iterations have been performed
     * After every setpoint shift, a new sensitivity analysis is performed.
     * This function returns a pair of a prePerimeterResult, and a map of activated range actions during the shift, with their
     * newly computed setpoints, both used to compute an AutomatonPerimeterResult.
     */
    RangeAutomatonSimulationResult shiftRangeActionsUntilFlowCnecsSecure(List<RangeAction<?>> alignedRangeActions,
                                                                         Set<FlowCnec> flowCnecs,
                                                                         Network network,
                                                                         PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis,
                                                                         PrePerimeterResult prePerimeterSensitivityOutput,
                                                                         State automatonState) {

        Set<Pair<FlowCnec, TwoSides>> flowCnecsToBeExcluded = new HashSet<>();
        PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput = prePerimeterSensitivityOutput;
        Map<RangeAction<?>, Double> activatedRangeActionsWithSetpoint = new HashMap<>();
        List<Pair<FlowCnec, TwoSides>> flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);

        if (alignedRangeActions.stream().allMatch(HvdcRangeAction.class::isInstance) && !flowCnecsWithNegativeMargin.isEmpty()) {
            // Disable HvdcAngleDroopActivePowerControl for HVDC lines, fetch their set-point, re-run sensitivity analysis and fetch new negative margins
            Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = disableHvdcAngleDroopActivePowerControl(alignedRangeActions, network, preAutoPerimeterSensitivityAnalysis, automatonRangeActionOptimizationSensitivityAnalysisOutput, automatonState);
            automatonRangeActionOptimizationSensitivityAnalysisOutput = result.getLeft();
            activatedRangeActionsWithSetpoint.putAll(result.getRight());
            // If sensitivity analysis failed :
            if (automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new RangeAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, Collections.emptySet(), Collections.emptyMap());
            }
            flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);
        }

        // -- Define setpoint bounds
        // Aligned range actions have the same setpoint :
        double initialSetpoint = alignedRangeActions.get(0).getCurrentSetpoint(network);
        double minSetpoint = alignedRangeActions.stream().map(ra -> ra.getMinAdmissibleSetpoint(initialSetpoint)).max(Double::compareTo).orElseThrow();
        double maxSetpoint = alignedRangeActions.stream().map(ra -> ra.getMaxAdmissibleSetpoint(initialSetpoint)).min(Double::compareTo).orElseThrow();

        int iteration = 0; // security measure
        double direction = 0;
        FlowCnec previouslyShiftedCnec = null;
        double sensitivityUnderestimator = 1;
        while (!flowCnecsWithNegativeMargin.isEmpty()) {
            FlowCnec toBeShiftedCnec = flowCnecsWithNegativeMargin.get(0).getLeft();

            sensitivityUnderestimator = updateSensitivityUnderestimator(toBeShiftedCnec, previouslyShiftedCnec, sensitivityUnderestimator);

            TwoSides side = flowCnecsWithNegativeMargin.get(0).getRight();
            double sensitivityValue = computeTotalSensitivityValue(alignedRangeActions, sensitivityUnderestimator, automatonRangeActionOptimizationSensitivityAnalysisOutput, toBeShiftedCnec, side);

            // if sensitivity value is zero, CNEC cannot be secured. move on to the next CNEC with a negative margin
            if (Math.abs(sensitivityValue) < DOUBLE_NON_NULL) {
                flowCnecsToBeExcluded.add(Pair.of(toBeShiftedCnec, side));
                flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);
                continue;
            }

            // Aligned range actions have the same set-point :
            double currentSetpoint = alignedRangeActions.get(0).getCurrentSetpoint(network);
            double conversionToMegawatt = RaoUtil.getFlowUnitMultiplier(toBeShiftedCnec, side, flowUnit, MEGAWATT);
            double cnecFlow = conversionToMegawatt * automatonRangeActionOptimizationSensitivityAnalysisOutput.getFlow(toBeShiftedCnec, side, flowUnit);
            double cnecMargin = conversionToMegawatt * automatonRangeActionOptimizationSensitivityAnalysisOutput.getMargin(toBeShiftedCnec, side, flowUnit);
            double optimalSetpoint = computeOptimalSetpoint(currentSetpoint, cnecFlow, cnecMargin, sensitivityValue, alignedRangeActions.get(0), minSetpoint, maxSetpoint);

            // On first iteration, define direction
            if (iteration == 0) {
                direction = safeDiffSignum(optimalSetpoint, currentSetpoint);
            }
            // Compare direction with previous shift
            // If direction == 0, then the RA is at one of its bounds
            if (direction == 0 || direction != safeDiffSignum(optimalSetpoint, currentSetpoint) || iteration > MAX_NUMBER_OF_SENSI_IN_AUTO_SETPOINT_SHIFT) {
                return new RangeAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint.keySet(), activatedRangeActionsWithSetpoint);
            }

            TECHNICAL_LOGS.debug("Shifting set-point from {} to {} on range action(s) {} to secure CNEC {} on side {} (current margin: {} MW).",
                String.format(Locale.ENGLISH, "%.2f", alignedRangeActions.get(0).getCurrentSetpoint(network)),
                String.format(Locale.ENGLISH, "%.2f", optimalSetpoint),
                alignedRangeActions.stream().map(Identifiable::getId).collect(Collectors.joining(", ")),
                toBeShiftedCnec.getId(), side,
                String.format(Locale.ENGLISH, "%.2f", cnecMargin));

            applyAllRangeActions(alignedRangeActions, network, optimalSetpoint, activatedRangeActionsWithSetpoint);

            automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, operatorsNotSharingCras, null);
            // If sensitivity analysis fails, stop shifting and return all applied range actions
            if (automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new RangeAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint.keySet(), activatedRangeActionsWithSetpoint);
            }
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);
            flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);
            iteration++;
            previouslyShiftedCnec = toBeShiftedCnec;
        }
        return new RangeAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint.keySet(), activatedRangeActionsWithSetpoint);
    }

    private static void applyAllRangeActions(List<RangeAction<?>> alignedRangeActions, Network network, double optimalSetpoint, Map<RangeAction<?>, Double> activatedRangeActionsWithSetpoint) {
        for (RangeAction<?> rangeAction : alignedRangeActions) {
            rangeAction.apply(network, optimalSetpoint);
            activatedRangeActionsWithSetpoint.put(rangeAction, optimalSetpoint);
        }
    }

    private double computeTotalSensitivityValue(List<RangeAction<?>> alignedRangeActions, double sensitivityUnderestimator, PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput, FlowCnec toBeShiftedCnec, TwoSides side) {
        double sensitivityValue = 0;
        // Under-estimate range action sensitivity if convergence to margin = 0 is slow (ie if multiple passes
        // through this loop have been needed to secure the same CNEC)
        for (RangeAction<?> rangeAction : alignedRangeActions) {
            sensitivityValue += sensitivityUnderestimator * automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityValue(toBeShiftedCnec, side, rangeAction, MEGAWATT);
        }
        return sensitivityValue;
    }

    private double updateSensitivityUnderestimator(FlowCnec toBeShiftedCnec, FlowCnec previouslyShiftedCnec, double previousUnderestimator) {
        if (toBeShiftedCnec.equals(previouslyShiftedCnec)) {
            return Math.max(SENSI_UNDER_ESTIMATOR_MIN, previousUnderestimator - SENSI_UNDER_ESTIMATOR_DECREMENT);
        } else {
            return 1;
        }
    }

    /**
     * Computes the signum of a value evolution "newValue - oldValue"
     * If the evolution is smaller than 1e-6 in absolute value, it returns 0
     * If the double signum is smaller than 1e-6 in absolute value, it returns 0
     * Else, it returns 1 if evolution is positive, -1 if evolution is negative
     */
    private static int safeDiffSignum(double newValue, double oldValue) {
        if (Math.abs(newValue - oldValue) < 1e-6) {
            return 0;
        }
        double signum = Math.signum(newValue - oldValue);
        if (Math.abs(signum) < 1e-6) {
            return 0;
        }
        if (signum > 0) {
            return 1;
        }
        return -1;
    }

    /**
     * This function builds a list of cnecs with negative margin, except cnecs in cnecsToBeExcluded.
     * N.B : margin is retrieved in MEGAWATT as only the sign matters.
     * Returns a sorted list of FlowCnecs-TwoSides pairs with negative margins.
     */
    List<Pair<FlowCnec, TwoSides>> getCnecsWithNegativeMarginWithoutExcludedCnecs(Set<FlowCnec> flowCnecs,
                                                                                  Set<Pair<FlowCnec, TwoSides>> cnecsToBeExcluded,
                                                                                  PrePerimeterResult prePerimeterSensitivityOutput) {
        Map<Pair<FlowCnec, TwoSides>, Double> cnecsAndMargins = new HashMap<>();
        flowCnecs.forEach(flowCnec -> flowCnec.getMonitoredSides().forEach(side -> {
            double margin = prePerimeterSensitivityOutput.getMargin(flowCnec, side, flowUnit);
            if (!cnecsToBeExcluded.contains(Pair.of(flowCnec, side)) && margin < 0) {
                cnecsAndMargins.put(Pair.of(flowCnec, side), margin);
            }
        }));
        return cnecsAndMargins.entrySet().stream()
            .sorted(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * This function computes the optimal setpoint to bring cnecMargin over 0.
     * Returns optimal setpoint.
     */
    double computeOptimalSetpoint(double currentSetpoint, double cnecFlow, double cnecMargin, double sensitivityValue, RangeAction<?> rangeAction, double minSetpointInAlignedRa, double maxSetpointInAlignedRa) {
        double optimalSetpoint = currentSetpoint + Math.signum(cnecFlow) * Math.min(cnecMargin, 0) / sensitivityValue;
        // Compare setpoint to min and max
        if (optimalSetpoint > maxSetpointInAlignedRa) {
            optimalSetpoint = maxSetpointInAlignedRa;
        }
        if (optimalSetpoint < minSetpointInAlignedRa) {
            optimalSetpoint = minSetpointInAlignedRa;
        }

        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            optimalSetpoint = roundUpAngleToTapWrtInitialSetpoint(pstRangeAction, optimalSetpoint, currentSetpoint);
        }
        return optimalSetpoint;
    }

    /**
     * This function converts angleToBeRounded in the angle corresponding to the first tap
     * after angleToBeRounded in the direction opposite of initialAngle.
     */
    static Double roundUpAngleToTapWrtInitialSetpoint(PstRangeAction rangeAction, double angleToBeRounded, double initialAngle) {
        double direction = safeDiffSignum(angleToBeRounded, initialAngle);
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

    private PrePerimeterResult buildPrePerimeterResultForOptimizedState(PrePerimeterResult postAutoResult, State optimizedState) {
        // Gather variables necessary for PrePerimeterResult construction
        FlowResult flowResult = postAutoResult.getFlowResult();
        SensitivityResult sensitivityResult = postAutoResult.getSensitivityResult();
        RangeActionSetpointResult rangeActionSetpointResult = postAutoResult.getRangeActionSetpointResult();
        // Gather flowCnecs defined on optimizedState
        Set<FlowCnec> cnecsForOptimizedState = postAutoResult.getObjectiveFunction().getFlowCnecs().stream()
            .filter(flowCnec -> flowCnec.getState().equals(optimizedState)).collect(Collectors.toSet());
        // Build ObjectiveFunctionResult based on cnecsForOptimizedState
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(cnecsForOptimizedState, toolProvider.getLoopFlowCnecs(cnecsForOptimizedState), initialFlowResult, prePerimeterSensitivityOutput, operatorsNotSharingCras, raoParameters);
        ObjectiveFunctionResult objectiveFunctionResult = new ObjectiveFunctionResultImpl(objectiveFunction, flowResult);
        return new PrePerimeterSensitivityResultImpl(flowResult, sensitivityResult, rangeActionSetpointResult, objectiveFunctionResult);

    }
}
