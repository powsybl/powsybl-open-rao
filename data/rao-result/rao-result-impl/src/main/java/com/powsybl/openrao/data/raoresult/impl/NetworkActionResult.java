/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.data.crac.api.State;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionResult {

    private final Set<State> isActivatedInStates;

    NetworkActionResult() {
        isActivatedInStates = new HashSet<>();
    }

    public boolean isActivated(State state) {
        return isActivatedInStates.contains(state);
    }

    public Set<State> getStatesWithActivation() {
        return isActivatedInStates;
    }

    public void addActivationForState(State state) {
        isActivatedInStates.add(state);
    }

    public void addActivationForStates(Set<State> states) {
        isActivatedInStates.addAll(states);
    }

}
