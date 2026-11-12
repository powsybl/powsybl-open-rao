package com.powsybl.openrao.monitoring.results;

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
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PostPerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.FlowResultExtensionHelper;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.networkpool.AbstractNetworkPool;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class RaoResultHelper {

    private RaoResultHelper() {
    }

    /**
     * Indicates whether all the CNECs of a given type are secure (i.e. with a margin >= 0) at the last instant (i.e. after RAO).
     *
     * @deprecated since 7.6.0, use {@link RaoResult#isSecure(PhysicalParameter...)} instead
     *
     * @param raoResult     The RaoResult for which to check security.
     * @param crac          The CRAC for which to check security.
     * @param raoParameters The RaoParameters for which to check security.
     * @param u             The types of CNECs to check (FLOW -> FlowCNECs, ANGLE -> AngleCNECs, VOLTAGE -> VoltageCNECs). 1 to 3 arguments can be provided.
     * @return whether all the CNECs of the given type(s) are secure at the last instant (i.e. after RAO).
     */
    @Deprecated(since = "7.6.0")
    public static boolean isSecure(RaoResult raoResult, Crac crac, RaoParameters raoParameters, PhysicalParameter... u) {
        return raoResult.isSecure(u);
    }

    /**
     * Indicates whether all the CNECs of a given type are secure (i.e. with a margin >= 0) at the last instant (i.e. after RAO).
     *
     * @deprecated since 7.6.0, use {@link RaoResult#isSecure(PhysicalParameter...)} instead
     *
     * @param raoResult     The RaoResult for which to check security.
     * @param cracs         The CRACs for which to check security.
     * @param raoParameters The RaoParameters for which to check security.
     * @param u             The types of CNECs to check (FLOW -> FlowCNECs, ANGLE -> AngleCNECs, VOLTAGE -> VoltageCNECs). 1 to 3 arguments can be provided.
     * @return whether all the CNECs of the given type(s) are secure at the last instant (i.e. after RAO).
     */
    @Deprecated(since = "7.6.0")
    public static boolean isSecure(RaoResult raoResult, TemporalData<Crac> cracs, RaoParameters raoParameters, PhysicalParameter... u) {
        return raoResult.isSecure(u);
    }

    @Deprecated(since = "7.6.0")
    public static boolean isSecure(RaoResult raoResult, Crac crac, boolean excludeCnecsForTsosWithoutCras, Unit flowUnit, PhysicalParameter... u) {
        return raoResult.isSecure(u);
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
     * @apiNote Preventive remedial actions are not supported yet because {@link AppliedRemedialActions}
     * is only defined for post-outage remedial actions.
     */
    public static RaoResult addAppliedRemedialActions(RaoResult raoResult,
                                                      Crac crac,
                                                      Network network,
                                                      AppliedRemedialActions appliedRemedialAction,
                                                      RaoParameters raoParameters,
                                                      ReportNode reportNode) {
        return addAppliedRemedialActions(raoResult, crac, network, appliedRemedialAction, new EnumMap<>(PhysicalParameter.class), raoParameters, reportNode);
    }

    /**
     * Adds new remedial actions to the given {@link RaoResult}. This method returns a new {@link RaoResult}
     * which includes the newly applied remedial actions and updated flows.
     *
     * @param raoResult                  The RAO result instance that will be updated with the applied remedial actions.
     * @param crac                       The CRAC on which the original {@link RaoResult} is based.
     * @param network                    The network on which the computations were initially made.
     * @param appliedRemedialAction      The set of additional remedial actions to apply, including both network actions and range actions.
     * @param additionalSecurityStatuses The set of additional security statuses to apply (e.g. ANGLE or VOLTAGE).
     * @param raoParameters              The set of parameters used for the initial RAO computation.
     * @param reportNode                 The report node that logs the workflow and stores information related to the analysis progress.
     * @return The updated RAO result instance containing all applied remedial actions.
     * @apiNote Preventive remedial actions are not supported yet because {@link AppliedRemedialActions}
     * is only defined for post-outage remedial actions.
     */
    public static RaoResult addAppliedRemedialActions(RaoResult raoResult,
                                                      Crac crac,
                                                      Network network,
                                                      AppliedRemedialActions appliedRemedialAction,
                                                      EnumMap<PhysicalParameter, Boolean> additionalSecurityStatuses,
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

            final Map<State, PostPerimeterResult> postMergingContingencyResults = new ConcurrentHashMap<>();

            final List<Instant> postOutageInstants = crac.getSortedInstants().stream()
                .filter(instant -> instant.isAuto() || instant.isCurative())
                .toList();

            try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), getAvailableCPUs(raoParameters), true)) {
                List<ForkJoinTask<Object>> tasks = crac.getContingencies()
                    .stream()
                    .map(contingency -> networkPool.submit(() -> buildContingencyScenario(
                        raoResult,
                        crac,
                        networkPool,
                        contingency,
                        appliedRemedialAction,
                        postOutageInstants, initialFlowResult, preventivePostPerimeterResult, postMergingContingencyResults, toolProvider, raoParameters,
                        reportNode
                    )))
                    .toList();
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

            final RaoResultImpl.RaoResultImplBuilder raoResultImplBuilder = RaoResultImpl.builder(crac);
            RaoUtil.fillWithActivatedRemedialActions(raoResultImplBuilder, preventivePostPerimeterResult.optimizationResult(), crac.getPreventiveState(), postMergingContingencyResults);

            // add security statuses
            raoResultImplBuilder.withSecurityStatus(
                PhysicalParameter.FLOW,
                RaoUtil.computeSecurityStatus(
                    crac,
                    preventivePostPerimeterResult.prePerimeterResultForAllFollowingStates(),
                    postMergingContingencyResults,
                    RaoUtil.getFlowUnit(raoParameters),
                    RaoUtil.getOperatorsNotSharingCras(crac, raoParameters)
                )
            );

            additionalSecurityStatuses.keySet()
                .stream()
                .filter(physicalParameter -> physicalParameter != PhysicalParameter.FLOW)
                .forEach(physicalParameter ->
                    raoResultImplBuilder.withSecurityStatus(physicalParameter, additionalSecurityStatuses.get(physicalParameter)));

            // build final RAO Result
            final RaoResultImpl mergedRaoResult = raoResultImplBuilder.build();

            // add extensions
            mergedRaoResult.addExtension(FlowResult.class, FlowResultExtensionHelper.convertToExtension(
                initialFlowResult,
                preventivePostPerimeterResult.prePerimeterResultForAllFollowingStates(),
                postMergingContingencyResults,
                crac,
                raoParameters)
            );

            String executionDetails = null;
            Metadata metadata = raoResult.getExtension(Metadata.class);
            if (metadata != null) {
                executionDetails = metadata.getExecutionDetails().orElse(null);
            }
            fillAndAddMetadata(
                mergedRaoResult,
                crac,
                preventivePostPerimeterResult.prePerimeterResultForAllFollowingStates(),
                postMergingContingencyResults,
                executionDetails
            );
            cleanNetworkVariants(network, initialVariant, initialVariants);

            return mergedRaoResult;
        } catch (OpenRaoException e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("An error occurred during merging, returning original RAO Result. Error was: {}", e.getMessage());
            cleanNetworkVariants(network, initialVariant, initialVariants);
            return raoResult;
        }
    }

    private static Object buildContingencyScenario(RaoResult raoResult,
                                                   Crac crac,
                                                   AbstractNetworkPool networkPool,
                                                   Contingency contingency,
                                                   AppliedRemedialActions appliedRemedialAction,
                                                   List<Instant> postOutageInstants,
                                                   PrePerimeterResult initialFlowResult,
                                                   PostPerimeterResult preventivePostPerimeterResult,
                                                   Map<State, PostPerimeterResult> postMergingContingencyResults,
                                                   ToolProvider toolProvider,
                                                   RaoParameters raoParameters,
                                                   ReportNode reportNode) throws InterruptedException {
        Network network = networkPool.getAvailableNetwork();
        final AppliedRemedialActions allAppliedRemedialActions = new AppliedRemedialActions();

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

        networkPool.releaseUsedNetwork(network, true);
        return null;
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

    private static void fillAndAddMetadata(RaoResult raoResult,
                                           Crac crac,
                                           PrePerimeterResult postPreventiveResults,
                                           Map<State, PostPerimeterResult> postMergingContingencyResults,
                                           String executionDetails) {
        Metadata.MetadataBuilder metadataBuilder = Metadata.builder();
        if (executionDetails != null) {
            metadataBuilder.withExecutionDetails(executionDetails);
        }
        for (State state : crac.getStates()) {
            ComputationStatus computationStatus = getAppropriateResult(state, postPreventiveResults, postMergingContingencyResults)
                .getComputationStatus(state);
            if (computationStatus != ComputationStatus.DEFAULT) {
                metadataBuilder.addComputationStatus(state, computationStatus);
            }
        }
        raoResult.addExtension(Metadata.class, metadataBuilder.build());
    }

    private static PrePerimeterResult getAppropriateResult(State state,
                                                           PrePerimeterResult postPreventiveResults,
                                                           Map<State, PostPerimeterResult> postMergingContingencyResults) {
        if (state.isPreventive() || state.getInstant().isOutage()) {
            return postPreventiveResults;
        }
        PostPerimeterResult postPerimeterResult = postMergingContingencyResults.get(state);
        return postPerimeterResult != null ? postPerimeterResult.prePerimeterResultForAllFollowingStates() : postPreventiveResults;
    }
}
