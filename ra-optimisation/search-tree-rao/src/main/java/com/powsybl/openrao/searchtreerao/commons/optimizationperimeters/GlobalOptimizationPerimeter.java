/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.iidm.network.Network;

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

    public static GlobalOptimizationPerimeter build(Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult, ReportNode reportNode) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs();
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        // add preventive network actions
        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, crac.getPreventiveState(), prePerimeterResult, flowCnecs, network, raoParameters, reportNode))
            .collect(Collectors.toSet());

        Map<State, Set<RangeAction<?>>> availableRangeActions = new HashMap<>();
        // add preventive range actions
        availableRangeActions.put(crac.getPreventiveState(), crac.getRangeActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, crac.getPreventiveState(), prePerimeterResult, flowCnecs, network, raoParameters, reportNode))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult, reportNode))
            .collect(Collectors.toSet()));

        //add curative range actions
        crac.getStates().stream()
            .filter(s -> s.getInstant().isCurative())
            .forEach(state -> {
                Set<RangeAction<?>> availableRaForState = crac.getRangeActions().stream()
                    .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, state, prePerimeterResult, flowCnecs, network, raoParameters, reportNode))
                    .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult, reportNode))
                    .collect(Collectors.toSet());
                if (!availableRaForState.isEmpty()) {
                    availableRangeActions.put(state, availableRaForState);
                }
            });

        availableRangeActions.values().forEach(rangeActions -> removeAlignedRangeActionsWithDifferentInitialSetpoints(rangeActions, prePerimeterResult, reportNode));

        return new GlobalOptimizationPerimeter(crac.getPreventiveState(),
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }
}
