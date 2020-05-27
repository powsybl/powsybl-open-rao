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

    private int maxIterations;

    @Override
    public String getName() {
        return "IteratingLinearOptimizerParameters";
    }

    public IteratingLinearOptimizerParameters() { }

    public IteratingLinearOptimizerParameters(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = max(0, maxIterations);
    }
}
