/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class NetworkActionsResultImpl implements NetworkActionsResult {

    private final Map<State, Set<NetworkAction>> activatedNetworkActionsPerState;

    public NetworkActionsResultImpl(Map<State, Set<NetworkAction>> activatedNetworkActionsPerState) {
        this.activatedNetworkActionsPerState = activatedNetworkActionsPerState;
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return activatedNetworkActionsPerState.values().stream()
            .anyMatch(set -> set.contains(networkAction));
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return activatedNetworkActionsPerState.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    @Override
    public Map<State, Set<NetworkAction>> getActivatedNetworkActionsPerState() {
        return activatedNetworkActionsPerState;
    }
}
