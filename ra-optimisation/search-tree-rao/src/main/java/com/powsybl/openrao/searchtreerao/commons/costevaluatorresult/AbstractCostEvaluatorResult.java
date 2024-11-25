/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.DoubleStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractCostEvaluatorResult implements CostEvaluatorResult {
    private final Map<State, Double> costPerState;
    private final List<FlowCnec> costlyElements;
    protected final double defaultCost;

    protected AbstractCostEvaluatorResult(Map<State, Double> costPerState, List<FlowCnec> costlyElements, double defaultCost) {
        this.costPerState = costPerState;
        this.costlyElements = costlyElements;
        this.defaultCost = defaultCost;
    }

    private Optional<Double> getPreventiveCost() {
        return costPerState.entrySet().stream().filter(entry -> entry.getKey().getContingency().isEmpty()).map(Map.Entry::getValue).findFirst();
    }

    private DoubleStream getPostContingencyCosts(Set<String> contingenciesToExclude) {
        return costPerState.entrySet().stream().filter(entry -> entry.getKey().getContingency().isPresent() && !contingenciesToExclude.contains(entry.getKey().getContingency().get().getId())).mapToDouble(Map.Entry::getValue);
    }

    @Override
    public double getCost(Set<String> contingenciesToExclude) {
        return evaluateResultsWithSpecificStrategy(getPreventiveCost().orElse(defaultCost), getPostContingencyCosts(contingenciesToExclude));
    }

    protected abstract double evaluateResultsWithSpecificStrategy(double preventiveCost, DoubleStream postContingencyCosts);

    @Override
    public List<FlowCnec> getCostlyElements(Set<String> contingenciesToExclude) {
        return costlyElements.stream().filter(flowCnec -> flowCnec.getState().getContingency().isEmpty() || flowCnec.getState().getContingency().isPresent() && !contingenciesToExclude.contains(flowCnec.getState().getContingency().get().getId())).toList();
    }
}
