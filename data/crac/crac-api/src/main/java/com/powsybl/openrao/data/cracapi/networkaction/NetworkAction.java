/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.networkaction;

import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 * Remedial action interface specifying a direct action on the network.
 *
 * The Network Action is completely defined by itself.
 * It involves a Set of {@link ElementaryAction}.
 * When the apply method is called, an action is triggered on each of these Elementary
 * Actions.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface NetworkAction extends RemedialAction<NetworkAction> {

    /**
     * States if the remedial action would change the current state of the network. It has no impact on the network.
     *
     * @param network: Network that serves as reference for the impact.
     * @return True if the remedial action would have an impact on the network.
     */
    boolean hasImpactOnNetwork(final Network network);

    /**
     * Apply the action on a given network.
     * @param network the Network to apply the network action upon
     * @return true if the network action was applied, false if not (eg if it was already applied)
     */
    boolean apply(Network network);

    /**
     * Get the set of the elementary actions constituting then network action
     */
    Set<ElementaryAction> getElementaryActions();


    /**
     * Returns true if all the elementary actions can be applied to the given network
     */
    boolean canBeApplied(Network network);
}
