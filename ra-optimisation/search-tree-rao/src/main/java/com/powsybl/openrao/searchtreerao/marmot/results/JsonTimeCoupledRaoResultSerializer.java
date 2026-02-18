/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class JsonTimeCoupledRaoResultSerializer extends JsonSerializer<TimeCoupledRaoResult> {
    private static final String GLOBAL_RAO_SUMMARY = "INTER_TEMPORAL_RAO_SUMMARY";
    private static final String VERSION = "1.0";
    private static final String RESULT_PER_TIMESTAMP = "resultPerTimestamp";
    private static final String COST_RESULTS = "costResults";

    private static final DateTimeFormatter FIELD_NAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String individualRaoResultFilenameTemplate;
    private final List<Instant> instants;

    public JsonTimeCoupledRaoResultSerializer(String individualRaoResultFilenameTemplate, List<Instant> instants) {
        this.individualRaoResultFilenameTemplate = individualRaoResultFilenameTemplate;
        this.instants = instants;
    }

    @Override
    public void serialize(TimeCoupledRaoResult timeCoupledRaoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        // type and version
        jsonGenerator.writeStringField(JsonSerializationConstants.TYPE, GLOBAL_RAO_SUMMARY);
        jsonGenerator.writeStringField(JsonSerializationConstants.VERSION, VERSION);
        jsonGenerator.writeStringField(JsonSerializationConstants.INFO, RaoResultJsonConstants.RAO_RESULT_INFO);

        // computation status
        ComputationStatus computationStatus = timeCoupledRaoResult.getComputationStatus();
        jsonGenerator.writeStringField(COMPUTATION_STATUS, serializeStatus(computationStatus));

        serializeCostResults(timeCoupledRaoResult, jsonGenerator);
        serializeRaoResultPerTimestamp(timeCoupledRaoResult, jsonGenerator, individualRaoResultFilenameTemplate);

        jsonGenerator.writeEndObject();
    }

    private void serializeCostResults(TimeCoupledRaoResult timeCoupledRaoResult, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(COST_RESULTS);
        serializeCostsAfterGivenStep(timeCoupledRaoResult, jsonGenerator, null); // initial situation
        instants.forEach(instant -> {
            try {
                serializeCostsAfterGivenStep(timeCoupledRaoResult, jsonGenerator, instant);
            } catch (IOException e) {
                throw new OpenRaoException(e);
            }
        });
        jsonGenerator.writeEndObject();
    }

    private void serializeCostsAfterGivenStep(TimeCoupledRaoResult timeCoupledRaoResult, JsonGenerator jsonGenerator, Instant instant) throws IOException {
        jsonGenerator.writeObjectFieldStart(instant == null ? INITIAL_INSTANT_ID : instant.getName());
        jsonGenerator.writeNumberField(FUNCTIONAL_COST, roundDouble(timeCoupledRaoResult.getGlobalFunctionalCost(instant)));
        jsonGenerator.writeObjectFieldStart(VIRTUAL_COSTS);
        for (String virtualCostName : timeCoupledRaoResult.getVirtualCostNames().stream().sorted().toList()) {
            double virtualCostForAGivenName = timeCoupledRaoResult.getGlobalVirtualCost(instant, virtualCostName);
            if (!Double.isNaN(virtualCostForAGivenName)) {
                jsonGenerator.writeNumberField(virtualCostName, roundDouble(virtualCostForAGivenName));
            }
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    private static BigDecimal roundDouble(double doubleValue) {
        return BigDecimal.valueOf(doubleValue).setScale(2, RoundingMode.HALF_UP);
    }

    private static void serializeRaoResultPerTimestamp(TimeCoupledRaoResult timeCoupledRaoResult, JsonGenerator jsonGenerator, String individualRaoResultFilenameTemplate) throws IOException {
        jsonGenerator.writeObjectFieldStart(RESULT_PER_TIMESTAMP);
        for (OffsetDateTime timestamp : timeCoupledRaoResult.getTimestamps()) {
            jsonGenerator.writeStringField(timestamp.format(FIELD_NAME_DATE_TIME_FORMATTER), timestamp.format(DateTimeFormatter.ofPattern(individualRaoResultFilenameTemplate)));
        }
        jsonGenerator.writeEndObject();
    }
}
