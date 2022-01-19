/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ElementaryFlowCnecResult {

    private static final FlowCnecResultPerUnit DEFAULT_RESULT = new FlowCnecResultPerUnit();

    private final Map<Unit, FlowCnecResultPerUnit> resultPerUnit;
    private double ptdfZonalSum;

    private static class FlowCnecResultPerUnit {
        private double flow = Double.NaN;
        private double margin = Double.NaN;
        private double relativeMargin = Double.NaN;
        private double loopFlow = Double.NaN;
        private double commercialFlow = Double.NaN;
    }

    ElementaryFlowCnecResult() {
        this.resultPerUnit = new EnumMap<>(Unit.class);
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
