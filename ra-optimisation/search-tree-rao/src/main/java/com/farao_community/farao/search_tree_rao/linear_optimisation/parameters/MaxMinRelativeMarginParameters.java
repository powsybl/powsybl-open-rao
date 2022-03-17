/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MaxMinRelativeMarginParameters {
    private final double negativeMarginObjectiveCoefficient;
    private final double ptdfSumLowerBound;

    public MaxMinRelativeMarginParameters(double negativeMarginObjectiveCoefficient, double ptdfSumLowerBound) {
        this.negativeMarginObjectiveCoefficient = negativeMarginObjectiveCoefficient;
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    public double getNegativeMarginObjectiveCoefficient() {
        return negativeMarginObjectiveCoefficient;
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    public static MaxMinRelativeMarginParameters buildFromRaoParameters(RaoParameters raoParameters) {

        /*
        for now, values of MaxMinRelativeMarginParameters are constant over all the SearchTreeRao
        they can therefore be instantiated directly from a RaoParameters
         */

        if (raoParameters.getObjectiveFunction().relativePositiveMargins()) {
            return new MaxMinRelativeMarginParameters(raoParameters.getNegativeMarginObjectiveCoefficient(),
                raoParameters.getPtdfSumLowerBound());
        } else {
            return null;
        }
    }
}
