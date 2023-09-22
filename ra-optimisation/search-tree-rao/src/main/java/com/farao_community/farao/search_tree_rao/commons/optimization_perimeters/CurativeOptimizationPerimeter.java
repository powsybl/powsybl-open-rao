/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CurativeOptimizationPerimeter extends AbstractOptimizationPerimeter {

    public CurativeOptimizationPerimeter(State curativeState,
                                         Set<FlowCnec> flowCnecs,
                                         Set<FlowCnec> looopFlowCnecs,
                                         Set<FlowCnec> computedFlowCnecs,
                                         Set<NetworkAction> availableNetworkActions,
                                         Set<RangeAction<?>> availableRangeActions) {

        super(curativeState, flowCnecs, looopFlowCnecs, computedFlowCnecs, availableNetworkActions, Map.of(curativeState, availableRangeActions));

        if (!curativeState.getInstant().isCurative() && !curativeState.getInstant().isOutage()) {
            throw new FaraoException("a CurativeOptimizationContext must be based on a curative state");
        }
    }

    public static CurativeOptimizationPerimeter build(State curativeState, Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(curativeState);
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> ra.isRemedialActionAvailable(curativeState, RaoUtil.isAnyMarginNegative(prePerimeterResult, ra.getFlowCnecsConstrainingUsageRules(flowCnecs, network, curativeState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = crac.getRangeActions().stream()
            .filter(ra -> ra.isRemedialActionAvailable(curativeState, RaoUtil.isAnyMarginNegative(prePerimeterResult, ra.getFlowCnecsConstrainingUsageRules(flowCnecs, network, curativeState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult))
            .collect(Collectors.toSet());
        removeAlignedRangeActionsWithDifferentInitialSetpoints(availableRangeActions, prePerimeterResult);

        Set<FlowCnec> computedFlowCnecs = new HashSet<>();
        crac.getStates(curativeState.getContingency().orElseThrow())
            .stream().filter(state -> state.getInstant().comesAfter(curativeState.getInstant()))
            .forEach(state -> computedFlowCnecs.addAll(crac.getFlowCnecs(state)));

        return new CurativeOptimizationPerimeter(curativeState,
            flowCnecs,
            loopFlowCnecs,
            computedFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }
}
