/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.csaprofiles.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonCsaCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<CsaCracCreationParameters> {

    private static final String CAPACITY_CALCULATION_REGION_EIC_CODE = "capacity-calculation-region-eic-code";
    private static final String TSOS_WHICH_DO_NOT_USE_PATL_IN_FINAL_STATE = "tsos-which-do-not-use-patl-in-final-state";
    private static final String AUTO_INSTANT_APPLICATION_TIME = "auto-instant-application-time";
    private static final String CURATIVE_INSTANTS = "curative-instants";
    private static final String NAME = "name";
    private static final String APPLICATION_TIME = "application-time";
    private static final String BORDERS = "borders";
    private static final String EIC = "eic";
    private static final String DEFAULT_FOR_TSO = "default-for-tso";
    private static final String TIMESTAMP = "timestamp";

    @Override
    public void serialize(CsaCracCreationParameters csaParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeCapacityCalculationRegionEicCode(csaParameters.getCapacityCalculationRegionEicCode(), jsonGenerator);
        serializeAutoInstantApplicationTime(csaParameters.getAutoInstantApplicationTime(), jsonGenerator);
        serializeTsosWhichDoNotUsePatlInFinalState(csaParameters.getTsosWhichDoNotUsePatlInFinalState(), jsonGenerator);
        serializeCurativeInstants(csaParameters.getCurativeInstants(), jsonGenerator);
        serializeBorders(csaParameters.getBorders(), jsonGenerator);
        serializeTimestamp(csaParameters.getTimestamp(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public CsaCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, CsaCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case CAPACITY_CALCULATION_REGION_EIC_CODE:
                    jsonParser.nextToken();
                    parameters.setCapacityCalculationRegionEicCode(jsonParser.readValueAs(String.class));
                    break;
                case AUTO_INSTANT_APPLICATION_TIME:
                    jsonParser.nextToken();
                    parameters.setAutoInstantApplicationTime(jsonParser.readValueAs(Integer.class));
                    break;
                case TSOS_WHICH_DO_NOT_USE_PATL_IN_FINAL_STATE:
                    jsonParser.nextToken();
                    parameters.setTsosWhichDoNotUsePatlInFinalState(jsonParser.readValueAs(new TypeReference<HashSet<String>>() { }));
                    break;
                case CURATIVE_INSTANTS:
                    jsonParser.nextToken();
                    parameters.setCurativeInstants(deserializeCurativeInstants(jsonParser));
                    break;
                case BORDERS:
                    jsonParser.nextToken();
                    parameters.setBorders(deserializeBorders(jsonParser));
                    break;
                case TIMESTAMP:
                    jsonParser.nextToken();
                    parameters.setTimestamp(OffsetDateTime.parse(jsonParser.readValueAs(String.class)));
                    break;
                default:
                    throw new OpenRaoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "CsaCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super CsaCracCreationParameters> getExtensionClass() {
        return CsaCracCreationParameters.class;
    }

    private void serializeCapacityCalculationRegionEicCode(String eicCode, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStringField(CAPACITY_CALCULATION_REGION_EIC_CODE, eicCode);
    }

    private void serializeAutoInstantApplicationTime(Integer spsMaxTimeToImplementThresholdInSeconds, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStringField(AUTO_INSTANT_APPLICATION_TIME, spsMaxTimeToImplementThresholdInSeconds.toString());
    }

    private void serializeTsosWhichDoNotUsePatlInFinalState(Set<String> usePatlInFinalState, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(TSOS_WHICH_DO_NOT_USE_PATL_IN_FINAL_STATE);
        jsonGenerator.writeStartArray();
        usePatlInFinalState.forEach(tso -> {
            try {
                jsonGenerator.writeString(tso);
            } catch (IOException e) {
                throwSerializationError(TSOS_WHICH_DO_NOT_USE_PATL_IN_FINAL_STATE, e);
            }
        });
        jsonGenerator.writeEndArray();
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

    private void serializeBorders(Set<Border> borders, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(BORDERS);
        jsonGenerator.writeStartArray();
        borders.forEach(border -> {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField(NAME, border.name());
                jsonGenerator.writeStringField(EIC, border.eic());
                jsonGenerator.writeStringField(DEFAULT_FOR_TSO, border.defaultForTso());
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                throwSerializationError(BORDERS, e);
            }
        });
        jsonGenerator.writeEndArray();
    }

    private void serializeTimestamp(OffsetDateTime timestamp, JsonGenerator jsonGenerator) throws IOException {
        if (timestamp != null) {
            jsonGenerator.writeStringField(TIMESTAMP, timestamp.toString());
        }
    }

    @Override
    public CsaCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new CsaCracCreationParameters());
    }

    private Map<String, Integer> deserializeCurativeInstants(JsonParser jsonParser) throws IOException {
        Map<String, Integer> curativeInstants = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String name = null;
            Integer applicationTime = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                if (NAME.equals(jsonParser.getCurrentName())) {
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

    private Set<Border> deserializeBorders(JsonParser jsonParser) throws IOException {
        Set<Border> borders = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Map<String, String> borderData = new HashMap<>();
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                borderData.put(jsonParser.getCurrentName(), jsonParser.nextTextValue());
            }
            borders.add(new Border(borderData.get(NAME), borderData.get(EIC), borderData.get(DEFAULT_FOR_TSO)));
        }
        return borders;
    }

    private static void throwSerializationError(String nonSerializableField, IOException e) {
        throw new OpenRaoException("Could not serialize " + nonSerializableField + " map. Reason: " + e.getMessage());
    }
}
