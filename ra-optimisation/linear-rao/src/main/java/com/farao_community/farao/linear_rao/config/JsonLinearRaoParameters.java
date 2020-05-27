/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.config;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonLinearRaoParameters implements JsonRaoParameters.ExtensionSerializer<LinearRaoParameters> {

    @Override
    public void serialize(LinearRaoParameters linearRaoParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("objective-function", linearRaoParameters.getObjectiveFunction());
        jsonGenerator.writeBooleanField("security-analysis-without-rao", linearRaoParameters.isSecurityAnalysisWithoutRao());
        jsonGenerator.writeNumberField("sensitivity-fallback-overcost", linearRaoParameters.getFallbackOvercost());
        jsonGenerator.writeEndObject();
    }

    @Override
    public LinearRaoParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LinearRaoParameters linearRaoParameters = new LinearRaoParameters();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "objective-function":
                    linearRaoParameters.setObjectiveFunction(stringToObjectiveFunction(jsonParser.nextTextValue()));
                    break;
                case "security-analysis-without-rao":
                    jsonParser.nextToken();
                    linearRaoParameters.setSecurityAnalysisWithoutRao(jsonParser.getBooleanValue());
                    break;
                case "sensitivity-fallback-overcost":
                    jsonParser.nextToken();
                    linearRaoParameters.setFallbackOvercost(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return linearRaoParameters;
    }

    @Override
    public String getExtensionName() {
        return "LinearRaoParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LinearRaoParameters> getExtensionClass() {
        return LinearRaoParameters.class;
    }

    private LinearRaoParameters.ObjectiveFunction stringToObjectiveFunction(String string) {
        try {
            return LinearRaoParameters.ObjectiveFunction.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown objective function value : %s", string));
        }
    }
}
