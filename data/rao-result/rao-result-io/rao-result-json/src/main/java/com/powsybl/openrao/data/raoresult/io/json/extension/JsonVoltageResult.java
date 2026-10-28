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
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonVoltageResult implements RaoResultJsonUtils.ExtensionSerializer<VoltageResult> {
    @Override
    public void serialize(VoltageResult voltageResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        Crac crac = (Crac) serializerProvider.getAttribute("crac");
        if (crac == null) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("VoltageResult cannot be serialized because the serializerProvider does not contain a 'crac` attribute.");
            return;
        }

        List<Instant> instants = crac.getSortedInstants();
        List<VoltageCnec> voltageCnecs = crac.getVoltageCnecs().stream().sorted(Comparator.comparing(VoltageCnec::getId)).toList();

        jsonGenerator.writeStartArray();
        for (VoltageCnec voltageCnec : voltageCnecs) {
            serializeForVoltageCnec(jsonGenerator, voltageCnec, voltageResult, instants);
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public VoltageResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        if (crac == null) {
            throw new OpenRaoException("VoltageResult cannot be deserialized because the deserializationContext does not contain a 'crac` attribute.");
        }

        VoltageResult.VoltageResultBuilder voltageResultBuilder = VoltageResult.build();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            VoltageCnec voltageCnec = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "voltageCnecId" -> {
                        jsonParser.nextToken();
                        voltageCnec = crac.getVoltageCnec(jsonParser.getValueAsString());
                    }
                    case "initial" -> deserializeForInstant(jsonParser, voltageResultBuilder, voltageCnec, null, crac);
                    default -> deserializeForInstant(jsonParser, voltageResultBuilder, voltageCnec, jsonParser.currentName(), crac);
                }
            }
        }
        return voltageResultBuilder.build();
    }

    // serialization

    private static void serializeForVoltageCnec(JsonGenerator jsonGenerator, VoltageCnec voltageCnec, VoltageResult voltageResult, List<Instant> instants) throws IOException {
        if (hasVoltageMeasurements(voltageCnec, voltageResult, instants)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("voltageCnecId", voltageCnec.getId());

            // serialize initial results
            if (hasVoltageMeasurements(voltageCnec, voltageResult, (Instant) null)) {
                serializeForInstant(jsonGenerator, voltageCnec, voltageResult, null);
            }

            // serialize results per instant
            for (Instant instant : instants) {
                if (!instant.isOutage()
                    && !instant.comesAfter(voltageCnec.getState().getInstant())
                    && hasVoltageMeasurements(voltageCnec, voltageResult, instant)) {
                    serializeForInstant(jsonGenerator, voltageCnec, voltageResult, instant);
                }
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeForInstant(JsonGenerator jsonGenerator, VoltageCnec voltageCnec, VoltageResult voltageResult, Instant instant) throws IOException {
        jsonGenerator.writeObjectFieldStart(instant == null ? "initial" : instant.getId());
        jsonGenerator.writeObjectFieldStart(Unit.KILOVOLT.name().toLowerCase());
        jsonGenerator.writeNumberField("minVoltage", voltageResult.getMinVoltage(instant, voltageCnec, Unit.KILOVOLT));
        jsonGenerator.writeNumberField("maxVoltage", voltageResult.getMaxVoltage(instant, voltageCnec, Unit.KILOVOLT));
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    private static boolean hasVoltageMeasurements(VoltageCnec voltageCnec, VoltageResult voltageResult, List<Instant> instants) {
        return hasVoltageMeasurements(voltageCnec, voltageResult, (Instant) null)
            || instants.stream().anyMatch(instant -> hasVoltageMeasurements(voltageCnec, voltageResult, instant));
    }

    private static boolean hasVoltageMeasurements(VoltageCnec voltageCnec, VoltageResult voltageResult, Instant instant) {
        return !Double.isNaN(voltageResult.getMinVoltage(instant, voltageCnec, Unit.KILOVOLT))
            && !Double.isNaN(voltageResult.getMaxVoltage(instant, voltageCnec, Unit.KILOVOLT));
    }

    // deserialization

    private static void deserializeForInstant(JsonParser jsonParser,
                                              VoltageResult.VoltageResultBuilder voltageResultBuilder,
                                              VoltageCnec voltageCnec,
                                              String instantId,
                                              Crac crac) throws IOException {
        Instant instant = instantId == null ? null : crac.getInstant(instantId);
        jsonParser.nextToken();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.currentName().equals(Unit.KILOVOLT.name().toLowerCase())) {
                jsonParser.nextToken();
                Double minVoltage = null;
                Double maxVoltage = null;
                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    switch (jsonParser.currentName()) {
                        case "minVoltage" -> {
                            jsonParser.nextToken();
                            minVoltage = jsonParser.getDoubleValue();
                        }
                        case "maxVoltage" -> {
                            jsonParser.nextToken();
                            maxVoltage = jsonParser.getDoubleValue();
                        }
                        default -> throw new OpenRaoException("Unexpected token: " + jsonParser.getCurrentToken());
                    }
                }
                if (minVoltage != null && maxVoltage != null) {
                    voltageResultBuilder.addMeasurement(minVoltage, maxVoltage, instant, voltageCnec, Unit.KILOVOLT);
                } else {
                    throw new OpenRaoException("Missing min or max voltage for VoltageCNEC %s at instant %s.".formatted(voltageCnec.getId(), instant == null ? "initial" : instant.getId()));
                }
            } else {
                throw new OpenRaoException("Unsupported unit for voltage values: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    @Override
    public String getExtensionName() {
        return "voltage-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super VoltageResult> getExtensionClass() {
        return VoltageResult.class;
    }
}
