/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters.LoopFlowApproximationLevel;

public class LoopFlowParameters {
    private final LoopFlowApproximationLevel loopFlowApproximationLevel;
    private final double loopFlowAcceptableAugmentation;
    private final double loopFlowViolationCost;
    private final double loopFlowConstraintAdjustmentCoefficient;

    public LoopFlowParameters(LoopFlowApproximationLevel loopFlowApproximationLevel, double loopFlowAcceptableAugmentation, double loopFlowViolationCost, double loopFlowConstraintAdjustmentCoefficient) {
        this.loopFlowApproximationLevel = loopFlowApproximationLevel;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
        this.loopFlowViolationCost = loopFlowViolationCost;
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
