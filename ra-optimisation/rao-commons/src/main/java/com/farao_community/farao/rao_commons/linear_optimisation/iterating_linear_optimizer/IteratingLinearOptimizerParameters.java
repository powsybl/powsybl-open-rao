/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.commons.Unit;

import static java.lang.Math.max;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerParameters {
    private Unit unit;
    private int maxIterations;
    private double fallBackOverCost;

    public IteratingLinearOptimizerParameters(Unit unit, int maxIterations, double fallBackOverCost) {
        this.unit = unit;
        this.maxIterations = maxIterations;
        this.fallBackOverCost = fallBackOverCost;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = max(0, maxIterations);
    }

    public double getFallBackOverCost() {
        return fallBackOverCost;
    }

    public void setFallBackOverCost(double fallBackOverCost) {
        this.fallBackOverCost = fallBackOverCost;
    }
}
