/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.state_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.State;

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
    private final State curativeState;

    /**
     * Construct a post-contingency scenario
     * @param contingency the contingency (required)
     * @param automatonState the automaton state (can be null)
     * @param curativeState the curative state (required)
     */
    public ContingencyScenario(Contingency contingency, State automatonState, State curativeState) {
        Objects.requireNonNull(contingency);
        Objects.requireNonNull(curativeState);
        Optional<Contingency> curativeContingency = curativeState.getContingency();
        if (curativeContingency.isEmpty() || !curativeContingency.get().equals(contingency)) {
            throw new FaraoException(String.format("Curative state %s do not refer to the contingency %s", curativeState, contingency));
        }
        if (automatonState != null) {
            Optional<Contingency> automatonContingency = automatonState.getContingency();
            if (automatonContingency.isEmpty() || !automatonContingency.get().equals(contingency)) {
                throw new FaraoException(String.format("Automaton state %s do not refer to the contingency %s", automatonState, contingency));
            }
        }
        this.contingency = contingency;
        this.automatonState = automatonState;
        this.curativeState = curativeState;
    }

    /**
     * Construct a post-contingency scenario
     * @param automatonState the automaton state (can be null)
     * @param curativeState the curative state (required)
     */
    public ContingencyScenario(State automatonState, State curativeState) {
        this(curativeState.getContingency().orElse(null), automatonState, curativeState);
    }

    public Contingency getContingency() {
        return contingency;
    }

    public Optional<State> getAutomatonState() {
        return automatonState == null ? Optional.empty() : Optional.of(automatonState);
    }

    public State getCurativeState() {
        return curativeState;
    }
}
