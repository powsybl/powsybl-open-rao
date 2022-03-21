/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.optimization_contexts;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.algorithm.BasecaseScenario;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PreventiveOptimizationPerimeter extends AbstractOptimizationPerimeter {

    public PreventiveOptimizationPerimeter(State preventiveState,
                                           Set<FlowCnec> flowCnecs,
                                           Set<FlowCnec> looopFlowCnecs,
                                           Set<NetworkAction> availableNetworkActions,
                                           Set<RangeAction<?>> availableRangeActions) {

        super(preventiveState, flowCnecs, looopFlowCnecs, availableNetworkActions, Map.of(preventiveState, availableRangeActions));

        if (!preventiveState.isPreventive()) {
            throw new FaraoException("a PreventiveOptimizationContext must be based on the preventive state");
        }
    }

    public static PreventiveOptimizationPerimeter buildFullPreventivePerimeter(BasecaseScenario basecaseScenario, Crac crac, Network network, RaoParameters raoParameters, FlowResult prePerimeterFlowResult) {
        return buildForStates(basecaseScenario.getBasecaseState(), basecaseScenario.getAllStates(), crac, network, raoParameters, prePerimeterFlowResult);
    }


    public static PreventiveOptimizationPerimeter buildWithPreventiveCnecsOnly(State preventiveState, Crac crac, Network network, RaoParameters raoParameters, FlowResult prePerimeterFlowResult) {
        return buildForStates(preventiveState, Collections.singleton(preventiveState), crac, network, raoParameters, prePerimeterFlowResult);
    }


    private static PreventiveOptimizationPerimeter buildForStates(State preventiveState, Set<State> allMonitoredStates, Crac crac, Network network, RaoParameters raoParameters, FlowResult prePerimeterFlowResult) {

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> allMonitoredStates.contains(flowCnec.getState()))
            .collect(Collectors.toSet());

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, preventiveState, prePerimeterFlowResult))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = crac.getRangeActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, preventiveState, prePerimeterFlowResult))
            .collect(Collectors.toSet());

        return new PreventiveOptimizationPerimeter(preventiveState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }
}
