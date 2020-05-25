/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface PstRangeActionAdder extends NetworkElementParent<PstRangeActionAdder> {

    /**
     * Set the id of the new PastRangeAction
     * @param id: the id to set
     * @return the {@code PstRangeActionAdder} instance
     */
    PstRangeActionAdder setId(String id);

    /**
     * Set the operator of the PST
     * @param operator: the name of the operator
     * @return the {@code PstRangeActionAdder} instance
     */
    PstRangeActionAdder setOperator(String operator);

    /**
     * Set the unit of the PST
     * @param unit: unit to use
     * @return the {@code PstRangeActionAdder} instance
     */
    PstRangeActionAdder setUnit(Unit unit);

    /**
     * Set the PST's minimum value in the chosen unit
     * @param minValue: minimum value
     * @return the {@code PstRangeActionAdder} instance
     */
    PstRangeActionAdder setMinValue(Double minValue);

    /**
     * Set the PST's maximum value in the chosen unit
     * @param maxValue: minimum value
     * @return the {@code PstRangeActionAdder} instance
     */
    PstRangeActionAdder setMaxValue(Double maxValue);

    /**
     * Add the new PST Range Action to the Crac
     * @return the {@code Crac} instance
     */
    Crac add();
}
