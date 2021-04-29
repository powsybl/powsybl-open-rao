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
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import com.farao_community.farao.rao_commons.adapter.SystematicSensitivityResultAdapter;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.LoopFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CostEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowViolationCostEvaluator.class);

    private final Set<BranchCnec> loopflowCnecs;
    private final BranchResult initialLoopFLowResult;
    private final double loopFlowViolationCost;
    private final double loopFlowAcceptableAugmentation;
    private List<BranchCnec> sortedElements = new ArrayList<>();

    LoopFlowViolationCostEvaluator(Set<BranchCnec> loopflowCnecs,
                                   BranchResult initialLoopFlowResult,
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
    public double computeCost(BranchResult branchResult, SensitivityStatus sensitivityStatus) {
        double cost = loopflowCnecs
            .stream()
            .mapToDouble(cnec -> getLoopFlowExcess(branchResult, cnec) * loopFlowViolationCost)
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
    public List<BranchCnec> getCostlyElements(BranchResult branchResult, int numberOfElements) {
        if (sortedElements.isEmpty()) {
            sortedElements = loopflowCnecs.stream()
                    .sorted(Comparator.comparing(cnec -> getLoopFlowExcess(branchResult, cnec)))
                    .collect(Collectors.toList());
        }
        Collections.reverse(sortedElements);

        return sortedElements.subList(0, Math.min(sortedElements.size(), numberOfElements));
    }

    private double getLoopFlowExcess(BranchResult branchResult, BranchCnec cnec) {
        return Math.max(0, Math.abs(branchResult.getLoopFlow(cnec, Unit.MEGAWATT)) - getLoopFlowUpperBound(cnec));
    }

    private double getLoopFlowUpperBound(BranchCnec cnec) {
        //TODO: move threshold
        double loopFlowThreshold = cnec.getExtension(CnecLoopFlowExtension.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialLoopFLowResult.getLoopFlow(cnec, Unit.MEGAWATT);
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation));
    }
}
