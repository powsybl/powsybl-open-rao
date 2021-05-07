package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.*;
import com.farao_community.farao.rao_commons.PrePerimeterSensitivityOutput;

import java.util.*;

public class PrePerimeterOutput implements PerimeterResult {

    PerimeterResult perimeterResult;
    PrePerimeterSensitivityOutput prePerimeterSensitivityOutput;

    public PrePerimeterOutput(PrePerimeterSensitivityOutput prePerimeterSensitivityOutput, PerimeterResult perimeterResult) {
        this.prePerimeterSensitivityOutput = prePerimeterSensitivityOutput;
        this.perimeterResult = perimeterResult;
    }

    @Override
    public PerimeterStatus getStatus() {
        if (Objects.isNull(perimeterResult)) {
            return prePerimeterSensitivityOutput.getStatus();
        }
        return perimeterResult.getStatus();
    }

    @Override
    public Set<RangeAction> getActivatedRangeActions() {
        if (Objects.isNull(perimeterResult)) {
            return new HashSet<>();
        }
        return perimeterResult.getActivatedRangeActions();
    }

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        return prePerimeterSensitivityOutput.getFlow(branchCnec, unit);
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        return prePerimeterSensitivityOutput.getCommercialFlow(branchCnec, unit);
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        return prePerimeterSensitivityOutput.getPtdfZonalSum(branchCnec);
    }

    @Override
    public Map<BranchCnec, Double> getPtdfZonalSums() {
        return prePerimeterSensitivityOutput.getPtdfZonalSums();
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        if (Objects.isNull(perimeterResult)) {
            return false;
        }
        return perimeterResult.isActivated(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        if (Objects.isNull(perimeterResult)) {
            return new HashSet<>();
        }
        return perimeterResult.getActivatedNetworkActions();
    }

    @Override
    public double getFunctionalCost() {
        return perimeterResult.getFunctionalCost();
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        return perimeterResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return perimeterResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return perimeterResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return perimeterResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<BranchCnec> getCostlyElements(String virtualCostName, int number) {
        return getCostlyElements(virtualCostName, number);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return prePerimeterSensitivityOutput.getOptimizedTap(pstRangeAction);
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return prePerimeterSensitivityOutput.getOptimizedSetPoint(rangeAction);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return prePerimeterSensitivityOutput.getOptimizedTaps();
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return prePerimeterSensitivityOutput.getOptimizedSetPoints();
    }
}
