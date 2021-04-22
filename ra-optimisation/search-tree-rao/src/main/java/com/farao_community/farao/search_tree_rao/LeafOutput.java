package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerOutput;

import java.util.Map;
import java.util.Set;

public class LeafOutput {

    private LinearProblem.SolveStatus solveStatus;
    private final double functionalCost;
    private final double virtualCost;
    private final Map<RangeAction, Double> rangeActionSetpoints;
    private final Map<PstRangeAction, Integer> pstRangeActionTaps;
    private final SensitivityAndLoopflowResults sensitivityAndLoopflowResults;
    private final Set<NetworkAction> activatedNetworkActions;

    public LeafOutput(IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput, Set<NetworkAction> activatedNetworkActions) {
        this(iteratingLinearOptimizerOutput.getSolveStatus(),
                iteratingLinearOptimizerOutput.getFunctionalCost(),
                iteratingLinearOptimizerOutput.getVirtualCost(),
                iteratingLinearOptimizerOutput.getRangeActionSetpoints(),
                iteratingLinearOptimizerOutput.getPstRangeActionTaps(),
                iteratingLinearOptimizerOutput.getSensitivityAndLoopflowResults(),
                activatedNetworkActions);
    }

    public LeafOutput(LinearProblem.SolveStatus solveStatus,
                      double functionalCost,
                      double virtualCost,
                      Map<RangeAction, Double> optimalRangeActionSetpoints,
                      Map<PstRangeAction, Integer> optimalPstRangeActionTaps,
                      SensitivityAndLoopflowResults sensitivityAndLoopflowResults,
                      Set<NetworkAction> activatedNetworkActions) {
        this.functionalCost = functionalCost;
        this.virtualCost = virtualCost;
        this.sensitivityAndLoopflowResults = sensitivityAndLoopflowResults;
        this.solveStatus = solveStatus;
        this.rangeActionSetpoints = optimalRangeActionSetpoints;
        this.pstRangeActionTaps = optimalPstRangeActionTaps;
        this.activatedNetworkActions = activatedNetworkActions;
    }

    void setStatus(LinearProblem.SolveStatus solveStatus) {
        this.solveStatus = solveStatus;
    }

    public LinearProblem.SolveStatus getSolveStatus() {
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
