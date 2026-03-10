/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
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
            throw new OpenRaoException("a PreventiveOptimizationContext must be based on the preventive state");
        }
    }

    public static PreventiveOptimizationPerimeter buildFromBasecaseScenario(final Perimeter preventivePerimeter, final Crac crac, final Network network, final RaoParameters raoParameters, final PrePerimeterResult prePerimeterResult, final ReportNode reportNode) {
        return buildForStates(preventivePerimeter.getRaOptimisationState(), preventivePerimeter.getAllStates(), crac, network, raoParameters, prePerimeterResult, reportNode);
    }

    public static PreventiveOptimizationPerimeter buildWithPreventiveCnecsOnly(final Crac crac, final Network network, final RaoParameters raoParameters, final PrePerimeterResult prePerimeterResult, final ReportNode reportNode) {
        return buildForStates(crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), crac, network, raoParameters, prePerimeterResult, reportNode);
    }

    public static PreventiveOptimizationPerimeter buildForStates(final State preventiveState, final Set<State> allMonitoredStates, final Crac crac, final Network network, final RaoParameters raoParameters, final PrePerimeterResult prePerimeterResult, final ReportNode reportNode) {
        return buildForStates(preventiveState, allMonitoredStates, crac, network, crac.getRangeActions(preventiveState), raoParameters, prePerimeterResult, reportNode);
    }

    public static PreventiveOptimizationPerimeter buildForStates(final State preventiveState, final Set<State> allMonitoredStates, final Crac crac, final Network network, final Set<RangeAction<?>> rangeActions, final RaoParameters raoParameters, final PrePerimeterResult prePerimeterResult, final ReportNode reportNode) {
        Set<State> filteredStates = allMonitoredStates.stream()
            .filter(state -> !prePerimeterResult.getSensitivityStatus(state).equals(ComputationStatus.FAILURE))
            .collect(Collectors.toSet());

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> filteredStates.contains(flowCnec.getState()))
            .collect(Collectors.toSet());

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(preventiveState).stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, preventiveState, prePerimeterResult, flowCnecs, network, raoParameters))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = rangeActions.stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, preventiveState, prePerimeterResult, flowCnecs, network, raoParameters))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult, reportNode))
            .collect(Collectors.toSet());
        removeAlignedRangeActionsWithDifferentInitialSetpoints(availableRangeActions, prePerimeterResult, reportNode);

        return new PreventiveOptimizationPerimeter(preventiveState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }

    @Override
    public OptimizationPerimeter copyWithFilteredAvailableHvdcRangeAction(Network network) {
        return new PreventiveOptimizationPerimeter(
            this.getMainOptimizationState(),
            this.getFlowCnecs(),
            this.getLoopFlowCnecs(),
            this.getNetworkActions(),
            this.getRangeActionsWithoutHvdcInAcEmulation(network));
    }
}
