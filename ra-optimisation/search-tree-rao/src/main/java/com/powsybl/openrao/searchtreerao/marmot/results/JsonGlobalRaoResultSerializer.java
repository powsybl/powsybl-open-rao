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

import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class JsonGlobalRaoResultSerializer extends JsonSerializer<GlobalRaoResult> {
    private static final String GLOBAL_RAO_RESULT = "GLOBAL_RAO_RESULT";
    private static final String VERSION = "1.0";
    private static final DateTimeFormatter FIELD_NAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_NAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String FILE_NAME_TEMPLATE = "raoResult_%s.json";
    private static final String RESULT_PER_TIMESTAMP = "resultPerTimestamp";
    private static final String COST_RESULTS = "costResults";

    @Override
    public void serialize(GlobalRaoResult globalRaoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        // type and version
        jsonGenerator.writeStringField(JsonSerializationConstants.TYPE, GLOBAL_RAO_RESULT);
        jsonGenerator.writeStringField(JsonSerializationConstants.VERSION, VERSION);
        jsonGenerator.writeStringField(JsonSerializationConstants.INFO, RaoResultJsonConstants.RAO_RESULT_INFO);

        // computation status
        ComputationStatus computationStatus = globalRaoResult.getComputationStatus();
        jsonGenerator.writeStringField(COMPUTATION_STATUS, serializeStatus(computationStatus));

        serializeCostResult(globalRaoResult, jsonGenerator);
        serializeRaoResultPerTimestamp(globalRaoResult, jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    private static String getRaoResultFileName(OffsetDateTime timestamp) {
        return FILE_NAME_TEMPLATE.formatted(timestamp.format(FILE_NAME_DATE_TIME_FORMATTER));
    }

    private static void serializeRaoResultPerTimestamp(GlobalRaoResult globalRaoResult, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(RESULT_PER_TIMESTAMP);
        for (OffsetDateTime timestamp : globalRaoResult.getTimestamps()) {
            jsonGenerator.writeStringField(timestamp.format(FIELD_NAME_DATE_TIME_FORMATTER), getRaoResultFileName(timestamp));
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeCostResult(GlobalRaoResult globalRaoResult, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(COST_RESULTS);
        jsonGenerator.writeObjectFieldStart(PREVENTIVE_INSTANT_ID);
        jsonGenerator.writeNumberField(FUNCTIONAL_COST, roundDouble(globalRaoResult.getGlobalFunctionalCost()));

        jsonGenerator.writeObjectFieldStart(VIRTUAL_COSTS);
        for (String virtualCostName : globalRaoResult.getVirtualCostNames().stream().sorted().toList()) {
            double virtualCostForAGivenName = globalRaoResult.getGlobalVirtualCost(virtualCostName);
            if (!Double.isNaN(virtualCostForAGivenName)) {
                jsonGenerator.writeNumberField(virtualCostName, roundDouble(virtualCostForAGivenName));
            }
        }
        jsonGenerator.writeEndObject();

        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    private static BigDecimal roundDouble(double doubleValue) {
        return new BigDecimal(doubleValue).setScale(2, RoundingMode.HALF_UP);
    }
}
