/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionActivationResultImpl implements RangeActionActivationResult {

    private final Map<RangeAction<?>, ElementaryResult> elementaryResultMap = new HashMap<>();

    boolean shouldRecomputeSetpointsPerState = true;

    private Map<String, Map<State, Double> > setpointPerStatePerPstId;

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
            return setPointPerState.containsKey(state);
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
        shouldRecomputeSetpointsPerState = true;
        rangeActionSetpointResult.getRangeActions().forEach(ra -> elementaryResultMap.put(ra, new ElementaryResult(rangeActionSetpointResult.getSetpoint(ra))));
    }

    public void activate(RangeAction<?> rangeAction, State state, double setpoint) {
        shouldRecomputeSetpointsPerState = true;
        elementaryResultMap.get(rangeAction).activate(state, setpoint);
    }

    private void computeSetpointsPerStatePerPst() {
        if (!shouldRecomputeSetpointsPerState) {
            return;
        }
        setpointPerStatePerPstId = new HashMap<>();
        elementaryResultMap.forEach((rangeAction, elementaryResult) -> {
            String networkElementsIds = concatenateNetworkElementsIds(rangeAction);
            setpointPerStatePerPstId.computeIfAbsent(networkElementsIds, raId -> new HashMap<>());
            elementaryResult.getAllStatesWithActivation().forEach(state ->
                setpointPerStatePerPstId.get(networkElementsIds).put(state, elementaryResult.getSetpoint(state))
            );
        });
        shouldRecomputeSetpointsPerState = false;
    }

    private String concatenateNetworkElementsIds(RangeAction<?> rangeAction) {
        return rangeAction.getNetworkElements().stream().map(Identifiable::getId).sorted().collect(Collectors.joining("+"));
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return elementaryResultMap.keySet();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        computeSetpointsPerStatePerPst();
        return elementaryResultMap.entrySet().stream()
            .filter(e -> e.getValue().isExplicitlyActivatedDuringState(state))
            .filter(e -> {
                Optional<State> pState = getPreviousState(state);
                if (pState.isEmpty()) {
                    return Math.abs(getOptimizedSetpoint(e.getKey(), state) - e.getValue().refSetpoint) > 1e-6;
                } else {
                    return Math.abs(getOptimizedSetpoint(e.getKey(), state) - getOptimizedSetpoint(e.getKey(), pState.get())) > 1e-6;
                }
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        computeSetpointsPerStatePerPst();
        String networkElementsIds = concatenateNetworkElementsIds(rangeAction);
        // if at least one elementary result is on the correct network elements, find the right state to get the setpoint
        // else return the reference setpoint
        if (setpointPerStatePerPstId.containsKey(networkElementsIds)) {
            Map<State, Double> setPointPerState = setpointPerStatePerPstId.get(networkElementsIds);
            // if an elementary result is defined for the network element and state, return it
            // else find a previous state with an elementary result
            // if none are found, return reference setpoint
            if (setPointPerState.containsKey(state)) {
                return setPointPerState.get(state);
            } else {
                Optional<State> previousState = getPreviousState(state);
                while (previousState.isPresent()) {
                    if (setPointPerState.containsKey(previousState.get())) {
                        return setPointPerState.get(previousState.get());
                    }
                    previousState = getPreviousState(previousState.get());
                }
            }
        }
        if (!elementaryResultMap.containsKey(rangeAction)) {
            throw new FaraoException(format("range action %s is not present in the result", rangeAction.getName()));
        }
        return elementaryResultMap.get(rangeAction).refSetpoint;
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        computeSetpointsPerStatePerPst();
        Map<RangeAction<?>, Double> optimizedSetpoints = new HashMap<>();
        elementaryResultMap.forEach((ra, re) -> optimizedSetpoints.put(ra, getOptimizedSetpoint(ra, state)));
        return optimizedSetpoints;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        computeSetpointsPerStatePerPst();
        return pstRangeAction.convertAngleToTap(getOptimizedSetpoint(pstRangeAction, state));
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        computeSetpointsPerStatePerPst();
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
