/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonCimCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<CimCracCreationParameters> {

    private static final String RANGE_ACTION_GROUPS = "range-action-groups";
    private static final String RANGE_ACTION_SPEEDS = "range-action-speeds";
    private static final String RANGE_ACTION_ID = "range-action-id";
    private static final String SPEED = "speed";
    private static final String TIMESERIES_MRIDS = "timeseries-mrids";

    @Override
    public void serialize(CimCracCreationParameters cimParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeTimeseriesMrids(cimParameters, jsonGenerator);
        serializeRangeActionGroups(cimParameters, jsonGenerator);
        serializeRangeActionSpeedSet(cimParameters, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public CimCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new CimCracCreationParameters());
    }

    @Override
    public CimCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, CimCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case TIMESERIES_MRIDS:
                    jsonParser.nextToken();
                    parameters.setTimeseriesMrids(jsonParser.readValueAs(Set.class));
                    break;
                case RANGE_ACTION_GROUPS:
                    jsonParser.nextToken();
                    parameters.setRangeActionGroupsAsString(jsonParser.readValueAs(ArrayList.class));
                    break;
                case RANGE_ACTION_SPEEDS:
                    jsonParser.nextToken();
                    parameters.setRemedialActionSpeed(deserializeRangeActionSpeedSet(jsonParser));
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "CimCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super CimCracCreationParameters> getExtensionClass() {
        return CimCracCreationParameters.class;
    }


    private void serializeTimeseriesMrids(CimCracCreationParameters cimParameters, JsonGenerator jsonGenerator) throws IOException {
        if (!Objects.isNull(cimParameters.getTimeseriesMrids())) {
            serializeStringArray(TIMESERIES_MRIDS, cimParameters.getTimeseriesMrids().stream().sorted(String::compareTo).collect(Collectors.toList()), jsonGenerator);
        }
    }

    private void serializeRangeActionGroups(CimCracCreationParameters cimParameters, JsonGenerator jsonGenerator) throws IOException {
        serializeStringArray(RANGE_ACTION_GROUPS, cimParameters.getRangeActionGroupsAsString(), jsonGenerator);
    }

    private void serializeRangeActionSpeedSet(CimCracCreationParameters cimParameters, JsonGenerator jsonGenerator) throws IOException {
        if (!cimParameters.getRangeActionSpeedSet().isEmpty()) {
            jsonGenerator.writeFieldName(RANGE_ACTION_SPEEDS);
            jsonGenerator.writeStartArray();
            for (RangeActionSpeed rangeActionSpeeds : cimParameters.getRangeActionSpeedSet()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField(RANGE_ACTION_ID, rangeActionSpeeds.getRangeActionId());
                jsonGenerator.writeNumberField(SPEED, rangeActionSpeeds.getSpeed());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeStringArray(String fieldName, List<String> content, JsonGenerator jsonGenerator) throws IOException {
        if (!content.isEmpty()) {
            jsonGenerator.writeArrayFieldStart(fieldName);
            for (String string : content) {
                jsonGenerator.writeString(string);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private Set<RangeActionSpeed> deserializeRangeActionSpeedSet(JsonParser jsonParser) throws IOException {
        Set<String> ids = new HashSet<>();
        Set<RangeActionSpeed> set = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String rangeActionId = null;
            Integer speed = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case RANGE_ACTION_ID:
                        rangeActionId = jsonParser.nextTextValue();
                        break;
                    case SPEED:
                        speed = jsonParser.nextIntValue(Integer.MAX_VALUE);
                        break;
                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                }
            }
            if (rangeActionId == null) {
                throw new FaraoException(String.format("Missing range action ID in %s", RANGE_ACTION_SPEEDS));
            }
            if (speed == null) {
                throw new FaraoException(String.format("Missing speed in %s", RANGE_ACTION_SPEEDS));
            }
            if (ids.contains(rangeActionId)) {
                throw new FaraoException(String.format("Range action %s has two or more associated %s", rangeActionId, SPEED));
            }
            ids.add(rangeActionId);
            set.add(new RangeActionSpeed(rangeActionId, speed));
        }
        return set;
    }
}
