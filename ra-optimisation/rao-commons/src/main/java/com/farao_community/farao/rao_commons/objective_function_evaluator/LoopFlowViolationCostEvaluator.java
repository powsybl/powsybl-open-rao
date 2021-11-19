/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.LoopFlowParameters;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CostEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowViolationCostEvaluator.class);

    private final Set<FlowCnec> loopflowCnecs;
    private final FlowResult initialLoopFLowResult;
    private final double loopFlowViolationCost;
    private final double loopFlowAcceptableAugmentation;
    private List<FlowCnec> sortedElements = new ArrayList<>();

    public LoopFlowViolationCostEvaluator(Set<FlowCnec> loopflowCnecs,
                                          FlowResult initialLoopFlowResult,
                                          LoopFlowParameters loopFlowParameters) {
        this.loopflowCnecs = loopflowCnecs;
        this.initialLoopFLowResult = initialLoopFlowResult;
        this.loopFlowViolationCost = loopFlowParameters.getLoopFlowViolationCost();
        this.loopFlowAcceptableAugmentation = loopFlowParameters.getLoopFlowAcceptableAugmentation();
    }

    @Override
    public String getName() {
        return "loop-flow-cost";
    }

    @Override
    public double computeCost(FlowResult flowResult, ComputationStatus sensitivityStatus) {
        Map<FlowCnec, Double> loopflowCosts = new HashMap<>();
        loopflowCnecs.forEach(flowCnec -> loopflowCosts.put(flowCnec, getLoopFlowExcess(flowResult, flowCnec) * loopFlowViolationCost));
        Set<FlowCnec> sortedLoopflows = new TreeSet<>((c1, c2) -> -loopflowCosts.get(c1).compareTo(loopflowCosts.get(c2)));
        sortedLoopflows.addAll(loopflowCnecs);

        double cost = loopflowCnecs
                .stream()
                .mapToDouble(cnec -> getLoopFlowExcess(flowResult, cnec) * loopFlowViolationCost)
                .sum();

        if (cost > 0) {
            LOGGER.info("Some loopflow constraints are not respected.");
        }

        return cost;
    }

    @Override
    public Unit getUnit() {
        return Unit.MEGAWATT;
    }

    @Override
    public List<FlowCnec> getCostlyElements(FlowResult flowResult, int numberOfElements) {
        if (sortedElements.isEmpty()) {
            sortedElements = loopflowCnecs.stream()
                    .collect(Collectors.toMap(
                        Function.identity(),
                        cnec -> getLoopFlowExcess(flowResult, cnec)
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

    double getLoopFlowExcess(FlowResult flowResult, FlowCnec cnec) {
        return Math.max(0, Math.abs(flowResult.getLoopFlow(cnec, Unit.MEGAWATT)) - getLoopFlowUpperBound(cnec));
    }

    private double getLoopFlowUpperBound(FlowCnec cnec) {
        double loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialLoopFLowResult.getLoopFlow(cnec, Unit.MEGAWATT);
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation));
    }
}
