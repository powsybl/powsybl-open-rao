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
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
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

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<FlowCnec> sortedListOfFlowCnecs = crac.getFlowCnecs().stream()
            .sorted(Comparator.comparing(FlowCnec::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(FLOWCNEC_RESULTS);
        for (FlowCnec flowCnec : sortedListOfFlowCnecs) {
            serializeFlowCnecResult(flowCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeFlowCnecResult(FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForFlowCnec(raoResult, flowCnec, MEGAWATT) && !containsAnyResultForFlowCnec(raoResult, flowCnec, AMPERE)) {
            return;
        }
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(FLOWCNEC_ID, flowCnec.getId());

        serializeFlowCnecResultForOptimizationState(OptimizationState.INITIAL, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_PRA, flowCnec, raoResult, jsonGenerator);

        if (!flowCnec.getState().isPreventive()) {
            serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_ARA, flowCnec, raoResult, jsonGenerator);
            serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_CRA, flowCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeFlowCnecResultForOptimizationState(OptimizationState optState, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForOptimizationState(raoResult, flowCnec, optState, MEGAWATT) && !containsAnyResultForOptimizationState(raoResult, flowCnec, optState, AMPERE)) {
            return;
        }
        jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));
        serializeFlowCnecResultForOptimizationStateAndUnit(optState, MEGAWATT, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationStateAndUnit(optState, AMPERE, flowCnec, raoResult, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private static void serializeFlowCnecResultForOptimizationStateAndUnit(OptimizationState optState, Unit unit, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        if (!containsAnyResultForFlowCnec(raoResult, flowCnec, unit)) {
            return;
        }
        jsonGenerator.writeObjectFieldStart(serializeUnit(unit));
        serializeFlowCnecMargin(optState, unit, flowCnec, raoResult, jsonGenerator);
        for (Side side : flowCnec.getMonitoredSides()) {
            serializeFlowCnecFlows(optState, unit, flowCnec, side, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeFlowCnecMargin(OptimizationState optState, Unit unit, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        double margin = safeGetMargin(raoResult, flowCnec, optState, unit);
        double relativeMargin = safeGetRelativeMargin(raoResult, flowCnec, optState, unit);

        if (Double.isNaN(margin) && Double.isNaN(relativeMargin)) {
            return;
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(MARGIN, margin);
        }
        if (!Double.isNaN(relativeMargin)) {
            jsonGenerator.writeNumberField(RELATIVE_MARGIN, relativeMargin);
        }
    }

    private static void serializeFlowCnecFlows(OptimizationState optState, Unit unit, FlowCnec flowCnec, Side side, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        double flow = safeGetFlow(raoResult, flowCnec, side, optState, unit);
        double loopFlow = safeGetLoopFlow(raoResult, flowCnec, side, optState, unit);
        double commercialFlow = safeGetCommercialFlow(raoResult, flowCnec, side, optState, unit);
        double ptdfZonalSum = safeGetPtdfZonalSum(raoResult, flowCnec, side, optState);

        if (Double.isNaN(flow) && Double.isNaN(loopFlow) && Double.isNaN(commercialFlow) && (!unit.equals(MEGAWATT) || Double.isNaN(ptdfZonalSum))) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeSide(side));
        if (!Double.isNaN(flow)) {
            jsonGenerator.writeNumberField(FLOW, flow);
        }
        if (!Double.isNaN(loopFlow)) {
            jsonGenerator.writeNumberField(LOOP_FLOW, loopFlow);
        }
        if (!Double.isNaN(commercialFlow)) {
            jsonGenerator.writeNumberField(COMMERCIAL_FLOW, commercialFlow);
        }
        if (unit.equals(MEGAWATT) && !Double.isNaN(ptdfZonalSum)) {
            jsonGenerator.writeNumberField(ZONAL_PTDF_SUM, ptdfZonalSum);
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containsAnyResultForFlowCnec(RaoResult raoResult, FlowCnec flowCnec, Unit unit) {
        if (flowCnec.getState().isPreventive()) {
            return containsAnyResultForOptimizationState(raoResult, flowCnec, OptimizationState.INITIAL, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, OptimizationState.AFTER_PRA, unit);
        } else {
            return containsAnyResultForOptimizationState(raoResult, flowCnec, OptimizationState.INITIAL, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, OptimizationState.AFTER_PRA, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, OptimizationState.AFTER_ARA, unit) ||
                containsAnyResultForOptimizationState(raoResult, flowCnec, OptimizationState.AFTER_CRA, unit);
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, FlowCnec flowCnec, OptimizationState optState, Unit unit) {
        return !Double.isNaN(safeGetMargin(raoResult, flowCnec, optState, unit)) ||
            !Double.isNaN(safeGetRelativeMargin(raoResult, flowCnec, optState, unit)) ||
            containsAnyFlowResultForOptimizationState(raoResult, flowCnec, Side.LEFT, optState, unit) ||
            containsAnyFlowResultForOptimizationState(raoResult, flowCnec, Side.RIGHT, optState, unit);
    }

    private static boolean containsAnyFlowResultForOptimizationState(RaoResult raoResult, FlowCnec flowCnec, Side side, OptimizationState optState, Unit unit) {
        return !Double.isNaN(safeGetFlow(raoResult, flowCnec, side, optState, unit)) ||
            !Double.isNaN(safeGetLoopFlow(raoResult, flowCnec, side, optState, unit)) ||
            !Double.isNaN(safeGetCommercialFlow(raoResult, flowCnec, side, optState, unit)) ||
            (!Double.isNaN(safeGetPtdfZonalSum(raoResult, flowCnec, side, optState)) && unit.equals(MEGAWATT));
    }

    private static double safeGetFlow(RaoResult raoResult, FlowCnec flowCnec, Side side, OptimizationState optState, Unit unit) {
        // methods getFlow can return an exception if RAO is executed on one state only
        try {
            return raoResult.getFlow(optState, flowCnec, side, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, FlowCnec flowCnec, OptimizationState optState, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optState, flowCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetRelativeMargin(RaoResult raoResult, FlowCnec flowCnec, OptimizationState optState, Unit unit) {
        // methods getRelativeMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getRelativeMargin(optState, flowCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetLoopFlow(RaoResult raoResult, FlowCnec flowCnec, Side side, OptimizationState optState, Unit unit) {
        // methods getLoopFlow can throw an exception if queried in AMPERE
        try {
            return raoResult.getLoopFlow(optState, flowCnec, side, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCommercialFlow(RaoResult raoResult, FlowCnec flowCnec, Side side, OptimizationState optState, Unit unit) {
        // methods getCommercialFlow can throw an exception if queried in AMPERE
        try {
            return raoResult.getCommercialFlow(optState, flowCnec, side, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetPtdfZonalSum(RaoResult raoResult, FlowCnec flowCnec, Side side, OptimizationState optState) {
        // methods getPtdfZonalSum can throw an exception if RAO is executed on one state only
        try {
            return raoResult.getPtdfZonalSum(optState, flowCnec, side);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }
}
