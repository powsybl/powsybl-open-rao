/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.api.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class JsonCracCreationParametersConstants {

    static final String CRAC_FACTORY = "crac-factory";

    static final String DEFAULT_MONITORED_LINE_SIDE = "default-monitored-line-side";
    private static final String MONITOR_LINES_ON_LEFT_SIDE_TEXT = "monitor-lines-on-left-side";
    private static final String MONITOR_LINES_ON_RIGHT_SIDE_TEXT = "monitor-lines-on-right-side";
    private static final String MONITOR_LINES_ON_BOTH_SIDES_TEXT = "monitor-lines-on-both-sides";
    public static final String RA_USAGE_LIMITS_PER_INSTANT = "ra-usage-limits-per-instant";
    public static final String INSTANT = "instant";
    public static final String MAX_RA = "max-ra";
    public static final String MAX_TSO = "max-tso";
    public static final String MAX_TOPO_PER_TSO = "max-topo-per-tso";
    public static final String MAX_PST_PER_TSO = "max-pst-per-tso";
    public static final String MAX_RA_PER_TSO = "max-ra-per-tso";

    private JsonCracCreationParametersConstants() {
        // should not be instantiated
    }

    static String serializeMonitoredLineSide(CracCreationParameters.MonitoredLineSide monitoredLineSide) {
        switch (monitoredLineSide) {
            case MONITOR_LINES_ON_LEFT_SIDE:
                return MONITOR_LINES_ON_LEFT_SIDE_TEXT;
            case MONITOR_LINES_ON_RIGHT_SIDE:
                return MONITOR_LINES_ON_RIGHT_SIDE_TEXT;
            case MONITOR_LINES_ON_BOTH_SIDES:
                return MONITOR_LINES_ON_BOTH_SIDES_TEXT;
            default:
                throw new OpenRaoException(String.format("Unknown monitored line side: %s", monitoredLineSide));
        }
    }

    static CracCreationParameters.MonitoredLineSide deserializeMonitoredLineSide(String monitoredLineSide) {
        switch (monitoredLineSide) {
            case MONITOR_LINES_ON_LEFT_SIDE_TEXT:
                return CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_LEFT_SIDE;
            case MONITOR_LINES_ON_RIGHT_SIDE_TEXT:
                return CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_RIGHT_SIDE;
            case MONITOR_LINES_ON_BOTH_SIDES_TEXT:
                return CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES;
            default:
                throw new OpenRaoException(String.format("Unknown monitored line side: %s", monitoredLineSide));
        }
    }

    static void serializeRaUsageLimits(CracCreationParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(RA_USAGE_LIMITS_PER_INSTANT);
        jsonGenerator.writeStartArray();
        for (Map.Entry<String, RaUsageLimits> entry : parameters.getRaUsageLimitsPerInstant().entrySet()) {
            RaUsageLimits raUsageLimits = entry.getValue();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, entry.getKey());
            jsonGenerator.writeNumberField(MAX_RA, raUsageLimits.getMaxRa());
            jsonGenerator.writeNumberField(MAX_TSO, raUsageLimits.getMaxTso());
            jsonGenerator.writeObjectField(MAX_TOPO_PER_TSO, new TreeMap<>(raUsageLimits.getMaxTopoPerTso()));
            jsonGenerator.writeObjectField(MAX_PST_PER_TSO, new TreeMap<>(raUsageLimits.getMaxPstPerTso()));
            jsonGenerator.writeObjectField(MAX_RA_PER_TSO, new TreeMap<>(raUsageLimits.getMaxRaPerTso()));
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    static void deserializeRaUsageLimitsAndUpdateParameters(JsonParser jsonParser, CracCreationParameters parameters, ReportNode reportNode) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Pair<String, RaUsageLimits> pairOfInstantAndItsRaUsageLimits = deserializeRaUsageLimits(jsonParser, reportNode);
            parameters.addRaUsageLimitsForInstant(pairOfInstantAndItsRaUsageLimits.getLeft(), pairOfInstantAndItsRaUsageLimits.getRight());
        }
    }

    private static Map<String, Integer> readStringToPositiveIntMap(JsonParser jsonParser) throws IOException {
        HashMap<String, Integer> map = jsonParser.readValueAs(HashMap.class);
        // Check types
        map.forEach((Object o, Object o2) -> {
            if (!(o instanceof String) || !(o2 instanceof Integer)) {
                throw new OpenRaoException("Unexpected key or value type in a Map<String, Integer> parameter!");
            }
            if ((int) o2 < 0) {
                throw new OpenRaoException("Unexpected negative integer!");
            }
        });
        return map;
    }

    private static Pair<String, RaUsageLimits> deserializeRaUsageLimits(JsonParser jsonParser, ReportNode reportNode) throws IOException {
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        String instant = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case INSTANT:
                    jsonParser.nextToken();
                    instant = jsonParser.getValueAsString();
                    break;
                case MAX_RA:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxRa(jsonParser.getIntValue(), reportNode);
                    break;
                case MAX_TSO:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxTso(jsonParser.getIntValue(), reportNode);
                    break;
                case MAX_TOPO_PER_TSO:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxTopoPerTso(readStringToPositiveIntMap(jsonParser), reportNode);
                    break;
                case MAX_PST_PER_TSO:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxPstPerTso(readStringToPositiveIntMap(jsonParser), reportNode);
                    break;
                case MAX_RA_PER_TSO:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxRaPerTso(readStringToPositiveIntMap(jsonParser), reportNode);
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize ra-usage-limits-per-instant parameters: unexpected field in %s (%s)", RA_USAGE_LIMITS_PER_INSTANT, jsonParser.getCurrentName()));
            }
        }
        return Pair.of(instant, raUsageLimits);
    }
}
