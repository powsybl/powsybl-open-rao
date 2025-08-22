/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class OnFlowConstraintFilter extends AbstractNetworkActionCombinationFilter {
    private final State state;
    private final Set<FlowCnec> flowCnecs;
    private final Network network;
    private final Unit unit;

    public OnFlowConstraintFilter(State state, Set<FlowCnec> flowCnecs, Network network, Unit unit) {
        super("at least one of their network actions is unavailable");
        this.state = state;
        this.flowCnecs = flowCnecs;
        this.network = network;
        this.unit = unit;
    }

    @Override
    public Set<NetworkActionCombination> filterOutCombinations(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        Set<FlowCnec> overloadedCnecs = RaoUtil.getOverloadedCnecs(flowCnecs, optimizationResult, unit);
        Map<Country, Set<FlowCnec>> overloadedCnecsPerCountry = RaoUtil.getOverloadedCnecsPerCountry(overloadedCnecs, network);
        return naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream()
                .allMatch(networkAction -> RaoUtil.isRemedialActionAvailable(networkAction, state, overloadedCnecs, overloadedCnecsPerCountry)))
            .collect(Collectors.toSet());
    }
}
