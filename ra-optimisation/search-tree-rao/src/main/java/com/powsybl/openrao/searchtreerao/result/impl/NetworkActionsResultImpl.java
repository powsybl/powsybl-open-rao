/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;

import java.util.Set;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class NetworkActionsResultImpl implements NetworkActionsResult {

    private final Set<NetworkAction> activatedNetworkActions;

    public NetworkActionsResultImpl(Set<NetworkAction> activatedNetworkActions) {
        this.activatedNetworkActions = activatedNetworkActions;
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return activatedNetworkActions.contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return activatedNetworkActions;
    }
}
