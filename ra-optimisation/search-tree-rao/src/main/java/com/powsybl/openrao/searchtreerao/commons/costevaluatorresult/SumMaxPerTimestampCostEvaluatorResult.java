/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class SumMaxPerTimestampCostEvaluatorResult implements CostEvaluatorResult {
    private final Map<State, Double> costPerState;
    private final List<FlowCnec> costlyElements;

    public SumMaxPerTimestampCostEvaluatorResult(Map<State, Double> costPerState, List<FlowCnec> costlyElements) {
        this.costPerState = costPerState;
        this.costlyElements = costlyElements;
    }

    @Override
    public double getCost(Set<String> contingenciesToExclude) {
        Map<OffsetDateTime, Set<State>> statesToEvaluatePerTimestamp = new HashMap<>();
        Set<State> statesToEvaluateWithoutTimestamp = new HashSet<>();

        costPerState.keySet().stream().forEach(state -> {
            if (statesContingencyMustBeKept(state, contingenciesToExclude)) {
                Optional<OffsetDateTime> timestamp = state.getTimestamp();
                if (timestamp.isPresent()) {
                    statesToEvaluatePerTimestamp.computeIfAbsent(timestamp.get(), s -> new HashSet<>()).add(state);
                } else {
                    statesToEvaluateWithoutTimestamp.add(state);
                }
            }
        });

        return statesToEvaluatePerTimestamp.values().stream().mapToDouble(states -> states.stream().mapToDouble(state -> costPerState.get(state)).max().orElse(0)).sum()
            + statesToEvaluateWithoutTimestamp.stream().mapToDouble(state -> costPerState.get(state)).max().orElse(0);

    }

    @Override
    public List<FlowCnec> getCostlyElements(Set<String> contingenciesToExclude) {
        return costlyElements.stream().filter(flowCnec -> statesContingencyMustBeKept(flowCnec.getState(), contingenciesToExclude)).toList();
    }

    private static boolean statesContingencyMustBeKept(State state, Set<String> contingenciesToExclude) {
        Optional<Contingency> contingency = state.getContingency();
        return contingency.isEmpty() || !contingenciesToExclude.contains(contingency.get().getId());
    }

}
