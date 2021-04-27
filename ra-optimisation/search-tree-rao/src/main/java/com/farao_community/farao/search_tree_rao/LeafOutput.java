package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LeafOutput implements PerimeterResult {

    private final BranchResult branchResult;
    private final RangeActionResult rangeActionResult;
    private final ObjectiveFunctionResult objectiveFunctionResult;
    private final Set<NetworkAction> activatedNetworkActions;
    private final Set<RangeAction> activatedRangeActions;
    private final PerimeterStatus perimeterStatus;

    public LeafOutput(LinearOptimizationResult linearOptimizationResult, Set<NetworkAction> activatedNetworkActions, Set<RangeAction> activatedRangeActions, PerimeterStatus perimeterStatus) {
        this(linearOptimizationResult, linearOptimizationResult, linearOptimizationResult, activatedNetworkActions, activatedRangeActions, perimeterStatus);
    }

    public LeafOutput(BranchResult branchResult, RangeActionResult rangeActionResult, ObjectiveFunctionResult objectiveFunctionResult, Set<NetworkAction> activatedNetworkActions, Set<RangeAction> activatedRangeActions, PerimeterStatus perimeterStatus) {
        this.branchResult = branchResult;
        this.rangeActionResult = rangeActionResult;
        this.objectiveFunctionResult = objectiveFunctionResult;
        this.activatedNetworkActions = activatedNetworkActions;
        this.activatedRangeActions = activatedRangeActions;
        this.perimeterStatus = perimeterStatus;
    }

    @Override
    public PerimeterStatus getStatus() {
        return perimeterStatus;
    }

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        return branchResult.getFlow(branchCnec, unit);
    }

    @Override
    public double getRelativeMargin(BranchCnec branchCnec, Unit unit) {
        return branchResult.getRelativeMargin(branchCnec, unit);
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        return branchResult.getCommercialFlow(branchCnec, unit);
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        return branchResult.getPtdfZonalSum(branchCnec);
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return activatedNetworkActions.contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return activatedNetworkActions;
    }

    @Override
    public double getFunctionalCost() {
        return objectiveFunctionResult.getFunctionalCost();
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        return objectiveFunctionResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return objectiveFunctionResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return objectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return objectiveFunctionResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<BranchCnec> getCostlyElements(String virtualCostName, int number) {
        return objectiveFunctionResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return rangeActionResult.getOptimizedTap(pstRangeAction);
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return rangeActionResult.getOptimizedSetPoint(rangeAction);
    }

    @Override
    public Set<RangeAction> getActivatedRangeActions() {
        return getActivatedRangeActions();
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return rangeActionResult.getOptimizedTaps();
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return rangeActionResult.getOptimizedSetPoints();
    }
}
