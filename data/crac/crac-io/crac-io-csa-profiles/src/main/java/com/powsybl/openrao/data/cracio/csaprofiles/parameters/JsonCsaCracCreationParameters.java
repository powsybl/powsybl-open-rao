/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParameters;

import java.io.IOException;
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
    private static final String SPS_MAX_TIME_TO_IMPLEMENT_THRESHOLD_IN_SECONDS = "sps-max-time-to-implement-threshold-in-seconds";
    private static final String USE_PATL_IN_FINAL_STATE = "use-patl-in-final-state";
    private static final String CRA_APPLICATION_WINDOW = "cra-application-window";
    private static final String BORDERS = "borders";
    private static final String NAME = "name";
    private static final String EIC = "eic";
    private static final String DEFAULT_FOR_TSO = "default-for-tso";

    @Override
    public void serialize(CsaCracCreationParameters csaParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeCapacityCalculationRegionEicCode(csaParameters.getCapacityCalculationRegionEicCode(), jsonGenerator);
        serializeSpsMaxTimeToImplementThresholdInSeconds(csaParameters.getSpsMaxTimeToImplementThresholdInSeconds(), jsonGenerator);
        serializeUsePatlInFinalState(csaParameters.getUsePatlInFinalState(), jsonGenerator);
        serializeCraApplicationWindow(csaParameters.getCraApplicationWindow(), jsonGenerator);
        serializeBorders(csaParameters.getBorders(), jsonGenerator);
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
                case SPS_MAX_TIME_TO_IMPLEMENT_THRESHOLD_IN_SECONDS:
                    jsonParser.nextToken();
                    parameters.setSpsMaxTimeToImplementThresholdInSeconds(jsonParser.readValueAs(Integer.class));
                    break;
                case USE_PATL_IN_FINAL_STATE:
                    jsonParser.nextToken();
                    parameters.setUsePatlInFinalState(deserializeUsePatlInFinalStateMap(jsonParser));
                    break;
                case CRA_APPLICATION_WINDOW:
                    jsonParser.nextToken();
                    parameters.setCraApplicationWindow(deserializeCraApplicationWindowMap(jsonParser));
                    break;
                case BORDERS:
                    jsonParser.nextToken();
                    parameters.setBorders(deserializeBorders(jsonParser));
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

    private void serializeSpsMaxTimeToImplementThresholdInSeconds(Integer spsMaxTimeToImplementThresholdInSeconds, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStringField(SPS_MAX_TIME_TO_IMPLEMENT_THRESHOLD_IN_SECONDS, spsMaxTimeToImplementThresholdInSeconds.toString());
    }

    private void serializeUsePatlInFinalState(Map<String, Boolean> usePatlInFinalState, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(USE_PATL_IN_FINAL_STATE);
        jsonGenerator.writeStartObject();
        usePatlInFinalState.forEach((tso, usePatl) -> {
            try {
                jsonGenerator.writeBooleanField(tso, usePatl);
            } catch (IOException e) {
                throw new OpenRaoException("Could not serialize " + USE_PATL_IN_FINAL_STATE + " map. Reason: " + e.getMessage());
            }
        });
        jsonGenerator.writeEndObject();
    }

    private void serializeCraApplicationWindow(Map<String, Integer> craApplicationWindow, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(CRA_APPLICATION_WINDOW);
        jsonGenerator.writeStartObject();
        craApplicationWindow.forEach((instant, duration) -> {
            try {
                jsonGenerator.writeNumberField(instant, duration);
            } catch (IOException e) {
                throw new OpenRaoException("Could not serialize " + CRA_APPLICATION_WINDOW + " map. Reason: " + e.getMessage());
            }
        });
        jsonGenerator.writeEndObject();
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
                throw new OpenRaoException("Could not serialize " + BORDERS + " map. Reason: " + e.getMessage());
            }
        });
        jsonGenerator.writeEndArray();
    }

    @Override
    public CsaCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new CsaCracCreationParameters());
    }

    private Map<String, Boolean> deserializeUsePatlInFinalStateMap(JsonParser jsonParser) throws IOException {
        Map<String, Boolean> usePatlInFinalState = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            usePatlInFinalState.put(jsonParser.getCurrentName(), jsonParser.nextBooleanValue());
        }
        return usePatlInFinalState;
    }

    private Map<String, Integer> deserializeCraApplicationWindowMap(JsonParser jsonParser) throws IOException {
        Map<String, Integer> craApplicationWindow = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            craApplicationWindow.put(jsonParser.getCurrentName(), jsonParser.nextIntValue(0));
        }
        return craApplicationWindow;
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
}
