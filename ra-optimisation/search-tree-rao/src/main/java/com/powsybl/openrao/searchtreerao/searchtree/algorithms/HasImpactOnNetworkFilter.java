/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.reports.SearchTreeReports;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class HasImpactOnNetworkFilter implements NetworkActionCombinationFilter {
    private final Network network;

    public HasImpactOnNetworkFilter(Network network) {
        this.network = network;
    }

    @Override
    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult, ReportNode reportNode) {
        Set<NetworkActionCombination> filteredNaCombinations = naCombinations.stream().filter(this::hasImpactOnNetwork).collect(Collectors.toSet());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            SearchTreeReports.reportNetworkActionCombinationsHasNoImpactOnFilter(reportNode, naCombinations.size() - filteredNaCombinations.size());
        }
        return filteredNaCombinations;
    }

    private boolean hasImpactOnNetwork(NetworkActionCombination networkActionCombination) {
        return networkActionCombination.getNetworkActionSet().stream().anyMatch(networkAction -> networkAction.hasImpactOnNetwork(network));
    }
}
