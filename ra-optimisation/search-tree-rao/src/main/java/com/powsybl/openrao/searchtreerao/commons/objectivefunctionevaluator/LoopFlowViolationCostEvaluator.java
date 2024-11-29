/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> loopflowCnecs;
    private final FlowResult initialLoopFLowResult;
    private final double loopFlowViolationCost;
    private final double loopFlowAcceptableAugmentation;

    public LoopFlowViolationCostEvaluator(Set<FlowCnec> loopflowCnecs,
                                          FlowResult initialLoopFlowResult,
                                          double loopFlowViolationCost,
                                          double loopFlowAcceptableAugmentation) {
        this.loopflowCnecs = loopflowCnecs;
        this.initialLoopFLowResult = initialLoopFlowResult;
        this.loopFlowViolationCost = loopFlowViolationCost;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
    }

    @Override
    public String getName() {
        return "loop-flow-cost";
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> costlyElements = getCostlyElements(flowResult, contingenciesToExclude);
        double cost = costlyElements
            .stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .mapToDouble(cnec -> getLoopFlowExcess(flowResult, cnec) * loopFlowViolationCost)
            .sum();

        if (cost > 0) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Some loopflow constraints are not respected.");
        }

        return Pair.of(cost, costlyElements);
    }

    @Override
    public Unit getUnit() {
        return Unit.MEGAWATT;
    }

    private List<FlowCnec> getCostlyElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> costlyElements = loopflowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .collect(Collectors.toMap(
                Function.identity(),
                cnec -> getLoopFlowExcess(flowResult, cnec)
            ))
            .entrySet().stream()
            .filter(entry -> entry.getValue() != 0)
            .sorted(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        Collections.reverse(costlyElements);
        return costlyElements;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return loopflowCnecs;
    }

    double getLoopFlowExcess(FlowResult flowResult, FlowCnec cnec) {
        return cnec.getMonitoredSides()
            .stream().map(side -> Math.max(0, Math.abs(flowResult.getLoopFlow(cnec, side, Unit.MEGAWATT)) - getLoopFlowUpperBound(cnec, side)))
            .max(Double::compareTo).orElse(0.0);
    }

    private double getLoopFlowUpperBound(FlowCnec cnec, TwoSides side) {
        double loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialLoopFLowResult.getLoopFlow(cnec, side, Unit.MEGAWATT);
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation));
    }
}
