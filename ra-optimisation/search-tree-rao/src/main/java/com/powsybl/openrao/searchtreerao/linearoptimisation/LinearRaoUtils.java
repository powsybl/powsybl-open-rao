/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.AbstractOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.AbstractOptimizationPerimeter.removeAlignedRangeActionsWithDifferentInitialSetpoints;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class LinearRaoUtils {
    private LinearRaoUtils() {
    }

    public static PrePerimeterResult runInitialSensitivityAnalysis(Network network,
                                                                   Crac crac,
                                                                   ToolProvider toolProvider,
                                                                   RaoParameters raoParameters,
                                                                   ReportNode reportNode) {
        return new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true
        ).runInitialSensitivityAnalysis(network, reportNode);
    }

    public static IteratingLinearOptimizerInput buildOptimizerInput(RaoInput raoInput,
                                                                    OptimizationPerimeter optimizationPerimeter,
                                                                    PrePerimeterResult initialFlowResult,
                                                                    ToolProvider toolProvider,
                                                                    ObjectiveFunction globalObjectiveFunction) {
        PrePerimeterResult prePerimeterResult = initialFlowResult; // FIXME: use remedial actions from previous state
        PrePerimeterResult preOptimizationResult = initialFlowResult; // FIXME: use remedial actions from previous state and current state
        return IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optimizationPerimeter)
            .withInitialFlowResult(initialFlowResult)
            .withPrePerimeterFlowResult(prePerimeterResult)
            .withPrePerimeterSetpoints(prePerimeterResult)
            .withPreOptimizationFlowResult(preOptimizationResult)
            .withPreOptimizationSensitivityResult(preOptimizationResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(preOptimizationResult))
            .withAppliedNetworkActionsInPrimaryState(new NetworkActionsResultImpl(Map.of()))
            .withObjectiveFunction(globalObjectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(raoInput.getCrac().getOutageInstant())
            .build();
    }

    /**
     * Create the global optimization perimeter. There are different valid
     * scenarios:
     * <ul>
     *     <li>{@code states} is null or empty: all states will be considered</li>
     *     <li>
     *         {@code states} contains the preventive state: all provided states will be considered,
     *         with the preventive state being the main optimization state
     *     </li>
     *     <li>{@code states} only contains post-contingency states that all share the same contingency</li>
     * </ul>
     *
     * @param states : the set of states to be optimized / monitored by the RAO
     */
    static OptimizationPerimeter getOptimizationPerimeter(Set<State> states,
                                                                  Crac crac,
                                                                  Network network,
                                                                  PrePerimeterResult initialFlowResult,
                                                                  RaoParameters raoParameters,
                                                                  ReportNode reportNode) {
        Set<State> allStates = states == null || states.isEmpty() ? crac.getStates() : states;
        State mainOptimizationState = getMainOptimizationState(allStates, crac);
        Set<FlowCnec> flowCnecs = allStates.stream().map(crac::getFlowCnecs).flatMap(Set::stream).collect(Collectors.toSet());
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);
        Map<State, Set<RangeAction<?>>> rangeActions = gatherRangeActions(crac, allStates, initialFlowResult, flowCnecs, network, raoParameters, reportNode);
        return new GlobalOptimizationPerimeter(mainOptimizationState, flowCnecs, loopFlowCnecs, Set.of(), rangeActions);
    }

    private static State getMainOptimizationState(Set<State> states, Crac crac) {
        if (states.stream().anyMatch(state -> state.getInstant().isPreventive())) {
            return crac.getPreventiveState();
        } else if (states.stream().map(State::getContingency).collect(Collectors.toSet()).size() == 1) {
            return states.stream().filter(state -> !state.getInstant().isPreventive()).findFirst().orElseThrow();
        }
        throw new OpenRaoException("The preventive state is not part of the optimization and states from different contingencies shall be monitored. This is inconsistent.");
    }

    private static Map<State, Set<RangeAction<?>>> gatherRangeActions(Crac crac,
                                                                      Set<State> states,
                                                                      PrePerimeterResult prePerimeterResult,
                                                                      Set<FlowCnec> flowCnecs,
                                                                      Network network,
                                                                      RaoParameters raoParameters,
                                                                      ReportNode reportNode) {
        Map<State, Set<RangeAction<?>>> availableRangeActions = new HashMap<>();
        states.stream()
            .filter(state -> state.getInstant().isPreventive() || state.getInstant().isCurative())
            .forEach(state -> {
                    Set<RangeAction<?>> availableRaForState = crac.getRangeActions(state)
                        .stream()
                        .filter(rangeAction -> RaoUtil.canRemedialActionBeUsed(rangeAction, state, prePerimeterResult, flowCnecs, network, raoParameters))
                        .filter(rangeAction -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(rangeAction, prePerimeterResult, reportNode))
                        .collect(Collectors.toSet());
                    if (!availableRaForState.isEmpty()) {
                        availableRangeActions.put(state, availableRaForState);
                    }
                }
            );
        availableRangeActions.values().forEach(rangeActions -> removeAlignedRangeActionsWithDifferentInitialSetpoints(rangeActions, prePerimeterResult, reportNode));
        return availableRangeActions;
    }
}
