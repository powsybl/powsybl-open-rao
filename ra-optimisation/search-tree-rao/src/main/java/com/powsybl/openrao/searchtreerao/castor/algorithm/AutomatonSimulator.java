/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getLoadFlowProvider;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;

/**
 * Automaton simulator
 * Simulates the behavior of topological and range automatons
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class AutomatonSimulator {
    private static final double DOUBLE_NON_NULL = 1e-12;
    private static final int MAX_NUMBER_OF_SENSI_IN_AUTO_SETPOINT_SHIFT = 10;
    public static final double SENSI_UNDER_ESTIMATOR_MIN = 0.5;
    private static final double SENSI_UNDER_ESTIMATOR_DECREMENT = 0.15;
    private static final int DEFAULT_SPEED = 0;

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
     * This function simulates automatons at AUTO instant, by order of speed.
     * Automatons are gathered by speed and sorted from the fastest to the slowest.
     * Batches are then simulated speed-wise, applying topological automatons first, then range actions.
     * Returns an AutomatonPerimeterResult
     */
    AutomatonPerimeterResultImpl simulateAutomatonState(State automatonState, Set<State> curativeStates, Network network) {
        TECHNICAL_LOGS.info("Optimizing automaton state {}.", automatonState.getId());

        PrePerimeterSensitivityAnalysis preAutoPstOptimizationSensitivityAnalysis = getPreAutoPerimeterSensitivityAnalysis(automatonState, curativeStates);
        // Sensitivity analysis failed :
        if (prePerimeterSensitivityOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
            return createFailedAutomatonPerimeterResult(prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, Set.of(), Set.of(), Map.of(), automatonState, "before topological automatons simulation.");
        }

        TECHNICAL_LOGS.info("Initial situation:");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, prePerimeterSensitivityOutput, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);

        Map<RangeAction<?>, Double> initialSetPoints = new HashMap<>();
        crac.getRangeActions(automatonState, UsageMethod.FORCED).forEach(rangeAction -> initialSetPoints.put(rangeAction, rangeAction.getCurrentSetpoint(network)));

        TopoAutomatonSimulationResult topoSimulationResult = new TopoAutomatonSimulationResult(prePerimeterSensitivityOutput, Set.of());
        RangeAutomatonSimulationResult rangeAutomatonSimulationResult = new RangeAutomatonSimulationResult(prePerimeterSensitivityOutput, Set.of(), initialSetPoints, initialSetPoints);

        for (int speed : getAllSortedSpeeds(automatonState)) {
            TECHNICAL_LOGS.info("Simulating automaton batch of speed {} for automaton state {}", speed, automatonState.getId());
            // I) Simulate FORCED topological automatons
            topoSimulationResult = simulateTopologicalAutomatons(automatonState, network, preAutoPstOptimizationSensitivityAnalysis, speed, topoSimulationResult.activatedNetworkActions(), rangeAutomatonSimulationResult.perimeterResult());

            // Sensitivity analysis failed :
            if (topoSimulationResult.perimeterResult().getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return createFailedAutomatonPerimeterResult(rangeAutomatonSimulationResult.perimeterResult(), topoSimulationResult.perimeterResult(), topoSimulationResult.activatedNetworkActions(), rangeAutomatonSimulationResult.activatedRangeActions(), rangeAutomatonSimulationResult.rangeActionsWithSetpoint(), automatonState, "after topological automatons simulation for speed %s.".formatted(speed));
            }

            // II) Simulate range actions
            rangeAutomatonSimulationResult = simulateRangeAutomatons(automatonState, curativeStates, network, preAutoPstOptimizationSensitivityAnalysis, topoSimulationResult.perimeterResult(), speed, rangeAutomatonSimulationResult.activatedRangeActions(), initialSetPoints, rangeAutomatonSimulationResult.rangeActionsWithSetpoint());

            // Sensitivity analysis failed :
            if (rangeAutomatonSimulationResult.perimeterResult().getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return createFailedAutomatonPerimeterResult(topoSimulationResult.perimeterResult(), rangeAutomatonSimulationResult.perimeterResult(), topoSimulationResult.activatedNetworkActions(), rangeAutomatonSimulationResult.activatedRangeActions(), rangeAutomatonSimulationResult.rangeActionsWithSetpoint(), automatonState, "after range automatons simulation for speed %s.".formatted(speed));
            }
        }

        // Build and return optimization result
        RemedialActionActivationResult remedialActionActivationResult = buildRemedialActionActivationResult(topoSimulationResult, rangeAutomatonSimulationResult, automatonState);
        PrePerimeterResult prePerimeterResultForOptimizedState = buildPrePerimeterResultForOptimizedState(rangeAutomatonSimulationResult, automatonState, remedialActionActivationResult);
        Map<RangeAction<?>, Double> rangeActionsWithSetpoint = rangeAutomatonSimulationResult.rangeActionsWithSetpoint();
        prePerimeterResultForOptimizedState.getRangeActionSetpointResult().getRangeActions().forEach(ra -> rangeActionsWithSetpoint.putIfAbsent(ra, prePerimeterResultForOptimizedState.getSetpoint(ra)));
        AutomatonPerimeterResultImpl automatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(
            topoSimulationResult.perimeterResult(),
            prePerimeterResultForOptimizedState,
            topoSimulationResult.activatedNetworkActions(),
            rangeAutomatonSimulationResult.activatedRangeActions(),
            rangeActionsWithSetpoint,
            automatonState);
        TECHNICAL_LOGS.info("Automaton state {} has been optimized.", automatonState.getId());
        RaoLogger.logOptimizationSummary(BUSINESS_LOGS, automatonState, automatonPerimeterResultImpl.getActivatedNetworkActions(), getRangeActionsAndTheirTapsAppliedOnState(automatonPerimeterResultImpl, automatonState), null, automatonPerimeterResultImpl);
        return automatonPerimeterResultImpl;
    }

    private List<Integer> getAllSortedSpeeds(State automatonState) {
        Set<Integer> automatonSpeeds = crac.getRangeActions(automatonState, UsageMethod.FORCED).stream().map(this::getSpeed).collect(Collectors.toSet());
        automatonSpeeds.addAll(crac.getNetworkActions(automatonState, UsageMethod.FORCED).stream().map(this::getSpeed).collect(Collectors.toSet()));
        return automatonSpeeds.stream().sorted().toList();
    }

    private int getSpeed(RemedialAction<?> remedialAction) {
        return remedialAction.getSpeed().orElse(DEFAULT_SPEED);
    }

    private PrePerimeterSensitivityAnalysis getPreAutoPerimeterSensitivityAnalysis(State automatonState, Set<State> curativeStates) {
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(automatonState);
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>(crac.getRangeActions(automatonState, UsageMethod.FORCED));
        for (State curativeState : curativeStates) {
            flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
            rangeActionsInSensi.addAll(crac.getRangeActions(curativeState, UsageMethod.AVAILABLE));
        }
        return new PrePerimeterSensitivityAnalysis(crac, flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProvider);
    }

    public static Map<RangeAction<?>, Double> getRangeActionsAndTheirTapsAppliedOnState(OptimizationResult optimizationResult, State state) {
        Set<RangeAction<?>> setActivatedRangeActions = optimizationResult.getActivatedRangeActions(state);
        Map<RangeAction<?>, Double> allRangeActions = new HashMap<>();
        setActivatedRangeActions.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).forEach(pstRangeAction -> allRangeActions.put(pstRangeAction, (double) optimizationResult.getOptimizedTap(pstRangeAction, state)));
        setActivatedRangeActions.stream().filter(ra -> !(ra instanceof PstRangeAction)).forEach(rangeAction -> allRangeActions.put(rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)));
        return allRangeActions;
    }

    private AutomatonPerimeterResultImpl createFailedAutomatonPerimeterResult(PrePerimeterResult preAutomatonSensitivityAnalysisOutput, PrePerimeterResult postAutomatonSensitivityAnalysisOutput, Set<NetworkAction> activatedNetworkActions, Set<RangeAction<?>> activatedRangeActions, Map<RangeAction<?>, Double> rangeActionsWithSetPoint, State automatonState, String failDescription) {
        AutomatonPerimeterResultImpl failedAutomatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(
            preAutomatonSensitivityAnalysisOutput,
            postAutomatonSensitivityAnalysisOutput,
            activatedNetworkActions,
            activatedRangeActions,
            rangeActionsWithSetPoint,
            automatonState);
        TECHNICAL_LOGS.info("Automaton state {} has failed during sensitivity computation {}", automatonState.getId(), failDescription);
        RaoLogger.logFailedOptimizationSummary(BUSINESS_LOGS, automatonState, failedAutomatonPerimeterResultImpl.getActivatedNetworkActions(), getRangeActionsAndTheirTapsAppliedOnState(failedAutomatonPerimeterResultImpl, automatonState));
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
    TopoAutomatonSimulationResult simulateTopologicalAutomatons(State automatonState, Network network, PrePerimeterSensitivityAnalysis preAutoPstOptimizationSensitivityAnalysis, int speed, Set<NetworkAction> previouslyActivatedTopologicalAutomatons, PrePerimeterResult preAutomatonsPerimeterResult) {
        // -- Apply network actions
        // -- First get forced network actions
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(automatonState);
        Set<NetworkAction> appliedNetworkActions = new HashSet<>();
        crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionForced(ra, automatonState, preAutomatonsPerimeterResult, flowCnecs, network, raoParameters))
            .filter(ra -> getSpeed(ra) == speed)
            .forEach(networkAction -> {
                if (networkAction.hasImpactOnNetwork(network)) {
                    appliedNetworkActions.add(networkAction);
                } else {
                    TECHNICAL_LOGS.info("Automaton {} - {} has been skipped as it has no impact on network.", networkAction.getId(), networkAction.getName());
                }
            });

        if (appliedNetworkActions.isEmpty()) {
            return new TopoAutomatonSimulationResult(preAutomatonsPerimeterResult, previouslyActivatedTopologicalAutomatons);
        }

        // -- Apply
        appliedNetworkActions.forEach(na -> {
            TECHNICAL_LOGS.debug("Activating automaton {} - {}.", na.getId(), na.getName());
            na.apply(network);
        });

        Set<NetworkAction> allAppliedAutomatons = new HashSet<>(previouslyActivatedTopologicalAutomatons);
        allAppliedAutomatons.addAll(appliedNetworkActions);

        // -- Sensitivity analysis must be run to evaluate available auto range actions
        // -- If network actions have been applied, run sensitivity :
        PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutomatonsPerimeterResult;
        if (!appliedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("Running sensitivity analysis post application of auto network actions for automaton state {} for speed {}.", automatonState.getId(), speed);
            automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPstOptimizationSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
            if (automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new TopoAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, allAppliedAutomatons);
            }
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);
        }

        return new TopoAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, allAppliedAutomatons);
    }

    /**
     * Utility class to hold the results of auto range actions simulation
     */
    record RangeAutomatonSimulationResult(PrePerimeterResult perimeterResult, Set<RangeAction<?>> activatedRangeActions,
                                          Map<RangeAction<?>, Double> rangeActionsWithInitialSetpoint,
                                          Map<RangeAction<?>, Double> rangeActionsWithSetpoint
    ) {
    }

    RangeAutomatonSimulationResult simulateRangeAutomatons(State automatonState, Set<State> curativeStates, Network network, PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis, PrePerimeterResult postTopoResult, int speed, Set<RangeAction<?>> previouslyAppliedRangeAutomatons, Map<RangeAction<?>, Double> initialSetPoints, Map<RangeAction<?>, Double> setPoints) {
        PrePerimeterResult finalPostAutoResult = postTopoResult;
        // -- Create groups of aligned range actions
        List<List<RangeAction<?>>> rangeActionsOnAutomatonState = buildRangeActionsGroupsForSpeed(finalPostAutoResult, automatonState, network, speed);
        // -- Build AutomatonPerimeterResultImpl objects
        // -- rangeActionsWithSetpoint contains all available automaton range actions
        Map<RangeAction<?>, Double> rangeActionsWithSetpoint = new HashMap<>(setPoints);
        Set<RangeAction<?>> activatedRangeActions = new HashSet<>();

        if (rangeActionsOnAutomatonState.isEmpty()) {
            return new RangeAutomatonSimulationResult(finalPostAutoResult, previouslyAppliedRangeAutomatons, initialSetPoints, rangeActionsWithSetpoint);
        }

        Set<RangeAction<?>> allActivatedRangeAutomatons = new HashSet<>(previouslyAppliedRangeAutomatons);

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
            allActivatedRangeAutomatons.addAll(postShiftResult.activatedRangeActions());
            rangeActionsWithSetpoint.putAll(postShiftResult.rangeActionsWithSetpoint());
            if (finalPostAutoResult.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new RangeAutomatonSimulationResult(finalPostAutoResult, allActivatedRangeAutomatons, initialSetPoints, rangeActionsWithSetpoint);
            }
        }

        if (!activatedRangeActions.isEmpty()) {
            finalPostAutoResult = runPostRangeAutomatonsSensitivityComputation(automatonState, curativeStates, network, speed);
            if (finalPostAutoResult.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new RangeAutomatonSimulationResult(finalPostAutoResult, allActivatedRangeAutomatons, initialSetPoints, rangeActionsWithSetpoint);
            }
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, finalPostAutoResult, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);
        }
        return new RangeAutomatonSimulationResult(finalPostAutoResult, allActivatedRangeAutomatons, initialSetPoints, rangeActionsWithSetpoint);
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
    List<List<RangeAction<?>>> buildRangeActionsGroupsForSpeed(PrePerimeterResult rangeActionSensitivity, State automatonState, Network network, int speed) {
        // 1) Get available range actions
        // -- First get forced range actions
        List<RangeAction<?>> availableRangeActions = crac.getRangeActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionForced(ra, automatonState, rangeActionSensitivity, crac.getFlowCnecs(), network, raoParameters))
            .filter(ra -> getSpeed(ra) == speed)
            .toList();

        // 2) Gather aligned range actions : they will be simulated simultaneously in one shot
        // -- Create groups of aligned range actions
        List<List<RangeAction<?>>> rangeActionsOnAutomatonState = new ArrayList<>();
        for (RangeAction<?> availableRangeAction : availableRangeActions) {
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
            if (!checkAlignedRangeActions(alignedRa, availableRangeActions)) {
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
    private PrePerimeterResult runPostRangeAutomatonsSensitivityComputation(State automatonState, Set<State> curativeStates, Network network, int speed) {
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
            crac,
            flowCnecs,
            curativeRangeActions,
            raoParameters,
            toolProvider);

        // Run computation
        TECHNICAL_LOGS.info("Running post range automatons sensitivity analysis after auto state {} for speed {}.", automatonState.getId(), speed);
        return prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
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
        Map<String, Double> controls = computeHvdcAngleDroopActivePowerControlValues(network, automatonState, getLoadFlowProvider(raoParameters), getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters());

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
        PrePerimeterResult result = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
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
     * Get setpoint set by AngleDroopActivePowerControl
     *
     * @param hvdcLine: HVDC line object
     * @return the setpoint computed by the HvdcAngleDroopActivePowerControl
     */
    private static double computeHvdcAngleDroopActivePowerControlValue(HvdcLine hvdcLine) {
        if (hvdcLine.getConvertersMode().equals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER)) {
            return hvdcLine.getConverterStation2().getTerminal().getP();
        } else {
            return hvdcLine.getConverterStation1().getTerminal().getP();
        }
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
        Map<RangeAction<?>, Double> activatedRangeActionsWithInitialSetpoint = new HashMap<>();
        alignedRangeActions.forEach(rangeAction -> activatedRangeActionsWithInitialSetpoint.put(rangeAction, rangeAction.getCurrentSetpoint(network)));
        Map<RangeAction<?>, Double> activatedRangeActionsWithSetpoint = new HashMap<>();
        List<Pair<FlowCnec, TwoSides>> flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);

        if (alignedRangeActions.stream().allMatch(HvdcRangeAction.class::isInstance) && !flowCnecsWithNegativeMargin.isEmpty()) {
            // Disable HvdcAngleDroopActivePowerControl for HVDC lines, fetch their set-point, re-run sensitivity analysis and fetch new negative margins
            Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = disableHvdcAngleDroopActivePowerControl(alignedRangeActions, network, preAutoPerimeterSensitivityAnalysis, automatonRangeActionOptimizationSensitivityAnalysisOutput, automatonState);
            automatonRangeActionOptimizationSensitivityAnalysisOutput = result.getLeft();
            activatedRangeActionsWithSetpoint.putAll(result.getRight());
            // If sensitivity analysis failed :
            if (automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new RangeAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap());
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
                return new RangeAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint.keySet(), activatedRangeActionsWithInitialSetpoint, activatedRangeActionsWithSetpoint);
            }

            TECHNICAL_LOGS.debug("Shifting set-point from {} to {} on range action(s) {} to secure CNEC {} on side {} (current margin: {} MW).",
                String.format(Locale.ENGLISH, "%.2f", alignedRangeActions.get(0).getCurrentSetpoint(network)),
                String.format(Locale.ENGLISH, "%.2f", optimalSetpoint),
                alignedRangeActions.stream().map(Identifiable::getId).collect(Collectors.joining(", ")),
                toBeShiftedCnec.getId(), side,
                String.format(Locale.ENGLISH, "%.2f", cnecMargin));

            applyAllRangeActions(alignedRangeActions, network, optimalSetpoint, activatedRangeActionsWithSetpoint);

            automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, operatorsNotSharingCras, null);
            // If sensitivity analysis fails, stop shifting and return all applied range actions
            if (automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
                return new RangeAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint.keySet(), activatedRangeActionsWithInitialSetpoint, activatedRangeActionsWithSetpoint);
            }
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);
            flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);
            iteration++;
            previouslyShiftedCnec = toBeShiftedCnec;
        }
        return new RangeAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint.keySet(), activatedRangeActionsWithInitialSetpoint, activatedRangeActionsWithSetpoint);
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

    private PrePerimeterResult buildPrePerimeterResultForOptimizedState(RangeAutomatonSimulationResult rangeAutomatonSimulationResult, State optimizedState, RemedialActionActivationResult remedialActionActivationResult) {
        // Gather variables necessary for PrePerimeterResult construction
        PrePerimeterResult postAutoResult = rangeAutomatonSimulationResult.perimeterResult();
        FlowResult flowResult = postAutoResult.getFlowResult();
        SensitivityResult sensitivityResult = postAutoResult.getSensitivityResult();
        RangeActionSetpointResult rangeActionSetpointResult = postAutoResult.getRangeActionSetpointResult();
        // Gather flowCnecs defined on optimizedState
        Set<FlowCnec> cnecsForOptimizedState = crac.getFlowCnecs(optimizedState);
        // Build ObjectiveFunctionResult based on cnecsForOptimizedState
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(cnecsForOptimizedState, toolProvider.getLoopFlowCnecs(cnecsForOptimizedState), initialFlowResult, prePerimeterSensitivityOutput, operatorsNotSharingCras, raoParameters, Set.of(optimizedState));
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(flowResult, remedialActionActivationResult);
        return new PrePerimeterSensitivityResultImpl(flowResult, sensitivityResult, rangeActionSetpointResult, objectiveFunctionResult);

    }

    private static RemedialActionActivationResult buildRemedialActionActivationResult(TopoAutomatonSimulationResult topoSimulationResult, RangeAutomatonSimulationResult rangeAutomatonSimulationResult, State automatonState) {
        Set<NetworkAction> allAppliedNetworkActions = new HashSet<>(topoSimulationResult.activatedNetworkActions());
        return new RemedialActionActivationResultImpl(buildRangeActionActivationResult(rangeAutomatonSimulationResult, automatonState), new NetworkActionsResultImpl(Map.of(automatonState, allAppliedNetworkActions)));
    }

    private static RangeActionActivationResult buildRangeActionActivationResult(RangeAutomatonSimulationResult rangeAutomatonSimulationResult, State optimizedState) {
        RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(new RangeActionSetpointResultImpl(rangeAutomatonSimulationResult.rangeActionsWithInitialSetpoint()));
        rangeAutomatonSimulationResult.rangeActionsWithSetpoint().forEach((rangeAction, setPoint) -> rangeActionActivationResult.putResult(rangeAction, optimizedState, setPoint));
        return rangeActionActivationResult;
    }
}
