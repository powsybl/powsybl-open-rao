/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginParametersExtension;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MaxMinRelativeMarginParameters {
    private final double ptdfSumLowerBound;

    public MaxMinRelativeMarginParameters(double ptdfSumLowerBound) {
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    public static MaxMinRelativeMarginParameters buildFromRaoParameters(RaoParameters raoParameters) {
        RelativeMarginParametersExtension relativeMarginParameters = raoParameters.getExtension(RelativeMarginParametersExtension.class);
        if (raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().relativePositiveMargins()) {
            if (Objects.isNull(relativeMarginParameters)) {
                throw new FaraoException("No relative margins parameters were defined with objective function " + raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType());
            }
            return new MaxMinRelativeMarginParameters(relativeMarginParameters.getPtdfSumLowerBound());
        } else {
            return null;
        }
    }
}
