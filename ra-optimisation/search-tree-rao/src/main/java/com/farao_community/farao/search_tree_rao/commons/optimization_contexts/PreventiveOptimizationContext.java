package com.farao_community.farao.search_tree_rao.commons.optimization_contexts;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

import java.util.Map;
import java.util.Set;

public class PreventiveOptimizationContext extends AbstractOptimizationPerimeter {

    public PreventiveOptimizationContext(State preventiveState, Set<RangeAction<?>> availableRangeActions) {
        super(preventiveState, Map.of(preventiveState, availableRangeActions));

        if (!preventiveState.isPreventive()) {
            throw new FaraoException("a PreventiveOptimizationContext must be based on the preventive state");
        }
    }
}
