/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultSerializer extends AbstractJsonSerializer<RaoResult> {

    @Override
    public void serialize(RaoResult raoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        serializeFlowCnecsResult(raoResult, jsonGenerator);
        jsonGenerator.writeEndObject();

    }

    private void serializeFlowCnecsResult(RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        List<FlowCnec> sortedListOfFlowCnecs = raoResult.getFlowCnecs().stream()
            .sorted(Comparator.comparing(FlowCnec::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(FLOWCNEC_RESULTS);
        for (FlowCnec flowCnec : sortedListOfFlowCnecs) {
            serializeFlowCnecResult(flowCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeFlowCnecResult(FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(FLOWCNEC_ID, flowCnec.getId());

        serializeFlowCnecResultForOptimizationState(OptimizationState.INITIAL, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_PRA, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_CRA, flowCnec, raoResult, jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    private void serializeFlowCnecResultForOptimizationState(OptimizationState optState, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));
        serializeFlowCnecResultForOptimizationStateAndUnit(optState, MEGAWATT, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationStateAndUnit(optState, AMPERE, flowCnec, raoResult, jsonGenerator);
        double ptdfZonalSum = raoResult.getPtdfZonalSum(optState, flowCnec);
        if (!Double.isNaN(ptdfZonalSum)) {
            jsonGenerator.writeNumberField(ZONAL_PTDF_SUM, ptdfZonalSum);
        }
        jsonGenerator.writeEndObject();
    }

    private void serializeFlowCnecResultForOptimizationStateAndUnit(OptimizationState optState, Unit unit, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double flow = raoResult.getFlow(optState, flowCnec, unit);
        double margin = raoResult.getMargin(optState, flowCnec, unit);
        double relativeMargin = raoResult.getRelativeMargin(optState, flowCnec, unit);
        double loopFlow = raoResult.getLoopFlow(optState, flowCnec, unit);
        double commercialFlow = raoResult.getCommercialFlow(optState, flowCnec, unit);

        if (Double.isNaN(flow) && Double.isNaN(margin) && Double.isNaN(relativeMargin) && Double.isNaN(loopFlow) && Double.isNaN(commercialFlow)) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeUnit(unit));
        if (!Double.isNaN(flow)) {
            jsonGenerator.writeNumberField(FLOW, flow);
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(MARGIN, margin);
        }
        if (!Double.isNaN(relativeMargin)) {
            jsonGenerator.writeNumberField(RELATIVE_MARGIN, relativeMargin);
        }
        if (!Double.isNaN(loopFlow)) {
            jsonGenerator.writeNumberField(LOOP_FLOW, loopFlow);
        }
        if (!Double.isNaN(commercialFlow)) {
            jsonGenerator.writeNumberField(COMMERCIAL_FLOW, commercialFlow);
        }
        jsonGenerator.writeEndObject();
    }
}
