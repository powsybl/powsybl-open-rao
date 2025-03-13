/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.FlowCnecSorting;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluatorUtils.groupFlowCnecsPerState;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaxCostEvaluatorResult implements CostEvaluatorResult{

    private final List<FlowCnec> costlyElements;
    private final Map<FlowCnec, Double> marginPerCnec;
    private final Unit unit;

    public MaxCostEvaluatorResult(Map<FlowCnec, Double> marginPerCnec, List<FlowCnec> costlyElements, Unit unit) {
        this.marginPerCnec = marginPerCnec;
        this.costlyElements = costlyElements;
        this.unit = unit;
    }

    @Override
    public double getCost(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {

        Map<FlowCnec, Double> filteredCnecs = marginPerCnec.entrySet().stream()
            .filter(entry -> !cnecsToExclude.contains(entry.getKey().getId()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        Map<State, Set<FlowCnec>> flowCnecsPerState = groupFlowCnecsPerState(filteredCnecs.keySet());
        Map<State, Double> costPerState = flowCnecsPerState.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> computeCostForState(entry.getValue())));
        DoubleStream filteredCosts = costPerState.entrySet().stream()
            .filter(entry -> statesContingencyMustBeKept(entry.getKey(), contingenciesToExclude))
            .mapToDouble(Map.Entry::getValue);

        return evaluateResultsWithSpecificStrategy(filteredCosts);
    }

    private double getHighestThresholdAmongFlowCnecs() {
        return marginPerCnec.keySet().stream().map(this::getHighestThreshold).max(Double::compareTo).orElse(0.0);
    }

    private double getHighestThreshold(FlowCnec flowCnec) {
        return Math.max(
            Math.max(
                flowCnec.getUpperBound(TwoSides.ONE, unit).orElse(0.0),
                flowCnec.getUpperBound(TwoSides.TWO, unit).orElse(0.0)),
            Math.max(
                -flowCnec.getLowerBound(TwoSides.ONE, unit).orElse(0.0),
                -flowCnec.getLowerBound(TwoSides.TWO, unit).orElse(0.0)));
    }

    protected double computeCostForState(Set<FlowCnec> flowCnecsOfState) {
        List<FlowCnec> flowCnecsByMargin = FlowCnecSorting.sortByMargin(flowCnecsOfState, marginPerCnec);
        FlowCnec limitingElement;
        if (flowCnecsByMargin.isEmpty()) {
            limitingElement = null;
        } else {
            limitingElement = flowCnecsByMargin.get(0);
        }
        if (limitingElement == null) {
            // In case there is no limiting element (may happen in perimeters where only MNECs exist),
            // return a finite value, so that the virtual cost is not hidden by the functional cost
            // This finite value should only be equal to the highest possible margin, i.e. the highest cnec threshold
            return -getHighestThresholdAmongFlowCnecs();
        }
        double margin = marginPerCnec.get(limitingElement);
        if (margin >= Double.MAX_VALUE / 2) {
            // In case margin is infinite (may happen in perimeters where only unoptimized CNECs exist, none of which has seen its margin degraded),
            // return a finite value, like MNEC case above
            return -getHighestThresholdAmongFlowCnecs();
        }
        return -margin;
    }

    @Override
    public List<FlowCnec> getCostlyElements(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {
        return costlyElements.stream().filter(flowCnec -> !cnecsToExclude.contains(flowCnec.getId()))
            .filter(flowCnec -> statesContingencyMustBeKept(flowCnec.getState(), contingenciesToExclude)).toList();
    }

    private static boolean statesContingencyMustBeKept(State state, Set<String> contingenciesToExclude) {
        Optional<Contingency> contingency = state.getContingency();
        return contingency.isEmpty() || !contingenciesToExclude.contains(contingency.get().getId());
    }

    protected double evaluateResultsWithSpecificStrategy(DoubleStream filteredCostsStream) {
        return filteredCostsStream.max().orElse(-Double.MAX_VALUE);
    }
}
