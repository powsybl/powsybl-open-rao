package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;

import java.util.HashMap;
import java.util.Map;

public class ElementaryFlowCnecResult {

    static final FlowCnecResultPerUnit DEFAULT_RESULT = new FlowCnecResultPerUnit();

    private Map<Unit, FlowCnecResultPerUnit> resultPerUnit;
    private double ptdfZonalSum;

    private static class FlowCnecResultPerUnit {
        private double flow = Double.NaN;
        private double margin = Double.NaN;
        private double relativeMargin = Double.NaN;
        private double loopFlow = Double.NaN;
        private double commercialFlow = Double.NaN;
    }

    public ElementaryFlowCnecResult() {
        this.resultPerUnit = new HashMap<>();
        this.ptdfZonalSum = Double.NaN;
    }

    public double getFlow(Unit unit) {
        return resultPerUnit.getOrDefault(unit, DEFAULT_RESULT).flow;
    }

    public double getMargin(Unit unit) {
        return resultPerUnit.getOrDefault(unit, DEFAULT_RESULT).margin;
    }

    public double getRelativeMargin(Unit unit) {
        return resultPerUnit.getOrDefault(unit, DEFAULT_RESULT).relativeMargin;
    }

    public double getLoopFlow(Unit unit) {
        return resultPerUnit.getOrDefault(unit, DEFAULT_RESULT).loopFlow;
    }

    public double getCommercialFlow(Unit unit) {
        return resultPerUnit.getOrDefault(unit, DEFAULT_RESULT).commercialFlow;
    }

    public double getPtdfZonalSum() {
        return ptdfZonalSum;
    }

    public void setFlow(double flow, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).flow = flow;
    }

    public void setMargin(double margin, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).margin = margin;
    }

    public void setRelativeMargin(double relativeMargin, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).relativeMargin = relativeMargin;
    }

    public void setLoopFlow(double loopFlow, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).loopFlow = loopFlow;
    }

    public void setCommercialFlow(double commercialFlow, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).commercialFlow = commercialFlow;
    }

    public void setPtdfZonalSum(double ptdfZonalSum) {
        this.ptdfZonalSum = ptdfZonalSum;
    }

    private void setMapForUnitIfNecessary(Unit unit) {
        if (unit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new FaraoException("FlowCnecResult can only be defined for a FLOW unit");
        }
        resultPerUnit.putIfAbsent(unit, new FlowCnecResultPerUnit());
    }

}
