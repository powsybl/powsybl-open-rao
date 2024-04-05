/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaximumNumberOfTsosFilter implements NetworkActionCombinationFilter {
    private final int maxTso;
    private final State optimizedStateForNetworkActions;

    public MaximumNumberOfTsosFilter(int maxTso, State optimizedStateForNetworkActions) {
        this.maxTso = maxTso;
        this.optimizedStateForNetworkActions = optimizedStateForNetworkActions;
    }

    public Map<NetworkActionCombination, Boolean> filter(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {

        Set<String> alreadyActivatedTsos = getTsosWithActivatedNetworkActions(fromLeaf);
        Map<NetworkActionCombination, Boolean> filteredNaCombinations = new HashMap<>();
        for (Map.Entry<NetworkActionCombination, Boolean> entry : naCombinations.entrySet()) {
            NetworkActionCombination naCombination = entry.getKey();
            if (!exceedMaxNumberOfTsos(naCombination, alreadyActivatedTsos)) {
                Set<String> alreadyActivatedTsosWithRangeActions = new HashSet<>(alreadyActivatedTsos);
                fromLeaf.getActivatedRangeActions(optimizedStateForNetworkActions)
                    .stream().map(RemedialAction::getOperator)
                    .filter(Objects::nonNull)
                    .forEach(alreadyActivatedTsosWithRangeActions::add);
                boolean removeRangeActions = exceedMaxNumberOfTsos(naCombination, alreadyActivatedTsosWithRangeActions);
                filteredNaCombinations.put(naCombination, removeRangeActions || naCombinations.get(naCombination));
            }
        }

        if (naCombinations.size() > filteredNaCombinations.size()) {
            TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the max number of usable TSOs has been reached", naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    Set<String> getTsosWithActivatedNetworkActions(Leaf leaf) {
        return leaf.getActivatedNetworkActions().stream().map(RemedialAction::getOperator).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private boolean exceedMaxNumberOfTsos(NetworkActionCombination naCombination, Set<String> alreadyActivatedTsos) {
        Set<String> involvedTsos = naCombination.getOperators();
        involvedTsos.addAll(alreadyActivatedTsos);
        return involvedTsos.size() > maxTso;
    }
}
