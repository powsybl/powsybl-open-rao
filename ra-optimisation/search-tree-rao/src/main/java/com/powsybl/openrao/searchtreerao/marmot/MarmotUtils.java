/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
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
        Set<RangeAction<?>> rangeActions = crac.getRangeActions(preventiveState, UsageMethod.AVAILABLE);
        return new PrePerimeterSensitivityAnalysis(getPreventivePerimeterCnecs(crac), rangeActions, raoParameters, ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters)).runInitialSensitivityAnalysis(network, crac);
    }

    public static PrePerimeterResult runSensitivityAnalysisBasedOnInitialResult(RaoInput raoInput, RaoParameters raoParameters, FlowResult initialFlowResult) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        State preventiveState = crac.getPreventiveState();
        Set<RangeAction<?>> rangeActions = crac.getRangeActions(preventiveState, UsageMethod.AVAILABLE);
        return new PrePerimeterSensitivityAnalysis(getPreventivePerimeterCnecs(crac), rangeActions, raoParameters, ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters)).runBasedOnInitialResults(network, crac, initialFlowResult, Set.of(), new AppliedRemedialActions());
    }

    public static Set<FlowCnec> getPreventivePerimeterCnecs(Crac crac) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(crac.getPreventiveState());
        crac.getStates(crac.getInstant(InstantKind.OUTAGE)).forEach(state -> flowCnecs.addAll(crac.getFlowCnecs(state)));
        return flowCnecs;
    }

    public static TemporalData<TopologicalOptimizationResult> getTopologicalOptimizationResult(TemporalData<RaoInput> raoInputs, TemporalData<RaoResult> topologicalOptimizationResults) {
        List<OffsetDateTime> timestamps = raoInputs.getTimestamps();
        Map<OffsetDateTime, TopologicalOptimizationResult> topologicalOptimizationResultMap = new HashMap<>();
        timestamps.forEach(timestamp -> topologicalOptimizationResultMap.put(timestamp, new TopologicalOptimizationResult(raoInputs.getData(timestamp).orElseThrow(), topologicalOptimizationResults.getData(timestamp).orElseThrow())));
        return new TemporalDataImpl<>(topologicalOptimizationResultMap);
    }

    public static TemporalData<PostOptimizationResult> getPostOptimizationResults(TemporalData<RaoInput> raoInputs, TemporalData<PrePerimeterResult> initialResults, GlobalLinearOptimizationResult globalLinearOptimizationResult, TemporalData<RaoResult> topologicalOptimizationResults, RaoParameters raoParameters) {
        List<OffsetDateTime> timestamps = raoInputs.getTimestamps();
        Map<OffsetDateTime, PostOptimizationResult> postOptimizationResults = new HashMap<>();
        timestamps.forEach(timestamp -> postOptimizationResults.put(timestamp, new PostOptimizationResult(raoInputs.getData(timestamp).orElseThrow(), initialResults.getData(timestamp).orElseThrow(), initialResults.getData(timestamp).orElseThrow(), globalLinearOptimizationResult.getFlowResult(timestamp), globalLinearOptimizationResult.getSensitivityResult(timestamp), globalLinearOptimizationResult.getRangeActionActivationResult(timestamp), topologicalOptimizationResults.getData(timestamp).orElseThrow(), raoParameters)));
        return new TemporalDataImpl<>(postOptimizationResults);
    }

    public static <T> T getDataFromState(TemporalData<T> temporalData, State state) {
        return temporalData.getData(state.getTimestamp().orElseThrow()).orElseThrow();
    }

    /**
     * This function combines computation statuses from a Temporal Data.
     * If any <T> has ComputationStatus.FAILURE, return ComputationStatus.FAILURE
     * Else, if any <T> has  ComputationStatus.PARTIAL_FAILURE, return ComputationStatus.PARTIAL_FAILURE
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
}
