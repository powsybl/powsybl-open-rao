/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.CountryGraph;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class FarFromMostLimitingElementFilter implements NetworkActionCombinationFilter {
    private final Network network;
    private final CountryGraph countryGraph;
    private final int maxNumberOfBoundariesForSkippingNetworkActions;

    public FarFromMostLimitingElementFilter(Network network, int maxNumberOfBoundariesForSkippingNetworkActions) {
        this.network = network;
        countryGraph = new CountryGraph(network);
        this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
    }

    /**
     * Removes network actions far from most limiting elements, using the user's parameters for activating/deactivating this
     * feature, and setting the number of boundaries allowed between the network action and the limiting element.
     * The most limiting elements are the most limiting functional cost element, and all elements with a non-zero virtual cost.
     */
    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        Set<Country> worstCnecLocation = getOptimizedMostLimitingElementsLocation(optimizationResult);

        Set<NetworkActionCombination> filteredNaCombinations = naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream().anyMatch(na -> isNetworkActionCloseToLocations(na, worstCnecLocation, countryGraph)))
            .collect(Collectors.toSet());

        if (naCombinations.size() > filteredNaCombinations.size()) {
            TECHNICAL_LOGS.info("{} network action combinations have been filtered out because they are too far from the most limiting element", naCombinations.size() - filteredNaCombinations.size());
        }
        return filteredNaCombinations;
    }

    Set<Country> getOptimizedMostLimitingElementsLocation(OptimizationResult optimizationResult) {
        Set<Country> locations = new HashSet<>();
        optimizationResult.getMostLimitingElements(1).forEach(element -> locations.addAll(element.getLocation(network)));
        for (String virtualCost : optimizationResult.getVirtualCostNames()) {
            optimizationResult.getCostlyElements(virtualCost, Integer.MAX_VALUE).forEach(element -> locations.addAll(element.getLocation(network)));
        }
        return locations;
    }

    /**
     * Says if a network action is close to a given set of countries, respecting the maximum number of boundaries
     */
    boolean isNetworkActionCloseToLocations(NetworkAction networkAction, Set<Country> locations, CountryGraph countryGraph) {
        if (locations.isEmpty()) {
            return true;
        }
        Set<Country> networkActionCountries = networkAction.getLocation(network);
        if (networkActionCountries.isEmpty()) {
            return true;
        }
        for (Country location : locations) {
            for (Country networkActionCountry : networkActionCountries) {
                if (countryGraph.areNeighbors(location, networkActionCountry, maxNumberOfBoundariesForSkippingNetworkActions)) {
                    return true;
                }
            }
        }
        return false;
    }
}
