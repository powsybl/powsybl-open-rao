/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cim.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class JsonVoltageCnecsCreationParameters {

    public static final String MONITORED_STATES_AND_THRESHOLDS = "monitored-states-and-thresholds";
    public static final String MONITORED_NETWORK_ELEMENTS = "monitored-network-elements";
    public static final String INSTANT = "instant";
    public static final String CONTINGENCY_NAMES = "contingency-names";
    public static final String THRESHOLDS_PER_NOMINAL_V = "thresholds-per-nominal-v";
    public static final String NOMINAL_V = "nominalV";
    public static final String UNIT = "unit";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String KILOVOLT = "kilovolt";

    private JsonVoltageCnecsCreationParameters() {
        // should not be used
    }

    static VoltageCnecsCreationParameters deserialize(JsonParser jsonParser) throws IOException {
        Map<String, VoltageMonitoredContingenciesAndThresholds> voltageMonitoringStatesAndThresholds = new TreeMap<>();
        Set<String> monitoredNetworkElements = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {
                case MONITORED_STATES_AND_THRESHOLDS:
                    jsonParser.nextToken();
                    try {
                        voltageMonitoringStatesAndThresholds = deserializeStatesAndThresholds(jsonParser);
                    } catch (NoSuchFieldException e) {
                        throw new OpenRaoException(String.format("Could not deserialize %s", MONITORED_STATES_AND_THRESHOLDS), e);
                    }
                    break;
                case MONITORED_NETWORK_ELEMENTS:
                    jsonParser.nextToken();
                    monitoredNetworkElements = jsonParser.readValueAs(new TypeReference<HashSet<String>>() {
                    });
                    break;
                default:
                    throw new OpenRaoException("Unexpected field in voltage-cnecs-creation-parameters: " + jsonParser.getCurrentName());
            }
        }
        return new VoltageCnecsCreationParameters(voltageMonitoringStatesAndThresholds, monitoredNetworkElements);
    }

    private static Map<String, VoltageMonitoredContingenciesAndThresholds> deserializeStatesAndThresholds(JsonParser jsonParser) throws IOException, NoSuchFieldException {
        Map<String, VoltageMonitoredContingenciesAndThresholds> statesAndThresholds = new HashMap<>();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String instantId = null;
            Set<String> contingencyNames = null;
            Map<Double, VoltageThreshold> thresholdPerNominalV = new TreeMap<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        instantId = jsonParser.nextTextValue();
                        break;
                    case CONTINGENCY_NAMES:
                        jsonParser.nextToken();
                        contingencyNames = jsonParser.readValueAs(new TypeReference<HashSet<String>>() {
                        });
                        break;
                    case THRESHOLDS_PER_NOMINAL_V:
                        jsonParser.nextToken();
                        thresholdPerNominalV = deserializeThresholdsPerNominalV(jsonParser);
                        break;
                    default:
                        throw new NoSuchFieldException(String.format("Unexpected field in %s: ", MONITORED_STATES_AND_THRESHOLDS) + jsonParser.getCurrentName());
                }
            }
            Objects.requireNonNull(instantId);
            if (instantId.equals(InstantKind.PREVENTIVE.toString().toLowerCase()) && !Objects.isNull(contingencyNames) && !contingencyNames.isEmpty()) {
                throw new OpenRaoException("When monitoring the preventive instant, no contingency can be defined.");
            }
            if (statesAndThresholds.containsKey(instantId)) {
                throw new OpenRaoException(String.format("A threshold is already defined for instant %s.", instantId));
            } else {
                statesAndThresholds.put(instantId, new VoltageMonitoredContingenciesAndThresholds(contingencyNames, thresholdPerNominalV));
            }
        }
        return statesAndThresholds;
    }

    private static Map<Double, VoltageThreshold> deserializeThresholdsPerNominalV(JsonParser jsonParser) throws IOException, NoSuchFieldException {
        Map<Double, VoltageThreshold> map = new TreeMap<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Double nominalV = null;
            Unit unit = null;
            Double min = null;
            Double max = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case NOMINAL_V:
                        jsonParser.nextToken();
                        nominalV = jsonParser.getValueAsDouble();
                        break;
                    case UNIT:
                        unit = stringToUnit(jsonParser.nextTextValue());
                        break;
                    case MIN:
                        jsonParser.nextToken();
                        min = jsonParser.getValueAsDouble();
                        break;
                    case MAX:
                        jsonParser.nextToken();
                        max = jsonParser.getValueAsDouble();
                        break;
                    default:
                        throw new NoSuchFieldException(String.format("Unexpected field in %s: ", THRESHOLDS_PER_NOMINAL_V) + jsonParser.getCurrentName());
                }
            }
            if (nominalV == null) {
                throw new OpenRaoException(String.format("Field %s for %s should be defined.", NOMINAL_V, THRESHOLDS_PER_NOMINAL_V));
            } else if (map.containsKey(nominalV)) {
                throw new OpenRaoException(String.format(Locale.ENGLISH, "Multiple thresholds for same nominalV (%.1f) defined", nominalV));
            } else {
                Objects.requireNonNull(unit);
                map.put(nominalV, new VoltageThreshold(unit, min, max));
            }
        }
        return map;
    }

    static void serialize(VoltageCnecsCreationParameters voltageCnecsCreationParameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        serializeMonitoredStatesAndThresholds(voltageCnecsCreationParameters.getMonitoredStatesAndThresholds(), jsonGenerator);
        serializeMonitoredNetworkElements(voltageCnecsCreationParameters.getMonitoredNetworkElements(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private static void serializeMonitoredStatesAndThresholds(Map<String, VoltageMonitoredContingenciesAndThresholds> monitoredStatesAndThresholds, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeArrayFieldStart(MONITORED_STATES_AND_THRESHOLDS);
        for (Map.Entry<String, VoltageMonitoredContingenciesAndThresholds> entry : monitoredStatesAndThresholds.entrySet()) {
            jsonGenerator.writeStartObject();

            // Instant
            jsonGenerator.writeStringField(INSTANT, entry.getKey());

            VoltageMonitoredContingenciesAndThresholds data = entry.getValue();

            // Thresholds
            jsonGenerator.writeArrayFieldStart(THRESHOLDS_PER_NOMINAL_V);
            for (Map.Entry<Double, VoltageThreshold> thresholdEntry : data.getThresholdPerNominalV().entrySet()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField(NOMINAL_V, thresholdEntry.getKey());
                VoltageThreshold thresh = thresholdEntry.getValue();
                jsonGenerator.writeStringField(UNIT, unitToString(thresh.getUnit()));
                if (thresh.getMin() != null) {
                    jsonGenerator.writeNumberField(MIN, thresh.getMin());
                }
                if (thresh.getMax() != null) {
                    jsonGenerator.writeNumberField(MAX, thresh.getMax());
                }
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();

            // Contingency names
            writeStringArray(CONTINGENCY_NAMES, data.getContingencyNames(), jsonGenerator);

            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeMonitoredNetworkElements(Set<String> monitoredNetworkElements, JsonGenerator jsonGenerator) throws IOException {
        writeStringArray(MONITORED_NETWORK_ELEMENTS, monitoredNetworkElements, jsonGenerator);
    }

    private static void writeStringArray(String fieldName, Set<String> array, JsonGenerator jsonGenerator) throws IOException {
        if (array == null || array.isEmpty()) {
            return;
        }
        List<String> sortedArray = array.stream().sorted().toList();
        jsonGenerator.writeArrayFieldStart(fieldName);
        for (String str : sortedArray) {
            jsonGenerator.writeString(str);
        }
        jsonGenerator.writeEndArray();
    }

    private static Unit stringToUnit(String string) {
        if (string.equalsIgnoreCase(KILOVOLT)) {
            return Unit.KILOVOLT;
        } else {
            throw new OpenRaoException(String.format("Unhandled unit in voltage monitoring: %s", string));
        }
    }

    private static String unitToString(Unit unit) {
        if (unit.equals(Unit.KILOVOLT)) {
            return KILOVOLT;
        } else {
            throw new OpenRaoException(String.format("Unhandled unit in voltage monitoring: %s", unit));
        }
    }
}
