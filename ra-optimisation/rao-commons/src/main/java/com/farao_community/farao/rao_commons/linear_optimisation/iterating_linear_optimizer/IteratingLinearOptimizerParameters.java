/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

import static java.lang.Math.max;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerParameters extends AbstractExtension<RaoParameters> {
    static final int DEFAULT_MAX_NUMBER_OF_ITERATIONS = 10;
    static final boolean DEFAULT_LOOPFLOW_APPROXIMATION = false;

    private int maxIterations = DEFAULT_MAX_NUMBER_OF_ITERATIONS;

    /**
     *  loopflow approximation means using previous calculated ptdf and net position values to compute loopflow
     *  ptdf is calculated by a sensitivity analysis, net position is derived from current network
     *  if "loopflowApproximation" is set to "false", then each loopflow computation do a sensi for ptdf;
     *  if "loopflowApproximation" is set to "true", loopflow computation tries to use previous saved ptdf and netposition.
     *  note: Loopflow = reference flow - ptdf * net position
     */
    private boolean loopflowApproximation = DEFAULT_LOOPFLOW_APPROXIMATION;

    @Override
    public String getName() {
        return "IteratingLinearOptimizerParameters";
    }

    public IteratingLinearOptimizerParameters() {
        // Mandatory for deserialization
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = max(0, maxIterations);
    }

    public boolean isLoopflowApproximation() {
        return loopflowApproximation;
    }

    public void setLoopflowApproximation(boolean loopflowApproximation) {
        this.loopflowApproximation = loopflowApproximation;
    }
}
