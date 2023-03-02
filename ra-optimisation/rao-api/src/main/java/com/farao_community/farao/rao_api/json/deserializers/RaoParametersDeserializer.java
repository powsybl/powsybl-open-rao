/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersDeserializer extends StdDeserializer<RaoParameters> {

    public RaoParametersDeserializer() {
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
                case VERSION:
                    parser.nextToken();
                    break;
                case OBJECTIVE_FUNCTION:
                    parser.nextToken();
                    ObjectiveFunctionParametersDeserializer.deserialize(parser, parameters);
                    break;
                case RANGE_ACTIONS_OPTIMIZATION:
                    parser.nextToken();
                    RangeActionsOptimizationParametersDeserializer.deserialize(parser, parameters);
                    break;
                case TOPOLOGICAL_ACTIONS_OPTIMIZATION:
                    parser.nextToken();
                    TopoOptimizationParametersDeserializer.deserialize(parser, parameters);
                    break;
                case MULTI_THREADING:
                    parser.nextToken();
                    MultiThreadingParametersDeserializer.deserialize(parser, parameters);
                    break;
                case SECOND_PREVENTIVE_RAO:
                    parser.nextToken();
                    SecondPreventiveRaoParametersDeserializer.deserialize(parser, parameters);
                    break;
                case RA_USAGE_LIMITS_PER_CONTINGENCY:
                    parser.nextToken();
                    RaUsageLimitsPerContingencyParametersDeserializer.deserialize(parser, parameters);
                    break;
                case NOT_OPTIMIZED_CNECS:
                    parser.nextToken();
                    NotOptimizedCnecsParametersDeserializer.deserialize(parser, parameters);
                    break;
                case LOAD_FLOW_AND_SENSITIVITY_COMPUTATION:
                    parser.nextToken();
                    LoadFlowAndSensitivityComputationParametersDeserializer.deserialize(parser, parameters);
                    break;
                case "extensions":
                    parser.nextToken();
                    extensions = JsonUtil.updateExtensions(parser, deserializationContext, JsonRaoParameters.getExtensionSerializers(), parameters);
                    break;
                default:
                    throw new FaraoException("Unexpected field in rao parameters: " + parser.getCurrentName());
            }
        }
        extensions.forEach(extension -> parameters.addExtension((Class) extension.getClass(), extension));
        return parameters;
    }
}
