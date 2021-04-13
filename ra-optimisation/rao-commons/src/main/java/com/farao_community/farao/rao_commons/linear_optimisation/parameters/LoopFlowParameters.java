/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.parameters;

import com.farao_community.farao.rao_api.RaoParameters.LoopFlowApproximationLevel;

public class LoopFlowParameters {
    private final boolean raoWithLoopFlowLimitation;
    private final LoopFlowApproximationLevel loopFlowApproximationLevel;
    private final double loopFlowAcceptableAugmentation;
    private final double loopFlowViolationCost;
    private final double loopFlowConstraintAdjustmentCoefficient;

    public LoopFlowParameters(boolean raoWithLoopFlowLimitation,
                              LoopFlowApproximationLevel loopFlowApproximationLevel,
                              double loopFlowAcceptableAugmentation,
                              double loopFlowViolationCost,
                              double loopFlowConstraintAdjustmentCoefficient) {
        this.raoWithLoopFlowLimitation = raoWithLoopFlowLimitation;
        this.loopFlowApproximationLevel = loopFlowApproximationLevel;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
        this.loopFlowViolationCost = loopFlowViolationCost;
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
    }

    public boolean isRaoWithLoopFlowLimitation() {
        return raoWithLoopFlowLimitation;
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
