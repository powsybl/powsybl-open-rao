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
public interface CnecAdder extends NetworkElementParent {

    /**
     * Set the ID of the cnec
     * @param id: ID to set
     * @return the {@code ContingencyAdder} instance
     */
    CnecAdder setId(String id);

    /**
     * Set the name of the cnec
     * @param name: name to give
     * @return the {@code ContingencyAdder} instance
     */
    CnecAdder setName(String name);

    /**
     * Set the state of the cnec
     * @param state: state of the created cnec
     * @return the {@code ContingencyAdder} instance
     */
    CnecAdder setState(State state);

    /**
     * Add a network element to the cnec
     * @return a {@code NetworkElementAdder<CnecAdder>} instance to construct a network element
     */
    NetworkElementAdder<CnecAdder> newNetworkElement();

    /**
     * Add a threshold to the created cnec
     * @return a {@code ThresholdAdder} instance
     */
    ThresholdAdder newThreshold();

    /**
     * Add the new state to the Crac
     * @return the created {@code Cnec} instance
     */
    Cnec add();
}
