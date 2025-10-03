/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionActivationResultImpl implements RangeActionActivationResult {

    private final Map<RangeAction<?>, ElementaryResult> elementaryResultMap = new HashMap<>();

    boolean shouldRecomputeSetpointsPerState;

    private Map<String, Map<State, Double> > setpointPerStatePerPstId;
    private Map<State, Optional<State>> memoizedPreviousState = new HashMap<>();

    private static final class ElementaryResult {
        private final double refSetpoint;
        private final Map<State, Double> setPointPerState;

        ElementaryResult(double refSetpoint) {
            this.refSetpoint = refSetpoint;
            this.setPointPerState = new HashMap<>();
        }

        private void put(State state, Double setpoint) {
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

    public void putResult(RangeAction<?> rangeAction, State state, double setpoint) {
        shouldRecomputeSetpointsPerState = true;
        elementaryResultMap.get(rangeAction).put(state, setpoint);
        memoizedPreviousState = new HashMap<>();
    }

    private synchronized void computeSetpointsPerStatePerPst() {
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
                    return Math.abs(getOptimizedSetpoint(e.getKey(), state) - e.getValue().refSetpoint) > 1e-6; // TODO : use same parameter as min variation of MIP here?
                } else {
                    return Math.abs(getOptimizedSetpoint(e.getKey(), state) - getOptimizedSetpoint(e.getKey(), pState.get())) > 1e-6;
                }
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public Map<State, Set<RangeAction<?>>> getActivatedRangeActionsPerState() {
        Set<State> states = new HashSet<>();
        elementaryResultMap.values().stream()
            .map(ElementaryResult::getAllStatesWithActivation)
            .forEach(states::addAll);
        Map<State, Set<RangeAction<?>>> activatedRangeActionsPerState = new HashMap<>();
        states.forEach(state -> activatedRangeActionsPerState.put(state, getActivatedRangeActions(state)));
        return activatedRangeActionsPerState;
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
            Double lastSetpoint = getSetpointForState(setPointPerState, state);
            if (lastSetpoint != null) {
                return lastSetpoint;
            }
        }
        if (!elementaryResultMap.containsKey(rangeAction)) {
            throw new OpenRaoException(format("range action %s is not present in the result", rangeAction.getName()));
        }
        return elementaryResultMap.get(rangeAction).refSetpoint;
    }

    private Double getSetpointForState(Map<State, Double> setPointPerState, State state) {
        if (setPointPerState.containsKey(state)) {
            return setPointPerState.get(state);
        }
        Optional<State> previousState = getPreviousState(state);
        // setPointPerState does not contain a setpoint for any of state's previous states
        return previousState.map(value -> getSetpointForState(setPointPerState, value)).orElse(null);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        computeSetpointsPerStatePerPst();
        Map<RangeAction<?>, Double> optimizedSetpoints = new HashMap<>();
        elementaryResultMap.forEach((ra, re) -> optimizedSetpoints.put(ra, getOptimizedSetpoint(ra, state)));
        return optimizedSetpoints;
    }

    @Override
    public double getSetPointVariation(RangeAction<?> rangeAction, State state) {
        computeSetpointsPerStatePerPst();
        Optional<State> previousState = getPreviousState(state);
        if (previousState.isEmpty()) {
            return getOptimizedSetpoint(rangeAction, state) - elementaryResultMap.get(rangeAction).refSetpoint;
        } else {
            return getOptimizedSetpoint(rangeAction, state) - getOptimizedSetpoint(rangeAction, previousState.get());
        }
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

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        computeSetpointsPerStatePerPst();
        Optional<State> previousState = getPreviousState(state);
        if (previousState.isEmpty()) {
            return getOptimizedTap(pstRangeAction, state) - pstRangeAction.convertAngleToTap(elementaryResultMap.get(pstRangeAction).refSetpoint);
        } else {
            return getOptimizedTap(pstRangeAction, state) - getOptimizedTap(pstRangeAction, previousState.get());
        }
    }

    private Optional<State> getPreviousState(State state) {
        if (memoizedPreviousState.containsKey(state)) {
            return memoizedPreviousState.get(state);
        }
        Optional<State> previousState = elementaryResultMap.values().stream()
            .flatMap(eR -> eR.getAllStatesWithActivation().stream())
            .filter(s -> s.getContingency().equals(state.getContingency()) || s.getContingency().isEmpty())
            .filter(s -> s.getInstant().comesBefore(state.getInstant()))
            .max(Comparator.comparingInt(s -> s.getInstant().getOrder()));
        memoizedPreviousState.put(state, previousState);
        return previousState;
    }
}
