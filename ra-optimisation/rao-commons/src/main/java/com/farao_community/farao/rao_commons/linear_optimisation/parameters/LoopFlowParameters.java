/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.parameters;

import com.farao_community.farao.rao_api.RaoParameters.LoopFlowApproximationLevel;

public class LoopFlowParameters {
    private LoopFlowApproximationLevel loopFlowApproximationLevel;
    private double loopFlowAcceptableAugmentation;
    private double loopFlowViolationCost;
    private double loopFlowConstraintAdjustmentCoefficient;

    public void setLoopFlowApproximationLevel(LoopFlowApproximationLevel loopFlowApproximationLevel) {
        this.loopFlowApproximationLevel = loopFlowApproximationLevel;
    }

    public void setLoopFlowAcceptableAugmentation(double loopFlowAcceptableAugmentation) {
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
    }

    public void setLoopFlowViolationCost(double loopFlowViolationCost) {
        this.loopFlowViolationCost = loopFlowViolationCost;
    }

    public void setLoopFlowConstraintAdjustmentCoefficient(double loopFlowConstraintAdjustmentCoefficient) {
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
    }

    public LoopFlowApproximationLevel getLoopFlowApproximationLevel() {
        return loopFlowApproximationLevel;
    }

    public double getLoopFlowAcceptableAugmentation() {
        return loopFlowAcceptableAugmentation;
    }

    public double getLoopFlowViolationCost() {
        return loopFlowViolationCost;
    }

    public double getLoopFlowConstraintAdjustmentCoefficient() {
        return loopFlowConstraintAdjustmentCoefficient;
    }
}
