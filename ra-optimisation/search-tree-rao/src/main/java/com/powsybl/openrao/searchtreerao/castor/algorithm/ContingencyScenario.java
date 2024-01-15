/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This class represents the functional contingency scenario
 * It contains the auto and curative states that should be optimized after a given contingency
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ContingencyScenario {
    private final Contingency contingency;
    private final List<State> curativeStates;

    /**
     * Construct a post-contingency scenario
     * @param contingency the contingency (required)
     * @param curativeStates the automaton and curative states (required)
     */
    public ContingencyScenario(Contingency contingency, List<State> curativeStates) {
        Objects.requireNonNull(contingency);
        if (curativeStates.isEmpty()) {
            throw new OpenRaoException("There should be at least one contingency state");
        }
        for (State curativeState : curativeStates.values()) {
            if (!curativeState.getInstant().isCurative()) {
                throw new OpenRaoException(String.format("State %s is not curative.", curativeState.getId()));
            }
            Optional<Contingency> stateContingency = curativeState.getContingency();
            if (stateContingency.isEmpty()) {
                throw new OpenRaoException("State %s has no contingency.");
            } else if (!contingency.equals(stateContingency.get())) {
                throw new OpenRaoException(String.format("State %s does not refer to expected contingency %s.", curativeState.getId(), contingency.getId()));
            }
        }
        if (automatonState != null) {
            Optional<Contingency> automatonContingency = automatonState.getContingency();
            if (automatonContingency.isEmpty() || !automatonContingency.get().equals(contingency)) {
                throw new OpenRaoException(String.format("Automaton state %s do not refer to the contingency %s", automatonState, contingency));
            }
        }
        this.contingency = contingency;
        this.automatonState = automatonState;
        this.curativeStates = new HashMap<>(curativeStates);
    }

    /**
     * Construct a post-contingency scenario
     * @param automatonState the automaton state (can be null)
     * @param curativeStates the curative states (required)
     */
    public ContingencyScenario(State automatonState, Map<Instant, State> curativeStates) {
        this(contingencyStates.values().iterator().next().getContingency().orElse(null), automatonState, curativeStates);
    }

    public Contingency getContingency() {
        return contingency;
    }

    public Optional<State> getAutomatonState() {
        return automatonState == null ? Optional.empty() : Optional.of(automatonState);
    }

    public List<State> getCurativeStates() {
        return curativeStates;
    }

    private static Contingency getContingencyFromCurativeStatesList(List<State> curativeStates) {
        if (curativeStates.isEmpty()) {
            throw new OpenRaoException("No curative states provided.");
        }
        Optional<Contingency> contingency = curativeStates.get(0).getContingency();
        if (contingency.isEmpty()) {
            throw new OpenRaoException("No contingency defined for the provided curative states.");
        }
        String contingencyId = contingency.get().getId();
        for (State curativeState : curativeStates) {
            if (!curativeState.getInstant().isCurative()) {
                throw new OpenRaoException("State %s is not curative.".formatted(curativeState.getId()));
            }
            Optional<Contingency> stateContingency = curativeState.getContingency();
            if (stateContingency.isEmpty()) {
                throw new OpenRaoException("Curative state %s has no associated contingency.".formatted(curativeState.getId()));
            }
            String stateContingencyId = stateContingency.get().getId();
            if (!contingencyId.equals(stateContingencyId)) {
                throw new OpenRaoException("The contingency of curative state %s 's contingency inconsistent with the contingency of the other states of the list: '%s' (expected) != '%s' (actual).".formatted(curativeState.getId(), contingencyId, stateContingencyId));
            }
        }
        return contingency.get();
    }
}
