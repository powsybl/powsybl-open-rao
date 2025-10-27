/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaximumNumberOfRemedialActionPerTsoFilter implements NetworkActionCombinationFilter {
    private final Map<String, Integer> maxTopoPerTso;
    private final Map<String, Integer> maxRaPerTso;

    public MaximumNumberOfRemedialActionPerTsoFilter(Map<String, Integer> maxTopoPerTso, Map<String, Integer> maxRaPerTso) {
        this.maxTopoPerTso = maxTopoPerTso;
        this.maxRaPerTso = maxRaPerTso;
    }

    /**
     * For each network actions combination, we iterate on the TSOs that operate the network actions, and for each one of them, two checks are carried out:
     * <ol>
     *     <li>We ensure that the cumulated number of network actions in the combination and already applied network actions in the root leaf does not exceed the limit number of remedial actions that the TSO can apply so the applied network actions can be kept</li>
     *     <li>If so, we also need to ensure that the cumulated number of network actions (combination + root leaf) and range actions (root leaf) does not exceed the limit number of remedial actions that the TSO can apply, so we know whether keeping the TSO's network actions requires unapplying the TSO's range actions or not.</li>
     * </ol>
     * If the first condition is not met for at least one TSO, the combination is not kept. If the second condition is not met for at least one TSO, the combination is kept but the range actions will be unapplied for the next optimization.
     */
    @Override
    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        Set<NetworkActionCombination> filteredNaCombinations = new HashSet<>();
        Map<String, Integer> maxNaPerTso = getMaxNetworkActionPerTso(optimizationResult);
        for (NetworkActionCombination naCombination : naCombinations) {
            Set<String> operators = naCombination.getOperators();
            boolean naShouldBeKept = true;
            for (String tso : operators) {
                int naCombinationSize = (int) naCombination.getNetworkActionSet().stream().filter(networkAction -> tso.equals(networkAction.getOperator())).count();
                // The number of already applied network actions is taken in account in getMaxNetworkActionPerTso
                if (naCombinationSize > maxNaPerTso.getOrDefault(tso, Integer.MAX_VALUE)) {
                    naShouldBeKept = false;
                    break;
                }
            }
            if (naShouldBeKept) {
                filteredNaCombinations.add(naCombination);
            }
        }

        if (naCombinations.size() > filteredNaCombinations.size()) {
            TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the maximum number of network actions for their TSO has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    /**
     * This function computes the allowed number of network actions for each TSO, as the minimum between the given
     * parameter and the maximum number of RA reduced by the number of remedial actions already used
     */
    private Map<String, Integer> getMaxNetworkActionPerTso(OptimizationResult optimizationResult) {
        Map<String, Integer> updatedMaxTopoPerTso = new HashMap<>();

        // get set of all TSOs considered in the max number of RA limitation
        Set<String> tsos = new HashSet<>(maxRaPerTso.keySet());
        tsos.addAll(maxTopoPerTso.keySet());

        // get max number of network action which can still be activated, per Tso
        tsos.forEach(tso -> {
            int activatedTopoForTso = (int) optimizationResult.getActivatedNetworkActions().stream().filter(networkAction -> tso.equals(networkAction.getOperator())).count();

            int limitationDueToMaxRa = maxRaPerTso.getOrDefault(tso, Integer.MAX_VALUE) - activatedTopoForTso;
            int limitationDueToMaxTopo = maxTopoPerTso.getOrDefault(tso, Integer.MAX_VALUE) - activatedTopoForTso;

            updatedMaxTopoPerTso.put(tso, Math.min(limitationDueToMaxRa, limitationDueToMaxTopo));
        });
        return updatedMaxTopoPerTso;
    }
}
