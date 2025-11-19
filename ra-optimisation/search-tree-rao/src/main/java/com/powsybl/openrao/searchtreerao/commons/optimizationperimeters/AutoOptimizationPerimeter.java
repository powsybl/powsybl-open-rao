/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AutoOptimizationPerimeter extends AbstractOptimizationPerimeter {
    public AutoOptimizationPerimeter(State automatonState, Set<FlowCnec> flowCnecs, Set<FlowCnec> loopFlowCnecs, Set<NetworkAction> availableNetworkActions) {
        // Only network ARA can be available
        super(automatonState, flowCnecs, loopFlowCnecs, availableNetworkActions, Map.of());
        if (!automatonState.getInstant().isAuto()) {
            throw new OpenRaoException("an AutoOptimizationPerimeter must be based on an auto state");
        }
    }

    public static AutoOptimizationPerimeter build(State automatonState, Crac crac, Network network, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(automatonState);
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(automatonState).stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, automatonState, prePerimeterResult, flowCnecs, network, raoParameters))
            .collect(Collectors.toSet());

        return new AutoOptimizationPerimeter(automatonState, flowCnecs, loopFlowCnecs, availableNetworkActions);
    }

    @Override
    public OptimizationPerimeter copyWithFilteredAvailableHvdcRangeAction(Network network) {
        return this;
    }
}
