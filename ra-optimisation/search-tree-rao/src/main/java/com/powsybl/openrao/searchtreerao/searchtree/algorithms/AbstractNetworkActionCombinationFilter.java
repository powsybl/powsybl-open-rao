/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractNetworkActionCombinationFilter implements NetworkActionCombinationFilter {
    protected final String filteringReason;

    protected AbstractNetworkActionCombinationFilter(String filteringReason) {
        this.filteringReason = filteringReason;
    }

    @Override
    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        Set<NetworkActionCombination> filteredNaCombinations = filterOutCombinations(naCombinations, optimizationResult);
        int numberOfFilteredCombinations = naCombinations.size() - filteredNaCombinations.size();
        if (numberOfFilteredCombinations > 0) {
            logFilteringReason(numberOfFilteredCombinations);
        }
        return filteredNaCombinations;
    }

    protected abstract Set<NetworkActionCombination> filterOutCombinations(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult);

    private void logFilteringReason(int numberOfFilteredCombinations) {
        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because {}", numberOfFilteredCombinations, filteringReason);
    }
}
