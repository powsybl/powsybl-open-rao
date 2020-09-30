/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_api.RaoParameters;

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

    public MinMarginObjectiveFunction(RaoParameters raoParameters) {

        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
                this.unit = Unit.AMPERE;
                break;
            case MAX_MIN_MARGIN_IN_MEGAWATT:
                this.unit = Unit.MEGAWATT;
                break;
            default:
                throw new FaraoException(String.format("%s is not a MinMarginObjectiveFunction", raoParameters.getObjectiveFunction().toString()));
        }

        this.minMarginEvaluator = new MinMarginEvaluator(this.unit);
        this.mnecViolationCostEvaluator = new MnecViolationCostEvaluator(unit, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
        this.isRaoWithLoopFlow = raoParameters.isRaoWithLoopFlowLimitation();
        this.loopFlowViolationCostEvaluator = new LoopFlowViolationCostEvaluator(raoParameters.getLoopFlowViolationCost());
    }

    @Override
    public double getFunctionalCost(RaoData raoData) {
        return minMarginEvaluator.getCost(raoData);
    }

    @Override
    public double getVirtualCost(RaoData raoData) {
        if (isRaoWithLoopFlow) {
            return mnecViolationCostEvaluator.getCost(raoData) + loopFlowViolationCostEvaluator.getCost(raoData);
        } else {
            return mnecViolationCostEvaluator.getCost(raoData);
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
