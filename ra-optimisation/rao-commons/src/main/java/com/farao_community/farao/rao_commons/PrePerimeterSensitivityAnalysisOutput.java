package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.PerimeterResult;
import com.farao_community.farao.rao_api.results.PerimeterStatus;

import java.util.*;
import java.util.stream.Collectors;

public class PrePerimeterSensitivityAnalysisOutput implements PerimeterResult  {
    private PerimeterStatus perimeterStatus;
    private Map<BranchCnec, Double> cnecFlowsInMW;
    private Map<BranchCnec, Double> cnecFlowsInA;
    private Map<BranchCnec, Double> commercialFlowsInMW;
    private Map<BranchCnec, Double> ptdfZonalSums;
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
        switch (unit) {
            case MEGAWATT:
                return cnecFlowsInMW.get(branchCnec);
            case AMPERE:
                return cnecFlowsInA.get(branchCnec);
            default:
                throw new FaraoException("Flows should only be in MW or A.");
        }
    }

    @Override
    public double getRelativeMargin(BranchCnec branchCnec, Unit unit) {
        return 0;
    }

    @Override
    public double getLoopFlow(BranchCnec branchCnec, Unit unit) {
        return getFlow(branchCnec, unit) - getCommercialFlow(branchCnec, unit);
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        switch (unit) {
            case MEGAWATT:
            case AMPERE:
                return commercialFlowsInMW.get(branchCnec) * RaoUtil.getBranchFlowUnitMultiplier(branchCnec, Side.LEFT, Unit.MEGAWATT, unit);
            default:
                throw new FaraoException("Flows should only be in MW or A.");
        }
    }

    public Map<BranchCnec, Double> getCommercialFlows(Unit unit) {
        switch (unit) {
            case MEGAWATT:
                return commercialFlowsInMW;
            case AMPERE:
                Map<BranchCnec, Double> commercialFlowsInA = new HashMap<>();
                commercialFlowsInMW.keySet().forEach(cnec -> commercialFlowsInA.put(cnec, commercialFlowsInMW.get(cnec) * RaoUtil.getBranchFlowUnitMultiplier(cnec, Side.LEFT, Unit.MEGAWATT, Unit.AMPERE)));
                return commercialFlowsInA;
            default:
                throw new FaraoException("Flows should only be in MW or A.");
        }
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        return ptdfZonalSums.get(branchCnec);
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
        Map<BranchCnec, Double> cnecMarginsInMW = new HashMap<>();
        cnecFlowsInMW.keySet().forEach(cnec -> cnecMarginsInMW.put(cnec, getMargin(cnec, Unit.MEGAWATT)));

        List<BranchCnec> mostLimitingElements = cnecMarginsInMW
                .entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return mostLimitingElements.subList(0, Math.min(number, mostLimitingElements.size()));
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

    public void setCnecFlowsInMW(Map<BranchCnec, Double> cnecFlowsInMW) {
        this.cnecFlowsInMW = cnecFlowsInMW;
    }

    public void setCnecFlowsInA(Map<BranchCnec, Double> cnecFlowsInA) {
        this.cnecFlowsInA = cnecFlowsInA;
    }

    public void setCommercialFlowsInMW(Map<BranchCnec, Double> commercialFlowsInMW) {
        this.commercialFlowsInMW = commercialFlowsInMW;
    }

    public void setPtdfZonalSums(Map<BranchCnec, Double> ptdfZonalSums) {
        this.ptdfZonalSums = ptdfZonalSums;
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

    public CnecResults getCnecResults() {
        CnecResults cnecResults = new CnecResults();
        cnecResults.setAbsolutePtdfSums(ptdfZonalSums);
        cnecResults.setFlowsInA(cnecFlowsInA);
        cnecResults.setFlowsInMW(cnecFlowsInMW);
        cnecResults.setCommercialFlowsInMW(commercialFlowsInMW);
        Map<BranchCnec, Double> loopflowsInMW = new HashMap<>();
        commercialFlowsInMW.keySet().forEach(cnec -> loopflowsInMW.put(cnec, cnecFlowsInMW.get(cnec) - commercialFlowsInMW.get(cnec)));
        cnecResults.setLoopflowsInMW(loopflowsInMW);
        return cnecResults;
    }

    public SensitivityAndLoopflowResults getSensitivityAndLoopflowResults() {
        return sensitivityAndLoopflowResults;
    }
}
