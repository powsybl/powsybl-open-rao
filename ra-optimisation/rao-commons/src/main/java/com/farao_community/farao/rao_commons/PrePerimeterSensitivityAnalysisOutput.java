package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.PerimeterResult;
import com.farao_community.farao.rao_api.results.PerimeterStatus;

import java.util.*;

public class PrePerimeterSensitivityAnalysisOutput implements PerimeterResult  {
    private PerimeterStatus perimeterStatus;
    private BranchResult branchResult;
    private double functionalCost;
    private double virtualCost;
    private Map<RangeAction, Double> rangeActionSetPoints;
    private Map<PstRangeAction, Integer> pstTaps;
    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;

    public PrePerimeterSensitivityAnalysisOutput() {

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
    public boolean isActivated(NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return new HashSet<>();
    }

    @Override
    public double getFunctionalCost() {
        return functionalCost;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        //TODO : keep map of cnec -> cost according to objective function
        /*Map<BranchCnec, Double> cnecMarginsInMW = new HashMap<>();
        cnecFlowsInMW.keySet().forEach(cnec -> cnecMarginsInMW.put(cnec, getMargin(cnec, Unit.MEGAWATT)));

        List<BranchCnec> mostLimitingElements = cnecMarginsInMW
                .entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return mostLimitingElements.subList(0, Math.min(number, mostLimitingElements.size()));*/
        return null;
    }

    @Override
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

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return pstTaps.get(pstRangeAction);
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return rangeActionSetPoints.get(rangeAction);
    }

    @Override
    public Set<RangeAction> getActivatedRangeActions() {
        return new HashSet<>();
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return pstTaps;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return rangeActionSetPoints;
    }

    public void setPerimeterStatus(PerimeterStatus perimeterStatus) {
        this.perimeterStatus = perimeterStatus;
    }

    public void setBranchResult(BranchResult branchResult) {
        this.branchResult = branchResult;
    }

    public void setFunctionalCost(double functionalCost) {
        this.functionalCost = functionalCost;
    }

    public void setVirtualCost(double virtualCost) {
        this.virtualCost = virtualCost;
    }

    public void setRangeActionSetPoints(Map<RangeAction, Double> rangeActionSetPoints) {
        this.rangeActionSetPoints = rangeActionSetPoints;
    }

    public void setPstTaps(Map<PstRangeAction, Integer> pstTaps) {
        this.pstTaps = pstTaps;
    }

    public void setSensitivityAndLoopflowResults(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        this.sensitivityAndLoopflowResults = sensitivityAndLoopflowResults;
    }

    public SensitivityAndLoopflowResults getSensitivityAndLoopflowResults() {
        return sensitivityAndLoopflowResults;
    }

    public BranchResult getBranchResult() {
        return branchResult;
    }
}
