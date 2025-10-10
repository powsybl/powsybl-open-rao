/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class MarmotUtils {

    private MarmotUtils() {
    }

    public static PrePerimeterResult runSensitivityAnalysis(RaoInput raoInput, RaoParameters raoParameters) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        State preventiveState = crac.getPreventiveState();
        Set<RangeAction<?>> rangeActions = crac.getRangeActions(preventiveState);
        return new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), rangeActions, raoParameters, ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters)).runInitialSensitivityAnalysis(network);
    }

    public static PrePerimeterResult runInitialPrePerimeterSensitivityAnalysisWithoutRangeActions(RaoInput raoInput, AppliedRemedialActions curativeRemedialActions, PrePerimeterResult initialResult, RaoParameters raoParameters) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        return new PrePerimeterSensitivityAnalysis(
            crac,
            crac.getFlowCnecs(), // want results on all cnecs
            new HashSet<>(), // with no range actions for faster computations, only flow values are required
            raoParameters,
            ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters)
        ).runBasedOnInitialResults(network, initialResult, null, curativeRemedialActions);
    }

    public static TemporalData<AppliedRemedialActions> getAppliedRemedialActionsInCurative(TemporalData<RaoInput> inputs, TemporalData<RaoResult> raoResults) {
        TemporalData<AppliedRemedialActions> curativeRemedialActions = new TemporalDataImpl<>();
        inputs.getTimestamps().forEach(timestamp -> {
            Crac crac = inputs.getData(timestamp).orElseThrow().getCrac();
            RaoResult raoResult = raoResults.getData(timestamp).orElseThrow();
            AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
            for (State state : crac.getStates(crac.getLastInstant())) {
                try {
                    appliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
                    raoResult.getActivatedRangeActionsDuringState(state).forEach(ra -> appliedRemedialActions.addAppliedRangeAction(state, ra, raoResult.getOptimizedSetPointOnState(state, ra)));
                } catch (OpenRaoException e) {
                    if (!e.getMessage().equals("Trying to access perimeter result for the wrong state.")) {
                        throw e;
                    }
                }
            }
            curativeRemedialActions.put(timestamp, appliedRemedialActions);
        });
        return curativeRemedialActions;
    }

    public static PrePerimeterResult runSensitivityAnalysisBasedOnInitialResult(RaoInput raoInput, AppliedRemedialActions curativeRemedialActions, FlowResult initialFlowResult, RaoParameters raoParameters, Set<FlowCnec> consideredCnecs) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        State preventiveState = crac.getPreventiveState();
        Set<RangeAction<?>> rangeActions = crac.getRangeActions(preventiveState);
        return new PrePerimeterSensitivityAnalysis(crac, consideredCnecs, rangeActions, raoParameters, ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters)).runBasedOnInitialResults(network, initialFlowResult, Set.of(), curativeRemedialActions);
    }

    public static Set<FlowCnec> getPreventivePerimeterCnecs(Crac crac) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(crac.getPreventiveState());
        crac.getStates(crac.getInstant(InstantKind.OUTAGE)).forEach(state -> flowCnecs.addAll(crac.getFlowCnecs(state)));
        return flowCnecs;
    }

    public static TemporalData<PostOptimizationResult> getPostOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> initialResults, GlobalLinearOptimizationResult globalLinearOptimizationResult, TemporalData<RaoResult> topologicalOptimizationResults, RaoParameters raoParameters) {
        List<OffsetDateTime> timestamps = raoInputs.getTimestamps();
        Map<OffsetDateTime, PostOptimizationResult> postOptimizationResults = new HashMap<>();
        timestamps.forEach(timestamp -> postOptimizationResults.put(timestamp, new PostOptimizationResult(raoInputs.getData(timestamp).orElseThrow(), initialResults.getData(timestamp).orElseThrow(), globalLinearOptimizationResult, topologicalOptimizationResults.getData(timestamp).orElseThrow(), raoParameters)));
        return new TemporalDataImpl<>(postOptimizationResults);
    }

    public static <T> T getDataFromState(TemporalData<T> temporalData, State state) {
        return temporalData.getData(state.getTimestamp().orElseThrow()).orElseThrow();
    }

    /**
     * This function combines computation statuses from a Temporal Data.
     * If any &ltT&gt has ComputationStatus.FAILURE, return ComputationStatus.FAILURE
     * Else, if any &ltT&gt has  ComputationStatus.PARTIAL_FAILURE, return ComputationStatus.PARTIAL_FAILURE
     * Else, return ComputationStatus.DEFAULT
     */
    // TODO : add synchronized for multithreading ?
    public static <T> ComputationStatus getGlobalComputationStatus(TemporalData<T> temporalData, Function<T, ComputationStatus> computationStatusCalculator) {
        Set<ComputationStatus> allStatuses = new HashSet<>(temporalData.map(computationStatusCalculator).getDataPerTimestamp().values());
        if (allStatuses.contains(ComputationStatus.FAILURE)) {
            return ComputationStatus.FAILURE;
        }
        return allStatuses.contains(ComputationStatus.PARTIAL_FAILURE) ? ComputationStatus.PARTIAL_FAILURE : ComputationStatus.DEFAULT;
    }

    public static void applyPreventiveRemedialActions(RaoInput raoInput, NetworkActionsResult networkActionsResult, String initialVariantId, String newVariantId) {
        Network network = raoInput.getNetwork();
        Crac crac = raoInput.getCrac();

        State preventiveState = crac.getPreventiveState();
        network.getVariantManager().setWorkingVariant(initialVariantId);
        network.getVariantManager().cloneVariant(initialVariantId, newVariantId);
        network.getVariantManager().setWorkingVariant(newVariantId);
        Set<NetworkAction> networkActionsToBeApplied = networkActionsResult.getActivatedNetworkActionsPerState().get(preventiveState);
        if (networkActionsToBeApplied.isEmpty()) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] No preventive topological actions applied for timestamp {}", crac.getTimestamp().orElseThrow());
        } else {
            networkActionsToBeApplied.forEach(networkAction -> networkAction.apply(network));
        }
    }
}
