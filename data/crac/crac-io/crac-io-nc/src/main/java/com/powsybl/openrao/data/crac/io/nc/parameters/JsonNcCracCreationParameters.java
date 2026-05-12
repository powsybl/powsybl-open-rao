/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonNcCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<NcCracCreationParameters> {

    private static final String CAPACITY_CALCULATION_REGION = "capacity-calculation-region";
    private static final String CURATIVE_INSTANTS = "curative-instants";
    private static final String NAME = "name";
    private static final String APPLICATION_TIME = "application-time";
    private static final String TIMESTAMP = "timestamp";

    @Override
    public void serialize(NcCracCreationParameters ncParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeTimestamp(ncParameters.getTimestamp(), jsonGenerator);
        serializeCapacityCalculationRegion(ncParameters.getCapacityCalculationRegion(), jsonGenerator);
        serializeCurativeInstants(ncParameters.getCurativeInstants(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public NcCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, NcCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case TIMESTAMP:
                    jsonParser.nextToken();
                    parameters.setTimestamp(OffsetDateTime.parse(jsonParser.readValueAs(String.class)));
                    break;
                case CAPACITY_CALCULATION_REGION:
                    jsonParser.nextToken();
                    parameters.setCapacityCalculationRegion(CapacityCalculationRegion.fromEIC(jsonParser.readValueAs(String.class)));
                    break;
                case CURATIVE_INSTANTS:
                    jsonParser.nextToken();
                    parameters.setCurativeInstants(deserializeCurativeInstants(jsonParser));
                    break;
                default:
                    throw new OpenRaoException("Unexpected field: " + jsonParser.currentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "NcCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super NcCracCreationParameters> getExtensionClass() {
        return NcCracCreationParameters.class;
    }

    private void serializeCapacityCalculationRegion(CapacityCalculationRegion capacityCalculationRegion, JsonGenerator jsonGenerator) throws IOException {
        if (capacityCalculationRegion != null) {
            jsonGenerator.writeStringField(CAPACITY_CALCULATION_REGION, capacityCalculationRegion.getEIC());
        }
    }

    private void serializeCurativeInstants(Map<String, Integer> curativeInstants, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(CURATIVE_INSTANTS);
        jsonGenerator.writeStartArray();
        curativeInstants.forEach((name, applicationTime) -> {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField(NAME, name);
                jsonGenerator.writeNumberField(APPLICATION_TIME, applicationTime);
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                throwSerializationError(CURATIVE_INSTANTS, e);
            }
        });
        jsonGenerator.writeEndArray();
    }

    private void serializeTimestamp(OffsetDateTime timestamp, JsonGenerator jsonGenerator) throws IOException {
        if (timestamp != null) {
            jsonGenerator.writeStringField(TIMESTAMP, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(timestamp));
        }
    }

    @Override
    public NcCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new NcCracCreationParameters());
    }

    private Map<String, Integer> deserializeCurativeInstants(JsonParser jsonParser) throws IOException {
        Map<String, Integer> curativeInstants = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String name = null;
            Integer applicationTime = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                if (NAME.equals(jsonParser.currentName())) {
                    name = jsonParser.nextTextValue();
                } else if (APPLICATION_TIME.equals(jsonParser.getCurrentName())) {
                    applicationTime = jsonParser.nextIntValue(0);
                } else {
                    throw new OpenRaoException("Unexpected field in %s: %s".formatted(CURATIVE_INSTANTS, jsonParser.getCurrentName()));
                }
            }
            if (name == null || applicationTime == null) {
                throw new OpenRaoException("Incomplete data for curative instant; please provide both a %s and an %s".formatted(NAME, APPLICATION_TIME));
            }
            curativeInstants.put(name, applicationTime);
        }
        return curativeInstants;
    }

    private static void throwSerializationError(String nonSerializableField, IOException e) {
        throw new OpenRaoException("Could not serialize " + nonSerializableField + " map. Reason: " + e.getMessage());
    }
}
