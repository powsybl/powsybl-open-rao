/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.threshold.adder;

import com.farao_community.farao.commons.Unit;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface ThresholdAdder<I extends ThresholdAdder<I>> {

    /**
     * Set the unit for the threshold to add to cnec
     * @param unit: unit of the threshold
     * @return the {@code ThresholdAdder} instance
     */
    I setUnit(Unit unit);

    /**
     * Set the value of the threshold to add to cnec
     * @param max: value of threshold
     * @return the {@code ThresholdAdder} instance
     */
    I setMax(Double max);

    /**
     * Set the value of the threshold to add to cnec
     * @param min: value of threshold
     * @return the {@code ThresholdAdder} instance
     */
    I setMin(Double min);
}
