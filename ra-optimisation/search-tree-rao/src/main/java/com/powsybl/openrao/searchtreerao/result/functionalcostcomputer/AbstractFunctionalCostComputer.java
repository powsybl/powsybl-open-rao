/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.functionalcostcomputer;

import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.Map;
import java.util.stream.DoubleStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractFunctionalCostComputer {
    protected final OptimizationResult optimizationResult;
    protected final Map<State, OptimizationResult> postContingencyResults;

    protected AbstractFunctionalCostComputer(OptimizationResult optimizationResult, Map<State, OptimizationResult> postContingencyResults) {
        this.optimizationResult = optimizationResult;
        this.postContingencyResults = postContingencyResults;
    }

    protected DoubleStream streamPostContingencyResultsBeforeInstant(Instant instant) {
        return postContingencyResults.entrySet().stream()
            .filter(entry -> !entry.getKey().getInstant().comesAfter(instant))
            .map(Map.Entry::getValue)
            .filter(AbstractFunctionalCostComputer::hasActualFunctionalCost)
            .mapToDouble(OptimizationResult::getFunctionalCost);
    }

    /**
     * Returns true if the perimeter has an actual functional cost, ie has CNECs
     * (as opposed to a perimeter with pure MNECs only)
     */
    private static boolean hasActualFunctionalCost(OptimizationResult perimeterResult) {
        return !perimeterResult.getMostLimitingElements(1).isEmpty();
    }

    public abstract double computeFunctionalCost(Instant instant);
}
