/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.ParametersProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an objective function divided into:
 * - functional cost: minimum margin
 * - virtual cost: mnec margin violation
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinMarginObjectiveFunction implements ObjectiveFunctionEvaluator {
    private final MinMarginEvaluator minMarginEvaluator;
    private final MnecViolationCostEvaluator mnecViolationCostEvaluator;
    private final LoopFlowViolationCostEvaluator loopFlowViolationCostEvaluator;
    private final SensitivityFallbackOvercostEvaluator sensitivityFallbackOvercostEvaluator;
    private final Unit unit = ParametersProvider.getUnit();
    private final boolean relativePositiveMargins = ParametersProvider.hasRelativeMargins();
    private final boolean raoWithLoopFlowLimitation = ParametersProvider.isRaoWithLoopFlowLimitation();

    public MinMarginObjectiveFunction(Set<BranchCnec> cnecs,
                                      Set<BranchCnec> loopflowCnecs,
                                      Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW,
                                      Map<BranchCnec, Double> initialAbsolutePtdfSums,
                                      Map<BranchCnec, Double> initialFlows,
                                      Map<BranchCnec, Double> initialLoopflowsInMW,
                                      double sensitivityFallbackOverCost) {
        switch (ParametersProvider.getObjectiveFuntion()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
            case MAX_MIN_MARGIN_IN_MEGAWATT:
            case MAX_MIN_RELATIVE_MARGIN_IN_AMPERE:
            case MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT:
                break;
            default:
                throw new FaraoException(String.format("%s is not a MinMarginObjectiveFunction", ParametersProvider.getObjectiveFuntion().toString()));
        }

        this.minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMarginsInAbsoluteMW, initialAbsolutePtdfSums);
        this.mnecViolationCostEvaluator = new MnecViolationCostEvaluator(cnecs, initialFlows);
        this.loopFlowViolationCostEvaluator = new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopflowsInMW);
        this.sensitivityFallbackOvercostEvaluator = new SensitivityFallbackOvercostEvaluator(sensitivityFallbackOverCost);
    }

    public boolean isRelative() {
        return relativePositiveMargins;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(SensitivityAndLoopflowResults sensitivityAndLoopflowResults, int numberOfElements) {
        //TODO : maybe some day include mnecs and even loopflows here
        return minMarginEvaluator.getMostLimitingElements(sensitivityAndLoopflowResults, numberOfElements);
    }

    @Override
    public double computeFunctionalCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        return minMarginEvaluator.computeCost(sensitivityAndLoopflowResults);
    }

    @Override
    public double computeVirtualCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        double baseVirtualCost = mnecViolationCostEvaluator.computeCost(sensitivityAndLoopflowResults) + sensitivityFallbackOvercostEvaluator.computeCost(sensitivityAndLoopflowResults);

        if (raoWithLoopFlowLimitation) {
            return baseVirtualCost + loopFlowViolationCostEvaluator.computeCost(sensitivityAndLoopflowResults);
        } else {
            return baseVirtualCost;
        }
    }

    /**
     * Returns the sum of functional and virtual costs
     */
    @Override
    public double computeCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        return computeFunctionalCost(sensitivityAndLoopflowResults) + computeVirtualCost(sensitivityAndLoopflowResults);
    }

    @Override
    public Unit getUnit() {
        return unit;
    }
}
