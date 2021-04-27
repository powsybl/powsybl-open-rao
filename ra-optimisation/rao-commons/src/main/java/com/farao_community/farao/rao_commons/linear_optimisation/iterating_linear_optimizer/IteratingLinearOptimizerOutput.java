package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.*;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerOutput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class IteratingLinearOptimizerOutput implements LinearOptimizationResult {

    private LinearProblem.SolveStatus solveStatus;
    private final double functionalCost;
    private final double virtualCost;
    private final Map<RangeAction, Double> rangeActionSetpoints;
    private final Map<PstRangeAction, Integer> pstRangeActionTaps;
    private final SensitivityAndLoopflowResults sensitivityAndLoopflowResults;

    public IteratingLinearOptimizerOutput(LinearProblem.SolveStatus solveStatus, double functionalCost, double virtualCost, LinearOptimizerOutput linearOptimizerOutput, SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        this(solveStatus, functionalCost, virtualCost, linearOptimizerOutput.getRangeActionSetpoints(), linearOptimizerOutput.getPstRangeActionTaps(), sensitivityAndLoopflowResults);
    }

    public IteratingLinearOptimizerOutput(LinearProblem.SolveStatus solveStatus, double functionalCost, double virtualCost, Map<RangeAction, Double> optimalRangeActionSetpoints, Map<PstRangeAction, Integer> optimalPstRangeActionTaps, SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        this.functionalCost = functionalCost;
        this.virtualCost = virtualCost;
        this.sensitivityAndLoopflowResults = sensitivityAndLoopflowResults;
        this.solveStatus = solveStatus;
        this.rangeActionSetpoints = optimalRangeActionSetpoints;
        this.pstRangeActionTaps = optimalPstRangeActionTaps;
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

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        return null;
    }

    public double getVirtualCost() {
        return virtualCost;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return null;
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return 0;
    }

    @Override
    public List<BranchCnec> getCostlyElements(String virtualCostName, int number) {
        return null;
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

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getRelativeMargin(BranchCnec branchCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        return 0;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return 0;
    }

    @Override
    public Set<RangeAction> getActivatedRangeActions() {
        return null;
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return null;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return null;
    }

    @Override
    public LinearOptimizationStatus getStatus() {
        return null;
    }
}
