/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.State;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * This class represents a functional perimeter
 * It contains the optimisation state for which the remedial actions are available,
 * as well as other states that should have their CNECs optimised
 * at the optimisation state's instant (ie outage states and curative states that have no RAs)
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class Perimeter {
    private final State optimisationState;
    private final Set<State> otherStates;

    /**
     * Construct a perimeter
     * @param optimisationState the optimisation state for which remedial actions are available (required)
     * @param otherStates the other states to optimize in the perimeter (can be empty or null)
     */
    public Perimeter(State optimisationState, Set<State> otherStates) {
        Objects.requireNonNull(optimisationState);
        this.optimisationState = optimisationState;
        if (Objects.nonNull(otherStates)) {
            otherStates.forEach(this::checkStateConsistency);
            this.otherStates = otherStates;
        } else {
            this.otherStates = new HashSet<>();
        }
    }

    public State getOptimisationState() {
        return optimisationState;
    }

    public Set<State> getOtherStates() {
        return otherStates;
    }

    public Set<State> getAllStates() {
        Set<State> states = new HashSet<>(otherStates);
        states.add(optimisationState);
        return states;
    }

    void addOtherState(State state) {
        checkStateConsistency(state);
        otherStates.add(state);
    }

    private void checkStateConsistency(State state) {
        Optional<Contingency> optimisationStateContingency = optimisationState.getContingency();
        if (optimisationStateContingency.isPresent() && !optimisationStateContingency.equals(state.getContingency())) {
            throw new OpenRaoException("Contingency should be the same for the optimisation state and the other states.");
        }
        if (!optimisationState.getInstant().comesBefore(state.getInstant())) {
            throw new OpenRaoException("Other states should occur after the optimisation state.");
        }
    }
}
