/*
 * Copyright (c) 2019, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoParameters;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersDeserializer extends StdDeserializer<RaoParameters> {

    RaoParametersDeserializer() {
        super(RaoParameters.class);
    }

    @Override
    public RaoParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new RaoParameters());
    }

    @Override
    public RaoParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, RaoParameters parameters) throws IOException {

        List<Extension<RaoParameters>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "version":
                    parser.nextToken();
                    break;
                case "rao-with-loop-flow-limitation":
                    parser.nextToken();
                    parameters.setRaoWithLoopFlowLimitation(parser.getBooleanValue());
                    break;
                case "loopflow-approximation":
                    parser.nextToken();
                    parameters.setLoopflowApproximation(parser.getBooleanValue());
                    break;
                case "loopflow-constraint-adjustment-coefficient":
                    parser.nextToken();
                    parameters.setLoopflowConstraintAdjustmentCoefficient(parser.getDoubleValue());
                    break;
                case "extensions":
                    parser.nextToken();
                    extensions = JsonUtil.readExtensions(parser, deserializationContext, JsonRaoParameters.getExtensionSerializers());
                    break;
                default:
                    throw new AssertionError("Unexpected field: " + parser.getCurrentName());
            }
        }

        JsonRaoParameters.getExtensionSerializers().addExtensions(parameters, extensions);
        return parameters;
    }

}
