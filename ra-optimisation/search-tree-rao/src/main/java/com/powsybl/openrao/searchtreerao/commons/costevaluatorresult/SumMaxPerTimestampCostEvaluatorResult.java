/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.google.common.util.concurrent.AtomicDouble;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class SumMaxPerTimestampCostEvaluatorResult implements CostEvaluatorResult {
    private final List<FlowCnec> costlyElements;
    private final Map<FlowCnec, Double> marginPerCnec;
    private final boolean capAtZero;
    private static final double COST_LIMIT = 1e9;

    public SumMaxPerTimestampCostEvaluatorResult(Map<FlowCnec, Double> marginPerCnec, List<FlowCnec> costlyElements, boolean capAtZero) {
        this.marginPerCnec = marginPerCnec;
        this.costlyElements = costlyElements;
        this.capAtZero = capAtZero;
    }

    /*
     * For virtual costs, capAtZero is set to true. This allows us to ensure that the virtual cost is always positive for each timestamp.
     * When no "real" value can be returned (either because no cnecs are present or they have all been filtered out),
     *  we need to return a value that is smaller than other costs (in case we take the max of these costs for multiple states).
     *  However we can not return -inf because we need the other costs to still have an impact on the global objective function
     *  when we are adding costs together (for instance a functional cost plus a virtual cost).
     *  In such case we therefore return -1e9, which should be negative enough compared to realistic margins, but
     */
    @Override
    public double getCost(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {
        // Exclude cnecs and contingencies
        Map<FlowCnec, Double> filteredCnecs = marginPerCnec.entrySet().stream()
            .filter(entry -> !cnecsToExclude.contains(entry.getKey().getId()))
            .filter(entry -> statesContingencyMustBeKept(entry.getKey().getState(), contingenciesToExclude))
            .filter(entry -> entry.getKey().isOptimized())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (filteredCnecs.isEmpty()) {
            return capAtZero ? 0. : -COST_LIMIT;
        }

        // Compute max cost (= -margin) per timestamp
        Map<OffsetDateTime, Double> maxCostPerTimestamp = new HashMap<>();
        AtomicBoolean stateWithoutTimestampIsPresent = new AtomicBoolean(false);
        AtomicDouble maxCostWithoutTimestamp = new AtomicDouble(-COST_LIMIT);
        filteredCnecs.forEach((flowCnec, margin) -> {
            //FIXME: check why NaN values can be present
            if (Double.isNaN(margin)) {
                return;
            }
            if (flowCnec.getState().getTimestamp().isPresent()) {
                maxCostPerTimestamp.merge(flowCnec.getState().getTimestamp().get(), -margin, Math::max);
            } else {
                stateWithoutTimestampIsPresent.set(true);
                maxCostWithoutTimestamp.set(Math.max(maxCostWithoutTimestamp.get(), -margin));
            }
        });

        // Compute total cost by summing over timestamps
        double totalCost = maxCostPerTimestamp.values().stream().mapToDouble(d -> capAtZero ? Math.max(0., d) : d).sum();
        if (stateWithoutTimestampIsPresent.get()) {
            totalCost += capAtZero ? Math.max(0., maxCostWithoutTimestamp.get()) : maxCostWithoutTimestamp.get();
        }
        return totalCost;
    }

    @Override
    public List<FlowCnec> getCostlyElements(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {
        return costlyElements.stream()
            .filter(flowCnec -> !cnecsToExclude.contains(flowCnec.getId()))
            .filter(flowCnec -> statesContingencyMustBeKept(flowCnec.getState(), contingenciesToExclude))
            .toList();
    }

    private static boolean statesContingencyMustBeKept(State state, Set<String> contingenciesToExclude) {
        Optional<Contingency> contingency = state.getContingency();
        return contingency.isEmpty() || !contingenciesToExclude.contains(contingency.get().getId());
    }

}
