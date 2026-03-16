/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.action.Action;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaximumNumberOfElementaryActionsFilter implements NetworkActionCombinationFilter {
    private final Map<String, Integer> maxElementaryActionsPerTso;

    public MaximumNumberOfElementaryActionsFilter(Map<String, Integer> maxElementaryActionsPerTso) {
        this.maxElementaryActionsPerTso = maxElementaryActionsPerTso;
    }

    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        Set<NetworkActionCombination> filteredNaCombinations = new HashSet<>();
        naCombinations.stream().forEach(networkActionCombination -> {
            // TODO: do the same for removePsts
            // we use a set of elementary actions in case some network actions share the same elementary action which should thus only be counted once
            Map<String, Set<Action>> elementaryActionsPerTso = new HashMap<>();
            networkActionCombination.getNetworkActionSet().forEach(networkAction -> elementaryActionsPerTso
                .computeIfAbsent(networkAction.getOperator(), e -> new HashSet<>())
                .addAll(networkAction.getElementaryActions())
            );
            if (networkActionCombination.getOperators().stream()
                .anyMatch(operator -> elementaryActionsPerTso.getOrDefault(operator, Set.of()).size() > maxElementaryActionsPerTso.getOrDefault(operator, Integer.MAX_VALUE))) {
                TECHNICAL_LOGS.info(
                    "{} network action combinations have been filtered out because the maximum number of elementary actions has been exceeded for one of its operators",
                    naCombinations.size() - filteredNaCombinations.size()
                );
            } else {
                filteredNaCombinations.add(networkActionCombination);
            }
        });
        return filteredNaCombinations;
    }
}
