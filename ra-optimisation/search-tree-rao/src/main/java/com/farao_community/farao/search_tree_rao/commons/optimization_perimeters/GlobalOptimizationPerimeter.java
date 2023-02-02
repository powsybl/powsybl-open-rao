/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
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

    public static GlobalOptimizationPerimeter build(Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs();
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        // add preventive network actions
        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, crac.getPreventiveState(), prePerimeterResult, flowCnecs, network, raoParameters.getObjectiveFunction().getUnit()))
            .collect(Collectors.toSet());

        Map<State, Set<RangeAction<?>>> availableRangeActions = new HashMap<>();
        // add preventive range actions
        availableRangeActions.put(crac.getPreventiveState(), crac.getRangeActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, crac.getPreventiveState(), prePerimeterResult, flowCnecs, network, raoParameters.getObjectiveFunction().getUnit()))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult))
            .collect(Collectors.toSet()));

        //add curative range actions
        crac.getStates().stream()
            .filter(s -> s.getInstant().equals(Instant.CURATIVE))
            .forEach(state -> {
                Set<RangeAction<?>> availableRaForState = crac.getRangeActions().stream()
                    .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, state, prePerimeterResult, flowCnecs, network, raoParameters.getObjectiveFunction().getUnit()))
                    .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult))
                    .collect(Collectors.toSet());
                if (!availableRaForState.isEmpty()) {
                    availableRangeActions.put(state, availableRaForState);
                }
            });

        availableRangeActions.values().forEach(rangeActions -> removeAlignedRangeActionsWithDifferentInitialSetpoints(rangeActions, prePerimeterResult));

        return new GlobalOptimizationPerimeter(crac.getPreventiveState(),
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }
}
