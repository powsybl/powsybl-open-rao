/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionCombination {

    private final Set<NetworkAction> networkActionSet;

    NetworkActionCombination(Set<NetworkAction> networkActionSet) {
        this.networkActionSet = networkActionSet;
    }

    NetworkActionCombination(NetworkAction networkAction) {
        this.networkActionSet = Collections.singleton(networkAction);
    }

    Set<NetworkAction> getNetworkActionSet() {
        return networkActionSet;
    }

    Set<String> getOperators() {
        return networkActionSet.stream()
            .map(NetworkAction::getOperator)
            .collect(Collectors.toSet());
    }

    String getConcatenatedId() {
        return networkActionSet.stream()
            .map(Identifiable::getId)
            .collect(Collectors.joining(" + "));
    }
}
