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
public interface NetworkElementParent<T extends NetworkElementParent<T>> {
    /**
     * Get a {@code NetworkElement} adder, to add a network element
     * @return a {@code NetworkElementAdder} instance
     */
    NetworkElementAdder<T> newNetworkElement();

    /**
     * Add a (or set the) network element using a {@code NetworkElement} object
     * @param networkElement: {@code NetworkElement}
     * @return the added {@code NetworkElement} object
     */
    T addNetworkElement(NetworkElement networkElement);
}
