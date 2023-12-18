/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.commons.optimization_perimeters;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import com.powsybl.open_rao.search_tree_rao.castor.algorithm.BasecaseScenario;
import com.powsybl.open_rao.search_tree_rao.commons.RaoUtil;
import com.powsybl.open_rao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PreventiveOptimizationPerimeter extends AbstractOptimizationPerimeter {

    public PreventiveOptimizationPerimeter(State preventiveState,
                                           Set<FlowCnec> flowCnecs,
                                           Set<FlowCnec> loopFlowCnecs,
                                           Set<NetworkAction> availableNetworkActions,
                                           Set<RangeAction<?>> availableRangeActions) {

        super(preventiveState, flowCnecs, loopFlowCnecs, availableNetworkActions, availableRangeActions.isEmpty() ? Collections.emptyMap() : Map.of(preventiveState, availableRangeActions));

        if (!preventiveState.isPreventive()) {
            throw new FaraoException("a PreventiveOptimizationContext must be based on the preventive state");
        }
    }

    public static PreventiveOptimizationPerimeter buildFromBasecaseScenario(BasecaseScenario basecaseScenario, Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {
        return buildForStates(basecaseScenario.getBasecaseState(), basecaseScenario.getAllStates(), crac, crac.getRangeActions(), network, raoParameters, prePerimeterResult);
    }

    public static PreventiveOptimizationPerimeter buildWithPreventiveCnecsOnly(Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {
        return buildForStates(crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), crac, crac.getRangeActions(), network, raoParameters, prePerimeterResult);
    }

    public static PreventiveOptimizationPerimeter buildWithAllCnecs(Crac crac, Set<RangeAction<?>> rangeActions, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {
        return buildForStates(crac.getPreventiveState(), crac.getStates(), crac, rangeActions, network, raoParameters, prePerimeterResult);
    }

    public static PreventiveOptimizationPerimeter buildForStates(State preventiveState, Set<State> allMonitoredStates, Crac crac, Set<RangeAction<?>> rangeActions, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {
        Set<State> filteredStates = allMonitoredStates.stream()
            .filter(state -> !prePerimeterResult.getSensitivityStatus(state).equals(ComputationStatus.FAILURE))
            .collect(Collectors.toSet());

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> filteredStates.contains(flowCnec.getState()))
            .collect(Collectors.toSet());

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> ra.isRemedialActionAvailable(preventiveState, RaoUtil.isAnyMarginNegative(prePerimeterResult, ra.getFlowCnecsConstrainingUsageRules(flowCnecs, network, preventiveState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = rangeActions.stream()
            .filter(ra -> ra.isRemedialActionAvailable(preventiveState, RaoUtil.isAnyMarginNegative(prePerimeterResult, ra.getFlowCnecsConstrainingUsageRules(flowCnecs, network, preventiveState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult))
            .collect(Collectors.toSet());
        removeAlignedRangeActionsWithDifferentInitialSetpoints(availableRangeActions, prePerimeterResult);

        return new PreventiveOptimizationPerimeter(preventiveState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }
}
