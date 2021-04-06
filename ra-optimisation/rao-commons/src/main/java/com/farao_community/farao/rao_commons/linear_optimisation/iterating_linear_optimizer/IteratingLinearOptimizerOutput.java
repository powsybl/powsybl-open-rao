package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerOutput;

import java.util.Map;

public class IteratingLinearOptimizerOutput {

    public enum SolveStatus {
        OPTIMAL,
        FEASIBLE,
        INFEASIBLE,
        UNBOUNDED,
        ABNORMAL,
        NOT_SOLVED
    }
    private SolveStatus solveStatus;
    private final Map<RangeAction, Double> rangeActionSetpoints;
    private final Map<PstRangeAction, Integer> pstRangeActionTaps;
    private final SensitivityAndLoopflowResults sensitivityAndLoopflowResults;
    private final double functionalCost;
    private final double virtualCost;

    public IteratingLinearOptimizerOutput(double functionalCost, double virtualCost, SensitivityAndLoopflowResults sensitivityAndLoopflowResults, SolveStatus solveStatus, LinearOptimizerOutput linearOptimizerOutput) {
        this(functionalCost, virtualCost, sensitivityAndLoopflowResults, solveStatus, linearOptimizerOutput.getRangeActionSetpoints(), linearOptimizerOutput.getPstRangeActionTaps());
    }

    public IteratingLinearOptimizerOutput(double functionalCost, double virtualCost, SensitivityAndLoopflowResults sensitivityAndLoopflowResults, SolveStatus solveStatus, Map<RangeAction, Double> optimalRangeActionSetpoints, Map<PstRangeAction, Integer> optimalPstRangeActionTaps) {
        this.functionalCost = functionalCost;
        this.virtualCost = virtualCost;
        this.sensitivityAndLoopflowResults = sensitivityAndLoopflowResults;
        this.solveStatus = solveStatus;
        this.rangeActionSetpoints = optimalRangeActionSetpoints;
        this.pstRangeActionTaps = optimalPstRangeActionTaps;
    }

    void setStatus(SolveStatus solveStatus) {
        this.solveStatus = solveStatus;
    }

    public SolveStatus getSolveStatus() {
        return solveStatus;
    }

    public Map<RangeAction, Double> getRangeActionSetpoints() {
        return rangeActionSetpoints;
    }

    public Map<PstRangeAction, Integer> getPstRangeActionTaps() {
        return pstRangeActionTaps;
    }

    public SensitivityAndLoopflowResults getSensitivityAndLoopflowResults() {
        return sensitivityAndLoopflowResults;
    }

    public double getFunctionalCost() {
        return functionalCost;
    }

    public double getVirtualCost() {
        return virtualCost;
    }

    public double getCost() {
        return functionalCost + virtualCost;
    }
}
