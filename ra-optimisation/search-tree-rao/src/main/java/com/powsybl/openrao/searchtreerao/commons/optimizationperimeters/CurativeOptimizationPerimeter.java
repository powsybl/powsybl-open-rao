/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
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

    public static CurativeOptimizationPerimeter build(State curativeState, Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(curativeState);
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(curativeState).stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, curativeState, prePerimeterResult, flowCnecs, network, raoParameters))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = crac.getRangeActions(curativeState).stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, curativeState, prePerimeterResult, flowCnecs, network, raoParameters))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult))
            .collect(Collectors.toSet());
        removeAlignedRangeActionsWithDifferentInitialSetpoints(availableRangeActions, prePerimeterResult);

        return new CurativeOptimizationPerimeter(curativeState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }

    public static CurativeOptimizationPerimeter buildForStates(State curativeState, Set<State> allMonitoredStates, Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {
        Set<RangeAction<?>> rangeActions = crac.getRangeActions(curativeState);

        Set<State> filteredStates = allMonitoredStates.stream()
            .filter(state -> !prePerimeterResult.getSensitivityStatus(state).equals(ComputationStatus.FAILURE))
            .collect(Collectors.toSet());

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> filteredStates.contains(flowCnec.getState()))
            .collect(Collectors.toSet());

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(curativeState).stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, curativeState, prePerimeterResult, flowCnecs, network, raoParameters))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = rangeActions.stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, curativeState, prePerimeterResult, flowCnecs, network, raoParameters))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult))
            .collect(Collectors.toSet());
        removeAlignedRangeActionsWithDifferentInitialSetpoints(availableRangeActions, prePerimeterResult);

        return new CurativeOptimizationPerimeter(
            curativeState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }

    @Override
    public OptimizationPerimeter copyWithFilteredAvailableHvdcRangeAction(Network network) {
        return new CurativeOptimizationPerimeter(
            this.getMainOptimizationState(),
            this.getFlowCnecs(),
            this.getLoopFlowCnecs(),
            this.getNetworkActions(),
            this.getRangeActionsWithoutHvdcInAcEmulation(network));
    }
}
