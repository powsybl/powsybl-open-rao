/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class GlobalOptimizationPerimeter extends AbstractOptimizationPerimeter {

    public GlobalOptimizationPerimeter(State mainOptimizationState,
                                       Set<FlowCnec> flowCnecs,
                                       Set<FlowCnec> loopFlowCnecs,
                                       Set<NetworkAction> availableNetworkActions,
                                       Map<State, Set<RangeAction<?>>> availableRangeActions) {
        super(mainOptimizationState, flowCnecs, loopFlowCnecs, availableNetworkActions, availableRangeActions);
    }

    public static GlobalOptimizationPerimeter build(final Crac crac,
                                                    final Network network,
                                                    final RaoParameters raoParameters,
                                                    final PrePerimeterResult prePerimeterResult,
                                                    final ReportNode reportNode) {
        return build(crac, network, raoParameters, prePerimeterResult, crac.getPreventiveState(), crac.getStates(), reportNode);
    }

    /**
     * Builds a global perimeter restricted to the given states: only the CNECs and the remedial actions of these states
     * are considered, and the remedial actions of the main optimization state are the "preventive" ones.
     */
    public static GlobalOptimizationPerimeter build(final Crac crac,
                                                    final Network network,
                                                    final RaoParameters raoParameters,
                                                    final PrePerimeterResult prePerimeterResult,
                                                    final State mainOptimizationState,
                                                    final Set<State> statesInScope,
                                                    final ReportNode reportNode) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream().filter(cnec -> statesInScope.contains(cnec.getState())).collect(Collectors.toSet());
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        // add preventive network actions
        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(mainOptimizationState).stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, mainOptimizationState, prePerimeterResult, flowCnecs, network, raoParameters))
            .collect(Collectors.toSet());

        Map<State, Set<RangeAction<?>>> availableRangeActions = new HashMap<>();
        // add preventive range actions
        availableRangeActions.put(mainOptimizationState, crac.getRangeActions(mainOptimizationState).stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, mainOptimizationState, prePerimeterResult, flowCnecs, network, raoParameters))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult, reportNode))
            .collect(Collectors.toSet()));

        //add curative range actions
        crac.getStates().stream()
            .filter(s -> s.getInstant().isCurative() && statesInScope.contains(s) && !s.equals(mainOptimizationState))
            .forEach(state -> {
                Set<RangeAction<?>> availableRaForState = crac.getRangeActions(state).stream()
                    .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, state, prePerimeterResult, flowCnecs, network, raoParameters))
                    .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult, reportNode))
                    .collect(Collectors.toSet());
                if (!availableRaForState.isEmpty()) {
                    availableRangeActions.put(state, availableRaForState);
                }
            });

        availableRangeActions.values().forEach(rangeActions -> removeAlignedRangeActionsWithDifferentInitialSetpoints(rangeActions, prePerimeterResult, reportNode));

        return new GlobalOptimizationPerimeter(mainOptimizationState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }

    @Override
    public OptimizationPerimeter copyWithFilteredAvailableHvdcRangeAction(Network network) {
        return new GlobalOptimizationPerimeter(
            this.getMainOptimizationState(),
            this.getFlowCnecs(),
            this.getLoopFlowCnecs(),
            this.getNetworkActions(),
            this.getRangeActionsWithoutHvdcInAcEmulationPerState(network));
    }
}
