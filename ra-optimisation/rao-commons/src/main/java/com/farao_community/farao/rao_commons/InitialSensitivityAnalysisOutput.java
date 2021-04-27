package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.PerimeterResult;
import com.farao_community.farao.rao_api.results.PerimeterStatus;

import java.util.*;
import java.util.stream.Collectors;

public class InitialSensitivityAnalysisOutput implements PerimeterResult,  {
    private PerimeterStatus perimeterStatus;
    private Map<BranchCnec, Double> cnecFlowsInMW;
    private Map<BranchCnec, Double> cnecFlowsInA;
    private Map<BranchCnec, Double> commercialFlowsInMW;
    private Map<BranchCnec, Double> commercialFlowsInA;
    private Map<BranchCnec, Double> ptdfZonalSums;
    private double functionalCost;
    private double virtualCost;
    private Map<RangeAction, Double> rangeActionSetPoints;
    private Map<PstRangeAction, Integer> pstTaps;

    @Override
    public PerimeterStatus getStatus() {
        return perimeterStatus;
    }

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        switch(unit) {
            case MEGAWATT:
                return cnecFlowsInMW.get(branchCnec);
            case AMPERE:
                return cnecFlowsInA.get(branchCnec);
            default:
                throw new FaraoException("Flows should only be in MW or A.");
        }
    }

    @Override
    public double getLoopFlow(BranchCnec branchCnec, Unit unit) {
        return getFlow(branchCnec, unit) - getCommercialFlow(branchCnec, unit);
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        switch(unit) {
            case MEGAWATT:
                return commercialFlowsInMW.get(branchCnec);
            case AMPERE:
                return commercialFlowsInA.get(branchCnec);
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
    public List<BranchCnec> getCostlyElements(String virtualCostName) {
        return null;
    }

    @Override
    public int getTap(PstRangeAction pstRangeAction) {
        return pstTaps.get(pstRangeAction);
    }

    @Override
    public double getSetPoint(RangeAction rangeAction) {
        return rangeActionSetPoints.get(rangeAction);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState() {
        return pstTaps;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState() {
        return rangeActionSetPoints;
    }
}
