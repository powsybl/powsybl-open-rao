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
public interface PstRangeActionAdder extends NetworkElementParent {

    /**
     * Set the id of the new PastRangeAction
     * @param id: the id to set
     * @return the {@code PstRangeActionAdder} instance
     */
    PstRangeActionAdder setId(String id);

    /**
     * Set the unit
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
     * Add a network element to the PstRangeAction
     * @return a {@code NetworkElementAdder<PstRangeActionAdder>} instance to construct a network element
     */
    NetworkElementAdder<PstRangeActionAdder> newNetworkElement();

    /**
     * Add the new instant to the Crac
     * @return the {@code Crac} instance
     */
    Crac add();
}
