/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionActivationResultImpl implements RangeActionActivationResult {

    private final Map<RangeAction<?>, ElementaryResult> elementaryResultMap = new HashMap<>();

    private static class ElementaryResult {
        private final double refSetpoint;
        private final Map<State, Double> setPointPerState;

        ElementaryResult(double refSetpoint) {
            this.refSetpoint = refSetpoint;
            this.setPointPerState = new HashMap<>();
        }

        private void activate(State state, Double setpoint) {
            setPointPerState.put(state, setpoint);
        }

        private boolean isExplicitlyActivatedDuringState(State state) {
            if (!setPointPerState.containsKey(state)) {
                return false;
            } else if (state.isPreventive()) {
                return Math.abs(setPointPerState.get(state) - refSetpoint) > 1e-6;
            } else {
                return true;
            }
        }

        private double getSetpoint(State state) {
            if (setPointPerState.containsKey(state)) {
                return setPointPerState.get(state);
            } else {
                Optional<State> lastActivation = getLastPreviousActivation(state);
                if (lastActivation.isPresent()) {
                    return setPointPerState.get(lastActivation.get());
                } else {
                    return refSetpoint;
                }
            }
        }

        private Pair<Double, Optional<State>> getSetpointAndLastActivation(State state) {
            if (state.getContingency().isEmpty()) {
                if (setPointPerState.containsKey(state) && Math.abs(setPointPerState.get(state) - refSetpoint) > 1e-6) {
                    // activated in preventive, with setpoint modification
                    return Pair.of(setPointPerState.get(state), Optional.of(state));
                } else {
                    // not activated in preventive
                    return Pair.of(refSetpoint, Optional.empty());
                }
            } else {
                Optional<State> lastActivation = getLastPreviousActivation(state);

                if (setPointPerState.containsKey(state) && (
                    (lastActivation.isPresent() && Math.abs(setPointPerState.get(lastActivation.get()) - setPointPerState.get(state)) > 1e-6) ||
                    (lastActivation.isEmpty() && Math.abs(refSetpoint - setPointPerState.get(state)) > 1e-6))) {
                    // activated during this post-contingency state, with setpoint modification
                    return Pair.of(setPointPerState.get(state), Optional.of(state));

                } else if (lastActivation.isPresent()) {
                    // not activated during this post-contingency state, but activated in a previous instant
                    return getSetpointAndLastActivation(lastActivation.get());
                } else {
                    // not activated during this post-contingency state, or during a previous instant
                    return Pair.of(refSetpoint, Optional.empty());
                }
            }
        }

        private Optional<State> getLastPreviousActivation(State state) {
            return setPointPerState.keySet().stream()
                .filter(s -> s.getContingency().equals(state.getContingency()) || s.getContingency().isEmpty())
                .filter(s -> s.getInstant().comesBefore(state.getInstant()))
                .max(Comparator.comparingInt(s -> s.getInstant().getOrder()));
        }

        private Set<State> getAllStatesWithActivation() {
            return setPointPerState.keySet();
        }
    }

    public RangeActionActivationResultImpl(RangeActionSetpointResult rangeActionSetpointResult) {
        rangeActionSetpointResult.getRangeActions().forEach(ra -> elementaryResultMap.put(ra, new ElementaryResult(rangeActionSetpointResult.getSetpoint(ra))));
    }

    /**
     * initiate rangeAction result with initial results read from network
     */
    public RangeActionActivationResultImpl(Network network, Set<RangeAction<?>> rangeActions) {
        rangeActions.forEach(ra -> elementaryResultMap.put(ra, new ElementaryResult(ra.getCurrentSetpoint(network))));
    }

    public void activate(RangeAction<?> rangeAction, State state, double setpoint) {
        elementaryResultMap.get(rangeAction).activate(state, setpoint);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return elementaryResultMap.keySet();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return elementaryResultMap.entrySet().stream()
            .filter(e -> e.getValue().isExplicitlyActivatedDuringState(state))
            .filter(e -> {
                Optional<State> pState = getPreviousState(state);
                if (pState.isEmpty()) {
                    return true;
                } else {
                    return Math.abs(getOptimizedSetpoint(e.getKey(), state) - getOptimizedSetpoint(e.getKey(), pState.get())) > 1e-6;
                }
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {

        // find all range actions on same network elements
        Set<RangeAction<?>> correspondingRa = elementaryResultMap.keySet().stream()
            .filter(ra -> ra.getId().equals(rangeAction.getId()) || (ra.getNetworkElements().equals(rangeAction.getNetworkElements())))
            .collect(Collectors.toSet());

        if (correspondingRa.isEmpty()) {
            throw new FaraoException(format("range action %s is not present in the result", rangeAction.getName()));
        }

        RangeAction<?> lastActivatedRa = correspondingRa.stream()
            .max(Comparator.comparingInt(ra -> {
                Optional<State> lastActivation = elementaryResultMap.get(ra).getSetpointAndLastActivation(state).getRight();
                return lastActivation.isPresent() ? lastActivation.get().getInstant().getOrder() : -1;
            })).get();

        return elementaryResultMap.get(lastActivatedRa).getSetpoint(state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        Map<RangeAction<?>, Double> optimizedSetpoints = new HashMap<>();
        elementaryResultMap.forEach((ra, re) -> optimizedSetpoints.put(ra, getOptimizedSetpoint(ra, state)));
        return optimizedSetpoints;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return pstRangeAction.convertAngleToTap(getOptimizedSetpoint(pstRangeAction, state));
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        Map<PstRangeAction, Integer> optimizedTaps = new HashMap<>();
        elementaryResultMap.entrySet().stream()
            .filter(e -> e.getKey() instanceof PstRangeAction)
            .forEach(e -> optimizedTaps.put((PstRangeAction) e.getKey(), getOptimizedTap((PstRangeAction) e.getKey(), state)));
        return optimizedTaps;
    }

    private Optional<State> getPreviousState(State state) {
        return elementaryResultMap.values().stream()
            .flatMap(eR -> eR.getAllStatesWithActivation().stream())
            .filter(s -> s.getContingency().equals(state.getContingency()) || s.getContingency().isEmpty())
            .filter(s -> s.getInstant().comesBefore(state.getInstant()))
            .max(Comparator.comparingInt(s -> s.getInstant().getOrder()));
    }
}
