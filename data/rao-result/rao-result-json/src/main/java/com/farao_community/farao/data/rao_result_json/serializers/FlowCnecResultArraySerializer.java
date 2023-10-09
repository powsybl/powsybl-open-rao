/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.COMMERCIAL_FLOW;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class FlowCnecResultArraySerializer {

    private FlowCnecResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, Set<Unit> flowUnits, JsonGenerator jsonGenerator) throws IOException {

        List<FlowCnec> sortedListOfFlowCnecs = crac.getFlowCnecs().stream()
            .sorted(Comparator.comparing(FlowCnec::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(FLOWCNEC_RESULTS);
        for (FlowCnec flowCnec : sortedListOfFlowCnecs) {
            serializeFlowCnecResult(flowCnec, raoResult, flowUnits, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeFlowCnecResult(FlowCnec flowCnec, RaoResult raoResult, Set<Unit> flowUnits, JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForFlowCnec(raoResult, flowCnec, MEGAWATT) && !containsAnyResultForFlowCnec(raoResult, flowCnec, AMPERE)) {
            return;
        }
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(FLOWCNEC_ID, flowCnec.getId());

        serializeFlowCnecResultForOptimizationState(null, flowCnec, raoResult, flowUnits, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(Instant.PREVENTIVE, flowCnec, raoResult, flowUnits, jsonGenerator);
        Instant flowCnecInstant = flowCnec.getState().getInstant();
        if (flowCnecInstant.equals(Instant.CURATIVE) || flowCnecInstant.equals(Instant.AUTO)) {
            serializeFlowCnecResultForOptimizationState(Instant.AUTO, flowCnec, raoResult, flowUnits, jsonGenerator);
            serializeFlowCnecResultForOptimizationState(Instant.CURATIVE, flowCnec, raoResult, flowUnits, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeFlowCnecResultForOptimizationState(Instant optInstant, FlowCnec flowCnec, RaoResult raoResult, Set<Unit> flowUnits, JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForOptimizationState(raoResult, flowCnec, optInstant, MEGAWATT) && !containsAnyResultForOptimizationState(raoResult, flowCnec, optInstant, AMPERE)) {
            return;
        }
        jsonGenerator.writeObjectFieldStart(serializeInstant(optInstant));
        for (Unit flowUnit : flowUnits.stream().sorted().collect(Collectors.toList())) {
            serializeFlowCnecResultForOptimizationStateAndUnit(optInstant, flowUnit, flowCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeFlowCnecResultForOptimizationStateAndUnit(Instant optInstant, Unit unit, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForFlowCnec(raoResult, flowCnec, unit)) {
            return;
        }
        jsonGenerator.writeObjectFieldStart(serializeUnit(unit));
        serializeFlowCnecMargin(optInstant, unit, flowCnec, raoResult, jsonGenerator);
        for (Side side : flowCnec.getMonitoredSides().stream().sorted(Comparator.comparing(Side::toString)).collect(Collectors.toList())) {
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
            jsonGenerator.writeNumberField(MARGIN, Math.round(100.0 * margin) / 100.0);
        }
        if (!Double.isNaN(relativeMargin)) {
            jsonGenerator.writeNumberField(RELATIVE_MARGIN, Math.round(100.0 * relativeMargin) / 100.0);
        }
    }

    private static void serializeFlowCnecFlows(Instant optInstant, Unit unit, FlowCnec flowCnec, Side side, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        double flow = safeGetFlow(raoResult, flowCnec, side, optInstant, unit);
        double loopFlow = safeGetLoopFlow(raoResult, flowCnec, side, optInstant, unit);
        double commercialFlow = safeGetCommercialFlow(raoResult, flowCnec, side, optInstant, unit);
        double ptdfZonalSum = safeGetPtdfZonalSum(raoResult, flowCnec, side, optInstant);

        if (Double.isNaN(flow) && Double.isNaN(loopFlow) && Double.isNaN(commercialFlow) && (!unit.equals(MEGAWATT) || Double.isNaN(ptdfZonalSum))) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeSide(side));
        if (!Double.isNaN(flow)) {
            jsonGenerator.writeNumberField(FLOW, Math.round(100.0 * flow) / 100.0);
        }
        if (!Double.isNaN(loopFlow)) {
            jsonGenerator.writeNumberField(LOOP_FLOW, Math.round(100.0 * loopFlow) / 100.0);
        }
        if (!Double.isNaN(commercialFlow)) {
            jsonGenerator.writeNumberField(COMMERCIAL_FLOW, Math.round(100.0 * commercialFlow) / 100.0);
        }
        if (unit.equals(MEGAWATT) && !Double.isNaN(ptdfZonalSum)) {
            jsonGenerator.writeNumberField(ZONAL_PTDF_SUM, Math.round(1000000.0 * ptdfZonalSum) / 1000000.0);
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containsAnyResultForFlowCnec(RaoResult raoResult, FlowCnec flowCnec, Unit unit) {
        if (flowCnec.getState().isPreventive()) {
            return containsAnyResultForOptimizationState(raoResult, flowCnec, null, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, Instant.PREVENTIVE, unit);
        } else {
            return containsAnyResultForOptimizationState(raoResult, flowCnec, null, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, Instant.PREVENTIVE, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, Instant.AUTO, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, Instant.CURATIVE, unit);
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, FlowCnec flowCnec, Instant optInstant, Unit unit) {
        return !Double.isNaN(safeGetMargin(raoResult, flowCnec, optInstant, unit)) ||
            !Double.isNaN(safeGetRelativeMargin(raoResult, flowCnec, optInstant, unit)) ||
            containsAnyResultForOptimizationStateAndSide(raoResult, flowCnec, Side.LEFT, optInstant, unit) ||
            containsAnyResultForOptimizationStateAndSide(raoResult, flowCnec, Side.RIGHT, optInstant, unit);
    }

    private static boolean containsAnyResultForOptimizationStateAndSide(RaoResult raoResult, FlowCnec flowCnec, Side side, Instant optInstant, Unit unit) {
        return !Double.isNaN(safeGetFlow(raoResult, flowCnec, side, optInstant, unit)) ||
            !Double.isNaN(safeGetLoopFlow(raoResult, flowCnec, side, optInstant, unit)) ||
            !Double.isNaN(safeGetCommercialFlow(raoResult, flowCnec, side, optInstant, unit)) ||
            (!Double.isNaN(safeGetPtdfZonalSum(raoResult, flowCnec, side, optInstant)) && unit.equals(MEGAWATT));
    }

    private static double safeGetFlow(RaoResult raoResult, FlowCnec flowCnec, Side side, Instant optInstant, Unit unit) {
        // methods getFlow can return an exception if RAO is executed on one state only
        try {
            return raoResult.getFlow(optInstant, flowCnec, side, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, FlowCnec flowCnec, Instant optInstant, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optInstant, flowCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetRelativeMargin(RaoResult raoResult, FlowCnec flowCnec, Instant optInstant, Unit unit) {
        // methods getRelativeMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getRelativeMargin(optInstant, flowCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetLoopFlow(RaoResult raoResult, FlowCnec flowCnec, Side side, Instant optInstant, Unit unit) {
        // methods getLoopFlow can throw an exception if queried in AMPERE
        try {
            return raoResult.getLoopFlow(optInstant, flowCnec, side, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCommercialFlow(RaoResult raoResult, FlowCnec flowCnec, Side side, Instant optInstant, Unit unit) {
        // methods getCommercialFlow can throw an exception if queried in AMPERE
        try {
            return raoResult.getCommercialFlow(optInstant, flowCnec, side, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetPtdfZonalSum(RaoResult raoResult, FlowCnec flowCnec, Side side, Instant optInstant) {
        // methods getPtdfZonalSum can throw an exception if RAO is executed on one state only
        try {
            return raoResult.getPtdfZonalSum(optInstant, flowCnec, side);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }
}
