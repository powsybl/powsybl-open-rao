package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.*;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;
import java.util.stream.Collectors;

public class PerimeterOutput implements PerimeterResult {

    OptimizationResult optimizationResult;
    PrePerimeterResult prePerimeterResult;

    public PerimeterOutput(PrePerimeterResult prePerimeterResult, OptimizationResult optimizationResult) {
        this.prePerimeterResult = prePerimeterResult;
        this.optimizationResult = optimizationResult;
    }

    @Override
    public PerimeterStatus getPerimeterStatus() {
        return null;
    }

    @Override
    public Set<RangeAction> getActivatedRangeActions() {
        return optimizationResult.getRangeActions().stream()
                .filter(rangeAction -> prePerimeterResult.getOptimizedSetPoint(rangeAction) != optimizationResult.getOptimizedSetPoint(rangeAction))
                .collect(Collectors.toSet());
    }

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        return optimizationResult.getFlow(branchCnec, unit);
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        return optimizationResult.getCommercialFlow(branchCnec, unit);
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        return optimizationResult.getPtdfZonalSum(branchCnec);
    }

    @Override
    public Map<BranchCnec, Double> getPtdfZonalSums() {
        return optimizationResult.getPtdfZonalSums();
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return optimizationResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return optimizationResult.getActivatedNetworkActions();
    }

    @Override
    public double getFunctionalCost() {
        return optimizationResult.getFunctionalCost();
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        return optimizationResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return optimizationResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return optimizationResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return optimizationResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<BranchCnec> getCostlyElements(String virtualCostName, int number) {
        return getCostlyElements(virtualCostName, number);
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        return prePerimeterResult.getRangeActions();
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return optimizationResult.getOptimizedTap(pstRangeAction);
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return optimizationResult.getOptimizedSetPoint(rangeAction);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return optimizationResult.getOptimizedTaps();
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return optimizationResult.getOptimizedSetPoints();
    }

    @Override
    public SensitivityStatus getSensitivityStatus() {
        return null;
    }

    @Override
    public double getSensitivityValue(BranchCnec branchCnec, RangeAction rangeAction, Unit unit) {
        return 0;
    }

    @Override
    public double getSensitivityValue(BranchCnec branchCnec, LinearGlsk linearGlsk, Unit unit) {
        return 0;
    }
}
