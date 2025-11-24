/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.reports.SearchTreeReports;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaximumNumberOfTsosFilter implements NetworkActionCombinationFilter {
    private final int maxTso;

    public MaximumNumberOfTsosFilter(int maxTso) {
        this.maxTso = maxTso;
    }

    @Override
    public Set<NetworkActionCombination> filter(final Set<NetworkActionCombination> naCombinations,
                                                final OptimizationResult optimizationResult,
                                                final ReportNode reportNode) {

        Set<String> alreadyActivatedTsos = getTsosWithActivatedNetworkActions(optimizationResult);
        Set<NetworkActionCombination> filteredNaCombinations = new HashSet<>();
        for (NetworkActionCombination naCombination : naCombinations) {
            if (!exceedMaxNumberOfTsos(naCombination, alreadyActivatedTsos)) {
                filteredNaCombinations.add(naCombination);
            }
        }

        if (naCombinations.size() > filteredNaCombinations.size()) {
            SearchTreeReports.reportNetworkActionCombinationsFilteredOutMaxUsableTsosReached(reportNode, naCombinations.size() - filteredNaCombinations.size());
        }

        return filteredNaCombinations;
    }

    Set<String> getTsosWithActivatedNetworkActions(OptimizationResult optimizationResult) {
        return optimizationResult.getActivatedNetworkActions().stream().map(RemedialAction::getOperator).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private boolean exceedMaxNumberOfTsos(NetworkActionCombination naCombination, Set<String> alreadyActivatedTsos) {
        Set<String> involvedTsos = naCombination.getOperators();
        involvedTsos.addAll(alreadyActivatedTsos);
        return involvedTsos.size() > maxTso;
    }
}
