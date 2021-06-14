package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.State;

import java.util.HashSet;
import java.util.Set;

public class NetworkActionResult {

    private Set<State> isBeingActivatedStates;

    NetworkActionResult() {
        isBeingActivatedStates = new HashSet<>();
    }

    public Set<State> getStatesWithActivation() {
        return isBeingActivatedStates;
    }

    public void addActivationForState(State state) {
        isBeingActivatedStates.add(state);
    }

    public void addActivationForStates(Set<State> states) {
        isBeingActivatedStates.addAll(states);
    }

}
