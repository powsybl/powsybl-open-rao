/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class AppliedRemedialActions {

    private final Map<State, AppliedRemedialActionsPerState> appliedRa = new HashMap<>();

    private static final class AppliedRemedialActionsPerState {
        private final Set<NetworkAction> networkActions = new HashSet<>();
        private final Map<RangeAction<?>, Double> rangeActions = new HashMap<>();
    }

    public void addAppliedNetworkAction(State state, NetworkAction networkAction) {
        if (networkAction != null) {
            checkState(state);
            appliedRa.get(state).networkActions.add(networkAction);
        }
    }

    public void addAppliedNetworkActions(State state, Set<NetworkAction> networkActions) {
        if (!networkActions.isEmpty()) {
            checkState(state);
            appliedRa.get(state).networkActions.addAll(networkActions);
        }
    }

    public void addAppliedRangeAction(State state, RangeAction<?> rangeAction, double setpoint) {
        if (rangeAction != null) {
            checkState(state);
            appliedRa.get(state).rangeActions.put(rangeAction, setpoint);
        }
    }

    public void addAppliedRangeActions(State state, Map<RangeAction<?>, Double> rangeActions) {
        if (!rangeActions.isEmpty()) {
            checkState(state);
            appliedRa.get(state).rangeActions.putAll(rangeActions);
        }
    }

    public boolean isEmpty(Network network) {
        return getStatesWithRa(network).isEmpty();
    }

    public Set<State> getStatesWithRa(Network network) {
        // state with at least one network action applied
        // or state with at least one range action whose setpoint is different from the one in the network
        return appliedRa.entrySet().stream()
            .filter(stateE -> !stateE.getValue().networkActions.isEmpty() || stateE.getValue().rangeActions.entrySet().stream()
                .anyMatch(raE -> Math.abs(raE.getKey().getCurrentSetpoint(network) - raE.getValue()) > 1e-6))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public Set<NetworkAction> getAppliedNetworkActions(State state) {
        if (appliedRa.containsKey(state)) {
            return appliedRa.get(state).networkActions;
        } else {
            return new HashSet<>();
        }
    }

    public Map<RangeAction<?>, Double> getAppliedRangeActions(State state) {
        if (appliedRa.containsKey(state)) {
            return appliedRa.get(state).rangeActions;
        } else {
            return new HashMap<>();
        }
    }

    public void applyOnNetwork(State state, Network network) {
        // Apply remedial actions from all states before or equal to given state
        appliedRa.keySet().stream().filter(stateBefore ->
            (stateBefore.getInstant().comesBefore(state.getInstant()) || stateBefore.getInstant().equals(state.getInstant()))
                && (stateBefore.getContingency().isEmpty() || stateBefore.getContingency().equals(state.getContingency())))
            .sorted(Comparator.comparingInt(stateBefore -> stateBefore.getInstant().getOrder()))
            .forEach(stateBefore -> {
                // network actions need to be applied BEFORE range actions because to apply HVDC range actions we need to apply AC emulation deactivation network actions beforehand
                appliedRa.get(stateBefore).networkActions.forEach(networkAction -> networkAction.apply(network));
                appliedRa.get(stateBefore).rangeActions.forEach((rangeAction, setPoint) -> rangeAction.apply(network, setPoint));
            });
    }

    public AppliedRemedialActions copy() {
        AppliedRemedialActions ara = new AppliedRemedialActions();
        appliedRa.forEach((state, appliedRaOnState) -> {
            ara.addAppliedNetworkActions(state, appliedRaOnState.networkActions);
            ara.addAppliedRangeActions(state, appliedRaOnState.rangeActions);
        });
        return ara;
    }

    public AppliedRemedialActions copyNetworkActionsAndAutomaticRangeActions() {
        AppliedRemedialActions ara = new AppliedRemedialActions();
        appliedRa.forEach((state, appliedRaOnState) -> ara.addAppliedNetworkActions(state, appliedRaOnState.networkActions));
        appliedRa.forEach((state, appliedRaOnState) -> {
            if (state.getInstant().isAuto()) {
                ara.addAppliedRangeActions(state, appliedRaOnState.rangeActions);
            }
        });
        return ara;
    }

    private void checkState(State state) {
        if (!state.getInstant().isCurative() && !state.getInstant().isAuto()) {
            throw new OpenRaoException("Sensitivity analysis with applied remedial actions only work with CURATIVE and AUTO remedial actions.");
        }
        appliedRa.putIfAbsent(state, new AppliedRemedialActionsPerState());
    }

    public AppliedRemedialActions copyCurative() {
        Map<State, AppliedRemedialActionsPerState> curativeMap = appliedRa.entrySet().stream().filter(entry -> entry.getKey().getInstant().isCurative())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        AppliedRemedialActions ara = new AppliedRemedialActions();
        curativeMap.forEach((state, appliedRaOnState) -> {
            ara.addAppliedNetworkActions(state, appliedRaOnState.networkActions);
            ara.addAppliedRangeActions(state, appliedRaOnState.rangeActions);
        });
        return ara;
    }
}
