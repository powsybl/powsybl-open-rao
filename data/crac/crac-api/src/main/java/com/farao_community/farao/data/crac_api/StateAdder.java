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
public interface StateAdder {

    /**
     * Set the instant of the new state
     * @param instant: the instant to set
     * @return the {@code StateAdder} instance
     */
    StateAdder setInstant(Instant instant);

    /**
     * Set the contingency of the new state
     * @param contingency: the contingency to set
     * @return the {@code StateAdder} instance
     */
    StateAdder setContingency(Contingency contingency);

    /**
     * Add the new state to the Crac
     * @return the {@code State} instance create
     */
    State add();
}
