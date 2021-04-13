/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 * Interface to manage contingencies
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface Contingency extends Identifiable<Contingency>, Synchronizable {

    /**
     * Gather all the network elements present in the contingency. It returns a set because network
     * elements must not be duplicated inside a contingency and there is no defined order for network elements.
     *
     * @return A set of network elements.
     */
    Set<NetworkElement> getNetworkElements();

    /**
     * Apply contingency on network
     * @param network network
     * @param computationManager computation manager
     */
    void apply(Network network, ComputationManager computationManager);
}
