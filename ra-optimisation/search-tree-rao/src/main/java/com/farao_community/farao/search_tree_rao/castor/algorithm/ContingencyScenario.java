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
        if (contingencyStates.isEmpty()) {
            throw new FaraoException("There should be at least one contingency state");
        }
        State anyState = contingencyStates.values().iterator().next();
        Optional<Contingency> anyStateContingency = anyState.getContingency();
        if (anyStateContingency.isEmpty() || !anyStateContingency.get().equals(contingency)) {
            throw new FaraoException(String.format("Curative state %s do not refer to the contingency %s", anyState, contingency));
        }
        if (contingencyStates.values().stream().anyMatch(state -> !state.getContingency().equals(anyStateContingency))) {
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
        // TODO : remove this, only used in tests
        this(curativeState.getContingency().orElse(null), null);
    }

    public ContingencyScenario(Contingency contingency, State automatonState, State curativeState) {
        // TODO : remove this, only used in tests
        this(contingency, null);
    }

    public Contingency getContingency() {
        return contingency;
    }

    public Optional<State> getAutomatonState() {
        return contingencyStates.entrySet().stream()
            .filter(e -> e.getKey().isAuto())
            .map(Map.Entry::getValue)
            .findAny();
    }

    public State getAnyCurativeState() {
        return contingencyStates.entrySet().stream()
            .filter(e -> e.getKey().isCurative())
            .map(Map.Entry::getValue)
            .findAny().orElseThrow();
    }

    // TODO : UT
    public List<State> getCurativeStates() {
        return contingencyStates.entrySet().stream()
            .filter(e -> e.getKey().isCurative())
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }
}
