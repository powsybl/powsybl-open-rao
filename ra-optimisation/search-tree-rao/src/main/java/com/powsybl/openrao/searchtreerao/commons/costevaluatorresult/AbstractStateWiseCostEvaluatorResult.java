/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.DoubleStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractStateWiseCostEvaluatorResult implements CostEvaluatorResult {
    private final Map<State, Double> costPerState;
    private final List<FlowCnec> costlyElements;

    protected AbstractStateWiseCostEvaluatorResult(Map<State, Double> costPerState, List<FlowCnec> costlyElements) {
        this.costPerState = costPerState;
        this.costlyElements = costlyElements;
    }

    @Override
    public double getCost(Set<String> contingenciesToExclude) {
        DoubleStream filteredCosts = costPerState.entrySet().stream()
            .filter(entry -> statesContingencyMustBeKept(entry.getKey(), contingenciesToExclude))
            .mapToDouble(Map.Entry::getValue);
        return evaluateResultsWithSpecificStrategy(filteredCosts);
    }

    protected abstract double evaluateResultsWithSpecificStrategy(DoubleStream filteredCostsStream);

    @Override
    public List<FlowCnec> getCostlyElements(Set<String> contingenciesToExclude) {
        return costlyElements.stream().filter(flowCnec -> statesContingencyMustBeKept(flowCnec.getState(), contingenciesToExclude)).toList();
    }

    private static boolean statesContingencyMustBeKept(State state, Set<String> contingenciesToExclude) {
        Optional<Contingency> contingency = state.getContingency();
        return contingency.isEmpty() || !contingenciesToExclude.contains(contingency.get().getId());
    }
}
