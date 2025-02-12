/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.cim.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

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
    private static final String VOLTAGE_CNECS_CREATION_PARAMETERS = "voltage-cnecs-creation-parameters";
    private static final String TIMESTAMP = "timestamp";

    @Override
    public void serialize(CimCracCreationParameters cimParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeTimeseriesMrids(cimParameters.getTimeseriesMrids(), jsonGenerator);
        serializeRangeActionGroups(cimParameters.getRangeActionGroupsAsString(), jsonGenerator);
        serializeRangeActionSpeedSet(cimParameters.getRangeActionSpeedSet(), jsonGenerator);
        serializeVoltageCnecsCreationParameters(cimParameters.getVoltageCnecsCreationParameters(), jsonGenerator);
        serializeTimestamp(cimParameters.getTimestamp(), jsonGenerator);
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
                case VOLTAGE_CNECS_CREATION_PARAMETERS:
                    jsonParser.nextToken();
                    parameters.setVoltageCnecsCreationParameters(JsonVoltageCnecsCreationParameters.deserialize(jsonParser));
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

    private void serializeTimeseriesMrids(Set<String> timeseriesMrids, JsonGenerator jsonGenerator) throws IOException {
        if (!timeseriesMrids.isEmpty()) {
            serializeStringArray(TIMESERIES_MRIDS, timeseriesMrids.stream().sorted(String::compareTo).toList(), jsonGenerator);
        }
    }

    private void serializeRangeActionGroups(List<String> rangeActionGroupsAsString, JsonGenerator jsonGenerator) throws IOException {
        serializeStringArray(RANGE_ACTION_GROUPS, rangeActionGroupsAsString, jsonGenerator);
    }

    private void serializeRangeActionSpeedSet(Set<RangeActionSpeed> rangeActionSpeedSet, JsonGenerator jsonGenerator) throws IOException {
        if (!rangeActionSpeedSet.isEmpty()) {
            jsonGenerator.writeFieldName(RANGE_ACTION_SPEEDS);
            jsonGenerator.writeStartArray();
            for (RangeActionSpeed rangeActionSpeeds : rangeActionSpeedSet) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField(RANGE_ACTION_ID, rangeActionSpeeds.getRangeActionId());
                jsonGenerator.writeNumberField(SPEED, rangeActionSpeeds.getSpeed());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeVoltageCnecsCreationParameters(VoltageCnecsCreationParameters voltageCnecsCreationParameters, JsonGenerator jsonGenerator) throws IOException {
        if (voltageCnecsCreationParameters != null) {
            jsonGenerator.writeFieldName(VOLTAGE_CNECS_CREATION_PARAMETERS);
            JsonVoltageCnecsCreationParameters.serialize(voltageCnecsCreationParameters, jsonGenerator);
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

    private void serializeTimestamp(OffsetDateTime timestamp, JsonGenerator jsonGenerator) throws IOException {
        if (timestamp != null) {
            jsonGenerator.writeStringField(TIMESTAMP, timestamp.toString());
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
                        throw new OpenRaoException("Unexpected field: " + jsonParser.getCurrentName());
                }
            }
            if (rangeActionId == null) {
                throw new OpenRaoException(String.format("Missing range action ID in %s", RANGE_ACTION_SPEEDS));
            }
            if (speed == null) {
                throw new OpenRaoException(String.format("Missing speed in %s", RANGE_ACTION_SPEEDS));
            }
            if (ids.contains(rangeActionId)) {
                throw new OpenRaoException(String.format("Range action %s has two or more associated %s", rangeActionId, SPEED));
            }
            ids.add(rangeActionId);
            set.add(new RangeActionSpeed(rangeActionId, speed));
        }
        return set;
    }
}
