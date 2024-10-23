/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class ActivationCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final MarginEvaluator marginEvaluator;
    private static final double MARGIN_PENALTY_COEFFICIENT = 1000;

    public ActivationCostEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
    }

    @Override
    public String getName() {
        return "activation-cost-evaluator";
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    private List<FlowCnec> getCostlyElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        Map<FlowCnec, Double> margins = new HashMap<>();

        flowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .filter(Cnec::isOptimized)
            .forEach(flowCnec -> margins.put(flowCnec, marginEvaluator.getMargin(flowResult, flowCnec, unit)));

        return margins.keySet().stream()
            .filter(Cnec::isOptimized)
            .sorted(Comparator.comparing(margins::get))
            .toList();
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> costlyElements = getCostlyElements(flowResult, contingenciesToExclude);
        FlowCnec limitingElement;
        if (costlyElements.isEmpty()) {
            limitingElement = null;
        } else {
            limitingElement = costlyElements.get(0);
        }
        // Cost is the activation cost of the range action
        // + need to add a huge penalty if not secure

        double activationCost = getTotalActivationCostFromRangeActions(rangeActionActivationResult);

        double margin = marginEvaluator.getMargin(flowResult, limitingElement, unit);
        if (margin < 0) {
            activationCost -= MARGIN_PENALTY_COEFFICIENT * margin;
        }
        return Pair.of(activationCost, costlyElements);
    }

    private double getTotalActivationCostFromRangeActions(RangeActionActivationResult rangeActionActivationResult) {
        AtomicReference<Double> totalActivationCost = new AtomicReference<>((double) 0);

        rangeActionActivationResult.getStatesPerRangeAction().forEach((rangeAction, states) -> {
            states.forEach(state -> {
                double absoluteVariation = Math.abs(rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state) - rangeActionActivationResult.getOptimizedSetpointOnStatePreceding(rangeAction, state));
                totalActivationCost.updateAndGet(v -> v + rangeAction.getActivationCost() * absoluteVariation);
            });
        });

        return totalActivationCost.get();
    }
}
