/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;

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
    private MinMarginEvaluator minRelativeMarginEvaluator;
    private MnecViolationCostEvaluator mnecViolationCostEvaluator;
    private LoopFlowViolationCostEvaluator loopFlowViolationCostEvaluator;
    private SensitivityFallbackOvercostEvaluator sensitivityFallbackOvercostEvaluator;
    private boolean relativeMargin;

    public MinMarginObjectiveFunction(RaoParameters raoParameters) {
        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
                this.unit = Unit.AMPERE;
                this.relativeMargin = false;
                break;
            case MAX_MIN_MARGIN_IN_MEGAWATT:
                this.unit = Unit.MEGAWATT;
                this.relativeMargin = false;
                break;
            case MAX_MIN_RELATIVE_MARGIN_IN_AMPERE:
                this.unit = Unit.AMPERE;
                this.relativeMargin = true;
                break;
            case MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT:
                this.unit = Unit.MEGAWATT;
                this.relativeMargin = true;
                break;
            default:
                throw new FaraoException(String.format("%s is not a MinMarginObjectiveFunction", raoParameters.getObjectiveFunction().toString()));
        }

        this.minMarginEvaluator = new MinMarginEvaluator(this.unit, false);
        this.minRelativeMarginEvaluator = new MinMarginEvaluator(this.unit, true, raoParameters.getPtdfSumLowerBound());
        this.mnecViolationCostEvaluator = new MnecViolationCostEvaluator(unit, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
        this.isRaoWithLoopFlow = raoParameters.isRaoWithLoopFlowLimitation();
        this.loopFlowViolationCostEvaluator = new LoopFlowViolationCostEvaluator(raoParameters.getLoopFlowViolationCost());
        this.sensitivityFallbackOvercostEvaluator = new SensitivityFallbackOvercostEvaluator(raoParameters.getFallbackOverCost());
    }

    @Override
    public double getFunctionalCost(RaoData raoData) {
        double inverseMinimumMargin = minMarginEvaluator.getCost(raoData);
        if ((!relativeMargin) || (inverseMinimumMargin > 0)) {
            return inverseMinimumMargin;
        } else {
            return minRelativeMarginEvaluator.getCost(raoData);
        }
    }

    @Override
    public double getVirtualCost(RaoData raoData) {
        double baseVirtualCost = mnecViolationCostEvaluator.getCost(raoData) + sensitivityFallbackOvercostEvaluator.getCost(raoData);

        if (isRaoWithLoopFlow) {
            return baseVirtualCost + loopFlowViolationCostEvaluator.getCost(raoData);
        } else {
            return baseVirtualCost;
        }
    }

    /**
     * Returns the sum of functional and virtual costs
     */
    @Override
    public double getCost(RaoData raoData) {
        return getFunctionalCost(raoData) + getVirtualCost(raoData);
    }

    @Override
    public Unit getUnit() {
        return unit;
    }
}
