/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.commons.parameters.MnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.apache.commons.lang3.tuple.Pair;

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
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        return computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, new HashSet<>());
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, Set<String> contingenciesToExclude) {
        if (Math.abs(mnecViolationCost) < 1e-10) {
            return Pair.of(0., new ArrayList<>());
        }
        double totalMnecMarginViolation = 0;
        List<FlowCnec> costlyElements = getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult);
        for (FlowCnec mnec : costlyElements) {
            Optional<Contingency> contingency = mnec.getState().getContingency();
            if (mnec.isMonitored() && (mnec.getState().getContingency().isEmpty() || contingency.isPresent() && !contingenciesToExclude.contains(contingency.get().getId()))) {
                totalMnecMarginViolation += computeCost(flowResult, mnec);
            }
        }
        return Pair.of(mnecViolationCost * totalMnecMarginViolation, costlyElements);
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    private List<FlowCnec> getCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult) {
        return getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, new HashSet<>());
    }

    private List<FlowCnec> getCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Set<String> contingenciesToExclude) {
        if (sortedElements.isEmpty()) {
            sortedElements = flowCnecs.stream()
                .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
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

        return sortedElements;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }
}
