/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.parameters;

public class MnecParameters {
    private double mnecAcceptableMarginDiminution;
    private double mnecViolationCost;
    private double mnecConstraintAdjustmentCoefficient;

    public MnecParameters() {
    }

    public void setMnecAcceptableMarginDiminution(double mnecAcceptableMarginDiminution) {
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
    }

    public void setMnecViolationCost(double mnecViolationCost) {
        this.mnecViolationCost = mnecViolationCost;
    }

    public void setMnecConstraintAdjustmentCoefficient(double mnecConstraintAdjustmentCoefficient) {
        this.mnecConstraintAdjustmentCoefficient = mnecConstraintAdjustmentCoefficient;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public double getMnecViolationCost() {
        return mnecViolationCost;
    }

    public double getMnecConstraintAdjustmentCoefficient() {
        return mnecConstraintAdjustmentCoefficient;
    }
}
