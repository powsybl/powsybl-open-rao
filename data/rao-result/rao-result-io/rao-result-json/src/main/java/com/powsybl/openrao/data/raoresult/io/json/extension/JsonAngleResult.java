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
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.extension.AngleResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonAngleResult implements RaoResultJsonUtils.ExtensionSerializer<AngleResult> {
    @Override
    public void serialize(AngleResult angleResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        Crac crac = (Crac) serializerProvider.getAttribute("crac");
        if (crac == null) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("AngleResult cannot be serialized because the serializerProvider does not contain a 'crac' attribute.");
            return;
        }

        List<Instant> instants = crac.getSortedInstants();
        List<AngleCnec> angleCnecs = crac.getAngleCnecs().stream().sorted(Comparator.comparing(AngleCnec::getId)).toList();

        jsonGenerator.writeStartArray();
        for (AngleCnec angleCnec : angleCnecs) {
            serializeForAngleCnec(jsonGenerator, angleCnec, angleResult, instants);
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public AngleResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        if (crac == null) {
            throw new OpenRaoException("AngleResult cannot be deserialized because the deserializationContext does not contain a 'crac` attribute.");
        }
        AngleResult.AngleResultBuilder angleResultBuilder = AngleResult.build();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AngleCnec angleCnec = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "angleCnecId" -> {
                        jsonParser.nextToken();
                        angleCnec = crac.getAngleCnec(jsonParser.getValueAsString());
                    }
                    case "initial" -> deserializeForInstant(jsonParser, angleResultBuilder, angleCnec, null, crac);
                    default -> deserializeForInstant(jsonParser, angleResultBuilder, angleCnec, jsonParser.currentName(), crac);
                }
            }
        }
        return angleResultBuilder.build();
    }

    // serialization

    private static void serializeForAngleCnec(JsonGenerator jsonGenerator, AngleCnec angleCnec, AngleResult angleResult, List<Instant> instants) throws IOException {
        if (hasAngleMeasurements(angleCnec, angleResult, instants)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("angleCnecId", angleCnec.getId());

            // serialize initial results
            if (hasAngleMeasurements(angleCnec, angleResult, (Instant) null)) {
                serializeForInstant(jsonGenerator, angleCnec, angleResult, null);
            }

            // serialize results per instant
            for (Instant instant : instants) {
                if (!instant.isOutage()
                    && !instant.comesAfter(angleCnec.getState().getInstant())
                    && hasAngleMeasurements(angleCnec, angleResult, instant)) {
                    serializeForInstant(jsonGenerator, angleCnec, angleResult, instant);
                }
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeForInstant(JsonGenerator jsonGenerator, AngleCnec angleCnec, AngleResult angleResult, Instant instant) throws IOException {
        jsonGenerator.writeObjectFieldStart(instant == null ? "initial" : instant.getId());
        jsonGenerator.writeObjectFieldStart(Unit.DEGREE.name().toLowerCase());
        jsonGenerator.writeNumberField("angle", angleResult.getAngle(instant, angleCnec, Unit.DEGREE));
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    private static boolean hasAngleMeasurements(AngleCnec angleCnec, AngleResult angleResult, List<Instant> instants) {
        return hasAngleMeasurements(angleCnec, angleResult, (Instant) null)
            || instants.stream().anyMatch(instant -> hasAngleMeasurements(angleCnec, angleResult, instant));
    }

    private static boolean hasAngleMeasurements(AngleCnec angleCnec, AngleResult angleResult, Instant instant) {
        return !Double.isNaN(angleResult.getAngle(instant, angleCnec, Unit.DEGREE));
    }

    // deserialization

    private static void deserializeForInstant(JsonParser jsonParser, AngleResult.AngleResultBuilder angleResultBuilder, AngleCnec angleCnec, String instantId, Crac crac) throws IOException {
        Instant instant = instantId == null ? null : crac.getInstant(instantId);
        jsonParser.nextToken();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.currentName().equals(Unit.DEGREE.name().toLowerCase())) {
                jsonParser.nextToken();
                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    if ("angle".equals(jsonParser.currentName())) {
                        jsonParser.nextToken();
                        angleResultBuilder.addMeasurement(jsonParser.getDoubleValue(), instant, angleCnec, Unit.DEGREE);
                    } else {
                        throw new OpenRaoException("Unexpected token: " + jsonParser.getCurrentToken());
                    }
                }
            } else {
                throw new OpenRaoException("Unsupported unit for angle values: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    @Override
    public String getExtensionName() {
        return "angle-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super AngleResult> getExtensionClass() {
        return AngleResult.class;
    }
}
