/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.action.Action;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.iidm.modification.NetworkModificationImpact;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.reports.SearchTreeReports;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class IneffectiveActionsFilter implements NetworkActionCombinationFilter {
    private final TemporalData<Network> networks;

    public IneffectiveActionsFilter(TemporalData<Network> networks) {
        this.networks = networks;
    }

    @Override
    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult, ReportNode reportNode) {
        Set<Set<Action>> elementaryActions = new HashSet<>();

        // iterate on combinations with increasing cost so that cheapest version of identical actions is kept
        List<NetworkActionCombination> combinationsPerIncreasingCost = naCombinations.stream()
            .sorted((naCombination1, naCombination2) ->
                Double.compare(getTotalCostOfCombination(naCombination1), getTotalCostOfCombination(naCombination2)))
            .toList();

        // filter out combinations that have no impact on the network or that are duplicate
        Set<NetworkActionCombination> filteredNaCombinations = combinationsPerIncreasingCost.stream()
            .filter(naCombination -> extractElementaryActionsWithImpact(naCombination, elementaryActions))
            .collect(Collectors.toSet());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            SearchTreeReports.reportNetworkActionCombinationsHasNoImpactOnNetwork(reportNode, naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    private double getTotalCostOfCombination(NetworkActionCombination networkActionCombination) {
        return networkActionCombination.getNetworkActionSet().stream()
            .mapToDouble(networkAction -> networkAction.getActivationCost().orElse(0.0)).sum();
    }

    /**
     * Indicate whether a network action combination has an impact on the network. The notion of impact is defined as follows:
     * <ul>
     *     <li>at least one of its elementary actions has an impact on the network;</li>
     *     <li>its total impact on the network, after filtering out the ineffective actions, is not identical to a cheaper combination's.</li>
     * </ul>
     * <p>
     * The {@code elementaryActions} set is filled progressively as we stream the network actions. Thus, it contains the sets of effective
     * elementary actions that belong to the network action combinations that have been processed previously.
     *
     * @param networkActionCombination : the network action combination to check
     * @param elementaryActions        : the set of elementary actions that have already been extracted from the network action combinations that have been processed before
     * @return {@code true} if the network action combination has an impact on the network, {@code false} otherwise
     */
    private boolean extractElementaryActionsWithImpact(NetworkActionCombination networkActionCombination, Set<Set<Action>> elementaryActions) {
        Set<Action> effectiveActions = new HashSet<>();

        for (NetworkAction networkAction : networkActionCombination.getNetworkActionSet()) {
            for (Action action : networkAction.getElementaryActions()) {
                if (hasImpactOnAtLeastOneNetwork(action)) {
                    effectiveActions.add(action);
                }
            }
        }

        if (effectiveActions.isEmpty() || elementaryActions.contains(effectiveActions)) {
            return false;
        }

        elementaryActions.add(effectiveActions);
        return true;
    }

    /**
     * Indicate whether an elementary action has an impact on at least one of the timestamps' networks.
     */
    private boolean hasImpactOnAtLeastOneNetwork(Action action) {
        NetworkModification networkModification = action.toModification();
        return networks.getDataPerTimestamp().values().stream()
            .anyMatch(network -> networkModification.hasImpactOnNetwork(network) == NetworkModificationImpact.HAS_IMPACT_ON_NETWORK);
    }
}
