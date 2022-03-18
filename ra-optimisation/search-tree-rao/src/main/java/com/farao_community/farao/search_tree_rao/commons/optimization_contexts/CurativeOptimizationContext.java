package com.farao_community.farao.search_tree_rao.commons.optimization_contexts;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

import java.util.Map;
import java.util.Set;

public class CurativeOptimizationContext extends AbstractOptimizationPerimeter {

    public CurativeOptimizationContext(State curativeState, Set<RangeAction<?>> availableRangeActions) {
        super(curativeState, Map.of(curativeState, availableRangeActions));

        if (!curativeState.getInstant().equals(Instant.CURATIVE)) {
            throw new FaraoException("a CurativeOptimizationContext must be based on a curative state");
        }
    }
}
