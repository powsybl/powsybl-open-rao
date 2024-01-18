/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.State;

import java.util.Comparator;
import java.util.List;
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
    private final State automatonState;
    private final List<State> curativeStates;

    /**
     * Construct a post-contingency scenario
     *
     * @param contingency the contingency (required)
     * @param automatonState the automaton state
     * @param curativeStates the curative states (required)
     */
    public ContingencyScenario(Contingency contingency, State automatonState, List<State> curativeStates) {
        Objects.requireNonNull(contingency);
        Objects.requireNonNull(curativeStates);
        if (curativeStates.isEmpty()) {
            throw new OpenRaoException("There should be at least one contingency state.");
        }
        // Automaton state
        if (automatonState != null) {
            if (!automatonState.getInstant().isAuto()) {
                throw new OpenRaoException(String.format("State %s is not auto.", automatonState.getId()));
            }
            checkStateContingency(contingency, automatonState);
        }
        // Curative states
        for (State curativeState : curativeStates) {
            if (!curativeState.getInstant().isCurative()) {
                throw new OpenRaoException(String.format("State %s is not curative.", curativeState.getId()));
            }
            checkStateContingency(contingency, curativeState);
        }
        this.contingency = contingency;
        this.automatonState = automatonState;
        this.curativeStates = curativeStates.stream().sorted(Comparator.comparing(state -> state.getInstant().getOrder())).toList();
    }

    private static void checkStateContingency(Contingency contingency, State state) {
        Optional<Contingency> stateContingency = state.getContingency();
        if (stateContingency.isEmpty()) {
            throw new OpenRaoException(String.format("State %s has no contingency.", state.getId()));
        } else if (!contingency.equals(stateContingency.get())) {
            throw new OpenRaoException(String.format("State %s does not refer to expected contingency %s.", state.getId(), contingency.getId()));
        }
    }

    /**
     * Construct a post-contingency scenario
     *
     * @param curativeStates thecurative states (required)
     */
    public ContingencyScenario(List<State> curativeStates) {
        this(curativeStates.iterator().next().getContingency().orElse(null), null, curativeStates);
    }

    /**
     * Construct a post-contingency scenario
     *
     * @param contingency the contingency (required)
     * @param curativeStates the curative states (required)
     */
    public ContingencyScenario(Contingency contingency, List<State> curativeStates) {
        this(contingency, null, curativeStates);
    }

    /**
     * Construct a post-contingency scenario
     *
     * @param automatonState the automaton state (required)
     * @param curativeStates the curative states (required)
     */
    public ContingencyScenario(State automatonState, List<State> curativeStates) {
        this(automatonState.getContingency().orElse(null), automatonState, curativeStates);
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
}
