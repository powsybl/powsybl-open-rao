/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.iidm.network.TwoSides;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ElementaryFlowCnecResult {

    private static final FlowCnecResultPerUnit DEFAULT_RESULT = new FlowCnecResultPerUnit();

    private final Map<Unit, FlowCnecResultPerUnit> resultPerUnit;
    private final Map<TwoSides, Double> ptdfZonalSum;

    private static final class FlowCnecResultPerUnit {
        private final Map<TwoSides, Double> flow = new EnumMap<>(Map.of(TwoSides.ONE, Double.NaN, TwoSides.TWO, Double.NaN));
        private double margin = Double.NaN;
        private double relativeMargin = Double.NaN;
        private final Map<TwoSides, Double> loopFlow = new EnumMap<>(Map.of(TwoSides.ONE, Double.NaN, TwoSides.TWO, Double.NaN));
        private final Map<TwoSides, Double> commercialFlow = new EnumMap<>(Map.of(TwoSides.ONE, Double.NaN, TwoSides.TWO, Double.NaN));
    }

    ElementaryFlowCnecResult() {
        this.resultPerUnit = new EnumMap<>(Unit.class);
        this.ptdfZonalSum = new EnumMap<>(Map.of(TwoSides.ONE, Double.NaN, TwoSides.TWO, Double.NaN));
    }

    public double getFlow(TwoSides side, Unit unit) {
        if (!resultPerUnit.containsKey(unit)) {
            return DEFAULT_RESULT.flow.get(side);
        }
        return resultPerUnit.get(unit).flow.getOrDefault(side, DEFAULT_RESULT.flow.get(side));
    }

    public double getMargin(Unit unit) {
        return resultPerUnit.getOrDefault(unit, DEFAULT_RESULT).margin;
    }

    public double getRelativeMargin(Unit unit) {
        return resultPerUnit.getOrDefault(unit, DEFAULT_RESULT).relativeMargin;
    }

    public double getLoopFlow(TwoSides side, Unit unit) {
        if (!resultPerUnit.containsKey(unit)) {
            return DEFAULT_RESULT.loopFlow.get(side);
        }
        return resultPerUnit.get(unit).loopFlow.getOrDefault(side, DEFAULT_RESULT.loopFlow.get(side));
    }

    public double getCommercialFlow(TwoSides side, Unit unit) {
        if (!resultPerUnit.containsKey(unit)) {
            return DEFAULT_RESULT.commercialFlow.get(side);
        }
        return resultPerUnit.get(unit).commercialFlow.getOrDefault(side, DEFAULT_RESULT.commercialFlow.get(side));
    }

    public double getPtdfZonalSum(TwoSides side) {
        return ptdfZonalSum.get(side);
    }

    public void setFlow(TwoSides side, double flow, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).flow.put(side, flow);
    }

    public void setMargin(double margin, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).margin = margin;
    }

    public void setRelativeMargin(double relativeMargin, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).relativeMargin = relativeMargin;
    }

    public void setLoopFlow(TwoSides side, double loopFlow, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).loopFlow.put(side, loopFlow);
    }

    public void setCommercialFlow(TwoSides side, double commercialFlow, Unit unit) {
        setMapForUnitIfNecessary(unit);
        resultPerUnit.get(unit).commercialFlow.put(side, commercialFlow);
    }

    public void setPtdfZonalSum(TwoSides side, double ptdfZonalSum) {
        this.ptdfZonalSum.put(side, ptdfZonalSum);
    }

    private void setMapForUnitIfNecessary(Unit unit) {
        if (unit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new OpenRaoException("FlowCnecResult can only be defined for a FLOW unit");
        }
        resultPerUnit.putIfAbsent(unit, new FlowCnecResultPerUnit());
    }
}
