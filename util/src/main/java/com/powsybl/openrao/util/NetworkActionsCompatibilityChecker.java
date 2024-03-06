/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class NetworkActionsCompatibilityChecker {
    private NetworkActionsCompatibilityChecker() { }

    public static Set<NetworkAction> filterOutIncompatibleRemedialActions(Set<NetworkAction> appliedNetworkActions, Set<NetworkAction> availableRemedialActions) {
        Set<NetworkAction> compatibleNetworkActions = new HashSet<>();
        for (NetworkAction availableRemedialAction : availableRemedialActions) {
            if (appliedNetworkActions.stream().allMatch(networkAction -> networkAction.isCompatibleWith(availableRemedialAction))) {
                compatibleNetworkActions.add(availableRemedialAction);
            }
        }
        return compatibleNetworkActions;
    }
}
