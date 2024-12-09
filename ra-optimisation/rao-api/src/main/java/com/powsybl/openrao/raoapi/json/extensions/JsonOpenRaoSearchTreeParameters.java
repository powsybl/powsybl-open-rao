/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.*;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonOpenRaoSearchTreeParameters implements JsonRaoParameters.ExtensionSerializer<OpenRaoSearchTreeParameters> {
    @Override
    public void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        JsonObjectiveFunctionParameters.serialize(parameters, jsonGenerator);
        JsonRangeActionsOptimizationParameters.serialize(parameters, jsonGenerator);
        JsonTopoOptimizationParameters.serialize(parameters, jsonGenerator);
        JsonSecondPreventiveRaoParameters.serialize(parameters, jsonGenerator);
        JsonLoadFlowAndSensitivityComputationParameters.serialize(parameters, jsonGenerator, serializerProvider);
        JsonMultiThreadingParameters.serialize(parameters, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public OpenRaoSearchTreeParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new OpenRaoSearchTreeParameters());
    }

    @Override
    public OpenRaoSearchTreeParameters deserializeAndUpdate(JsonParser parser, DeserializationContext deserializationContext, OpenRaoSearchTreeParameters parameters) throws IOException {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case OBJECTIVE_FUNCTION:
                    parser.nextToken();
                    JsonObjectiveFunctionParameters.deserialize(parser, parameters);
                    break;
                case RANGE_ACTIONS_OPTIMIZATION:
                    parser.nextToken();
                    JsonRangeActionsOptimizationParameters.deserialize(parser, parameters);
                    break;
                case TOPOLOGICAL_ACTIONS_OPTIMIZATION:
                    parser.nextToken();
                    JsonTopoOptimizationParameters.deserialize(parser, parameters);
                    break;
                case MULTI_THREADING:
                    parser.nextToken();
                    JsonMultiThreadingParameters.deserialize(parser, parameters);
                    break;
                case SECOND_PREVENTIVE_RAO:
                    parser.nextToken();
                    JsonSecondPreventiveRaoParameters.deserialize(parser, parameters);
                    break;
                case LOAD_FLOW_AND_SENSITIVITY_COMPUTATION:
                    parser.nextToken();
                    JsonLoadFlowAndSensitivityComputationParameters.deserialize(parser, parameters);
                    break;
                default:
                    throw new OpenRaoException("Unexpected field in open rao search tree parameters: " + parser.getCurrentName());
            }
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return SEARCH_TREE_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super OpenRaoSearchTreeParameters> getExtensionClass() {
        return OpenRaoSearchTreeParameters.class;
    }

}
