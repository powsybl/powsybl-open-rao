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
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.json.JsonSensitivityComputationParameters;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonLinearRaoParameters implements JsonRaoParameters.ExtensionSerializer<LinearRaoParameters> {

    @Override
    public void serialize(LinearRaoParameters linearRaoParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeNumberField("max-number-of-iterations", linearRaoParameters.getMaxIterations());
        jsonGenerator.writeBooleanField("security-analysis-without-rao", linearRaoParameters.isSecurityAnalysisWithoutRao());
        jsonGenerator.writeNumberField("pst-sensitivity-threshold", linearRaoParameters.getPstSensitivityThreshold());
        jsonGenerator.writeNumberField("pst-penalty-cost", linearRaoParameters.getPstPenaltyCost());

        jsonGenerator.writeFieldName("sensitivity-parameters");
        JsonSensitivityComputationParameters.serialize(linearRaoParameters.getSensitivityComputationParameters(), jsonGenerator, serializerProvider);

        if (linearRaoParameters.getFallbackSensiParameters() != null) {
            jsonGenerator.writeFieldName("fallback-sensitivity-parameters");
            JsonSensitivityComputationParameters.serialize(linearRaoParameters.getFallbackSensiParameters(), jsonGenerator, serializerProvider);
        }

        jsonGenerator.writeEndObject();
    }

    @Override
    public LinearRaoParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LinearRaoParameters linearRaoParameters = new LinearRaoParameters();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "max-number-of-iterations":
                    jsonParser.nextToken();
                    linearRaoParameters.setMaxIterations(jsonParser.getIntValue());
                    break;
                case "security-analysis-without-rao":
                    jsonParser.nextToken();
                    linearRaoParameters.setSecurityAnalysisWithoutRao(jsonParser.getBooleanValue());
                    break;
                case "pst-sensitivity-threshold":
                    jsonParser.nextToken();
                    linearRaoParameters.setPstSensitivityThreshold(jsonParser.getDoubleValue());
                    break;
                case "pst-penalty-cost":
                    jsonParser.nextToken();
                    linearRaoParameters.setPstPenaltyCost(jsonParser.getDoubleValue());
                    break;
                case "sensitivity-parameters":
                    jsonParser.nextToken();
                    JsonSensitivityComputationParameters.deserialize(jsonParser, deserializationContext, linearRaoParameters.getSensitivityComputationParameters());
                    break;
                case "fallback-sensitivity-parameters":
                    jsonParser.nextToken();
                    if (linearRaoParameters.getFallbackSensiParameters() == null) {
                        linearRaoParameters.setFallbackSensiParameters(new SensitivityComputationParameters());
                    }
                    JsonSensitivityComputationParameters.deserialize(jsonParser, deserializationContext, linearRaoParameters.getFallbackSensiParameters());
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
}
