package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.*;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;

public class PrePerimeterSensitivityOutput implements PrePerimeterResult {

    private BranchResult branchResult;
    private SensitivityResult sensitivityResult;
    private RangeActionResult rangeActionResult;

    public PrePerimeterSensitivityOutput(BranchResult branchResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        this.branchResult = branchResult;
        this.sensitivityResult = sensitivityResult;
        this.rangeActionResult = rangeActionResult;
    }

    @Override
    public SensitivityStatus getSensitivityStatus() {
        return sensitivityResult.getSensitivityStatus();
    }

    @Override
    public double getSensitivityValue(BranchCnec branchCnec, RangeAction rangeAction, Unit unit) {
        return sensitivityResult.getSensitivityValue(branchCnec, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(BranchCnec branchCnec, LinearGlsk linearGlsk, Unit unit) {
        return sensitivityResult.getSensitivityValue(branchCnec, linearGlsk, unit);
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
    public double getLoopFlow(BranchCnec branchCnec, Unit unit) {
        return branchResult.getLoopFlow(branchCnec, unit);
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
    public Map<BranchCnec, Double> getPtdfZonalSums() {
        return branchResult.getPtdfZonalSums();
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        return rangeActionResult.getRangeActions();
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
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return rangeActionResult.getOptimizedTaps();
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return rangeActionResult.getOptimizedSetPoints();
    }

    public BranchResult getBranchResult() {
        return branchResult;
    }

    public SensitivityResult getSensitivityResult() {
        return sensitivityResult;
    }

    @Override
    public double getFunctionalCost() {
        return 0;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        return null;
    }

    @Override
    public double getVirtualCost() {
        return 0;
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
}
