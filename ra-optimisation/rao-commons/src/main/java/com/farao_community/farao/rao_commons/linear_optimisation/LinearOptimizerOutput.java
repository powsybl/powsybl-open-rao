package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;

import java.util.Map;

public class LinearOptimizerOutput {
    private final LinearProblem.SolveStatus solveStatus;
    private final Map<RangeAction, Double> rangeActionSetpoints;
    private final Map<PstRangeAction, Integer> pstRangeActionTaps;

    public LinearOptimizerOutput(LinearProblem.SolveStatus solveStatus, Map<RangeAction, Double> rangeActionSetpoints, Map<PstRangeAction, Integer> pstRangeActionTaps) {
        this.solveStatus = solveStatus;
        this.rangeActionSetpoints = rangeActionSetpoints;
        this.pstRangeActionTaps = pstRangeActionTaps;
    }

    public Map<RangeAction, Double> getRangeActionSetpoints() {
        return rangeActionSetpoints;
    }

    public Map<PstRangeAction, Integer> getPstRangeActionTaps() {
        return pstRangeActionTaps;
    }

    public LinearProblem.SolveStatus getSolveStatus() {
        return solveStatus;
    }
}
