package com.powsybl.openrao.util;

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PostPerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PreventiveAndCurativesRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class RaoResultHelper {

    private RaoResultHelper() {
    }

    /**
     * Indicates whether all the CNECs of a given type are secure (i.e. with a margin >= 0) at the last instant (i.e. after RAO).
     *
     * @param raoResult     The RaoResult for which to check security.
     * @param crac          The CRAC for which to check security.
     * @param raoParameters The RaoParameters for which to check security.
     * @param u             The types of CNECs to check (FLOW -> FlowCNECs, ANGLE -> AngleCNECs, VOLTAGE -> VoltageCNECs). 1 to 3 arguments can be provided.
     * @return whether all the CNECs of the given type(s) are secure at the last instant (i.e. after RAO).
     */
    public static boolean isSecure(RaoResult raoResult, Crac crac, RaoParameters raoParameters, PhysicalParameter... u) {
        boolean excludeCnecsForTsosWithoutCras = raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras();
        Unit flowUnit = getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc() ? Unit.MEGAWATT : Unit.AMPERE;

        return isSecure(raoResult, crac, excludeCnecsForTsosWithoutCras, flowUnit, u);
    }

    /**
     * Indicates whether all the CNECs of a given type are secure (i.e. with a margin >= 0) at the last instant (i.e. after RAO).
     *
     * @param raoResult     The RaoResult for which to check security.
     * @param cracs         The CRACs for which to check security.
     * @param raoParameters The RaoParameters for which to check security.
     * @param u             The types of CNECs to check (FLOW -> FlowCNECs, ANGLE -> AngleCNECs, VOLTAGE -> VoltageCNECs). 1 to 3 arguments can be provided.
     * @return whether all the CNECs of the given type(s) are secure at the last instant (i.e. after RAO).
     */
    public static boolean isSecure(TimeCoupledRaoResult raoResult, TemporalData<Crac> cracs, RaoParameters raoParameters, PhysicalParameter... u) {

        for (OffsetDateTime timestamp : cracs.getTimestamps()) {
            if (!isSecure(raoResult.getIndividualRaoResult(timestamp), cracs.getData(timestamp).orElseThrow(), raoParameters, u)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSecure(RaoResult raoResult, Crac crac, boolean excludeCnecsForTsosWithoutCras, Unit flowUnit, PhysicalParameter... u) {
        Set<PhysicalParameter> parameters = new HashSet<>(Arrays.asList(u));
        if (parameters.isEmpty()) {
            throw new OpenRaoException("No physical parameter provided.");
        }
        if (raoResult.getComputationStatus() == ComputationStatus.FAILURE) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("RAO computation failed. It is not possible to assess security.");
            return false;
        }
        Set<String> tsosWithoutCras = new HashSet<>();

        if (excludeCnecsForTsosWithoutCras) {
            Set<String> allTsos = crac.getRemedialActions().stream().map(RemedialAction::getOperator).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<String> allTsosWithCras = crac.getRemedialActions()
                .stream()
                .filter(remedialAction ->
                    remedialAction
                        .getUsageRules()
                        .stream()
                        .anyMatch(usageRule -> usageRule.getInstant().isCurative())
                )
                .map(RemedialAction::getOperator)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            tsosWithoutCras.addAll(
                allTsos
                    .stream()
                    .filter(tso -> !allTsosWithCras.contains(tso)).collect(Collectors.toSet())
            );
        }
        if (parameters.contains(PhysicalParameter.FLOW)) {
            // use the same flow unit as the one use for the LF
            // some FlowCNECs shall not be taken into account for the security assessment:
            // - MNECs
            // - CNECs for TSOS without CRAs (if excludeCnecsForTsosWithoutCras is true)
            // - outage CNECs that were duplicated from auto CNECs
            for (FlowCnec flowCnec : crac.getFlowCnecs()) {
                if (flowCnec.isOptimized() && !tsosWithoutCras.contains(flowCnec.getOperator()) && !flowCnec.getId().contains("OUTAGE DUPLICATE")) {
                    Optional<Double> minMargin = safeGetDouble(raoResult.getMargin(flowCnec.getState().getInstant(), flowCnec, flowUnit));
                    if (minMargin.isPresent()) {
                        if (minMargin.get() < 0) {
                            return false;
                        }
                    } else {
                        // no flow value available: assume it is secure
                        throw new OpenRaoException("No flow value available for FlowCNEC %s.".formatted(flowCnec.getId()));
                    }
                }
            }
        }
        if (parameters.contains(PhysicalParameter.ANGLE)) {
            for (AngleCnec angleCnec : crac.getAngleCnecs()) {
                Optional<Double> minDegreeMargin = safeGetDouble(raoResult.getMargin(angleCnec.getState().getInstant(), angleCnec, Unit.DEGREE));
                if (minDegreeMargin.isPresent()) {
                    if (minDegreeMargin.get() < 0) {
                        return false;
                    }
                } else {
                    throw new OpenRaoException("No angle value available for AngleCNEC %s.".formatted(angleCnec.getId()));
                }
            }
        }
        if (parameters.contains(PhysicalParameter.VOLTAGE)) {
            for (VoltageCnec voltageCnec : crac.getVoltageCnecs()) {
                Optional<Double> minKiloVoltMargin = safeGetDouble(raoResult.getMargin(voltageCnec.getState().getInstant(), voltageCnec, Unit.KILOVOLT));
                if (minKiloVoltMargin.isPresent()) {
                    if (minKiloVoltMargin.get() < 0) {
                        return false;
                    }
                } else {
                    throw new OpenRaoException("No voltage value available for VoltageCNEC %s.".formatted(voltageCnec.getId()));
                }
            }
        }
        return true;
    }

    private static Optional<Double> safeGetDouble(double value) {
        return Double.isNaN(value) ? Optional.empty() : Optional.of(value);
    }

    /**
     * Adds new remedial actions to the given {@link RaoResult}. This method returns a new {@link RaoResult}
     * which includes the newly applied remedial actions and updated flows.
     *
     * @param raoResult             The RAO result instance that will be updated with the applied remedial actions.
     * @param crac                  The CRAC on which the original {@link RaoResult} is based.
     * @param network               The network on which the computations were initially made.
     * @param appliedRemedialAction The set of additional remedial actions to apply, including both network actions and range actions.
     * @param raoParameters         The set of parameters used for the initial RAO computation.
     * @param reportNode            The report node that logs the workflow and stores information related to the analysis progress.
     * @return The updated RAO result instance containing all applied remedial actions.
     *
     * @apiNote Preventive remedial actions are not supported yet because {@link AppliedRemedialActions}
     * is only defined for post-outage remedial actions.
     */
    public static RaoResult addAppliedRemedialActions(RaoResult raoResult,
                                                      Crac crac,
                                                      Network network,
                                                      AppliedRemedialActions appliedRemedialAction,
                                                      RaoParameters raoParameters,
                                                      ReportNode reportNode) {
        String initialVariant = network.getVariantManager().getWorkingVariantId();
        Set<String> initialVariants = new HashSet<>(network.getVariantManager().getVariantIds());

        try {
            final ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(
                RaoInput.build(network, crac).build(), raoParameters
            );
            final PrePerimeterSensitivityAnalysis initialPrePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true
            );
            final PrePerimeterResult initialFlowResult = initialPrePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode);

            // create a new network variant from initial variant to perform the results merging
            final String variantName = "RaoResultMerging";
            network.getVariantManager().cloneVariant(initialVariant, variantName);
            network.getVariantManager().setWorkingVariant(variantName);

            // apply PRAs
            final State preventiveState = crac.getPreventiveState();
            raoResult.getActivatedNetworkActionsDuringState(preventiveState).forEach(
                networkAction -> networkAction.apply(network)
            );
            raoResult.getActivatedRangeActionsDuringState(preventiveState).forEach(
                rangeAction -> rangeAction.apply(
                    network,
                    raoResult.getOptimizedSetPointOnState(preventiveState, rangeAction)
                )
            );

            // this result is only used as a data holder for flows: it does not contain the proper objective function value in costly
            final PrePerimeterResult preventivePrePerimeterResult = initialPrePerimeterSensitivityAnalysis.runBasedOnInitialResults(
                network, initialFlowResult, Set.of(), new AppliedRemedialActions(), reportNode
            );

            RangeActionActivationResultImpl preventiveRangeActionActivationResult = new RangeActionActivationResultImpl(initialFlowResult);
            raoResult.getActivatedRangeActionsDuringState(preventiveState).forEach(rangeAction ->
                preventiveRangeActionActivationResult.putResult(rangeAction, preventiveState, raoResult.getOptimizedSetPointOnState(preventiveState, rangeAction))
            );

            final OptimizationResult preventiveResult = new OptimizationResultImpl(
                preventivePrePerimeterResult, preventivePrePerimeterResult, preventivePrePerimeterResult,
                new NetworkActionsResultImpl(Map.of(
                    preventiveState, raoResult.getActivatedNetworkActionsDuringState(preventiveState)
                )),
                preventiveRangeActionActivationResult
            );

            final PostPerimeterResult preventivePostPerimeterResult =
                new PostPerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true)
                    .runBasedOnInitialPreviousAndOptimizationResults(network, initialFlowResult, preventivePrePerimeterResult, Set.of(), preventiveResult, new AppliedRemedialActions(), reportNode);

            final Map<State, PostPerimeterResult> postMergingContingencyResults = new HashMap<>();

            final List<Instant> postOutageInstants = crac.getSortedInstants().stream()
                .filter(instant -> instant.isAuto() || instant.isCurative())
                .toList();

            for (final Contingency contingency : crac.getContingencies()) {
                final AppliedRemedialActions allAppliedRemedialActions = new AppliedRemedialActions();

                network.getVariantManager().cloneVariant(variantName, contingency.getId());
                network.getVariantManager().setWorkingVariant(contingency.getId());

                PrePerimeterResult contingencyPrePerimeterResult = preventivePostPerimeterResult.prePerimeterResultForAllFollowingStates();

                for (final Instant instant : postOutageInstants) {
                    final State state = crac.getState(contingency, instant);
                    if (state != null) {
                        final RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(contingencyPrePerimeterResult);
                        allAppliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
                        raoResult.getActivatedRangeActionsDuringState(state).forEach(
                            rangeAction -> {
                                final double optimizedSetPointOnState = raoResult.getOptimizedSetPointOnState(state, rangeAction);
                                allAppliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizedSetPointOnState);
                                rangeActionActivationResult.putResult(rangeAction, state, optimizedSetPointOnState);
                            }
                        );
                        appliedRemedialAction.getAppliedNetworkActions(state).forEach(
                            networkAction -> allAppliedRemedialActions.addAppliedNetworkAction(state, networkAction)
                        );
                        appliedRemedialAction.getAppliedRangeActions(state).forEach(
                            (rangeAction, setPoint) -> {
                                allAppliedRemedialActions.addAppliedRangeAction(state, rangeAction, setPoint);
                                rangeActionActivationResult.putResult(rangeAction, state, setPoint);
                            }
                        );

                        final PrePerimeterSensitivityAnalysis statePrePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                            crac, crac.getFlowCnecs(state), crac.getRangeActions(), raoParameters, toolProvider, true
                        );

                        final PrePerimeterResult statePrePerimeterResult = statePrePerimeterSensitivityAnalysis.runBasedOnInitialResults(
                            network, initialFlowResult, Collections.emptySet(), allAppliedRemedialActions, reportNode
                        );

                        final OptimizationResult stateOptimizationResult = new OptimizationResultImpl(
                            statePrePerimeterResult,
                            statePrePerimeterResult,
                            statePrePerimeterResult,
                            new NetworkActionsResultImpl(Map.of(state, allAppliedRemedialActions.getAppliedNetworkActions(state))),
                            rangeActionActivationResult
                        );
                        final Set<FlowCnec> statePostPerimeterFlowCnecs = crac.getFlowCnecs().stream()
                            .filter(cnec -> !cnec.getState().getInstant().comesBefore(instant))
                            .filter(cnec -> cnec.getState().getContingency().orElseThrow().equals(contingency))
                            .collect(Collectors.toSet());

                        final PostPerimeterResult statePostPerimeterResult =
                            new PostPerimeterSensitivityAnalysis(crac, statePostPerimeterFlowCnecs, crac.getRangeActions(), raoParameters, toolProvider, true)
                                .runBasedOnInitialPreviousAndOptimizationResults(
                                    network,
                                    initialFlowResult,
                                    contingencyPrePerimeterResult,
                                    Set.of(),
                                    stateOptimizationResult,
                                    allAppliedRemedialActions,
                                    reportNode
                                );
                        postMergingContingencyResults.put(state, statePostPerimeterResult);

                        contingencyPrePerimeterResult = statePrePerimeterResult;
                    }
                }
            }

            final StateTree stateTree = new StateTree(crac, reportNode);
            final PreventiveAndCurativesRaoResultImpl mergedRaoResult = new PreventiveAndCurativesRaoResultImpl(
                stateTree,
                initialFlowResult,
                preventivePostPerimeterResult,
                postMergingContingencyResults,
                crac,
                raoParameters,
                reportNode
            );

            // TODO: clone metadata
            mergedRaoResult.setExecutionDetails(raoResult.getExecutionDetails());
            cleanNetworkVariants(network, initialVariant, initialVariants);
            return mergedRaoResult;
        } catch (OpenRaoException e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("An error occurred during merging, returning original RAO Result. Error was: {}", e.getMessage());
            cleanNetworkVariants(network, initialVariant, initialVariants);
            return raoResult;
        }
    }

    private static void cleanNetworkVariants(Network network, String initialVariant, Set<String> initialVariants) {
        network.getVariantManager().setWorkingVariant(initialVariant);
        Set<String> variantsToRemove = new HashSet<>();
        for (String variant : network.getVariantManager().getVariantIds()) {
            if (!initialVariants.contains(variant)) {
                variantsToRemove.add(variant);
            }
        }
        variantsToRemove.forEach(network.getVariantManager()::removeVariant);
    }
}
