/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AlreadyTestedCombinationsFilter implements NetworkActionCombinationFilter {
    private final List<NetworkActionCombination> preDefinedNaCombinations;

    public AlreadyTestedCombinationsFilter(List<NetworkActionCombination> preDefinedNaCombinations) {
        this.preDefinedNaCombinations = preDefinedNaCombinations;
    }

    /**
     * Remove combinations which have already been tested in the previous depths of the SearchTree.
     * <p>
     * For instance, if the preDefined combination ra1+ra2 exists, and if ra1 has already been selected, there is
     * no need to bloom on ra2. If the remedial action ra2 was relevant, the combination ra1+ra2 would have been
     * already selected in the previous depths.
     */
    public Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult) {
        List<NetworkAction> alreadyTestedNetworkActions = new ArrayList<>();

        for (NetworkActionCombination preDefinedCombination : preDefinedNaCombinations) {
            if (preDefinedCombination.isDetectedDuringRao()) {
                continue;
            }

            // elements of the combination which have not been activated yet
            List<NetworkAction> notTestedNaInCombination = preDefinedCombination.getNetworkActionSet().stream()
                .filter(na -> !optimizationResult.getActivatedNetworkActions().contains(na))
                .toList();

            // if all the actions of the combinations have been selected but one, there is no need
            // to test that individual action anymore
            if (notTestedNaInCombination.size() == 1) {
                alreadyTestedNetworkActions.add(notTestedNaInCombination.getFirst());
            }
        }

        return naCombinations.stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().size() != 1
                || !alreadyTestedNetworkActions.contains(naCombination.getNetworkActionSet().iterator().next()))
            .collect(Collectors.toSet());
    }
}
