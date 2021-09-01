/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.state_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents the functional contingency scenario
 * It contains the auto and curative states that should be optimized after a given contingency
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BasecaseScenario {
    private final State basecaseState;
    private final Set<State> otherStates;

    /**
     * Construct a basecase scenario
     * @param basecaseState the basecase state (required)
     * @param otherStates the other states to optimize in preventive (can be empty or null)
     */
    public BasecaseScenario(State basecaseState, Set<State> otherStates) {
        Objects.requireNonNull(basecaseState);
        if (!basecaseState.getInstant().equals(Instant.PREVENTIVE)) {
            throw new FaraoException(String.format("Basecase state %s is not preventive", basecaseState));
        }
        if (otherStates != null && otherStates.stream().anyMatch(state -> state.getInstant().equals(Instant.PREVENTIVE))) {
            throw new FaraoException("OtherStates should not be preventive");
        }
        this.basecaseState = basecaseState;
        this.otherStates = otherStates == null ? new HashSet<>() : otherStates;
    }

    public State getBasecaseState() {
        return basecaseState;
    }

    public Set<State> getOtherStates() {
        return otherStates;
    }

    public Set<State> getAllStates() {
        Set<State> states = new HashSet<>(otherStates);
        states.add(basecaseState);
        return states;
    }

    void addOtherState(State state) {
        if (state.getInstant().equals(Instant.PREVENTIVE)) {
            throw new FaraoException("OtherStates should not be preventive");
        }
        otherStates.add(state);
    }
}
