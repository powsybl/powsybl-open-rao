/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;

import java.util.List;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionParameters {

    private final List<NetworkActionCombination> networkActionCombinations;

    private final double absoluteNetworkActionMinimumImpactThreshold;
    private final double relativeNetworkActionMinimumImpactThreshold;

    private final boolean skipNetworkActionFarFromMostLimitingElements;
    private final int maxNumberOfBoundariesForSkippingNetworkActions;

    public NetworkActionParameters(List<NetworkActionCombination> networkActionCombinations,
                                   double absoluteNetworkActionMinimumImpactThreshold,
                                   double relativeNetworkActionMinimumImpactThreshold,
                                   boolean skipNetworkActionFarFromMostLimitingElements,
                                   int maxNumberOfBoundariesForSkippingNetworkActions) {
        this.networkActionCombinations = networkActionCombinations;
        this.absoluteNetworkActionMinimumImpactThreshold = absoluteNetworkActionMinimumImpactThreshold;
        this.relativeNetworkActionMinimumImpactThreshold = relativeNetworkActionMinimumImpactThreshold;
        this.skipNetworkActionFarFromMostLimitingElements = skipNetworkActionFarFromMostLimitingElements;
        this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
    }

    public List<NetworkActionCombination> getNetworkActionCombinations() {
        return networkActionCombinations;
    }

    public double getAbsoluteNetworkActionMinimumImpactThreshold() {
        return absoluteNetworkActionMinimumImpactThreshold;
    }

    public double getRelativeNetworkActionMinimumImpactThreshold() {
        return relativeNetworkActionMinimumImpactThreshold;
    }

    public boolean skipNetworkActionFarFromMostLimitingElements() {
        return skipNetworkActionFarFromMostLimitingElements;
    }

    public int getMaxNumberOfBoundariesForSkippingNetworkActions() {
        return maxNumberOfBoundariesForSkippingNetworkActions;
    }

    public static NetworkActionParameters buildFromRaoParameters(RaoParameters raoParameters, Crac crac) {

        /*
        for now, values of NetworkActionParameters are constant over all the SearchTreeRao
        they can therefore be instantiated directly from a RaoParameters
         */
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters == null) {
            throw new FaraoException("RaoParameters must contain SearchTreeRaoParameters when running a SearchTreeRao");
        }

        return new NetworkActionParameters(searchTreeRaoParameters.getNetworkActionCombinations(crac),
            searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold(),
            searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold(),
            searchTreeRaoParameters.getSkipNetworkActionsFarFromMostLimitingElement(),
            searchTreeRaoParameters.getMaxNumberOfBoundariesForSkippingNetworkActions());
    }

    public void addNetworkActionCombination(NetworkActionCombination networkActionCombination) {
        this.networkActionCombinations.add(networkActionCombination);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkActionParameters that = (NetworkActionParameters) o;
        return Double.compare(that.absoluteNetworkActionMinimumImpactThreshold, absoluteNetworkActionMinimumImpactThreshold) == 0 && Double.compare(that.relativeNetworkActionMinimumImpactThreshold, relativeNetworkActionMinimumImpactThreshold) == 0 && skipNetworkActionFarFromMostLimitingElements == that.skipNetworkActionFarFromMostLimitingElements && maxNumberOfBoundariesForSkippingNetworkActions == that.maxNumberOfBoundariesForSkippingNetworkActions && Objects.equals(networkActionCombinations, that.networkActionCombinations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkActionCombinations, absoluteNetworkActionMinimumImpactThreshold, relativeNetworkActionMinimumImpactThreshold, skipNetworkActionFarFromMostLimitingElements, maxNumberOfBoundariesForSkippingNetworkActions);
    }
}
