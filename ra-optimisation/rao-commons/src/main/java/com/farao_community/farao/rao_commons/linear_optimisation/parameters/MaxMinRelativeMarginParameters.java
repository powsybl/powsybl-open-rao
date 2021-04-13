/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.parameters;

import com.farao_community.farao.commons.Unit;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MaxMinRelativeMarginParameters extends MaxMinMarginParameters {
    public static final double DEFAULT_NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT = 1000;
    public static final double DEFAULT_PTDF_SUM_LOWER_BOUND = 0.01;

    private double negativeMarginObjectiveCoefficient = DEFAULT_NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT;
    private double ptdfSumLowerBound = DEFAULT_PTDF_SUM_LOWER_BOUND;

    public MaxMinRelativeMarginParameters(Unit unit) {
        super(unit);
    }

    public MaxMinRelativeMarginParameters(Unit unit, double pstPenaltyCost) {
        super(unit, pstPenaltyCost);
    }

    public MaxMinRelativeMarginParameters(Unit unit, double pstPenaltyCost, double negativeMarginObjectiveCoefficient, double ptdfSumLowerBound) {
        super(unit, pstPenaltyCost);
        this.negativeMarginObjectiveCoefficient = negativeMarginObjectiveCoefficient;
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    public double getNegativeMarginObjectiveCoefficient() {
        return negativeMarginObjectiveCoefficient;
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }
}
