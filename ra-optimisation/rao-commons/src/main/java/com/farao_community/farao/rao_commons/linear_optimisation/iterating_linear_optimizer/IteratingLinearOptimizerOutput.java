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
    private final double functionalCost;
    private final double virtualCost;
    private final Map<RangeAction, Double> rangeActionSetpoints;
    private final Map<PstRangeAction, Integer> pstRangeActionTaps;
    private final SensitivityAndLoopflowResults sensitivityAndLoopflowResults;

    public IteratingLinearOptimizerOutput(SolveStatus solveStatus, double functionalCost, double virtualCost, LinearOptimizerOutput linearOptimizerOutput, SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        this(solveStatus, functionalCost, virtualCost, linearOptimizerOutput.getRangeActionSetpoints(), linearOptimizerOutput.getPstRangeActionTaps(), sensitivityAndLoopflowResults);
    }

    public IteratingLinearOptimizerOutput(SolveStatus solveStatus, double functionalCost, double virtualCost, Map<RangeAction, Double> optimalRangeActionSetpoints, Map<PstRangeAction, Integer> optimalPstRangeActionTaps, SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
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

    public double getFunctionalCost() {
        return functionalCost;
    }

    public double getVirtualCost() {
        return virtualCost;
    }

    public double getCost() {
        return functionalCost + virtualCost;
    }

    public Map<RangeAction, Double> getRangeActionSetpoints() {
        return rangeActionSetpoints;
    }

    public Double getRangeActionSetpoint(RangeAction rangeAction) {
        return rangeActionSetpoints.get(rangeAction);
    }

    public Map<PstRangeAction, Integer> getPstRangeActionTaps() {
        return pstRangeActionTaps;
    }

    public Integer getPstRangeActionTap(PstRangeAction pstRangeAction) {
        return pstRangeActionTaps.get(pstRangeAction);
    }

    public SensitivityAndLoopflowResults getSensitivityAndLoopflowResults() {
        return sensitivityAndLoopflowResults;
    }
}
