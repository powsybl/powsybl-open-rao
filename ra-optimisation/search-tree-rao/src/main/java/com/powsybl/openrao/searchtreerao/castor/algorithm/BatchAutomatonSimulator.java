/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class BatchAutomatonSimulator {
    private final Crac crac;
    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;

    public BatchAutomatonSimulator(Crac crac, RaoParameters raoParameters, ToolProvider toolProvider) {
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
    }

    public OptimizationResult simulate(Network network, State automatonState, PrePerimeterResult prePerimeterResult) {
        TECHNICAL_LOGS.info("Optimizing automaton state {}.", automatonState.getId());
        PrePerimeterSensitivityAnalysis preAutoPstOptimizationSensitivityAnalysis = getPreAutoPerimeterSensitivityAnalysis(automatonState, crac.getStates().stream().filter(state -> state.getInstant().isCurative() && automatonState.getContingency().equals(state.getContingency())).collect(Collectors.toSet()));
        PrePerimeterResult prePerimeterSensitivityOutput = preAutoPstOptimizationSensitivityAnalysis.runBasedOnInitialResults(network, crac, prePerimeterResult, Set.of(), new AppliedRemedialActions());

        // Sensitivity analysis failed :
        if (prePerimeterSensitivityOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
            return createFailedAutomatonPerimeterResult(automatonState, prePerimeterSensitivityOutput, new HashSet<>(), "before");
        }
        TECHNICAL_LOGS.info("Initial situation:");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, prePerimeterSensitivityOutput, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), 2);

        List<AutomatonBatch> automatonBatches = groupAutomatonsByBatches(crac, automatonState);
        OptimizationResult postBatchResult = getInitialOptimizationResult(prePerimeterResult);
        for (AutomatonBatch automatonBatch : automatonBatches) {
            postBatchResult = automatonBatch.simulate(network, automatonState, postBatchResult);
            if (isSecure(postBatchResult)) {
                return postBatchResult;
            }
        }
        return postBatchResult;
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

    AutomatonPerimeterResultImpl createFailedAutomatonPerimeterResult(State autoState, PrePerimeterResult result, Set<NetworkAction> activatedNetworkActions, String defineMoment) {
        AutomatonPerimeterResultImpl failedAutomatonPerimeterResultImpl = new AutomatonPerimeterResultImpl(
            result,
            result,
            activatedNetworkActions,
            new HashSet<>(),
            new HashMap<>(),
            autoState);
        TECHNICAL_LOGS.info("Automaton state {} has failed during sensitivity computation {} topological automaton simulation.", autoState.getId(), defineMoment);
        RaoLogger.logFailedOptimizationSummary(BUSINESS_LOGS, autoState, failedAutomatonPerimeterResultImpl.getActivatedNetworkActions(), getRangeActionsAndTheirTapsAppliedOnState(failedAutomatonPerimeterResultImpl, autoState));
        return failedAutomatonPerimeterResultImpl;
    }

    public static Map<RangeAction<?>, Double> getRangeActionsAndTheirTapsAppliedOnState(OptimizationResult optimizationResult, State state) {
        Set<RangeAction<?>> setActivatedRangeActions = optimizationResult.getActivatedRangeActions(state);
        Map<RangeAction<?>, Double> allRangeActions = new HashMap<>();
        setActivatedRangeActions.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).forEach(pstRangeAction -> allRangeActions.put(pstRangeAction, (double) optimizationResult.getOptimizedTap(pstRangeAction, state)));
        setActivatedRangeActions.stream().filter(ra -> !(ra instanceof PstRangeAction)).forEach(rangeAction -> allRangeActions.put(rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)));
        return allRangeActions;
    }

    private static OptimizationResult getInitialOptimizationResult(PrePerimeterResult prePerimeterResult) {
        NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(Set.of());
        RangeActionActivationResult rangeActionActivationResult = new RangeActionActivationResultImpl(prePerimeterResult);
        return new OptimizationResultImpl(prePerimeterResult, prePerimeterResult, prePerimeterResult, networkActionsResult, rangeActionActivationResult);
    }

    private static boolean isSecure(OptimizationResult postBatchResult) {
        List<FlowCnec> mostLimitingElements = postBatchResult.getMostLimitingElements(1);
        return mostLimitingElements.isEmpty() || postBatchResult.getMargin(mostLimitingElements.get(0), Unit.MEGAWATT) >= 0;
    }

    private static List<AutomatonBatch> groupAutomatonsByBatches(Crac crac, State automatonState) {
        Map<Integer, AutomatonBatch> automatonBatches = new HashMap<>();
        crac.getRangeActions(automatonState, UsageMethod.FORCED).forEach(
            automaton -> automatonBatches.computeIfAbsent(automaton.getSpeed().orElse(0), k -> new AutomatonBatch(automaton.getSpeed().orElse(0))).add(automaton)
        );
        return automatonBatches.values().stream().sorted().toList();
    }
}
