package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;

import java.util.Map;

public class LinearOptimizerOutput {

    private final Map<RangeAction, Double> optimalRangeActionSetpoints;
    private final Map<PstRangeAction, Integer> optimalPstRangeActionTaps;

    public LinearOptimizerOutput(Map<RangeAction, Double> optimalRangeActionSetpoints, Map<PstRangeAction, Integer> optimalPstRangeActionTaps) {
        this.optimalRangeActionSetpoints = optimalRangeActionSetpoints;
        this.optimalPstRangeActionTaps = optimalPstRangeActionTaps;
    }

    public Map<RangeAction, Double> getOptimalRangeActionSetpoints() {
        return optimalRangeActionSetpoints;
    }

    public Map<PstRangeAction, Integer> getOptimalPstRangeActionTaps() {
        return optimalPstRangeActionTaps;
    }
}
