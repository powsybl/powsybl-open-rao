/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static com.powsybl.openrao.data.crac.api.RaUsageLimits.deserializeRaUsageLimits;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class JsonCracCreationParametersConstants {

    static final String CRAC_FACTORY = "crac-factory";

    static final String DEFAULT_MONITORED_LINE_SIDE = "default-monitored-line-side";
    private static final String MONITOR_LINES_ON_LEFT_SIDE_TEXT = "monitor-lines-on-left-side";
    private static final String MONITOR_LINES_ON_SIDE_ONE_TEXT = "monitor-lines-on-side-one";
    private static final String MONITOR_LINES_ON_RIGHT_SIDE_TEXT = "monitor-lines-on-right-side";
    private static final String MONITOR_LINES_ON_SIDE_TWO_TEXT = "monitor-lines-on-side-two";
    private static final String MONITOR_LINES_ON_BOTH_SIDES_TEXT = "monitor-lines-on-both-sides";
    public static final String RA_USAGE_LIMITS_PER_INSTANT = "ra-usage-limits-per-instant";
    public static final String INSTANT = "instant";
    public static final String MAX_RA = "max-ra";
    public static final String MAX_TSO = "max-tso";
    public static final String MAX_TOPO_PER_TSO = "max-topo-per-tso";
    public static final String MAX_PST_PER_TSO = "max-pst-per-tso";
    public static final String MAX_RA_PER_TSO = "max-ra-per-tso";
    public static final String MAX_ELEMENTARY_ACTIONS_PER_TSO = "max-elementary-actions-per-tso";

    private JsonCracCreationParametersConstants() {
        // should not be instantiated
    }

    static String serializeMonitoredLineSide(CracCreationParameters.MonitoredLineSide monitoredLineSide) {
        return switch (monitoredLineSide) {
            case MONITOR_LINES_ON_SIDE_ONE -> MONITOR_LINES_ON_SIDE_ONE_TEXT;
            case MONITOR_LINES_ON_SIDE_TWO -> MONITOR_LINES_ON_SIDE_TWO_TEXT;
            case MONITOR_LINES_ON_BOTH_SIDES -> MONITOR_LINES_ON_BOTH_SIDES_TEXT;
        };
    }

    static CracCreationParameters.MonitoredLineSide deserializeMonitoredLineSide(String monitoredLineSide) {
        return switch (monitoredLineSide) {
            case MONITOR_LINES_ON_SIDE_ONE_TEXT, MONITOR_LINES_ON_LEFT_SIDE_TEXT ->
                CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE;
            case MONITOR_LINES_ON_SIDE_TWO_TEXT, MONITOR_LINES_ON_RIGHT_SIDE_TEXT ->
                CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_TWO;
            case MONITOR_LINES_ON_BOTH_SIDES_TEXT ->
                CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES;
            default -> throw new OpenRaoException(String.format("Unknown monitored line side: %s", monitoredLineSide));
        };
    }

    static void serializeRaUsageLimits(CracCreationParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(RA_USAGE_LIMITS_PER_INSTANT);
        jsonGenerator.writeStartArray();
        for (Map.Entry<String, RaUsageLimits> entry : parameters.getRaUsageLimitsPerInstant().entrySet()) {
            serializeRaUsageLimitForOneInstant(jsonGenerator, entry);
        }
        jsonGenerator.writeEndArray();
    }

    public static void serializeRaUsageLimitForOneInstant(JsonGenerator jsonGenerator, Map.Entry<String, RaUsageLimits> entry) throws IOException {
        RaUsageLimits raUsageLimits = entry.getValue();
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(INSTANT, entry.getKey());
        jsonGenerator.writeNumberField(MAX_RA, raUsageLimits.getMaxRa());
        jsonGenerator.writeObjectField(MAX_TOPO_PER_TSO, new TreeMap<>(raUsageLimits.getMaxTopoPerTso()));
        jsonGenerator.writeObjectField(MAX_PST_PER_TSO, new TreeMap<>(raUsageLimits.getMaxPstPerTso()));
        jsonGenerator.writeObjectField(MAX_RA_PER_TSO, new TreeMap<>(raUsageLimits.getMaxRaPerTso()));
        jsonGenerator.writeObjectField(MAX_ELEMENTARY_ACTIONS_PER_TSO, new TreeMap<>(raUsageLimits.getMaxElementaryActionsPerTso()));
        jsonGenerator.writeEndObject();
    }

    static void deserializeRaUsageLimitsAndUpdateParameters(JsonParser jsonParser, CracCreationParameters parameters) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Pair<String, RaUsageLimits> pairOfInstantAndItsRaUsageLimits = deserializeRaUsageLimits(jsonParser);
            parameters.addRaUsageLimitsForInstant(pairOfInstantAndItsRaUsageLimits.getLeft(), pairOfInstantAndItsRaUsageLimits.getRight());
        }
    }

}
