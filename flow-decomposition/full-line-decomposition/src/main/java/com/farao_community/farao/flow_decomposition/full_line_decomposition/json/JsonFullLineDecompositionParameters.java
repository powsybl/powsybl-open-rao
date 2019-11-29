/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters;
import com.farao_community.farao.flow_decomposition.json.JsonFlowDecompositionParameters.ExtensionSerializer;

import java.io.IOException;

/**
 * JSON serialization plugin for full line decomposition parameters
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(ExtensionSerializer.class)
public class JsonFullLineDecompositionParameters implements ExtensionSerializer<FullLineDecompositionParameters> {
    @Override
    public void serialize(FullLineDecompositionParameters fullLineDecompositionParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("injectionStrategy", fullLineDecompositionParameters.getInjectionStrategy().name());
        jsonGenerator.writeNumberField("pexMatrixTolerance", fullLineDecompositionParameters.getPexMatrixTolerance());
        jsonGenerator.writeNumberField("threadsNumber", fullLineDecompositionParameters.getThreadsNumber());
        jsonGenerator.writeStringField("pstStrategy", fullLineDecompositionParameters.getPstStrategy().name());

        jsonGenerator.writeEndObject();
    }

    @Override
    public FullLineDecompositionParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        FullLineDecompositionParameters parameters = new FullLineDecompositionParameters();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "injectionStrategy":
                    parser.nextToken();
                    parameters.setInjectionStrategy(parser.readValueAs(FullLineDecompositionParameters.InjectionStrategy.class));
                    break;

                case "pexMatrixTolerance":
                    parser.nextToken();
                    parameters.setPexMatrixTolerance(parser.getNumberValue().doubleValue());
                    break;

                case "threadsNumber":
                    parser.nextToken();
                    parameters.setThreadsNumber(parser.getNumberValue().intValue());
                    break;

                case "pstStrategy":
                    parser.nextToken();
                    parameters.setPstStrategy(parser.readValueAs(FullLineDecompositionParameters.PstStrategy.class));
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + parser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "FullLineDecompositionParameters";
    }

    @Override
    public String getCategoryName() {
        return "flow-decomposition-parameters";
    }

    @Override
    public Class<? super FullLineDecompositionParameters> getExtensionClass() {
        return FullLineDecompositionParameters.class;
    }
}
