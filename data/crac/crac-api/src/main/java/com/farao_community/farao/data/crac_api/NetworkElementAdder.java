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
public interface NetworkElementAdder<T extends NetworkElementParent> {
    /**
     * Set the ID of the network element
     * @param id: ID to set
     * @return the {@code NetworkElementAdder} instance
     */
    NetworkElementAdder<T> setId(String id);

    /**
     * Set the name of the network element
     * @param name: name to set
     * @return the {@code NetworkElementAdder} instance
     */
    NetworkElementAdder<T> setName(String name);

    /**
     * Add the new network element to the parent
     * @return the {@code NetworkElementParent} parent
     */
    T add();
}
