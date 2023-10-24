/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 * Generic interface for the definition of elementary actions
 * <p>
 * An elementary action is an action on the network which can be
 * activated by a {@link NetworkAction}
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface ElementaryAction {

    /**
     * States if the remedial action would change the current state of the network. It has no impact on the network.
     *
     * @param network: Network that serves as reference for the impact.
     * @return True if the remedial action would have an impact on the network.
     */
    boolean hasImpactOnNetwork(final Network network);

    /**
     * Returns true if the elementary action can be applied to the given network
     */
    boolean canBeApplied(Network network);

    /**
     * Apply the elementary action on a given network.
     */
    void apply(Network network);

    /**
     * Get the Network Elements associated to the elementary action
     */
    Set<NetworkElement> getNetworkElements();
}
