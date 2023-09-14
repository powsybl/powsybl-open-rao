/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents the functional contingency scenario
 * It contains the auto and curative states that should be optimized after a given contingency
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ContingencyScenario {
    private final Contingency contingency;
    private final Map<Instant, State> contingencyStates;

    /**
     * Construct a post-contingency scenario
     * @param contingency the contingency (required)
     * @param contingencyStates TODO
     */
    public ContingencyScenario(Contingency contingency, Map<Instant, State> contingencyStates) {
        Objects.requireNonNull(contingency);
        State curativeState = contingencyStates.get(Instant.CURATIVE);
        Objects.requireNonNull(curativeState);
        Optional<Contingency> curativeContingency = curativeState.getContingency();
        if (curativeContingency.isEmpty() || !curativeContingency.get().equals(contingency)) {
            throw new FaraoException(String.format("Curative state %s do not refer to the contingency %s", curativeState, contingency));
        }
        if (contingencyStates.values().stream().anyMatch(state -> !state.getContingency().equals(curativeContingency))) {
            throw new FaraoException(String.format("All states do not refer to the same contingency %s", contingency));
        }
        this.contingency = contingency;
        this.contingencyStates = new HashMap<>(contingencyStates);
    }

    public ContingencyScenario(Map<Instant, State> contingencyStates) {
        this(contingencyStates.values().iterator().next().getContingency().orElse(null), contingencyStates);
    }

    /**
     * Construct a post-contingency scenario
     * @param automatonState the automaton state (can be null)
     * @param curativeState the curative state (required)
     */
    public ContingencyScenario(State automatonState, State curativeState) {
        // TODO : remove this
        this(curativeState.getContingency().orElse(null), Map.of(Instant.AUTO, automatonState, Instant.CURATIVE, curativeState));
    }

    public ContingencyScenario(Contingency contingency, State automatonState, State curativeState) {
        // TODO : remove this
        this(contingency, Map.of(Instant.AUTO, automatonState, Instant.CURATIVE, curativeState));
    }

    public Contingency getContingency() {
        return contingency;
    }

    public Optional<State> getAutomatonState() {
        return Optional.ofNullable(contingencyStates.get(Instant.AUTO));
    }

    public State getCurativeState() {
        return contingencyStates.get(Instant.CURATIVE);
    }

    // TODO : UT
    public List<State> getCurativeStates() {
        return Arrays.stream(Instant.values()).filter(instant -> instant.comesAfter(Instant.AUTO) && contingencyStates.containsKey(instant))
            .sorted().map(contingencyStates::get).collect(Collectors.toList());
    }
}
