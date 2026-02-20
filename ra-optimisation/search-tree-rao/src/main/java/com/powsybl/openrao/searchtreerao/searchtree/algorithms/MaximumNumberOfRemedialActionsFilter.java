/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.HashSet;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaximumNumberOfRemedialActionsFilter implements NetworkActionCombinationFilter {

    private final int maxRa;

    public MaximumNumberOfRemedialActionsFilter(int maxRa) {
        this.maxRa = maxRa;
    }

    /**
     * For each network actions combination, two checks are carried out:
     * <ol>
     *     <li>We ensure that the cumulated number of network actions in the combination and already applied network actions in the root leaf does not exceed the limit number of remedial actions so the applied network actions can be kept</li>
     *     <li>If so, we also need to ensure that the cumulated number of network actions (combination + root leaf) and range actions (root leaf) does not exceed the limit number of remedial actions, so we know whether keeping the network actions requires unapplying the range actions or not.</li>
     * </ol>
     * If the first condition is not met, the combination is not kept. If the second condition is not met, the combination is kept but the range actions will be unapplied for the next optimization.
     */
    @Override
    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        Set<NetworkActionCombination> filteredNaCombinations = new HashSet<>();
        for (NetworkActionCombination naCombination : naCombinations) {
            int naCombinationSize = naCombination.getNetworkActionSet().size();
            int alreadyActivatedNetworkActionsSize = optimizationResult.getActivatedNetworkActions().size();
            if (naCombinationSize + alreadyActivatedNetworkActionsSize <= maxRa) {
                filteredNaCombinations.add(naCombination);
            }
        }

        if (naCombinations.size() > filteredNaCombinations.size()) {
            TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the max number of usable RAs has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }
}
