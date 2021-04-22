/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class StateTree {

    Map<State, State> optimizedStatePerState = new HashMap<>();
    Map<State, Set<State>> perimeterPerOptimizedState = new HashMap<>();
    Set<String> operatorsNotSharingCras;

    public StateTree(Crac crac, State startingState) {
        List<List<State>> perimeters = new ArrayList<>();

        if (startingState.equals(crac.getPreventiveState())) {
            List<State> preventivePerimeter = new ArrayList<>();
            preventivePerimeter.add(startingState);
            perimeters.add(preventivePerimeter);

            List<State> currentPerimeter;
            for (Contingency contingency : crac.getContingencies()) {
                currentPerimeter = preventivePerimeter;
                for (State state : crac.getStates(contingency)) {
                    if (anyAvailableRemedialAction(crac, state)) {
                        currentPerimeter = new ArrayList<>();
                        perimeters.add(currentPerimeter);
                    }
                    currentPerimeter.add(state);
                }
            }
        } else {
            throw new NotImplementedException("Cannot create perimeters if starting state is different from preventive state");
        }

        perimeters.forEach(states -> {
            perimeterPerOptimizedState.put(states.get(0), new HashSet<>(states));
            states.forEach(state -> optimizedStatePerState.put(state, states.get(0)));
        });

        this.operatorsNotSharingCras = findOperatorsNotSharingCras(crac, getOptimizedStates());
    }

    public State getOptimizedState(State state) {
        return optimizedStatePerState.get(state);
    }

    public Set<State> getOptimizedStates() {
        return new HashSet<>(optimizedStatePerState.values());
    }

    public Set<State> getPerimeter(State optimizedState) {
        return perimeterPerOptimizedState.get(optimizedState);
    }

    public Set<String> getOperatorsNotSharingCras() {
        return operatorsNotSharingCras;
    }

    private static boolean anyAvailableRemedialAction(Crac crac, State state) {
        return !crac.getNetworkActions(state, UsageMethod.AVAILABLE).isEmpty() ||
                !crac.getRangeActions(state, UsageMethod.AVAILABLE).isEmpty();
    }

    static Set<String> findOperatorsNotSharingCras(Crac crac, Set<State> optimizedStates) {
        Set<String> tsos = crac.getFlowCnecs().stream().map(Cnec::getOperator).collect(Collectors.toSet());
        tsos.addAll(crac.getRangeActions().stream().map(RangeAction::getOperator).collect(Collectors.toSet()));
        tsos.addAll(crac.getNetworkActions().stream().map(NetworkAction::getOperator).collect(Collectors.toSet()));
        // <!> If a CNEC's operator is null, filter it out of the list of operators not sharing CRAs
        return tsos.stream().filter(tso -> !Objects.isNull(tso) && !tsoHasCra(tso, crac, optimizedStates)).collect(Collectors.toSet());
    }

    static boolean tsoHasCra(String tso, Crac crac, Set<State> optimizedStates) {
        for (State state : optimizedStates) {
            if ((!state.equals(crac.getPreventiveState())) &&
                    (crac.getNetworkActions(state, UsageMethod.AVAILABLE).stream().anyMatch(networkAction -> networkAction.getOperator().equals(tso)) ||
                            crac.getRangeActions(state, UsageMethod.AVAILABLE).stream().anyMatch(rangeAction -> rangeAction.getOperator().equals(tso)))) {
                return true;
            }
        }
        return false;
    }
}
