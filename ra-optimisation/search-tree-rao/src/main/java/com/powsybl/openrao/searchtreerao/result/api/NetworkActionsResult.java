/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.api;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;

import java.util.Map;
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

    /**
     * It retrieves the {@link NetworkAction} instances activated, grouped by {@link State}.
     *
     * @return A map where each key ({@link State}) corresponds to a set of activated network actions during said state.
     */
    Map<State, Set<NetworkAction>> getActivatedNetworkActionsPerState();
}
