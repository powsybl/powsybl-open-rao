/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_api.RaoParameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerWithLoopFLowsParameters extends IteratingLinearOptimizerParameters {

    /**
     *  loopflow approximation means using previous calculated ptdf and net position values to compute loopflow
     *  ptdf is calculated by a sensitivity analysis, net position is derived from current network
     *  if "loopflowApproximation" is set to "false", then each loopflow computation do a sensi for ptdf;
     *  if "loopflowApproximation" is set to "true", loopflow computation tries to use previous saved ptdf and netposition.
     *  note: Loopflow = reference flow - ptdf * net position
     */
    private RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel;

    private double loopFlowViolationCost;

    public IteratingLinearOptimizerWithLoopFLowsParameters(int maxIterations, double fallbackOverCost, RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel, double loopFlowViolationCost) {
        super(maxIterations, fallbackOverCost);
        this.loopFlowApproximationLevel = loopFlowApproximationLevel;
        this.loopFlowViolationCost = loopFlowViolationCost;
    }

    public RaoParameters.LoopFlowApproximationLevel getLoopflowApproximationLevel() {
        return loopFlowApproximationLevel;
    }

    public void setLoopFlowApproximationLevel(RaoParameters.LoopFlowApproximationLevel loopFlowApproximation) {
        this.loopFlowApproximationLevel = loopFlowApproximation;
    }

    public double getLoopFlowViolationCost() {
        return loopFlowViolationCost;
    }

    public void setLoopFlowViolationCost(double loopFlowViolationCost) {
        this.loopFlowViolationCost = loopFlowViolationCost;
    }
}
