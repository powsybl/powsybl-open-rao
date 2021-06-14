package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.State;

import java.util.HashSet;
import java.util.Set;

public class NetworkActionResult {

    private Set<State> isBeingActivatedStates;

    NetworkActionResult() {
        isBeingActivatedStates = new HashSet<>();
    }

    public Set<State> getIsBeingActivatedStates() {
        return isBeingActivatedStates;
    }

    public void addStateDuringWhichActionIsActivated(State state) {
        isBeingActivatedStates.add(state);
    }

    public void addStatesDuringWhichActionIsActivated(Set<State> states) {
        isBeingActivatedStates.addAll(states);
    }

}
