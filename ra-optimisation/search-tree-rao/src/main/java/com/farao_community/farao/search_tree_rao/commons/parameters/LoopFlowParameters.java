/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
// TODO : Replace with LoopFlowParametersExtension
public class LoopFlowParameters {
    private final Approximation loopFlowApproximationLevel;
    private final double loopFlowAcceptableAugmentation;
    private final double loopFlowViolationCost;
    private final double loopFlowConstraintAdjustmentCoefficient;

    public LoopFlowParameters(Approximation loopFlowApproximationLevel, double loopFlowAcceptableAugmentation, double loopFlowViolationCost, double loopFlowConstraintAdjustmentCoefficient) {
        this.loopFlowApproximationLevel = loopFlowApproximationLevel;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
        this.loopFlowViolationCost = loopFlowViolationCost;
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
    }

    public Approximation getLoopFlowApproximationLevel() {
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

    public static LoopFlowParameters buildFromRaoParameters(RaoParameters raoParameters) {
        LoopFlowParametersExtension loopFlowParameters = raoParameters.getExtension(LoopFlowParametersExtension.class);
        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            return new LoopFlowParameters(loopFlowParameters.getApproximation(),
                    loopFlowParameters.getAcceptableIncrease(),
                    loopFlowParameters.getViolationCost(),
                    loopFlowParameters.getConstraintAdjustmentCoefficient());
        } else {
            return null;
        }
    }
}
