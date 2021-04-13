/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CostEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowViolationCostEvaluator.class);

    private double violationCost;
    private double loopFlowAcceptableAugmentation;
    private Set<BranchCnec> loopflowCnecs;
    private Map<BranchCnec, Double> initialLoopflowsInMW;

    LoopFlowViolationCostEvaluator(Set<BranchCnec> loopflowCnecs, Map<BranchCnec, Double> initialLoopflowsInMW, double violationCost, double loopFlowAcceptableAugmentation) {
        this.loopflowCnecs = loopflowCnecs;
        this.initialLoopflowsInMW = initialLoopflowsInMW;
        this.violationCost = violationCost;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
    }

    @Override
    public double computeCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        double cost = loopflowCnecs
            .stream()
            .mapToDouble(cnec -> getLoopFlowExcess(sensitivityAndLoopflowResults, cnec) * violationCost)
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
    public List<BranchCnec> getMostLimitingElements(SensitivityAndLoopflowResults sensitivityAndLoopflowResults, int numberOfElements) {
        throw new NotImplementedException("getMostLimitingElements() not implemented yet for loopflow evaluators");
    }

    private double getLoopFlowExcess(SensitivityAndLoopflowResults sensitivityAndLoopflowResults, BranchCnec cnec) {
        return Math.max(0, Math.abs(sensitivityAndLoopflowResults.getLoopflow(cnec)) - getLoopFlowUpperBound(cnec));
    }

    private double getLoopFlowUpperBound(BranchCnec cnec) {
        //TODO: move threshold
        double loopFlowThreshold = cnec.getExtension(CnecLoopFlowExtension.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialLoopflowsInMW.get(cnec);
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation));
    }
}
