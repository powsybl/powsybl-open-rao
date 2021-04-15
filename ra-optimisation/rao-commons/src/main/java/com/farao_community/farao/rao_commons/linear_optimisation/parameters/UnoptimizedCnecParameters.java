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
public class UnoptimizedCnecParameters {
    private double highestThresholdValue;

    public double getHighestThresholdValue() {
        return highestThresholdValue;
    }

    public void setHighestThresholdValue(double highestThresholdValue) {
        this.highestThresholdValue = highestThresholdValue;
    }
}
