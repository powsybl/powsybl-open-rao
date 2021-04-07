package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;

import java.util.Map;

public class LinearOptimizerOutput {
    public enum SolveStatus {
        OPTIMAL,
        FEASIBLE,
        INFEASIBLE,
        UNBOUNDED,
        ABNORMAL,
        NOT_SOLVED
    }
    private final SolveStatus solveStatus;
    private final Map<RangeAction, Double> rangeActionSetpoints;
    private final Map<PstRangeAction, Integer> pstRangeActionTaps;

    public LinearOptimizerOutput(SolveStatus solveStatus, Map<RangeAction, Double> rangeActionSetpoints, Map<PstRangeAction, Integer> pstRangeActionTaps) {
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

    public SolveStatus getSolveStatus() {
        return solveStatus;
    }
}
