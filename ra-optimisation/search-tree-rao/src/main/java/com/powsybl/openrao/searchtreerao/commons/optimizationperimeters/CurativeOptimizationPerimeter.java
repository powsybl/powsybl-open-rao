/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.iidm.network.Network;

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
                                         Set<NetworkAction> availableNetworkActions,
                                         Set<RangeAction<?>> availableRangeActions) {

        super(curativeState, flowCnecs, looopFlowCnecs, availableNetworkActions, Map.of(curativeState, availableRangeActions));

        if (!curativeState.getInstant().isCurative() && !curativeState.getInstant().isOutage()) {
            throw new OpenRaoException("a CurativeOptimizationContext must be based on a curative state");
        }
    }

    public static CurativeOptimizationPerimeter build(State curativeState, Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult, ReportNode reportNode) {

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(curativeState);
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, curativeState, prePerimeterResult, flowCnecs, network, raoParameters, reportNode))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = crac.getRangeActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, curativeState, prePerimeterResult, flowCnecs, network, raoParameters, reportNode))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult, reportNode))
            .collect(Collectors.toSet());
        removeAlignedRangeActionsWithDifferentInitialSetpoints(availableRangeActions, prePerimeterResult, reportNode);

        return new CurativeOptimizationPerimeter(curativeState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions
        );
    }

    public static CurativeOptimizationPerimeter buildForStates(State curativeState, Set<State> allMonitoredStates, Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult, ReportNode reportNode) {
        Set<RangeAction<?>> rangeActions = crac.getPotentiallyAvailableRangeActions(curativeState);

        Set<State> filteredStates = allMonitoredStates.stream()
            .filter(state -> !prePerimeterResult.getSensitivityStatus(state).equals(ComputationStatus.FAILURE))
            .collect(Collectors.toSet());

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> filteredStates.contains(flowCnec.getState()))
            .collect(Collectors.toSet());

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, curativeState, prePerimeterResult, flowCnecs, network, raoParameters, reportNode))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = rangeActions.stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, curativeState, prePerimeterResult, flowCnecs, network, raoParameters, reportNode))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult, reportNode))
            .collect(Collectors.toSet());
        removeAlignedRangeActionsWithDifferentInitialSetpoints(availableRangeActions, prePerimeterResult, reportNode);

        return new CurativeOptimizationPerimeter(
            curativeState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }
}
