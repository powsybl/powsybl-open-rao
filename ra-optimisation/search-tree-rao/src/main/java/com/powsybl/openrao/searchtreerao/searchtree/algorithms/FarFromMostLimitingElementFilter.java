/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.CountryGraph;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.reports.SearchTreeReports;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This filter gets rid of the network action combinations that are too far from the most limiting elements.
 * <p>
 * In a time-coupled search tree, the objective function is global and the most limiting elements can belong to any
 * timestamp. The filtering here is done by the worst elements across all the timestamps.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class FarFromMostLimitingElementFilter implements NetworkActionCombinationFilter {
    private final TemporalData<Network> networks;
    private final TemporalData<CountryGraph> countryGraphs;
    private final int maxNumberOfBoundariesForSkippingNetworkActions;

    public FarFromMostLimitingElementFilter(TemporalData<Network> networks, int maxNumberOfBoundariesForSkippingNetworkActions) {
        this.networks = networks;
        // one country graph per timestamp
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

        Set<NetworkActionCombination> filteredNaCombinations = naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream().anyMatch(na -> isNetworkActionCloseToLocations(na, worstCnecLocations, countryGraphs)))
            .collect(Collectors.toSet());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            SearchTreeReports.reportNetworkActionCombinationsFilteredOutTooFar(reportNode, naCombinations.size() - filteredNaCombinations.size());
        }
        return filteredNaCombinations;
    }

    Set<Country> getOptimizedMostLimitingElementsLocation(OptimizationResult optimizationResult) {
        Set<Country> locations = new HashSet<>();
        optimizationResult.getMostLimitingElements(1).forEach(element -> locations.addAll(element.getLocation(getNetworkOfCnec(element))));
        for (String virtualCost : optimizationResult.getVirtualCostNames()) {
            optimizationResult.getCostlyElements(virtualCost, Integer.MAX_VALUE).forEach(element -> locations.addAll(element.getLocation(getNetworkOfCnec(element))));
        }
        return locations;
    }

    /**
     * Says if a network action is close to a given set of countries, respecting the maximum number of boundaries
     */
    boolean isNetworkActionCloseToLocations(NetworkAction networkAction, Set<Country> locations, TemporalData<CountryGraph> countryGraphs) {
        if (locations.isEmpty()) {
            return true;
        }
        // first timestamp whose network knows the action + the countries the action is located in
        Optional<Map.Entry<OffsetDateTime, Set<Country>>> timestampAndActionCountries = networks.getTimestamps().stream()
            .map(timestamp -> Map.entry(timestamp, networkAction.getLocation(networks.getData(timestamp).orElseThrow())))
            .filter(entry -> !entry.getValue().isEmpty())
            .findFirst();
        if (timestampAndActionCountries.isEmpty()) {
            // if no network knows this action's elements, it is kept
            return true;
        }
        CountryGraph countryGraph = countryGraphs.getData(timestampAndActionCountries.get().getKey()).orElseThrow();
        Set<Country> networkActionCountries = timestampAndActionCountries.get().getValue();
        // the action is considered close enough when of its countries is within the allowed number of boundaries of one of the limiting elements' countries
        return locations.stream().anyMatch(
            location ->
                networkActionCountries.stream().anyMatch(
                    networkActionCountry -> countryGraph.areNeighbors(location, networkActionCountry, maxNumberOfBoundariesForSkippingNetworkActions)
                )
        );
    }

    private Network getNetworkOfCnec(Cnec<?> cnec) {
        if (networks.getTimestamps().size() == 1) {
            return networks.getData(networks.getTimestamps().getFirst()).orElseThrow();
        }
        OffsetDateTime timestamp = cnec.getState().getTimestamp().orElseThrow(() -> new OpenRaoException("Cnec " + cnec.getId() + " has no timestamp"));
        return networks.getData(timestamp).orElseThrow();
    }
}
