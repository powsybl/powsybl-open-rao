/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonFlowResult implements RaoResultJsonUtils.ExtensionSerializer<FlowResult> {
    @Override
    public void serialize(FlowResult flowResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        Crac crac = (Crac) serializerProvider.getAttribute("crac");
        if (crac == null) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("FlowResult cannot be serialized because the serializerProvider does not contain a 'crac` attribute.");
            return;
        }
        Set<Unit> flowUnits = (Set<Unit>) serializerProvider.getAttribute("flowUnits");
        if (flowUnits == null) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("FlowResult cannot be serialized because the serializerProvider does not contain a 'flowUnits` attribute.");
            return;
        } else if (flowUnits.isEmpty()) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("FlowResult cannot be serialized because the flow units are empty.");
            return;
        }

        List<Unit> sortedUnits = flowUnits.stream().sorted(Comparator.comparing(Unit::toString)).toList();
        List<Instant> instants = crac.getSortedInstants();
        List<FlowCnec> flowCnecs = crac.getFlowCnecs().stream().sorted(Comparator.comparing(FlowCnec::getId)).toList();

        jsonGenerator.writeStartArray();
        for (FlowCnec flowCnec : flowCnecs) {
            serializeForFlowCnec(jsonGenerator, flowCnec, flowResult, instants, sortedUnits);
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public FlowResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        if (crac == null) {
            throw new OpenRaoException("FlowResult cannot be deserialized because the deserializationContext does not contain a 'crac` attribute.");
        }
        FlowResult.FlowResultBuilder flowResultBuilder = FlowResult.build();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            FlowCnec flowCnec = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "flowCnecId" -> {
                        jsonParser.nextToken();
                        flowCnec = crac.getFlowCnec(jsonParser.getValueAsString());
                    }
                    case "initial" -> deserializeForInstant(jsonParser, flowResultBuilder, flowCnec, null, crac);
                    default ->
                        deserializeForInstant(jsonParser, flowResultBuilder, flowCnec, jsonParser.currentName(), crac);
                }
            }
        }
        return flowResultBuilder.build();
    }

    // serialization

    private static void serializeForFlowCnec(JsonGenerator jsonGenerator, FlowCnec flowCnec, FlowResult flowResult, List<Instant> instants, List<Unit> flowUnits) throws IOException {
        if (hasFlowMeasurements(flowCnec, flowResult, instants, flowUnits)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("flowCnecId", flowCnec.getId());

            // serialize initial results
            if (hasFlowMeasurements(flowCnec, flowResult, (Instant) null, flowUnits)) {
                serializeForInstant(jsonGenerator, flowCnec, flowResult, null, flowUnits);
            }

            // serialize results per instant
            for (Instant instant : instants) {
                if (!instant.isOutage()
                    && !instant.comesAfter(flowCnec.getState().getInstant())
                    && hasFlowMeasurements(flowCnec, flowResult, instant, flowUnits)) {
                    serializeForInstant(jsonGenerator, flowCnec, flowResult, instant, flowUnits);
                }
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeForInstant(JsonGenerator jsonGenerator, FlowCnec flowCnec, FlowResult flowResult, Instant instant, List<Unit> flowUnits) throws IOException {
        jsonGenerator.writeObjectFieldStart(instant == null ? "initial" : instant.getId());
        for (Unit unit : flowUnits) {
            if (hasFlowMeasurements(flowCnec, flowResult, instant, unit)) {
                serializeForUnit(jsonGenerator, flowCnec, flowResult, instant, unit);
            }
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeForUnit(JsonGenerator jsonGenerator, FlowCnec flowCnec, FlowResult flowResult, Instant instant, Unit unit) throws IOException {
        jsonGenerator.writeObjectFieldStart(unit.name().toLowerCase());
        for (TwoSides side : TwoSides.values()) {
            if (hasFlowMeasurements(flowCnec, flowResult, instant, side, unit)) {
                serializeForSide(jsonGenerator, flowCnec, flowResult, instant, unit, side);
            }
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeForSide(JsonGenerator jsonGenerator, FlowCnec flowCnec, FlowResult flowResult, Instant instant, Unit unit, TwoSides side) throws IOException {
        jsonGenerator.writeObjectFieldStart("side" + side.getNum());

        // serialize flow
        double flow = flowResult.getFlow(instant, flowCnec, side, unit);
        if (!Double.isNaN(flow)) {
            jsonGenerator.writeNumberField("flow", flow);
        }

        // serialize commercial flow
        double commercialFlow = flowResult.getCommercialFlow(instant, flowCnec, side, unit);
        if (!Double.isNaN(commercialFlow)) {
            jsonGenerator.writeNumberField("commercialFlow", commercialFlow);
        }

        // serialize PTDF zonal sum -> TODO: kept here to mimic RAO Result, might have a better place
        if (unit == Unit.MEGAWATT) {
            double ptdfZonalSum = flowResult.getPtdfZonalSum(instant, flowCnec, side);
            if (!Double.isNaN(ptdfZonalSum)) {
                jsonGenerator.writeNumberField("zonalPtdfSum", ptdfZonalSum);
            }
        }

        jsonGenerator.writeEndObject();
    }

    private static boolean hasFlowMeasurements(FlowCnec flowCnec, FlowResult flowResult, List<Instant> instants, List<Unit> flowUnits) {
        return hasFlowMeasurements(flowCnec, flowResult, (Instant) null, flowUnits)
            || instants.stream().anyMatch(instant -> hasFlowMeasurements(flowCnec, flowResult, instant, flowUnits));
    }

    private static boolean hasFlowMeasurements(FlowCnec flowCnec, FlowResult flowResult, Instant instant, List<Unit> flowUnits) {
        return flowUnits.stream().anyMatch(unit -> hasFlowMeasurements(flowCnec, flowResult, instant, unit));
    }

    private static boolean hasFlowMeasurements(FlowCnec flowCnec, FlowResult flowResult, Instant instant, Unit unit) {
        return hasFlowMeasurements(flowCnec, flowResult, instant, TwoSides.ONE, unit) || hasFlowMeasurements(flowCnec, flowResult, instant, TwoSides.TWO, unit);
    }

    private static boolean hasFlowMeasurements(FlowCnec flowCnec, FlowResult flowResult, Instant instant, TwoSides side, Unit unit) {
        return !Double.isNaN(flowResult.getFlow(instant, flowCnec, side, unit));
    }

    // deserialization

    private static void deserializeForInstant(JsonParser jsonParser, FlowResult.FlowResultBuilder flowResultBuilder, FlowCnec flowCnec, String instantId, Crac crac) throws IOException {
        Instant instant = instantId == null ? null : crac.getInstant(instantId);
        jsonParser.nextToken();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case "ampere" -> {
                    jsonParser.nextToken();
                    deserializeForUnit(jsonParser, flowResultBuilder, flowCnec, instant, Unit.AMPERE);
                }
                case "megawatt" -> {
                    jsonParser.nextToken();
                    deserializeForUnit(jsonParser, flowResultBuilder, flowCnec, instant, Unit.MEGAWATT);
                }
                default ->
                    throw new OpenRaoException("Unsupported unit for flow values: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    private static void deserializeForUnit(JsonParser jsonParser, FlowResult.FlowResultBuilder flowResultBuilder, FlowCnec flowCnec, Instant instant, Unit unit) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case "side1" -> {
                    jsonParser.nextToken();
                    deserializeForSide(jsonParser, flowResultBuilder, flowCnec, instant, unit, TwoSides.ONE);
                }
                case "side2" -> {
                    jsonParser.nextToken();
                    deserializeForSide(jsonParser, flowResultBuilder, flowCnec, instant, unit, TwoSides.TWO);
                }
                default ->
                    throw new OpenRaoException("Unsupported side for flow values: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    private static void deserializeForSide(JsonParser jsonParser, FlowResult.FlowResultBuilder flowResultBuilder, FlowCnec flowCnec, Instant instant, Unit unit, TwoSides side) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case "flow" -> {
                    jsonParser.nextToken();
                    flowResultBuilder.addFlowMeasurement(jsonParser.getDoubleValue(), instant, flowCnec, side, unit);
                }
                case "commercialFlow" -> {
                    jsonParser.nextToken();
                    flowResultBuilder.addCommercialFlowMeasurement(jsonParser.getDoubleValue(), instant, flowCnec, side, unit);
                }
                case "zonalPtdfSum" -> {
                    if (unit != Unit.MEGAWATT) {
                        throw new OpenRaoException("Zonal PTDF sum only defined for measurements in MW.");
                    }
                    jsonParser.nextToken();
                    flowResultBuilder.addPtdfZonalSumMeasurement(jsonParser.getDoubleValue(), instant, flowCnec, side);
                }
                default ->
                    throw new OpenRaoException("Unsupported flow value: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    @Override
    public String getExtensionName() {
        return "flow-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super FlowResult> getExtensionClass() {
        return FlowResult.class;
    }
}
