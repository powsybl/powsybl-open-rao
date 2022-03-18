package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;

import java.util.List;

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
}
