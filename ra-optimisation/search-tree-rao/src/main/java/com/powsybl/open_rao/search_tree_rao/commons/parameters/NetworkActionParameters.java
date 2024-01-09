/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.commons.parameters;

import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.rao_api.parameters.TopoOptimizationParameters;
import com.powsybl.open_rao.search_tree_rao.commons.NetworkActionCombination;

import java.util.*;

import static com.powsybl.open_rao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionParameters {

    private final List<NetworkActionCombination> predefinedCombinations;

    private final double absoluteNetworkActionMinimumImpactThreshold;
    private final double relativeNetworkActionMinimumImpactThreshold;

    private final boolean skipNetworkActionFarFromMostLimitingElements;
    private final int maxNumberOfBoundariesForSkippingNetworkActions;

    public NetworkActionParameters(List<NetworkActionCombination> predefinedCombinations,
                                   double absoluteNetworkActionMinimumImpactThreshold,
                                   double relativeNetworkActionMinimumImpactThreshold,
                                   boolean skipNetworkActionFarFromMostLimitingElements,
                                   int maxNumberOfBoundariesForSkippingNetworkActions) {
        this.predefinedCombinations = predefinedCombinations;
        this.absoluteNetworkActionMinimumImpactThreshold = absoluteNetworkActionMinimumImpactThreshold;
        this.relativeNetworkActionMinimumImpactThreshold = relativeNetworkActionMinimumImpactThreshold;
        this.skipNetworkActionFarFromMostLimitingElements = skipNetworkActionFarFromMostLimitingElements;
        this.maxNumberOfBoundariesForSkippingNetworkActions = maxNumberOfBoundariesForSkippingNetworkActions;
    }

    public List<NetworkActionCombination> getNetworkActionCombinations() {
        return predefinedCombinations;
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

    public static NetworkActionParameters buildFromRaoParameters(TopoOptimizationParameters topoOptimizationParameters, Crac crac) {
        return new NetworkActionParameters(computePredefinedCombinations(crac, topoOptimizationParameters),
                topoOptimizationParameters.getAbsoluteMinImpactThreshold(),
                topoOptimizationParameters.getRelativeMinImpactThreshold(),
                topoOptimizationParameters.getSkipActionsFarFromMostLimitingElement(),
                topoOptimizationParameters.getMaxNumberOfBoundariesForSkippingActions());
    }

    public void addNetworkActionCombination(NetworkActionCombination networkActionCombination) {
        // It may happen that the 1st preventive RAO finds an optimal combination that was already defined
        // In this case, remove the old combination and add the new one (marked "detected during RAO")
        Optional<NetworkActionCombination> alreadyExistingNetworkActionCombination = this.predefinedCombinations
                .stream().filter(naCombination -> naCombination.getNetworkActionSet().equals(networkActionCombination.getNetworkActionSet()))
                .findAny();
        alreadyExistingNetworkActionCombination.ifPresent(this.predefinedCombinations::remove);
        this.predefinedCombinations.add(networkActionCombination);
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
        return Double.compare(that.absoluteNetworkActionMinimumImpactThreshold, absoluteNetworkActionMinimumImpactThreshold) == 0 && Double.compare(that.relativeNetworkActionMinimumImpactThreshold, relativeNetworkActionMinimumImpactThreshold) == 0 && skipNetworkActionFarFromMostLimitingElements == that.skipNetworkActionFarFromMostLimitingElements && maxNumberOfBoundariesForSkippingNetworkActions == that.maxNumberOfBoundariesForSkippingNetworkActions && Objects.equals(predefinedCombinations, that.predefinedCombinations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predefinedCombinations, absoluteNetworkActionMinimumImpactThreshold, relativeNetworkActionMinimumImpactThreshold, skipNetworkActionFarFromMostLimitingElements, maxNumberOfBoundariesForSkippingNetworkActions);
    }

    public static List<NetworkActionCombination> computePredefinedCombinations(Crac crac, TopoOptimizationParameters topoOptimizationParameters) {
        List<List<String>> predefinedCombinationsIds = topoOptimizationParameters.getPredefinedCombinations();
        List<NetworkActionCombination> computedPredefinedCombinations = new ArrayList<>();
        predefinedCombinationsIds.forEach(networkActionIds -> {
            Optional<NetworkActionCombination> optNaCombination = computePredefinedCombinationsFromIds(networkActionIds, crac);
            optNaCombination.ifPresent(computedPredefinedCombinations::add);
        });
        return computedPredefinedCombinations;
    }

    private static Optional<NetworkActionCombination> computePredefinedCombinationsFromIds(List<String> networkActionIds, Crac crac) {

        if (networkActionIds.size() < 2) {
            BUSINESS_WARNS.warn("A predefined combination should contain at least 2 NetworkAction ids");
            return Optional.empty();
        }

        Set<NetworkAction> networkActions = new HashSet<>();
        for (String naId : networkActionIds) {
            NetworkAction na = crac.getNetworkAction(naId);
            if (na == null) {
                BUSINESS_WARNS.warn("Unknown network action id in predefined-combinations parameter: {}", naId);
                return Optional.empty();
            }
            networkActions.add(na);
        }

        return Optional.of(new NetworkActionCombination(networkActions));
    }
}
