/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class AppliedRemedialActions {

    private Map<State, AppliedRemedialActionsPerState> appliedRa = new HashMap<>();

    private class AppliedRemedialActionsPerState {
        private Set<NetworkAction> networkActions = new HashSet<>();
        private Map<RangeAction, Double> rangeActions = new HashMap<>();
    }

    void addAppliedNetworkAction(State state, NetworkAction networkAction) {
        checkState(state);
        appliedRa.get(state).networkActions.add(networkAction);
    }

    void addAppliedRangeAction(State state, RangeAction rangeAction, double setpoint) {
        checkState(state);
        appliedRa.get(state).rangeActions.put(rangeAction, setpoint);
    }

    boolean isEmpty() {
        return appliedRa.isEmpty();
    }

    Set<State> getStatesWithRa() {
        return appliedRa.keySet();
    }

    void applyOnNetwork(State state, Network network) {
        if (appliedRa.containsKey(state)) {
            appliedRa.get(state).rangeActions.forEach((rangeAction, setPoint) -> rangeAction.apply(network, setPoint));
            appliedRa.get(state).networkActions.forEach(networkAction -> networkAction.apply(network));
        }
    }

    private void checkState(State state) {
        if (!state.getInstant().equals(Instant.CURATIVE)) {
            throw new FaraoException("Sensitivity analysis with applied remedial actions only work with CURATIVE remedial actions.");
        }
        appliedRa.putIfAbsent(state, new AppliedRemedialActionsPerState());
    }
}
