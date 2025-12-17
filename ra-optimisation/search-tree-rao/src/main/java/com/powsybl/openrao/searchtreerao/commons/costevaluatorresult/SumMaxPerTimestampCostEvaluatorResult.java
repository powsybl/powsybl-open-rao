/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.google.common.util.concurrent.AtomicDouble;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.Unit;
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
    private final Unit unit;
    private double highestThreshold = Double.NaN;
    private final boolean capAtZero;

    public SumMaxPerTimestampCostEvaluatorResult(Map<FlowCnec, Double> marginPerCnec, List<FlowCnec> costlyElements, Unit unit, boolean capAtZero) {
        this.marginPerCnec = marginPerCnec;
        this.costlyElements = costlyElements;
        this.unit = unit;
        this.capAtZero = capAtZero;
    }

    @Override
    public double getCost(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {
        // exclude cnecs
        Map<FlowCnec, Double> filteredCnecs = marginPerCnec.entrySet().stream()
            .filter(entry -> !cnecsToExclude.contains(entry.getKey().getId()))
            .filter(entry -> statesContingencyMustBeKept(entry.getKey().getState(), contingenciesToExclude))
            .filter(entry -> entry.getKey().isOptimized())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (filteredCnecs.isEmpty()) {
            return capAtZero ? 0. : -Double.MAX_VALUE / 2;
        }

        // Compute cost per timestamp
        Map<OffsetDateTime, Double> maxCostPerTimestamp = new HashMap<>();
        AtomicBoolean stateWithoutTimestampIsPresent = new AtomicBoolean(false);
        AtomicDouble maxCostWithoutTimestamp = new AtomicDouble(-Double.MAX_VALUE / 2);
        filteredCnecs.forEach((flowCnec, margin) -> {
            if (flowCnec.getState().getTimestamp().isPresent()) {
                maxCostPerTimestamp.merge(flowCnec.getState().getTimestamp().get(), -margin, Math::max);
            } else {
                stateWithoutTimestampIsPresent.set(true);
                maxCostWithoutTimestamp.set(Math.max(maxCostWithoutTimestamp.get(), -margin));
            }
        });

        return maxCostPerTimestamp.values().stream().mapToDouble(d -> capAtZero ? Math.max(0., d) : d).sum()
            + (stateWithoutTimestampIsPresent.get() ? (capAtZero ? Math.max(0., maxCostWithoutTimestamp.get()) : maxCostWithoutTimestamp.get()) : 0);

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
