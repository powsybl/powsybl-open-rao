/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.parameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MaxMinRelativeMarginParameters {
    private double negativeMarginObjectiveCoefficient;
    private double ptdfSumLowerBound;

    public void setNegativeMarginObjectiveCoefficient(double negativeMarginObjectiveCoefficient) {
        this.negativeMarginObjectiveCoefficient = negativeMarginObjectiveCoefficient;
    }

    public void setPtdfSumLowerBound(double ptdfSumLowerBound) {
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    public double getNegativeMarginObjectiveCoefficient() {
        return negativeMarginObjectiveCoefficient;
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }
}
