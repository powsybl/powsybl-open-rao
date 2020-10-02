/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;

/**
 * Represents an objective function divided into:
 * - functional cost: minimum margin, absolute if negative or relative if positive
 * - virtual cost: mnec margin violation, absolute if minimum amrgin is negative, or relative if minimum margin is positive
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinRelativeMarginObjectiveFunction implements ObjectiveFunctionEvaluator {
    private Unit unit;
    private MinMarginEvaluator minMarginEvaluator;
    private MnecViolationCostEvaluator mnecViolationCostEvaluator;
    private MinMarginEvaluator minRelativeMarginEvaluator;

    public MinRelativeMarginObjectiveFunction(Unit unit, double mnecAcceptableMarginDiminution, double mnecViolationCost) {
        this.unit = unit;
        this.minMarginEvaluator = new MinMarginEvaluator(unit, false);
        this.minRelativeMarginEvaluator = new MinMarginEvaluator(unit, true);
        this.mnecViolationCostEvaluator = new MnecViolationCostEvaluator(unit, mnecAcceptableMarginDiminution, mnecViolationCost);
    }

    @Override
    public double getFunctionalCost(RaoData raoData) {
        double inverseMinimumMargin = minMarginEvaluator.getCost(raoData);
        if (inverseMinimumMargin > 0) {
            return inverseMinimumMargin;
        } else {
            return minRelativeMarginEvaluator.getCost(raoData);
        }
    }

    @Override
    public double getVirtualCost(RaoData raoData) {
        return mnecViolationCostEvaluator.getCost(raoData);
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
