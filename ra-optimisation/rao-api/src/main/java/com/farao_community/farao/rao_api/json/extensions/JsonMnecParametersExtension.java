/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.MnecParametersExtension;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonMnecParametersExtension implements JsonRaoParameters.ExtensionSerializer<MnecParametersExtension> {

    @Override
    public void serialize(MnecParametersExtension mnecParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(ACCEPTABLE_MARGIN_DECREASE, mnecParameters.getAcceptableMarginDecrease());
        jsonGenerator.writeNumberField(VIOLATION_COST, mnecParameters.getViolationCost());
        jsonGenerator.writeNumberField(CONSTRAINT_ADJUSTMENT_COEFFICIENT, mnecParameters.getConstraintAdjustmentCoefficient());
        jsonGenerator.writeEndObject();
    }

    @Override
    public MnecParametersExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new MnecParametersExtension());
    }

    @Override
    public MnecParametersExtension deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, MnecParametersExtension parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case ACCEPTABLE_MARGIN_DECREASE:
                    jsonParser.nextToken();
                    parameters.setAcceptableMarginDecrease(jsonParser.getDoubleValue());
                    break;
                case VIOLATION_COST:
                    jsonParser.nextToken();
                    parameters.setViolationCost(jsonParser.getDoubleValue());
                    break;
                case CONSTRAINT_ADJUSTMENT_COEFFICIENT:
                    jsonParser.nextToken();
                    parameters.setConstraintAdjustmentCoefficient(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize mnec parameters: unexpected field in %s (%s)", MNEC_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return MNEC_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super MnecParametersExtension> getExtensionClass() {
        return MnecParametersExtension.class;
    }

}
