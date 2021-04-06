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
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;

import java.util.List;
import java.util.Set;

/**
 * Represents an objective function divided into:
 * - functional cost: minimum margin
 * - virtual cost: mnec margin violation
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinMarginObjectiveFunction implements ObjectiveFunctionEvaluator {

    private Unit unit;
    private boolean isRaoWithLoopFlow;
    private MinMarginEvaluator minMarginEvaluator;
    private MnecViolationCostEvaluator mnecViolationCostEvaluator;
    private LoopFlowViolationCostEvaluator loopFlowViolationCostEvaluator;
    private SensitivityFallbackOvercostEvaluator sensitivityFallbackOvercostEvaluator;
    private boolean relativePositiveMargins;

    public MinMarginObjectiveFunction(LinearOptimizerInput linearOptimizerInput, RaoParameters raoParameters, Set<String> operatorsNotToOptimize) {
        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
            case MAX_MIN_MARGIN_IN_MEGAWATT:
            case MAX_MIN_RELATIVE_MARGIN_IN_AMPERE:
            case MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT:
                this.unit = raoParameters.getObjectiveFunction().getUnit();
                this.relativePositiveMargins = raoParameters.getObjectiveFunction().relativePositiveMargins();
                break;
            default:
                throw new FaraoException(String.format("%s is not a MinMarginObjectiveFunction", raoParameters.getObjectiveFunction().toString()));
        }

        this.minMarginEvaluator = new MinMarginEvaluator(linearOptimizerInput, this.unit, operatorsNotToOptimize, this.relativePositiveMargins, raoParameters.getPtdfSumLowerBound());
        this.mnecViolationCostEvaluator = new MnecViolationCostEvaluator(linearOptimizerInput, unit, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
        this.isRaoWithLoopFlow = raoParameters.isRaoWithLoopFlowLimitation();
        this.loopFlowViolationCostEvaluator = new LoopFlowViolationCostEvaluator(linearOptimizerInput, raoParameters.getLoopFlowViolationCost(), raoParameters.getLoopFlowAcceptableAugmentation());
        this.sensitivityFallbackOvercostEvaluator = new SensitivityFallbackOvercostEvaluator(raoParameters.getFallbackOverCost());
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
    public double getFunctionalCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        return minMarginEvaluator.getCost(sensitivityAndLoopflowResults);
    }

    @Override
    public double getVirtualCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        double baseVirtualCost = mnecViolationCostEvaluator.getCost(sensitivityAndLoopflowResults) + sensitivityFallbackOvercostEvaluator.getCost(sensitivityAndLoopflowResults);

        if (isRaoWithLoopFlow) {
            return baseVirtualCost + loopFlowViolationCostEvaluator.getCost(sensitivityAndLoopflowResults);
        } else {
            return baseVirtualCost;
        }
    }

    /**
     * Returns the sum of functional and virtual costs
     */
    @Override
    public double getCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        return getFunctionalCost(sensitivityAndLoopflowResults) + getVirtualCost(sensitivityAndLoopflowResults);
    }

    @Override
    public Unit getUnit() {
        return unit;
    }
}
