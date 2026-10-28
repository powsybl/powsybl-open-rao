/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonCostResult implements RaoResultJsonUtils.ExtensionSerializer<CostResult> {
    @Override
    public void serialize(CostResult costResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        Crac crac = (Crac) serializerProvider.getAttribute("crac");
        if (crac == null) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("CostResult cannot be serialized because the serializerProvider does not contain a 'crac' attribute.");
            return;
        }

        List<Instant> instants = crac.getSortedInstants();

        jsonGenerator.writeStartObject();
        if (hasResultsForInstant(costResult, null)) {
            serializeForInstant(jsonGenerator, costResult, null);
        }
        for (Instant instant : instants) {
            if (!instant.isOutage()
                && hasResultsForInstant(costResult, instant)) {
                serializeForInstant(jsonGenerator, costResult, instant);
            }
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public CostResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        if (crac == null) {
            throw new OpenRaoException("CostResult cannot be deserialized because the deserializationContext does not contain a 'crac' attribute.");
        }
        CostResult.CostResultBuilder costResultBuilder = CostResult.build();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            jsonParser.nextToken();
            if ("initial".equals(jsonParser.currentName())) {
                deserializeElementaryCostResult(jsonParser, null, costResultBuilder);
            } else {
                Instant instant = crac.getInstant(jsonParser.currentName());
                deserializeElementaryCostResult(jsonParser, instant, costResultBuilder);
            }
        }
        return costResultBuilder.build();
    }

    // serialization

    private void serializeForInstant(JsonGenerator jsonGenerator, CostResult costResult, Instant instant) throws IOException {
        jsonGenerator.writeObjectFieldStart(instant == null ? "initial" : instant.getId());
        jsonGenerator.writeNumberField("functionalCost", costResult.getFunctionalCost(instant));
        jsonGenerator.writeObjectFieldStart("virtualCost");
        for (String virtualCostName : getVirtualCostNamesForInstant(costResult, instant)) {
            jsonGenerator.writeNumberField(virtualCostName, costResult.getVirtualCost(instant, virtualCostName));
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    private static boolean hasResultsForInstant(CostResult costResult, Instant instant) {
        return !Double.isNaN(costResult.getFunctionalCost(instant));
    }

    private static List<String> getVirtualCostNamesForInstant(CostResult costResult, Instant instant) {
        return costResult.getVirtualCostNames()
            .stream()
            .filter(virtualCostName -> !Double.isNaN(costResult.getVirtualCost(instant, virtualCostName)))
            .sorted()
            .toList();
    }

    // deserialization

    private void deserializeElementaryCostResult(JsonParser jsonParser, Instant instant, CostResult.CostResultBuilder costResultBuilder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            jsonParser.nextToken();
            if ("functionalCost".equals(jsonParser.currentName())) {
                costResultBuilder.addFunctionalCostResult(instant, jsonParser.getDoubleValue());
            } else if ("virtualCost".equals(jsonParser.currentName())) {
                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    jsonParser.nextToken();
                    costResultBuilder.addVirtualCostResult(instant, jsonParser.currentName(), jsonParser.getDoubleValue());
                }
            } else {
                throw new OpenRaoException("Unexpected field in '%s': '%s'.".formatted(jsonParser.currentName(), getExtensionName()));
            }
        }
    }

    @Override
    public String getExtensionName() {
        return "cost-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super CostResult> getExtensionClass() {
        return CostResult.class;
    }
}
