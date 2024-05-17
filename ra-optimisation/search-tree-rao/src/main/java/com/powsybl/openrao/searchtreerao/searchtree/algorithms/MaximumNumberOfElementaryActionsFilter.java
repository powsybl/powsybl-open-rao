/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaximumNumberOfElementaryActionsFilter implements NetworkActionCombinationFilter {
    private final Map<String, Integer> maxElementaryActionsPerTso;

    public MaximumNumberOfElementaryActionsFilter(Map<String, Integer> maxElementaryActionsPerTso) {
        this.maxElementaryActionsPerTso = maxElementaryActionsPerTso;
    }

    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        Set<NetworkActionCombination> filteredNaCombinations = new HashSet<>();
        // TODO : pst injection = 1 ea or abs(newtap - initialtap)?
        naCombinations.stream().forEach(networkActionCombination -> {
            int elementaryActions = 0;
            for (NetworkAction networkAction : networkActionCombination.getNetworkActionSet()) {
                // TODO: what if some network actions share common elementary action?
                elementaryActions = elementaryActions + networkAction.getElementaryActions().size();
            }
            // TODO: will require elementaryActionsPerTso instead (create test)
            for (String operator : networkActionCombination.getOperators()) {
                // The network action combination alone has more elementary action than the accepted limit, so it must be removed
                if (elementaryActions > maxElementaryActionsPerTso.getOrDefault(operator, Integer.MAX_VALUE)) {
                    TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the maximum number of elementary actions for TSO {} has been reached", naCombinations.size() - filteredNaCombinations.size(), operator);
                } else {
                    filteredNaCombinations.add(networkActionCombination);
                }
            }
        });
        return filteredNaCombinations;
    }
}
