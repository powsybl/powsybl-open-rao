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
public interface ContingencyAdder extends NetworkElementParent {

    /**
     * Set the ID of the contingency
     * @param id: ID to set
     * @return the {@code ContingencyAdder} instance
     */
    ContingencyAdder setId(String id);

    /**
     * Set the name of the contingency
     * @param name: name to set
     * @return the {@code ContingencyAdder} instance
     */
    ContingencyAdder setName(String name);

    /**
     * Add a network element to the contingency
     * @return a {@code NetworkElementAdder<ContingencyAdder>} instance to construct a network element
     */
    NetworkElementAdder<ContingencyAdder> newNetworkElement();

    /**
     * Add the new contingency to the Crac
     * @return the created {@code Contingency} instance
     */
    Contingency add();
}
