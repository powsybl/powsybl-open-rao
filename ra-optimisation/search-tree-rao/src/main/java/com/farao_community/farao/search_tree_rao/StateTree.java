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
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class StateTree {

    Map<State, State> optimizedStatePerState = new HashMap<>();
    Map<State, Set<State>> perimeterPerOptimizedState = new HashMap<>();

    public StateTree(Crac crac, Network network, State startingState) {
        List<List<State>> perimeters = new ArrayList<>();

        if (startingState.equals(crac.getPreventiveState())) {
            List<State> preventivePerimeter = new ArrayList<>();
            preventivePerimeter.add(startingState);
            perimeters.add(preventivePerimeter);

            List<State> currentPerimeter;
            for (Contingency contingency : crac.getContingencies()) {
                currentPerimeter = preventivePerimeter;
                for (State state : crac.getStates(contingency)) {
                    if (anyAvailableRemedialAction(crac, network, state)) {
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

    private static boolean anyAvailableRemedialAction(Crac crac, Network network, State state) {
        return !crac.getNetworkActions(network, state, UsageMethod.AVAILABLE).isEmpty() ||
            !crac.getRangeActions(network, state, UsageMethod.AVAILABLE).isEmpty();
    }
}
