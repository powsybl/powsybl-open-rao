/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 * Interface to contingencies
 *
 * A contingency is the triggering of one or several {@link NetworkElement}, after which
 * {@link Cnec} can be monitored and {@link RemedialAction} applied.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface Contingency extends Identifiable<Contingency>, Synchronizable {

    /**
     * Gather all the network elements present in the contingency. It returns a set because network
     * elements must not be duplicated inside a contingency and there is no defined order for network elements.
     */
    Set<NetworkElement> getNetworkElements();

    /**
     * Apply the contingency on a network
     */
    void apply(Network network, ComputationManager computationManager);
}
