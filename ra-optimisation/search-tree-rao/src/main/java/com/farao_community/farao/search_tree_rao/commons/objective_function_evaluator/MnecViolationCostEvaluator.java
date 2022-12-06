/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.commons.parameters.MnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final FlowResult initialFlowResult;
    private final double mnecAcceptableMarginDiminution;
    private final double mnecViolationCost;
    private List<FlowCnec> sortedElements = new ArrayList<>();

    public MnecViolationCostEvaluator(Set<FlowCnec> flowCnecs, Unit unit, FlowResult initialFlowResult, MnecParameters mnecParameters) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.initialFlowResult = initialFlowResult;
        mnecAcceptableMarginDiminution = mnecParameters.getMnecAcceptableMarginDiminution();
        mnecViolationCost = mnecParameters.getMnecViolationCost();
    }

    @Override
    public String getName() {
        return "mnec-cost";
    }

    private double computeCost(FlowResult flowResult, FlowCnec mnec) {
        double initialMargin = initialFlowResult.getMargin(mnec, unit);
        double currentMargin = flowResult.getMargin(mnec, unit);
        return Math.max(0, Math.min(0, initialMargin - mnecAcceptableMarginDiminution) - currentMargin);
    }

    @Override
    public double computeCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        if (Math.abs(mnecViolationCost) < 1e-10) {
            return 0;
        }
        double totalMnecMarginViolation = 0;
        for (FlowCnec mnec : flowCnecs) {
            if (mnec.isMonitored()) {
                totalMnecMarginViolation += computeCost(flowResult, mnec);
            }
        }
        return mnecViolationCost * totalMnecMarginViolation;
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public List<FlowCnec> getCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, int numberOfElements) {
        if (sortedElements.isEmpty()) {
            sortedElements = flowCnecs.stream()
                    .filter(Cnec::isMonitored)
                    .collect(Collectors.toMap(
                        Function.identity(),
                        cnec -> computeCost(flowResult, cnec)
                    ))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() != 0)
                    .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        Collections.reverse(sortedElements);

        return sortedElements.subList(0, Math.min(sortedElements.size(), numberOfElements));
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }
}
