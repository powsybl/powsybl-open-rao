/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

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
    private boolean loopFlowApproximation;

    private double loopFlowViolationCost;

    public IteratingLinearOptimizerWithLoopFLowsParameters(int maxIterations, double fallbackOverCost, double mnecAcceptableMarginDiminution, double mnecViolationCost, boolean loopFlowApproximation, double loopFlowViolationCost) {
        super(maxIterations, fallbackOverCost, mnecAcceptableMarginDiminution, mnecViolationCost);
        this.loopFlowApproximation = loopFlowApproximation;
        this.loopFlowViolationCost = loopFlowViolationCost;
    }

    public boolean isLoopflowApproximation() {
        return loopFlowApproximation;
    }

    public void setLoopFlowApproximation(boolean loopFlowApproximation) {
        this.loopFlowApproximation = loopFlowApproximation;
    }

    public double getLoopFlowViolationCost() {
        return loopFlowViolationCost;
    }

    public void setLoopFlowViolationCost(double loopFlowViolationCost) {
        this.loopFlowViolationCost = loopFlowViolationCost;
    }
}
