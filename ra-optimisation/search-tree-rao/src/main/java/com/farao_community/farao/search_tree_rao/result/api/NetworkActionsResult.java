/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.api;

import com.farao_community.farao.data.crac_api.network_action.NetworkAction;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface NetworkActionsResult {

    /**
     * It states if the {@link NetworkAction} is activated.
     *
     * @param networkAction: The network action to be studied.
     * @return True if the network action is chosen by the optimizer.
     */
    boolean isActivated(NetworkAction networkAction);

    /**
     * It gathers the {@link NetworkAction} that are activated.
     *
     * @return The set of activated network actions.
     */
    Set<NetworkAction> getActivatedNetworkActions();
}
