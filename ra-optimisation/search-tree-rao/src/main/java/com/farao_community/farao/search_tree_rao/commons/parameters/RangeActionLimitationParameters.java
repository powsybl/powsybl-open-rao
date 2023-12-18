/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.commons.parameters;

import com.powsybl.open_rao.data.crac_api.State;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeActionLimitationParameters {

    private final Map<State, RangeActionLimitation> rangeActionLimitationPerState;

    private static class RangeActionLimitation {
        private Integer maxRangeActions = null;
        private Integer maxTso = null;
        private Set<String> maxTsoExclusion = new HashSet<>();
        private Map<String, Integer> maxPstPerTso = new HashMap<>();
        private Map<String, Integer> maxRangeActionPerTso = new HashMap<>();
    }

    public RangeActionLimitationParameters() {
        this.rangeActionLimitationPerState = new HashMap<>();
    }

    public boolean areRangeActionLimitedForState(State state) {
        return rangeActionLimitationPerState.containsKey(state) && (
            rangeActionLimitationPerState.get(state).maxRangeActions != null
            || rangeActionLimitationPerState.get(state).maxTso != null
            || !rangeActionLimitationPerState.get(state).maxPstPerTso.isEmpty()
            || !rangeActionLimitationPerState.get(state).maxRangeActionPerTso.isEmpty());
    }

    public Integer getMaxRangeActions(State state) {
        if (rangeActionLimitationPerState.containsKey(state)) {
            return rangeActionLimitationPerState.get(state).maxRangeActions;
        } else {
            return null;
        }
    }

    public Integer getMaxTso(State state) {
        if (rangeActionLimitationPerState.containsKey(state)) {
            return rangeActionLimitationPerState.get(state).maxTso;
        } else {
            return null;
        }
    }

    public Set<String> getMaxTsoExclusion(State state) {
        if (rangeActionLimitationPerState.containsKey(state)) {
            return rangeActionLimitationPerState.get(state).maxTsoExclusion;
        } else {
            return new HashSet<>();
        }
    }

    public Map<String, Integer> getMaxPstPerTso(State state) {
        if (rangeActionLimitationPerState.containsKey(state)) {
            return rangeActionLimitationPerState.get(state).maxPstPerTso;
        } else {
            return new HashMap<>();
        }
    }

    public Map<String, Integer> getMaxRangeActionPerTso(State state) {
        if (rangeActionLimitationPerState.containsKey(state)) {
            return rangeActionLimitationPerState.get(state).maxRangeActionPerTso;
        } else {
            return new HashMap<>();
        }
    }

    public void setMaxRangeAction(State state, int maxRangeActions) {
        createIfAbsent(state);
        rangeActionLimitationPerState.get(state).maxRangeActions = maxRangeActions;
    }

    public void setMaxTso(State state, int maxTso) {
        createIfAbsent(state);
        rangeActionLimitationPerState.get(state).maxTso = maxTso;
    }

    public void setMaxTsoExclusion(State state, Set<String> maxTsoExclusion) {
        createIfAbsent(state);
        rangeActionLimitationPerState.get(state).maxTsoExclusion = maxTsoExclusion;
    }

    public void setMaxPstPerTso(State state, Map<String, Integer> maxPstPerTso) {
        createIfAbsent(state);
        rangeActionLimitationPerState.get(state).maxPstPerTso = maxPstPerTso;
    }

    public void setMaxRangeActionPerTso(State state, Map<String, Integer> maxRangeActionPerTso) {
        createIfAbsent(state);
        rangeActionLimitationPerState.get(state).maxRangeActionPerTso = maxRangeActionPerTso;
    }

    private void createIfAbsent(State state) {
        rangeActionLimitationPerState.computeIfAbsent(state, s -> new RangeActionLimitation());
    }
}
