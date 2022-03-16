package com.farao_community.farao.search_tree_rao.commons.optimization_contexts;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractOptimizationContext implements OptimizationContext {

    private final State firstOptimizedState;
    private final Map<State, Set<RangeAction<?>>> availableRangeActions;

    public AbstractOptimizationContext(State firstOptimizedState, Map<State, Set<RangeAction<?>>> availableRangeActionsPerState) {

        if (!availableRangeActionsPerState.containsKey(firstOptimizedState)) {
            throw new FaraoException("some range actions should be available on the first optimized state of the context");
        }

        this.firstOptimizedState = firstOptimizedState;

        this.availableRangeActions = new HashMap<>();
        availableRangeActionsPerState.forEach((state, rangeActions) -> {
            Set<RangeAction<?>> rangeActionSet = new TreeSet<>(Comparator.comparing(Identifiable::getId));
            rangeActionSet.addAll(availableRangeActionsPerState.get(state));
            this.availableRangeActions.put(state, rangeActionSet);
        });
    }

    public State getFirstOptimizedState() {
        return firstOptimizedState;
    }

    public Set<State> getAllOptimizedStates() {
        return availableRangeActions.keySet();
    }

    public Map<State, Set<RangeAction<?>>> getAvailableRangeActions() {
        return availableRangeActions;
    }

    public Set<RangeAction<?>> getAllRangeActions() {
        return availableRangeActions.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
