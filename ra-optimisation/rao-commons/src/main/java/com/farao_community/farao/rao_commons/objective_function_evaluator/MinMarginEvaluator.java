/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_commons.result_api.FlowResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinMarginEvaluator implements CostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final MarginEvaluator marginEvaluator;

    public MinMarginEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
    }

    @Override
    public String getName() {
        return "min-margin-evaluator";
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public List<FlowCnec> getCostlyElements(FlowResult flowResult, int numberOfElements) {
        Map<FlowCnec, Double> margins = new HashMap<>();
        flowCnecs.stream().filter(Cnec::isOptimized).forEach(flowCnec -> margins.put(flowCnec, marginEvaluator.getMargin(flowResult, flowCnec, unit)));
        
        List<FlowCnec> sortedElements = flowCnecs.stream()
            .filter(Cnec::isOptimized)
            .sorted(Comparator.comparing(margins::get))
            .collect(Collectors.toList());

        return sortedElements.subList(0, Math.min(sortedElements.size(), numberOfElements));
    }

    public FlowCnec getMostLimitingElement(FlowResult flowResult) {
        List<FlowCnec> costlyElements = getCostlyElements(flowResult, 1);
        if (costlyElements.isEmpty()) {
            return null;
        }
        return costlyElements.get(0);
    }

    @Override
    public double computeCost(FlowResult flowResult, ComputationStatus sensitivityStatus) {
        FlowCnec limitingElement = getMostLimitingElement(flowResult);
        if (limitingElement == null) {
            return 0;
        }
        return -marginEvaluator.getMargin(flowResult, limitingElement, unit);
    }
}
