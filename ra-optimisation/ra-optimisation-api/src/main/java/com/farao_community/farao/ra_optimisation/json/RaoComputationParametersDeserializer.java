/*
 * Copyright (c) 2018, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.farao_community.farao.ra_optimisation.RaoComputationParameters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class RaoComputationParametersDeserializer extends StdDeserializer<RaoComputationParameters> {

    RaoComputationParametersDeserializer() {
        super(RaoComputationParameters.class);
    }

    @Override
    public RaoComputationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new RaoComputationParameters());
    }

    @Override
    public RaoComputationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, RaoComputationParameters parameters) throws IOException {

        List<Extension<RaoComputationParameters>> extensions = Collections.emptyList();
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
                    extensions = JsonUtil.readExtensions(parser, deserializationContext, JsonRaoComputationParameters.getExtensionSerializers());
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + parser.getCurrentName());
            }
        }

        JsonRaoComputationParameters.getExtensionSerializers().addExtensions(parameters, extensions);

        return parameters;
    }

}
