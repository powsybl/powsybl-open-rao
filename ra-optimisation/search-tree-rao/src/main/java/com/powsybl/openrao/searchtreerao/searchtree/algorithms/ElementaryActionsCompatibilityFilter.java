/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class ElementaryActionsCompatibilityFilter extends AbstractNetworkActionCombinationFilter {
    protected ElementaryActionsCompatibilityFilter() {
        super("they are incompatible with some network actions that were already applied on the network");
    }

    @Override
    public Set<NetworkActionCombination> filterOutCombinations(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        return naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream().allMatch(networkAction -> optimizationResult.getActivatedNetworkActions().stream().allMatch(networkAction::isCompatibleWith)))
            .collect(Collectors.toSet());
    }
}
