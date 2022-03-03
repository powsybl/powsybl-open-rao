/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MaxMinRelativeMarginParameters extends MaxMinMarginParameters {
    private final double ptdfSumLowerBound;
    private final double highestThreshold;

    public MaxMinRelativeMarginParameters(double pstPenaltyCost, double hvdcPenaltyCost, double injectionPenaltyCost, double ptdfSumLowerBound, double highestThresholdValue) {
        super(pstPenaltyCost, hvdcPenaltyCost, injectionPenaltyCost);
        this.ptdfSumLowerBound = ptdfSumLowerBound;
        this.highestThreshold = highestThresholdValue;
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    public double getHighestThresholdValue() {
        return highestThreshold;
    }
}
