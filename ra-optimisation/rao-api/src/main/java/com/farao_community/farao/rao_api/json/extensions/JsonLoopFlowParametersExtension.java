/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonLoopFlowParametersExtension implements JsonRaoParameters.ExtensionSerializer<LoopFlowParametersExtension> {

    @Override
    public void serialize(LoopFlowParametersExtension loopFlowParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(ACCEPTABLE_INCREASE, loopFlowParameters.getAcceptableIncrease());
        jsonGenerator.writeObjectField(APPROXIMATION, loopFlowParameters.getApproximation());
        jsonGenerator.writeNumberField(CONSTRAINT_ADJUSTMENT_COEFFICIENT, loopFlowParameters.getConstraintAdjustmentCoefficient());
        jsonGenerator.writeNumberField(VIOLATION_COST, loopFlowParameters.getViolationCost());
        jsonGenerator.writeFieldName(COUNTRIES);
        jsonGenerator.writeStartArray();
        loopFlowParameters.getCountries().stream().map(Enum::toString).sorted().forEach(s -> {
            try {
                jsonGenerator.writeString(s);
            } catch (IOException e) {
                throw new FaraoException("error while serializing loopflow countries", e);
            }
        });
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    @Override
    public LoopFlowParametersExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, LoopFlowParametersExtension.loadDefault());
    }

    @Override
    public LoopFlowParametersExtension deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, LoopFlowParametersExtension parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case ACCEPTABLE_INCREASE:
                    jsonParser.nextToken();
                    parameters.setAcceptableIncrease(jsonParser.getDoubleValue());
                    break;
                case APPROXIMATION:
                    parameters.setApproximation(stringToApproximation(jsonParser.nextTextValue()));
                    break;
                case CONSTRAINT_ADJUSTMENT_COEFFICIENT:
                    jsonParser.nextToken();
                    parameters.setConstraintAdjustmentCoefficient(jsonParser.getDoubleValue());
                    break;
                case VIOLATION_COST:
                    jsonParser.nextToken();
                    parameters.setViolationCost(jsonParser.getDoubleValue());
                    break;
                case COUNTRIES:
                    jsonParser.nextToken();
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode arrayNode = objectMapper.readTree(jsonParser);
                    List<String> countryStrings = objectMapper.readValue(arrayNode.traverse(), new TypeReference<ArrayList<String>>() { });
                    parameters.setCountries(countryStrings);
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize loop flow parameters: unexpected field in %s (%s)", LOOP_FLOW_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return LOOP_FLOW_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LoopFlowParametersExtension> getExtensionClass() {
        return LoopFlowParametersExtension.class;
    }

    private static LoopFlowParametersExtension.Approximation stringToApproximation(String string) {
        try {
            return LoopFlowParametersExtension.Approximation.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown approximation value: %s", string));
        }
    }
}
