package com.farao_community.farao.search_tree_rao.commons.optimization_contexts;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

import java.util.Map;
import java.util.Set;

public interface OptimizationContext {

    State getFirstOptimizedState();

    Set<State> getAllOptimizedStates();

    Map<State, Set<RangeAction<?>>> getAvailableRangeActions();

    Set<RangeAction<?>> getAllRangeActions();
}
