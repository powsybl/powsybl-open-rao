/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import com.powsybl.sensitivity.json.JsonSensitivityComputationParameters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Json deserializer for flow decomposition parameters
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionParametersDeserializer extends StdDeserializer<FlowDecompositionParameters> {

    FlowDecompositionParametersDeserializer() {
        super(FlowDecompositionParameters.class);
    }

    @Override
    public FlowDecompositionParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new FlowDecompositionParameters());
    }

    @Override
    public FlowDecompositionParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, FlowDecompositionParameters parameters) throws IOException {

        List<Extension<FlowDecompositionParameters>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {

                case "version":
                    parser.nextToken();
                    break;

                case "load-flow-parameters":
                    parser.nextToken();
                    JsonLoadFlowParameters.deserialize(parser, deserializationContext, parameters.getLoadFlowParameters());
                    break;

                case "sensitivity-parameters":
                    parser.nextToken();
                    JsonSensitivityComputationParameters.deserialize(parser, deserializationContext, parameters.getSensitivityComputationParameters());
                    break;

                case "extensions":
                    parser.nextToken();
                    extensions = JsonUtil.readExtensions(parser, deserializationContext, JsonFlowDecompositionParameters.getExtensionSerializers());
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + parser.getCurrentName());
            }
        }

        JsonFlowDecompositionParameters.getExtensionSerializers().addExtensions(parameters, extensions);

        return parameters;
    }

}
