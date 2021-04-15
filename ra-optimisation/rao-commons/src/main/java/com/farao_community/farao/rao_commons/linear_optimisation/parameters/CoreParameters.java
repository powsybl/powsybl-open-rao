/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.parameters;

import com.farao_community.farao.rao_api.RaoParameters;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CoreParameters {
    private RaoParameters.ObjectiveFunction objectiveFunction;
    private boolean isRaoWithLoopFlowLimitation;
    private Set<String> operatorsNotToOptimize;
    private double pstSensitivityThreshold;

    public RaoParameters.ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public void setObjectiveFunction(RaoParameters.ObjectiveFunction objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
    }

    public boolean isRaoWithLoopFlowLimitation() {
        return isRaoWithLoopFlowLimitation;
    }

    public void setRaoWithLoopFlowLimitation(boolean raoWithLoopFlowLimitation) {
        isRaoWithLoopFlowLimitation = raoWithLoopFlowLimitation;
    }

    public Set<String> getOperatorsNotToOptimize() {
        return operatorsNotToOptimize;
    }

    public void setOperatorsNotToOptimize(Set<String> operatorsNotToOptimize) {
        this.operatorsNotToOptimize = operatorsNotToOptimize;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public void setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.pstSensitivityThreshold = pstSensitivityThreshold;
    }
}
