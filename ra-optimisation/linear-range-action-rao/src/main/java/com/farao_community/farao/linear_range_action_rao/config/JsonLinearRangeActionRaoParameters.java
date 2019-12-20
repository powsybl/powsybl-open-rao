/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_range_action_rao.config;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.sensitivity.json.JsonSensitivityComputationParameters;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonLinearRangeActionRaoParameters implements JsonRaoParameters.ExtensionSerializer<LinearRangeActionRaoParameters> {

    @Override
    public void serialize(LinearRangeActionRaoParameters linearRangeActionRaoParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeFieldName("sensitivity-parameters");
        JsonSensitivityComputationParameters.serialize(linearRangeActionRaoParameters.getSensitivityComputationParameters(), jsonGenerator, serializerProvider);

        jsonGenerator.writeEndObject();
    }

    @Override
    public LinearRangeActionRaoParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LinearRangeActionRaoParameters linearRangeActionRaoParameters = new LinearRangeActionRaoParameters();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "sensitivity-parameters":
                    jsonParser.nextToken();
                    JsonSensitivityComputationParameters.deserialize(jsonParser, deserializationContext, linearRangeActionRaoParameters.getSensitivityComputationParameters());
                    break;

                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return linearRangeActionRaoParameters;
    }

    @Override
    public String getExtensionName() {
        return "LinearRangeActionRaoParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LinearRangeActionRaoParameters> getExtensionClass() {
        return LinearRangeActionRaoParameters.class;
    }
}
