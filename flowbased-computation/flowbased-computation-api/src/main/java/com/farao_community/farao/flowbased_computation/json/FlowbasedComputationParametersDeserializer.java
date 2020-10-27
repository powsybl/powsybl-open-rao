/*
 * Copyright (c) 2018, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationParameters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowbasedComputationParametersDeserializer extends StdDeserializer<FlowbasedComputationParameters> {

    FlowbasedComputationParametersDeserializer() {
        super(FlowbasedComputationParameters.class);
    }

    @Override
    public FlowbasedComputationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new FlowbasedComputationParameters());
    }

    @Override
    public FlowbasedComputationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, FlowbasedComputationParameters parameters) throws IOException {

        List<Extension<FlowbasedComputationParameters>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {

                case "version":
                    parser.nextToken();
                    break;

                case "load-flow-parameters":
                    parser.nextToken();
                    JsonLoadFlowParameters.deserialize(parser, deserializationContext, parameters.getLoadFlowParameters());
                    break;

                case "extensions":
                    parser.nextToken();
                    extensions = JsonUtil.readExtensions(parser, deserializationContext, JsonFlowbasedComputationParameters.getExtensionSerializers());
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + parser.getCurrentName());
            }
        }

        JsonFlowbasedComputationParameters.getExtensionSerializers().addExtensions(parameters, extensions);

        return parameters;
    }

}
