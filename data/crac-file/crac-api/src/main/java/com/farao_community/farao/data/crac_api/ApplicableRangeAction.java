/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 *  Generic object to implement a simple range action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public interface ApplicableRangeAction {

    // The setpoint is computed by an optimiser.
    void apply(Network network, double setpoint);

    /**
     * Gather all the network elements present in the applicable range action. It returns a set because network
     * elements must not be duplicated inside an applicable range action and there is no defined order for network elements.
     *
     * @return A set of network elements.
     */
    @JsonIgnore
    Set<NetworkElement> getNetworkElements();

    double getCurrentValue(Network network);
}
