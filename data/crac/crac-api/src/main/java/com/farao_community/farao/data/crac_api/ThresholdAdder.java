/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.Unit;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface ThresholdAdder {

    /**
     * Set the unit for the threshold to add to cnec
     * @param unit: unit of the threshold
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setUnit(Unit unit);

    /**
     * Set the value of the threshold to add to cnec
     * @param maxValue: value of threshold
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setMaxValue(Double maxValue);

    ThresholdAdder onNonRegulatedSide();

    /**
     * Set the side of the threshold to add to cnec
     * @param side: side of threshold
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setSide(Side side);

    /**
     * Set the voltage level on which it should be applied
     * @param voltageLevel: can be either LOW or HIGH
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setVoltageLevel(VoltageLevel voltageLevel);

    /**
     * Set the direction of the threshold to add to cnec
     * @param direction: direction of threshold
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setDirection(Direction direction);

    /**
     * Add the new threshold to cnec
     * @return the {@code CnecAdder} instance
     */
    CnecAdder add();
}
