/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import static java.lang.Math.max;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerParameters {
    private int maxIterations;
    private double fallbackOverCost;
    private double mnecAcceptableMarginDiminution;
    private double mnecViolationCost;

    public IteratingLinearOptimizerParameters(int maxIterations, double fallbackOverCost, double mnecAcceptableMarginDiminution, double mnecViolationCost) {
        this.maxIterations = maxIterations;
        this.fallbackOverCost = fallbackOverCost;
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
        this.mnecViolationCost = mnecViolationCost;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = max(0, maxIterations);
    }

    public double getFallbackOverCost() {
        return fallbackOverCost;
    }

    public void setFallbackOverCost(double fallbackOverCost) {
        this.fallbackOverCost = fallbackOverCost;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public void setMnecAcceptableMarginDiminution(double mnecAcceptableMarginDiminution) {
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
    }

    public double getMnecViolationCost() {
        return mnecViolationCost;
    }

    public void setMnecViolationCost(double mnecViolationCost) {
        this.mnecViolationCost = mnecViolationCost;
    }
}
