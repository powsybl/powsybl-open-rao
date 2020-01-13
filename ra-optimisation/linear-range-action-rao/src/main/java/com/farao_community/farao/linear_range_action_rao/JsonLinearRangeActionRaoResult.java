/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_range_action_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(JsonRaoComputationResult.ExtensionSerializer.class)
public class JsonLinearRangeActionRaoResult implements  JsonRaoComputationResult.ExtensionSerializer<LinearRangeActionRaoResult> {

    @Override
    public void serialize(LinearRangeActionRaoResult linearRangeActionRaoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("security-status", linearRangeActionRaoResult.getSecurityStatus());
        jsonGenerator.writeObjectField("minimum-margin", linearRangeActionRaoResult.getMinMargin());
        jsonGenerator.writeEndObject();

    }

    @Override
    public LinearRangeActionRaoResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LinearRangeActionRaoResult ret = new LinearRangeActionRaoResult();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "security-status":
                    jsonParser.nextToken();
                    ret.setSecurityStatus(Enum.valueOf(LinearRangeActionRaoResult.SecurityStatus.class, jsonParser.getValueAsString()));
                    break;
                case "minimum-margin":
                    jsonParser.nextToken();
                    ret.setMinMargin(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        return ret;
    }

    @Override
    public String getExtensionName() {
        return "LinearRangeActionRaoResult";
    }

    @Override
    public String getCategoryName() {
        return "rao-computation-result";
    }

    @Override
    public Class<? super LinearRangeActionRaoResult> getExtensionClass() {
        return LinearRangeActionRaoResult.class;
    }

}
