/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.systematic_sensitivity;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.json.JsonSensitivityComputationParameters;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonSystematicSensitivityComputationParameters implements JsonRaoParameters.ExtensionSerializer<SystematicSensitivityComputationParameters> {

    @Override
    public void serialize(SystematicSensitivityComputationParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("sensitivity-parameters");
        JsonSensitivityComputationParameters.serialize(parameters.getDefaultParameters(), jsonGenerator, serializerProvider);
        if (parameters.getFallbackParameters() != null) {
            jsonGenerator.writeFieldName("fallback-sensitivity-parameters");
            JsonSensitivityComputationParameters.serialize(parameters.getFallbackParameters(), jsonGenerator, serializerProvider);
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public SystematicSensitivityComputationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        SystematicSensitivityComputationParameters parameters = new SystematicSensitivityComputationParameters();

        while (!parser.nextToken().isStructEnd()) {
            switch (parser.getCurrentName()) {
                case "sensitivity-parameters":
                    parser.nextToken();
                    JsonSensitivityComputationParameters.deserialize(parser, deserializationContext, parameters.getDefaultParameters());
                    break;
                case "fallback-sensitivity-parameters":
                    parser.nextToken();
                    if (parameters.getFallbackParameters() == null) {
                        parameters.setFallbackParameters(new SensitivityComputationParameters());
                    }
                    JsonSensitivityComputationParameters.deserialize(parser, deserializationContext, parameters.getFallbackParameters());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + parser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "SystematicSensitivityComputationParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super SystematicSensitivityComputationParameters> getExtensionClass() {
        return SystematicSensitivityComputationParameters.class;
    }
}
