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
public class ElementaryActionsCompatibilityFilter implements NetworkActionCombinationFilter {
    @Override
    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        return naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream()
                // all network actions from naCombinations have to be compatible with all activated network actions in the optimization result
                .allMatch(networkAction -> optimizationResult.getActivatedNetworkActions().stream().allMatch(networkAction::isCompatibleWith)))
            .collect(Collectors.toSet());
    }
}
