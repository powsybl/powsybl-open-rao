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
import com.powsybl.entsoe.utils.CapacityCalculationRegion;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final String COUNTER_TRADING_MIN_RANGE = "counter-trading-min-range";
    private static final String COUNTER_TRADING_MAX_RANGE = "counter-trading-max-range";
    private static final String CONNECTED_AREAS = "connected-areas";
    private static final String AREA = "area";
    private static final String RANGE_TYPE = "range-type";
    private static final String BORDER_RANGES = "border-ranges";
    private static final String BORDER_RANGE_MIN = "border-range-min";
    private static final String BORDER_RANGE_MAX = "border-range-max";

    @Override
    public void serialize(NcCracCreationParameters ncParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeTimestamp(ncParameters.getTimestamp(), jsonGenerator);
        serializeCapacityCalculationRegion(ncParameters.getCapacityCalculationRegion(), jsonGenerator);
        serializeCurativeInstants(ncParameters.getCurativeInstants(), jsonGenerator);
        serializeCounterTradingRange(ncParameters.getCounterTradingMinRange(), ncParameters.getCounterTradingMaxRange(), jsonGenerator);
        serializeConnectedAreas(ncParameters.getConnectedAreas(), jsonGenerator);
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
                case COUNTER_TRADING_MIN_RANGE:
                    jsonParser.nextToken();
                    parameters.setCounterTradingMinRange(jsonParser.readValueAs(Double.class));
                    break;
                case COUNTER_TRADING_MAX_RANGE:
                    jsonParser.nextToken();
                    parameters.setCounterTradingMaxRange(jsonParser.readValueAs(Double.class));
                    break;
                case CONNECTED_AREAS:
                    jsonParser.nextToken();
                    parameters.setConnectedAreas(deserializeConnectedAreas(jsonParser));
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

    private void serializeCounterTradingRange(Double counterTradingMinRange, Double counterTradingMaxRange, JsonGenerator jsonGenerator) throws IOException {
        if (counterTradingMinRange != null) {
            jsonGenerator.writeFieldName(COUNTER_TRADING_MIN_RANGE);
            jsonGenerator.writeNumber(counterTradingMinRange);
        }
        if (counterTradingMaxRange != null) {
            jsonGenerator.writeFieldName(COUNTER_TRADING_MAX_RANGE);
            jsonGenerator.writeNumber(counterTradingMaxRange);
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
                } else if (APPLICATION_TIME.equals(jsonParser.currentName())) {
                    applicationTime = jsonParser.nextIntValue(0);
                } else {
                    throw new OpenRaoException("Unexpected field in %s: %s".formatted(CURATIVE_INSTANTS, jsonParser.currentName()));
                }
            }
            if (name == null || applicationTime == null) {
                throw new OpenRaoException("Incomplete data for curative instant; please provide both a %s and an %s".formatted(NAME, APPLICATION_TIME));
            }
            curativeInstants.put(name, applicationTime);
        }
        return curativeInstants;
    }

    private void serializeConnectedAreas(List<NcCracCreationParameters.ConnectedArea> connectedAreas, JsonGenerator jsonGenerator) throws IOException {
        if (connectedAreas.isEmpty()) {
            return;
        }
        jsonGenerator.writeFieldName(CONNECTED_AREAS);
        jsonGenerator.writeStartArray();
        for (NcCracCreationParameters.ConnectedArea connectedArea : connectedAreas) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(AREA, connectedArea.area());
            jsonGenerator.writeFieldName(BORDER_RANGES);
            jsonGenerator.writeStartArray();
            for (NcCracCreationParameters.BorderRange borderRange : connectedArea.borderRanges()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField(RANGE_TYPE, borderRange.rangeType());
                if (borderRange.borderRangeMin() != null) {
                    jsonGenerator.writeNumberField(BORDER_RANGE_MIN, borderRange.borderRangeMin());
                }
                if (borderRange.borderRangeMax() != null) {
                    jsonGenerator.writeNumberField(BORDER_RANGE_MAX, borderRange.borderRangeMax());
                }
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private List<NcCracCreationParameters.ConnectedArea> deserializeConnectedAreas(JsonParser jsonParser) throws IOException {
        List<NcCracCreationParameters.ConnectedArea> connectedAreas = new ArrayList<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String area = null;
            List<NcCracCreationParameters.BorderRange> borderRanges = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case AREA:
                        area = jsonParser.nextTextValue();
                        break;
                    case BORDER_RANGES:
                        jsonParser.nextToken();
                        borderRanges = deserializeBorderRanges(jsonParser);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in %s: %s".formatted(CONNECTED_AREAS, jsonParser.currentName()));
                }
            }
            if (area == null || borderRanges == null) {
                throw new OpenRaoException("Incomplete data for connected areas; please provide both a %s and a %s".formatted(AREA, BORDER_RANGES));
            }
            connectedAreas.add(new NcCracCreationParameters.ConnectedArea(area, borderRanges));
        }
        return connectedAreas;
    }

    private List<NcCracCreationParameters.BorderRange> deserializeBorderRanges(JsonParser jsonParser) throws IOException {
        List<NcCracCreationParameters.BorderRange> borderRanges = new ArrayList<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String rangeType = null;
            Double borderRangeMin = null;
            Double borderRangeMax = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case RANGE_TYPE:
                        rangeType = jsonParser.nextTextValue();
                        break;
                    case BORDER_RANGE_MIN:
                        jsonParser.nextToken();
                        borderRangeMin = jsonParser.readValueAs(Double.class);
                        break;
                    case BORDER_RANGE_MAX:
                        jsonParser.nextToken();
                        borderRangeMax = jsonParser.readValueAs(Double.class);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in %s: %s".formatted(BORDER_RANGES, jsonParser.currentName()));
                }
            }
            if (rangeType == null) {
                throw new OpenRaoException("Incomplete data for border range; please provide an %s".formatted(RANGE_TYPE));
            }
            borderRanges.add(new NcCracCreationParameters.BorderRange(rangeType, borderRangeMin, borderRangeMax));
        }
        return borderRanges;
    }

    private static void throwSerializationError(String nonSerializableField, IOException e) {
        throw new OpenRaoException("Could not serialize " + nonSerializableField + " map. Reason: " + e.getMessage());
    }
}
