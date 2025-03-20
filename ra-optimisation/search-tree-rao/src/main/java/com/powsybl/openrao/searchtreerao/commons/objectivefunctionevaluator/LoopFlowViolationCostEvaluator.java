/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.loopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.SumCnecWiseCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluatorUtils.sortFlowCnecsByDecreasingCost;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> loopflowCnecs;
    private final FlowResult initialLoopFlowResult;
    private final double loopFlowViolationCost;
    private final double loopFlowAcceptableAugmentation;

    public LoopFlowViolationCostEvaluator(Set<FlowCnec> loopflowCnecs,
                                          FlowResult initialLoopFlowResult,
                                          double loopFlowAcceptableAugmentation, double loopFlowViolationCost) {
        this.loopflowCnecs = loopflowCnecs;
        this.initialLoopFlowResult = initialLoopFlowResult;
        this.loopFlowViolationCost = loopFlowViolationCost;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
    }

    @Override
    public String getName() {
        return "loop-flow-cost";
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Map<FlowCnec, Double> costPerLoopFlowCnec = loopflowCnecs.stream()
            .collect(Collectors.toMap(Function.identity(), loopFlowCnec -> getLoopFlowExcess(flowResult, loopFlowCnec)))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() * loopFlowViolationCost));

        if (costPerLoopFlowCnec.values().stream().anyMatch(loopFlowCost -> loopFlowCost > 0)) {
            // will be logged even if the contingency is filtered out at some point
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Some loopflow constraints are not respected.");
        }

        List<FlowCnec> sortedLoopFlowCnecs = sortFlowCnecsByDecreasingCost(costPerLoopFlowCnec);
        return new SumCnecWiseCostEvaluatorResult(costPerLoopFlowCnec, sortedLoopFlowCnecs);
    }

    double getLoopFlowExcess(FlowResult flowResult, FlowCnec cnec) {
        return cnec.getMonitoredSides()
            .stream().map(side -> Math.max(0, Math.abs(flowResult.getLoopFlow(cnec, side, Unit.MEGAWATT)) - getLoopFlowUpperBound(cnec, side)))
            .max(Double::compareTo).orElse(0.0);
    }

    private double getLoopFlowUpperBound(FlowCnec cnec, TwoSides side) {
        double loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialLoopFlowResult.getLoopFlow(cnec, side, Unit.MEGAWATT);
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation));
    }
}
