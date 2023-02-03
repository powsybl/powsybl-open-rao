/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoLogger;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunctionResultImpl;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.AutomatonPerimeterResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.PrePerimeterSensitivityResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.*;

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
    private final ToolProvider toolProvider;
    private final FlowResult initialFlowResult;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpointResult;
    private final PrePerimeterResult prePerimeterSensitivityOutput;
    private final Set<String> operatorsNotSharingCras;
    private final int numberLoggedElementsDuringRao;

    public AutomatonSimulator(Crac crac, RaoParameters raoParameters, ToolProvider toolProvider, FlowResult initialFlowResult, RangeActionSetpointResult prePerimeterRangeActionSetpointResult, PrePerimeterResult prePerimeterSensitivityOutput, Set<String> operatorsNotSharingCras, int numberLoggedElementsDuringRao) {
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterRangeActionSetpointResult = prePerimeterRangeActionSetpointResult;
        this.prePerimeterSensitivityOutput = prePerimeterSensitivityOutput;
        this.operatorsNotSharingCras = operatorsNotSharingCras;
        this.numberLoggedElementsDuringRao = numberLoggedElementsDuringRao;
    }

    /**
     * This function simulates automatons at AUTO instant. First, it simulates topological automatons, then range actions
     * by order of speed.
     * Returns an AutomatonPerimeterResult
     */
    AutomatonPerimeterResultImpl simulateAutomatonState(State automatonState, State curativeState, Network network) {
        TECHNICAL_LOGS.info("Optimizing automaton state {}.", automatonState.getId());
        if (!crac.getNetworkActions(automatonState, UsageMethod.AVAILABLE).isEmpty()) {
            BUSINESS_WARNS.warn("CRAC has network action automatons with usage method AVAILABLE. These are not supported.");
        }
        TECHNICAL_LOGS.info("Initial situation:");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, prePerimeterSensitivityOutput, Set.of(automatonState), raoParameters.getObjectiveFunction(), numberLoggedElementsDuringRao);

        PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis = getPreAutoPerimeterSensitivityAnalysis(automatonState, curativeState);

        // I) Simulate topological automatons
        TopoAutomatonSimulationResult topoSimulationResult = simulateTopologicalAutomatons(automatonState, network, preAutoPerimeterSensitivityAnalysis);

        // II) Simulate range actions
        RangeAutomatonSimulationResult rangeAutomatonSimulationResult = simulateRangeAutomatons(automatonState, curativeState, network, preAutoPerimeterSensitivityAnalysis, topoSimulationResult.getPerimeterResult());

        // Build and return optimization result
        PrePerimeterResult prePerimeterResultForOptimizedState = buildPrePerimeterResultForOptimizedState(rangeAutomatonSimulationResult.getPerimeterResult(), automatonState);
        AutomatonPerimeterResultImpl automatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(
            prePerimeterResultForOptimizedState,
            topoSimulationResult.getActivatedNetworkActions(),
            rangeAutomatonSimulationResult.getActivatedRangeActions(),
            rangeAutomatonSimulationResult.getRangeActionsWithSetpoint(),
            automatonState);
        TECHNICAL_LOGS.info("Automaton state {} has been optimized.", automatonState.getId());
        RaoLogger.logOptimizationSummary(BUSINESS_LOGS, automatonState, automatonPerimeterResultImpl.getActivatedNetworkActions().size(), automatonPerimeterResultImpl.getActivatedRangeActions(automatonState).size(), null, null, automatonPerimeterResultImpl);
        return automatonPerimeterResultImpl;
    }

    private PrePerimeterSensitivityAnalysis getPreAutoPerimeterSensitivityAnalysis(State automatonState, State curativeState) {
        Set<FlowCnec> flowCnecsInSensi = crac.getFlowCnecs(automatonState);
        flowCnecsInSensi.addAll(crac.getFlowCnecs(curativeState));
        Set<RangeAction<?>> rangeActionsInSensi = new HashSet<>();
        rangeActionsInSensi.addAll(crac.getRangeActions(automatonState, UsageMethod.FORCED, UsageMethod.TO_BE_EVALUATED));
        rangeActionsInSensi.addAll(crac.getRangeActions(curativeState, UsageMethod.AVAILABLE, UsageMethod.FORCED, UsageMethod.TO_BE_EVALUATED));
        return new PrePerimeterSensitivityAnalysis(flowCnecsInSensi, rangeActionsInSensi, raoParameters, toolProvider);
    }

    /**
     * Utility class to hold the results of topo actions simulation
     */
    static class TopoAutomatonSimulationResult {
        private final PrePerimeterResult perimeterResult;
        private final Set<NetworkAction> activatedNetworkActions;

        public TopoAutomatonSimulationResult(PrePerimeterResult perimeterResult, Set<NetworkAction> activatedNetworkActions) {
            this.perimeterResult = perimeterResult;
            this.activatedNetworkActions = activatedNetworkActions;
        }

        public PrePerimeterResult getPerimeterResult() {
            return perimeterResult;
        }

        public Set<NetworkAction> getActivatedNetworkActions() {
            return activatedNetworkActions;
        }
    }

    /**
     * This function simulates topological automatons.
     * Returns a pair of :
     * -- a PrePerimeterResult : a new sensi analysis is run after having applied the topological automatons,
     * -- and the set of applied network actions.
     */
    TopoAutomatonSimulationResult simulateTopologicalAutomatons(State automatonState, Network network, PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis) {
        // -- Apply network actions
        // -- First get forced network actions
        Set<NetworkAction> appliedNetworkActions = crac.getNetworkActions(automatonState, UsageMethod.FORCED);
        // -- Then add those with an OnFlowConstraint usage rule if their constraint is verified
        crac.getNetworkActions(automatonState, UsageMethod.TO_BE_EVALUATED).stream()
            .filter(na -> RaoUtil.isRemedialActionAvailable(na, automatonState, prePerimeterSensitivityOutput, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()))
            .forEach(appliedNetworkActions::add);

        if (appliedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("Topological automaton state {} has been skipped as no topological automatons were activated.", automatonState.getId());
            return new TopoAutomatonSimulationResult(prePerimeterSensitivityOutput, appliedNetworkActions);
        }

        // -- Apply
        appliedNetworkActions.forEach(na -> {
            TECHNICAL_LOGS.debug("Activating automaton {} - {}.", na.getId(), na.getName());
            na.apply(network);
        });

        // -- Sensi must be run to evaluate available auto range actions
        // -- If network actions have been applied, run sensi :
        PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput = prePerimeterSensitivityOutput;
        if (!appliedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("Running sensi post application of auto network actions for automaton state {}.", automatonState.getId());
            automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, prePerimeterRangeActionSetpointResult, operatorsNotSharingCras, null);
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, Set.of(automatonState), raoParameters.getObjectiveFunction(), numberLoggedElementsDuringRao);
        }

        return new TopoAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, appliedNetworkActions);
    }

    /**
     * Utility class to hold the results of auto range actions simulation
     */
    static class RangeAutomatonSimulationResult {
        private final PrePerimeterResult perimeterResult;
        private final Set<RangeAction<?>> activatedRangeActions;
        private final Map<RangeAction<?>, Double> rangeActionsWithSetpoint;

        RangeAutomatonSimulationResult(PrePerimeterResult perimeterResult, Set<RangeAction<?>> activatedRangeActions, Map<RangeAction<?>, Double> rangeActionsWithSetpoint) {
            this.perimeterResult = perimeterResult;
            this.activatedRangeActions = activatedRangeActions;
            this.rangeActionsWithSetpoint = rangeActionsWithSetpoint;
        }

        PrePerimeterResult getPerimeterResult() {
            return perimeterResult;
        }

        Set<RangeAction<?>> getActivatedRangeActions() {
            return activatedRangeActions;
        }

        Map<RangeAction<?>, Double> getRangeActionsWithSetpoint() {
            return rangeActionsWithSetpoint;
        }
    }

    RangeAutomatonSimulationResult simulateRangeAutomatons(State automatonState, State curativeState, Network network, PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis, PrePerimeterResult postAutoTopoResult) {
        PrePerimeterResult finalPostAutoResult = postAutoTopoResult;
        // -- Create groups of aligned range actions
        List<List<RangeAction<?>>> rangeActionsOnAutomatonState = buildRangeActionsGroupsOrderedBySpeed(finalPostAutoResult, automatonState, network);
        // -- Build AutomatonPerimeterResultImpl objects
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
            Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> postShiftResult = shiftRangeActionsUntilFlowCnecsSecure(
                alignedRa,
                flowCnecs,
                network,
                preAutoPerimeterSensitivityAnalysis,
                finalPostAutoResult,
                automatonState);
            finalPostAutoResult = postShiftResult.getLeft();
            activatedRangeActions.addAll(postShiftResult.getRight().keySet());
            rangeActionsWithSetpoint.putAll(postShiftResult.getRight());
        }

        if (!activatedRangeActions.isEmpty()) {
            finalPostAutoResult = runPreCurativeSensitivityComputation(automatonState, curativeState, network);
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
        // UsageMethod is either FORCED or TO_BE_EVALUATED
        if (availableRa.getUsageMethod(automatonState).equals(UsageMethod.FORCED)) {
            return crac.getFlowCnecs().stream()
                .filter(flowCnec -> flowCnec.getState().equals(automatonState))
                .collect(Collectors.toSet());
        } else if (availableRa.getUsageMethod(automatonState).equals(UsageMethod.TO_BE_EVALUATED)) {
            // Get flowcnecs constrained by OnFlowConstraint
            Set<FlowCnec> flowCnecs = availableRa.getUsageRules().stream()
                .filter(OnFlowConstraint.class::isInstance)
                .map(OnFlowConstraint.class::cast)
                .map(OnFlowConstraint::getFlowCnec)
                .filter(flowCnec -> flowCnec.getState().equals(automatonState))
                .collect(Collectors.toSet());
            // Get all cnecs in country if availableRa is available on a OnFlowConstraintInCountry usage rule
            Set<Country> countries = availableRa.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance)
                .map(OnFlowConstraintInCountry.class::cast)
                .map(OnFlowConstraintInCountry::getCountry)
                .collect(Collectors.toSet());
            flowCnecs.addAll(crac.getFlowCnecs().stream()
                .filter(flowCnec -> flowCnec.getState().equals(automatonState))
                .filter(flowCnec -> countries.stream().anyMatch(country -> RaoUtil.isCnecInCountry(flowCnec, country, network)))
                .collect(Collectors.toSet()));
            return flowCnecs;
        } else {
            throw new FaraoException(String.format("Range action %s has usage method %s although FORCED or TO_BE_EVALUATED were expected.", availableRa, availableRa.getUsageMethod(automatonState)));
        }
    }

    /**
     * This function sorts groups of aligned range actions by speed.
     */
    List<List<RangeAction<?>>> buildRangeActionsGroupsOrderedBySpeed(PrePerimeterResult rangeActionSensitivity, State automatonState, Network network) {
        // 1) Get available range actions
        // -- First get forced range actions
        Set<RangeAction<?>> availableRangeActions = crac.getRangeActions(automatonState, UsageMethod.FORCED);
        // -- Then add those with an OnFlowConstraint or OnFlowConstraintInCountry usage rule if their constraint is verified
        crac.getRangeActions(automatonState, UsageMethod.TO_BE_EVALUATED).stream()
            .filter(na -> RaoUtil.isRemedialActionAvailable(na, automatonState, rangeActionSensitivity, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()))
            .forEach(availableRangeActions::add);

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
            .collect(Collectors.toList());

        // 3) Gather aligned range actions : they will be simulated simultaneously in one shot
        // -- Create groups of aligned range actions
        List<List<RangeAction<?>>> rangeActionsOnAutomatonState = new ArrayList<>();
        for (RangeAction<?> availableRangeAction : rangeActionsOrderedBySpeed) {
            if (rangeActionsOnAutomatonState.stream().anyMatch(l -> l.contains(availableRangeAction))) {
                continue;
            }
            // Look for aligned range actions in all range actions : they have the same groupId and the same usageMethod
            Optional<String> groupId = availableRangeAction.getGroupId();
            List<RangeAction<?>> alignedRa;
            if (groupId.isPresent()) {
                alignedRa = crac.getRangeActions().stream()
                    .filter(rangeAction -> groupId.get().equals(rangeAction.getGroupId().orElse(null)))
                    .sorted(Comparator.comparing(RangeAction::getId))
                    .collect(Collectors.toList());
            } else {
                alignedRa = List.of(availableRangeAction);
            }
            if (!checkAlignedRangeActions(automatonState, alignedRa, rangeActionsOrderedBySpeed)) {
                continue;
            }
            rangeActionsOnAutomatonState.add(alignedRa);
        }
        return rangeActionsOnAutomatonState;
    }

    /**
     * This function checks that the group of aligned range actions :
     * - contains same type range actions (PST, HVDC, or other) : all-or-none principle
     * - contains range actions that share the same usage rule
     * - contains range actions that are all available at AUTO instant.
     * Returns true if checks are valid.
     */
    static boolean checkAlignedRangeActions(State automatonState, List<RangeAction<?>> alignedRa, List<RangeAction<?>> rangeActionsOrderedBySpeed) {
        if (alignedRa.size() == 1) {
            // nothing to check
            return true;
        }
        // Ignore aligned range actions with heterogeneous types
        if (alignedRa.stream().map(Object::getClass).distinct().count() > 1) {
            BUSINESS_WARNS.warn("Range action group {} contains range actions of different types; they are not simulated", alignedRa.get(0).getGroupId().orElseThrow());
            return false;
        }
        // Ignore aligned range actions when one element of the group has a different usage method than the others
        if (alignedRa.stream().map(rangeAction -> rangeAction.getUsageMethod(automatonState)).distinct().count() > 1) {
            BUSINESS_WARNS.warn("Range action group {} contains range actions with different usage methods; they are not simulated", alignedRa.get(0).getGroupId().orElseThrow());
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
     * This functions runs a sensi when the remedial actions simulation process is over.
     * The sensi analysis is run on curative range actions, to be used at curative instant.
     * This function returns a prePerimeterResult that will be used to build an AutomatonPerimeterResult.
     */
    private PrePerimeterResult runPreCurativeSensitivityComputation(State automatonState, State curativeState, Network network) {
        // -- Run sensitivity computation before running curative RAO later
        // -- Get curative range actions
        Set<RangeAction<?>> curativeRangeActions = crac.getRangeActions(curativeState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        // Get cnecs
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(automatonState);
        flowCnecs.addAll(crac.getFlowCnecs(curativeState));
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            flowCnecs,
            curativeRangeActions,
            raoParameters,
            toolProvider);

        // Run computation
        TECHNICAL_LOGS.info("Running pre curative sensi after auto state {}.", automatonState.getId());
        PrePerimeterResult postAutomatonSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, prePerimeterRangeActionSetpointResult, operatorsNotSharingCras, null);
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, postAutomatonSensitivityAnalysisOutput, Set.of(automatonState), raoParameters.getObjectiveFunction(), numberLoggedElementsDuringRao);
        return postAutomatonSensitivityAnalysisOutput;
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
            .filter(hvdcRa -> isAngleDroopActivePowerControlEnabled(hvdcRa, network))
            .collect(Collectors.toSet());

        if (hvdcRasWithControl.isEmpty()) {
            return Pair.of(prePerimeterSensitivityOutput, new HashMap<>());
        }

        TECHNICAL_LOGS.debug("Running load-flow computation to access HvdcAngleDroopActivePowerControl set-point values.");
        Map<String, Double> controls = computeHvdcAngleDroopActivePowerControlValues(network, automatonState, raoParameters.getLoadFlowProvider(), raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters());

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
        PrePerimeterResult result = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, prePerimeterRangeActionSetpointResult, operatorsNotSharingCras, null);
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, result, Set.of(automatonState), raoParameters.getObjectiveFunction(), numberLoggedElementsDuringRao);

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
            state.getContingency().orElseThrow().apply(network, null);
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

    private static boolean isAngleDroopActivePowerControlEnabled(HvdcRangeAction hvdcRangeAction, Network network) {
        HvdcLine hvdcLine = network.getHvdcLine(hvdcRangeAction.getNetworkElement().getId());
        HvdcAngleDroopActivePowerControl hvdcAngleDroopActivePowerControl = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
        return (hvdcAngleDroopActivePowerControl != null) && hvdcAngleDroopActivePowerControl.isEnabled();
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
     * After every setpoint shift, a new sensi analysis is performed.
     * This function returns a pair of a prePerimeterResult, and a map of activated range actions during the shift, with their
     * newly computed setpoints, both used to compute an AutomatonPerimeterResult.
     */
    Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftRangeActionsUntilFlowCnecsSecure(List<RangeAction<?>> alignedRangeActions,
                                                                                                Set<FlowCnec> flowCnecs,
                                                                                                Network network,
                                                                                                PrePerimeterSensitivityAnalysis preAutoPerimeterSensitivityAnalysis,
                                                                                                PrePerimeterResult prePerimeterSensitivityOutput,
                                                                                                State automatonState) {

        Set<Pair<FlowCnec, Side>> flowCnecsToBeExcluded = new HashSet<>();
        PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput = prePerimeterSensitivityOutput;
        Map<RangeAction<?>, Double> activatedRangeActionsWithSetpoint = new HashMap<>();
        List<Pair<FlowCnec, Side>> flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);

        if (alignedRangeActions.stream().allMatch(HvdcRangeAction.class::isInstance) && !flowCnecsWithNegativeMargin.isEmpty()) {
            // Disable HvdcAngleDroopActivePowerControl for HVDC lines, fetch their set-point, re-run sensitivity analysis and fetch new negative margins
            Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = disableHvdcAngleDroopActivePowerControl(alignedRangeActions, network, preAutoPerimeterSensitivityAnalysis, automatonRangeActionOptimizationSensitivityAnalysisOutput, automatonState);
            automatonRangeActionOptimizationSensitivityAnalysisOutput = result.getLeft();
            activatedRangeActionsWithSetpoint.putAll(result.getRight());
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
            if (toBeShiftedCnec.equals(previouslyShiftedCnec)) {
                sensitivityUnderestimator = Math.max(SENSI_UNDER_ESTIMATOR_MIN, sensitivityUnderestimator - SENSI_UNDER_ESTIMATOR_DECREMENT);
            } else {
                sensitivityUnderestimator = 1;
            }
            Side side = flowCnecsWithNegativeMargin.get(0).getRight();
            // Aligned range actions have the same set-point :
            double currentSetpoint = alignedRangeActions.get(0).getCurrentSetpoint(network);
            double optimalSetpoint = currentSetpoint;
            double conversionToMegawatt = RaoUtil.getFlowUnitMultiplier(toBeShiftedCnec, side, raoParameters.getObjectiveFunction().getUnit(), MEGAWATT);
            double cnecFlow = conversionToMegawatt * automatonRangeActionOptimizationSensitivityAnalysisOutput.getFlow(toBeShiftedCnec, side, raoParameters.getObjectiveFunction().getUnit());
            double cnecMargin = conversionToMegawatt * automatonRangeActionOptimizationSensitivityAnalysisOutput.getMargin(toBeShiftedCnec, side, raoParameters.getObjectiveFunction().getUnit());
            double sensitivityValue = 0;
            // Under-estimate range action sensitivity if convergence to margin = 0 is slow (ie if multiple passes
            // through this loop have been needed to secure the same CNEC)
            for (RangeAction<?> rangeAction : alignedRangeActions) {
                sensitivityValue += sensitivityUnderestimator * automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityValue(toBeShiftedCnec, side, rangeAction, MEGAWATT);
            }
            // if sensitivity value is zero, CNEC cannot be secured. move on to the next CNEC with a negative margin
            if (Math.abs(sensitivityValue) < DOUBLE_NON_NULL) {
                flowCnecsToBeExcluded.add(Pair.of(toBeShiftedCnec, side));
                flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);
                continue;
            }

            double optimalSetpointForSide = computeOptimalSetpoint(currentSetpoint, cnecFlow, cnecMargin, sensitivityValue, alignedRangeActions.get(0), minSetpoint, maxSetpoint);
            if (Math.abs(currentSetpoint - optimalSetpointForSide) > Math.abs(currentSetpoint - optimalSetpoint)) {
                optimalSetpoint = optimalSetpointForSide;
            }

            // On first iteration, define direction
            if (iteration == 0) {
                direction = safeDiffSignum(optimalSetpoint, currentSetpoint);
            }
            // Compare direction with previous shift
            // If direction == 0, then the RA is at one of its bounds
            if (direction == 0 || (direction != safeDiffSignum(optimalSetpoint, currentSetpoint)) || iteration > MAX_NUMBER_OF_SENSI_IN_AUTO_SETPOINT_SHIFT) {
                return Pair.of(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint);
            }

            TECHNICAL_LOGS.debug("Shifting set-point from {} to {} on range action(s) {} to secure CNEC {} on side {} (current margin: {} MW).",
                String.format(Locale.ENGLISH, "%.2f", alignedRangeActions.get(0).getCurrentSetpoint(network)),
                String.format(Locale.ENGLISH, "%.2f", optimalSetpoint),
                alignedRangeActions.stream().map(Identifiable::getId).collect(Collectors.joining(", ")),
                toBeShiftedCnec.getId(), side,
                String.format(Locale.ENGLISH, "%.2f", cnecMargin));
            for (RangeAction<?> rangeAction : alignedRangeActions) {
                rangeAction.apply(network, optimalSetpoint);
                activatedRangeActionsWithSetpoint.put(rangeAction, optimalSetpoint);
            }
            automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, prePerimeterRangeActionSetpointResult, operatorsNotSharingCras, null);
            RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, Set.of(automatonState), raoParameters.getObjectiveFunction(), numberLoggedElementsDuringRao);
            flowCnecsWithNegativeMargin = getCnecsWithNegativeMarginWithoutExcludedCnecs(flowCnecs, flowCnecsToBeExcluded, automatonRangeActionOptimizationSensitivityAnalysisOutput);

            iteration++;
            previouslyShiftedCnec = toBeShiftedCnec;
        }
        return Pair.of(automatonRangeActionOptimizationSensitivityAnalysisOutput, activatedRangeActionsWithSetpoint);
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
     * Returns a sorted list of FlowCnecs-Side pairs with negative margins.
     */
    List<Pair<FlowCnec, Side>> getCnecsWithNegativeMarginWithoutExcludedCnecs(Set<FlowCnec> flowCnecs,
                                                                              Set<Pair<FlowCnec, Side>> cnecsToBeExcluded,
                                                                              PrePerimeterResult prePerimeterSensitivityOutput) {
        Map<Pair<FlowCnec, Side>, Double> cnecsAndMargins = new HashMap<>();
        flowCnecs.forEach(flowCnec -> flowCnec.getMonitoredSides().forEach(side -> {
            double margin = prePerimeterSensitivityOutput.getMargin(flowCnec, side, raoParameters.getObjectiveFunction().getUnit());
            if (!cnecsToBeExcluded.contains(Pair.of(flowCnec, side)) && margin < 0) {
                cnecsAndMargins.put(Pair.of(flowCnec, side), margin);
            }
        }));
        return cnecsAndMargins.entrySet().stream()
            .sorted(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
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

        if (rangeAction instanceof PstRangeAction) {
            optimalSetpoint = roundUpAngleToTapWrtInitialSetpoint((PstRangeAction) rangeAction, optimalSetpoint, currentSetpoint);
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
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(cnecsForOptimizedState, toolProvider.getLoopFlowCnecs(cnecsForOptimizedState), initialFlowResult, prePerimeterSensitivityOutput, prePerimeterRangeActionSetpointResult, crac, operatorsNotSharingCras, raoParameters);
        RangeActionActivationResult rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        ComputationStatus status = postAutoResult.getSensitivityStatus();
        ObjectiveFunctionResult objectiveFunctionResult = new ObjectiveFunctionResultImpl(objectiveFunction, flowResult, rangeActionActivationResult, sensitivityResult, status);
        return new PrePerimeterSensitivityResultImpl(flowResult, sensitivityResult, rangeActionSetpointResult, objectiveFunctionResult);

    }
}
