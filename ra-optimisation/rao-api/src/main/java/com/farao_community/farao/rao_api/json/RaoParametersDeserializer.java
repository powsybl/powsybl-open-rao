/*
 * Copyright (c) 2019, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.json.JsonSensitivityComputationParameters;

import java.io.IOException;
import java.util.ArrayList;
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
                case "objective-function":
                    parameters.setObjectiveFunction(stringToObjectiveFunction(parser.nextTextValue()));
                    break;
                case "max-number-of-iterations":
                    parser.nextToken();
                    parameters.setMaxIterations(parser.getIntValue());
                    break;
                case "pst-penalty-cost":
                    parser.nextToken();
                    parameters.setPstPenaltyCost(parser.getDoubleValue());
                    break;
                case "pst-sensitivity-threshold":
                    parser.nextToken();
                    parameters.setPstSensitivityThreshold(parser.getDoubleValue());
                    break;
                case "sensitivity-fallback-over-cost":
                    parser.nextToken();
                    parameters.setFallbackOverCost(parser.getDoubleValue());
                    break;
                case "rao-with-loop-flow-limitation":
                    parser.nextToken();
                    parameters.setRaoWithLoopFlowLimitation(parser.getBooleanValue());
                    break;
                case "loop-flow-approximation":
                    parser.nextToken();
                    parameters.setLoopFlowApproximation(parser.getBooleanValue());
                    break;
                case "loop-flow-constraint-adjustment-coefficient":
                    parser.nextToken();
                    parameters.setLoopFlowConstraintAdjustmentCoefficient(parser.getDoubleValue());
                    break;
                case "loop-flow-violation-cost":
                    parser.nextToken();
                    parameters.setLoopFlowViolationCost(parser.getDoubleValue());
                    break;
                case "loop-flow-countries":
                    parser.nextToken();
                    List<String> countryStrings = new ArrayList<>();
                    ArrayNode node = (new ObjectMapper()).readTree(parser);
                    for (Object o : node) {
                        countryStrings.add(o.toString().replaceAll("\"", ""));
                    }
                    parameters.setLoopflowCountries(countryStrings);
                    break;
                case "mnec-acceptable-margin-diminution":
                    parser.nextToken();
                    parameters.setMnecAcceptableMarginDiminution(parser.getDoubleValue());
                    break;
                case "mnec-violation-cost":
                    parser.nextToken();
                    parameters.setMnecViolationCost(parser.getDoubleValue());
                    break;
                case "mnec-constraint-adjustment-coefficient":
                    parser.nextToken();
                    parameters.setMnecConstraintAdjustmentCoefficient(parser.getDoubleValue());
                    break;
                case "negative-margin-objective-coefficient":
                    parser.nextToken();
                    parameters.setNegativeMarginObjectiveCoefficient(parser.getDoubleValue());
                    break;
                case "ptdf-sum-lower-bound":
                    parser.nextToken();
                    parameters.setPtdfSumLowerBound(parser.getDoubleValue());
                    break;
                case "sensitivity-parameters":
                    parser.nextToken();
                    JsonSensitivityComputationParameters.deserialize(parser, deserializationContext, parameters.getDefaultSensitivityComputationParameters());
                    break;
                case "fallback-sensitivity-parameters":
                    parser.nextToken();
                    if (parameters.getFallbackSensitivityComputationParameters() == null) {
                        parameters.setFallbackSensitivityComputationParameters(new SensitivityComputationParameters());
                    }
                    JsonSensitivityComputationParameters.deserialize(parser, deserializationContext, parameters.getFallbackSensitivityComputationParameters());
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

    private RaoParameters.ObjectiveFunction stringToObjectiveFunction(String string) {
        try {
            return RaoParameters.ObjectiveFunction.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown objective function value : %s", string));
        }
    }
}
