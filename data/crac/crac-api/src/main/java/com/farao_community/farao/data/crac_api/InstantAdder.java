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
public interface InstantAdder {

    /**
     * Set the id of the new instant
     * @param id: the id to set
     * @return the {@code InstantAdder} instance
     */
    InstantAdder setId(String id);

    /**
     * Set the seconds of the new instant
     * @param seconds: the number of seconds of the instant to set
     * @return the {@code InstantAdder} instance
     */
    InstantAdder setSeconds(Integer seconds);

    /**
     * Add the new instant to the Crac
     * @return the {@code Instant} created
     */
    Instant add();
}
