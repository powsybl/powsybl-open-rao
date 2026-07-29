/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static com.powsybl.openrao.commons.Unit.AMPERE;
import static com.powsybl.openrao.commons.Unit.MEGAWATT;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class FlowCnecResultArraySerializer {

    private FlowCnecResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, Set<Unit> flowUnits, JsonGenerator jsonGenerator) throws IOException {

        List<FlowCnec> sortedListOfFlowCnecs = crac.getFlowCnecs().stream()
            .sorted(Comparator.comparing(FlowCnec::getId))
            .toList();

        jsonGenerator.writeArrayFieldStart(RaoResultJsonConstants.FLOWCNEC_RESULTS);
        for (FlowCnec flowCnec : sortedListOfFlowCnecs) {
            if (!flowCnec.getId().contains("OUTAGE DUPLICATE")) {
                serializeFlowCnecResult(flowCnec, raoResult, crac, flowUnits, jsonGenerator);
            }
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeFlowCnecResult(FlowCnec flowCnec, RaoResult raoResult, Crac crac, Set<Unit> flowUnits, JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForFlowCnec(raoResult, flowCnec, crac, MEGAWATT) && !containsAnyResultForFlowCnec(raoResult, flowCnec, crac, AMPERE)) {
            return;
        }
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(RaoResultJsonConstants.FLOWCNEC_ID, flowCnec.getId());

        serializeFlowCnecResultForOptimizationState(null, flowCnec, raoResult, crac, flowUnits, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(crac.getPreventiveInstant(), flowCnec, raoResult, crac, flowUnits, jsonGenerator);
        Instant instant = flowCnec.getState().getInstant();
        if (instant.isCurative() || instant.isAuto()) {
            if (crac.hasAutoInstant()) {
                serializeFlowCnecResultForOptimizationState(crac.getInstant(InstantKind.AUTO), flowCnec, raoResult, crac, flowUnits, jsonGenerator);
            }
            crac.getInstants(InstantKind.CURATIVE).forEach(curativeInstant -> {
                if (!curativeInstant.comesAfter(instant)) {
                    try {
                        serializeFlowCnecResultForOptimizationState(curativeInstant, flowCnec, raoResult, crac, flowUnits, jsonGenerator);
                    } catch (IOException e) {
                        throw new OpenRaoException("An error occurred when serializing FlowCNEC results", e);
                    }
                }
            });
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeFlowCnecResultForOptimizationState(Instant optInstant,
                                                                    FlowCnec flowCnec,
                                                                    RaoResult raoResult,
                                                                    Crac crac,
                                                                    Set<Unit> flowUnits,
                                                                    JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForOptimizationState(raoResult, flowCnec, optInstant, MEGAWATT) && !containsAnyResultForOptimizationState(raoResult, flowCnec, optInstant, AMPERE)) {
            return;
        }
        jsonGenerator.writeObjectFieldStart(RaoResultJsonConstants.serializeInstantId(optInstant));
        for (Unit flowUnit : flowUnits.stream().sorted().toList()) {
            serializeFlowCnecResultForOptimizationStateAndUnit(optInstant, flowUnit, flowCnec, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeFlowCnecResultForOptimizationStateAndUnit(Instant optInstant,
                                                                           Unit unit,
                                                                           FlowCnec flowCnec,
                                                                           RaoResult raoResult,
                                                                           Crac crac,
                                                                           JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForFlowCnec(raoResult, flowCnec, crac, unit)) {
            return;
        }
        jsonGenerator.writeObjectFieldStart(RaoResultJsonConstants.serializeUnit(unit));
        serializeFlowCnecMargin(optInstant, unit, flowCnec, raoResult, jsonGenerator);
        for (TwoSides side : flowCnec.getMonitoredSides().stream().sorted(Comparator.comparing(TwoSides::toString)).toList()) {
            serializeFlowCnecFlows(optInstant, unit, flowCnec, side, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeFlowCnecMargin(Instant optInstant, Unit unit, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        double margin = safeGetMargin(raoResult, flowCnec, optInstant, unit);
        double relativeMargin = safeGetRelativeMargin(raoResult, flowCnec, optInstant, unit);

        if (Double.isNaN(margin) && Double.isNaN(relativeMargin)) {
            return;
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.MARGIN, roundValueBasedOnMargin(margin, margin, 2));
        }
        if (!Double.isNaN(relativeMargin)) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.RELATIVE_MARGIN, roundValueBasedOnMargin(relativeMargin, margin, 2));
        }
    }

    private static void serializeFlowCnecFlows(Instant optInstant, Unit unit, FlowCnec flowCnec, TwoSides side, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        double flow = safeGetFlow(raoResult, flowCnec, side, optInstant, unit);
        double margin = safeGetMargin(raoResult, flowCnec, optInstant, unit);
        double loopFlow = safeGetLoopFlow(raoResult, flowCnec, side, optInstant, unit);
        double commercialFlow = safeGetCommercialFlow(raoResult, flowCnec, side, optInstant, unit);
        double ptdfZonalSum = safeGetPtdfZonalSum(raoResult, flowCnec, side, optInstant);

        if (Double.isNaN(flow) && Double.isNaN(loopFlow) && Double.isNaN(commercialFlow) && (!unit.equals(MEGAWATT) || Double.isNaN(ptdfZonalSum))) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(RaoResultJsonConstants.serializeSide(side));
        if (!Double.isNaN(flow)) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.FLOW, roundValueBasedOnMargin(flow, margin, 2));
        }
        if (!Double.isNaN(loopFlow)) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.LOOP_FLOW, roundValueBasedOnMargin(loopFlow, margin, 2));
        }
        if (!Double.isNaN(commercialFlow)) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.COMMERCIAL_FLOW, roundValueBasedOnMargin(commercialFlow, margin, 2));
        }
        if (unit.equals(MEGAWATT) && !Double.isNaN(ptdfZonalSum)) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.ZONAL_PTDF_SUM, roundValueBasedOnMargin(ptdfZonalSum, margin, 6));
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containsAnyResultForFlowCnec(RaoResult raoResult, FlowCnec flowCnec, Crac crac, Unit unit) {
        if (flowCnec.getState().isPreventive()) {
            return containsAnyResultForOptimizationState(raoResult, flowCnec, null, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, flowCnec.getState().getInstant(), unit);
        } else {
            return containsAnyResultForOptimizationState(raoResult, flowCnec, null, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, crac.getPreventiveInstant(), unit) ||
                crac.hasAutoInstant() && containsAnyResultForOptimizationState(raoResult, flowCnec, crac.getInstant(InstantKind.AUTO), unit) ||
                crac.getInstants(InstantKind.CURATIVE).stream()
                    .anyMatch(curativeInstant -> containsAnyResultForOptimizationState(raoResult, flowCnec, curativeInstant, unit));
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, FlowCnec flowCnec, Instant optInstant, Unit unit) {
        return !Double.isNaN(safeGetMargin(raoResult, flowCnec, optInstant, unit)) ||
            !Double.isNaN(safeGetRelativeMargin(raoResult, flowCnec, optInstant, unit)) ||
            containsAnyResultForOptimizationStateAndSide(raoResult, flowCnec, TwoSides.ONE, optInstant, unit) ||
            containsAnyResultForOptimizationStateAndSide(raoResult, flowCnec, TwoSides.TWO, optInstant, unit);
    }

    private static boolean containsAnyResultForOptimizationStateAndSide(RaoResult raoResult, FlowCnec flowCnec, TwoSides side, Instant optInstant, Unit unit) {
        return !Double.isNaN(safeGetFlow(raoResult, flowCnec, side, optInstant, unit)) ||
            !Double.isNaN(safeGetLoopFlow(raoResult, flowCnec, side, optInstant, unit)) ||
            !Double.isNaN(safeGetCommercialFlow(raoResult, flowCnec, side, optInstant, unit)) ||
            !Double.isNaN(safeGetPtdfZonalSum(raoResult, flowCnec, side, optInstant)) && unit.equals(MEGAWATT);
    }

    private static double safeGetFlow(RaoResult raoResult, FlowCnec flowCnec, TwoSides side, Instant optInstant, Unit unit) {
        // methods getFlow can return an exception if RAO is executed on one state only
        try {
            return raoResult.getFlow(optInstant, flowCnec, side, unit);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, FlowCnec flowCnec, Instant optInstant, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optInstant, flowCnec, unit);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetRelativeMargin(RaoResult raoResult, FlowCnec flowCnec, Instant optInstant, Unit unit) {
        // methods getRelativeMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getRelativeMargin(optInstant, flowCnec, unit);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetLoopFlow(RaoResult raoResult, FlowCnec flowCnec, TwoSides side, Instant optInstant, Unit unit) {
        // methods getLoopFlow can throw an exception if queried in AMPERE
        try {
            return raoResult.getLoopFlow(optInstant, flowCnec, side, unit);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCommercialFlow(RaoResult raoResult, FlowCnec flowCnec, TwoSides side, Instant optInstant, Unit unit) {
        // methods getCommercialFlow can throw an exception if queried in AMPERE
        try {
            return raoResult.getCommercialFlow(optInstant, flowCnec, side, unit);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetPtdfZonalSum(RaoResult raoResult, FlowCnec flowCnec, TwoSides side, Instant optInstant) {
        // methods getPtdfZonalSum can throw an exception if RAO is executed on one state only
        try {
            return raoResult.getPtdfZonalSum(optInstant, flowCnec, side);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }
}
