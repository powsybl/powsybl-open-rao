/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;

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
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class Perimeter {
    private final State raOptimisationState;
    private final Set<State> cnecStates;

    /**
     * Construct a perimeter
     *
     * @param raOptimisationState the optimisation state for which remedial actions are available (required)
     * @param cnecStates the other states to optimize in the perimeter (can be empty or null)
     */
    public Perimeter(State raOptimisationState, Set<State> cnecStates) {
        Objects.requireNonNull(raOptimisationState);
        this.raOptimisationState = raOptimisationState;
        this.cnecStates = new HashSet<>();
        if (Objects.nonNull(cnecStates)) {
            cnecStates.forEach(this::addOtherState);
        }
    }

    public State getRaOptimisationState() {
        return raOptimisationState;
    }

    public Set<State> getAllStates() {
        Set<State> states = new HashSet<>(cnecStates);
        states.add(raOptimisationState);
        return states;
    }

    void addOtherState(State state) {
        boolean isRaOptimizationState = checkStateConsistency(state);
        if (!isRaOptimizationState) {
            cnecStates.add(state);
        }
    }

    private boolean checkStateConsistency(State state) {
        Optional<Contingency> optimisationStateContingency = raOptimisationState.getContingency();
        if (optimisationStateContingency.isPresent() && !optimisationStateContingency.equals(state.getContingency())) {
            throw new OpenRaoException("Contingency should be the same for the optimisation state and the other states.");
        }
        if (raOptimisationState.getInstant().equals(state.getInstant())) {
            return true;
        }
        if (raOptimisationState.getInstant().comesAfter(state.getInstant())) {
            throw new OpenRaoException("Other states should occur after the optimisation state.");
        }
        return false;
    }
}
