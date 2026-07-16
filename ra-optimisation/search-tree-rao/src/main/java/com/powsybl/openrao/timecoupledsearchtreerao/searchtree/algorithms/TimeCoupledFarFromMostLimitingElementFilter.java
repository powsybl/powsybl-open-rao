/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.timecoupledsearchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.CountryGraph;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.timecoupledsearchtreerao.reports.SearchTreeReports;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.OptimizationResult;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p> Removes the network action combinations whose actions are all too far from the most limiting elements.</p>
 *
 * In time-coupled :
 * <li>With the global objective, the most limiting elements can belong to any timestamp, so each CNEC's location
 * is resolved on its own timestamp's network
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TimeCoupledFarFromMostLimitingElementFilter implements NetworkActionCombinationFilter {
    private final TemporalData<Network> networks;
    private final TemporalData<CountryGraph> countryGraphs;
    private final int maxNumberOfBoundariesForSkippingNetworkActions;

    public TimeCoupledFarFromMostLimitingElementFilter(TemporalData<Network> networks, int maxNumberOfBoundariesForSkippingNetworkActions) {
        this.networks = networks;
        // one country graph per timestamp, built on its own network
        countryGraphs = networks.map(CountryGraph::new);
        this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
    }

    /**
     * Removes network actions far from most limiting elements, using the user's parameters for activating/deactivating this
     * feature, and setting the number of boundaries allowed between the network action and the limiting element.
     * The most limiting elements are the most limiting functional cost element, and all elements with a non-zero virtual cost.
     */
    @Override
    public Set<NetworkActionCombination> filter(final Set<NetworkActionCombination> naCombinations,
                                                final OptimizationResult optimizationResult,
                                                final ReportNode reportNode) {
        Set<Country> worstCnecLocations = getOptimizedMostLimitingElementsLocation(optimizationResult);

        // a combination is kept as soon as one of its actions is close to the worst locations
        Set<NetworkActionCombination> filteredNaCombinations = naCombinations.stream()
                .filter(naCombination -> naCombination.getNetworkActionSet().stream().anyMatch(na -> isNetworkActionCloseToLocations(na, worstCnecLocations)))
                .collect(Collectors.toSet());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            SearchTreeReports.reportNetworkActionCombinationsFilteredOutTooFar(reportNode, naCombinations.size() - filteredNaCombinations.size());
        }
        return filteredNaCombinations;
    }

    /**
     * Gathers the locations of the most limiting elements -> with the global objective function these elements can belong to any timestamp,
     * so each element's location is resolved on its own timestamp's network.
     */
    Set<Country> getOptimizedMostLimitingElementsLocation(OptimizationResult optimizationResult) {
        Set<Country> locations = new HashSet<>();
        optimizationResult.getMostLimitingElements(1).forEach(element -> locations.addAll(element.getLocation(getNetworkOfCnec(element))));
        for (String virtualCost : optimizationResult.getVirtualCostNames()) {
            optimizationResult.getCostlyElements(virtualCost, Integer.MAX_VALUE).forEach(element -> locations.addAll(element.getLocation(getNetworkOfCnec(element))));
        }
        return locations;
    }

    /** gets the network of a CNEC's own timestamp. */
    private Network getNetworkOfCnec(Cnec<?> cnec) {
        OffsetDateTime timestamp = cnec.getState().getTimestamp().orElseThrow();
        return networks.getData(timestamp).orElseThrow();
    }

    /**
     * Says if a network action is close to a given set of countries, respecting the maximum number of boundaries.
     */
    boolean isNetworkActionCloseToLocations(NetworkAction networkAction, Set<Country> locations) {
        if (locations.isEmpty()) {
            return true;
        }
        // get the action's location on the first timestamp whose network knows its elements
        Optional<Map.Entry<OffsetDateTime, Set<Country>>> resolved = networks.getTimestamps().stream()
                .map(timestamp -> Map.entry(timestamp, networkAction.getLocation(networks.getData(timestamp).orElseThrow())))
                .filter(entry -> !entry.getValue().isEmpty())
                .findFirst();
        if (resolved.isEmpty()) {
            return true;
        }
        // the neighborhood is checked on the timestamp's own country graph
        CountryGraph countryGraph = countryGraphs.getData(resolved.get().getKey()).orElseThrow();
        Set<Country> networkActionCountries = resolved.get().getValue();
        return locations.stream().anyMatch(location ->
                networkActionCountries.stream().anyMatch(networkActionCountry ->
                        countryGraph.areNeighbors(location, networkActionCountry, maxNumberOfBoundariesForSkippingNetworkActions)));
    }
}
