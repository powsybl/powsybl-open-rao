/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.core;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonLinearProblemParameters implements JsonRaoParameters.ExtensionSerializer<LinearProblemParameters> {

    @Override
    public void serialize(LinearProblemParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("pst-penalty-cost", parameters.getPstPenaltyCost());
        jsonGenerator.writeNumberField("pst-sensitivity-threshold", parameters.getPstSensitivityThreshold());
        jsonGenerator.writeObjectField("objective-function", parameters.getObjectiveFunction());
        jsonGenerator.writeNumberField("loopflow-constraint-adjustment-coefficient", parameters.getLoopflowConstraintAdjustmentCoefficient());
        jsonGenerator.writeEndObject();
    }

    @Override
    public LinearProblemParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        LinearProblemParameters parameters = new LinearProblemParameters();

        while (!parser.nextToken().isStructEnd()) {
            switch (parser.getCurrentName()) {
                case "pst-penalty-cost":
                    parser.nextToken();
                    parameters.setPstPenaltyCost(parser.getDoubleValue());
                    break;
                case "pst-sensitivity-threshold":
                    parser.nextToken();
                    parameters.setPstSensitivityThreshold(parser.getDoubleValue());
                    break;
                case "objective-function":
                    parameters.setObjectiveFunction(stringToObjectiveFunction(parser.nextTextValue()));
                    break;
                case "loopflow-constraint-adjustment-coefficient":
                    parser.nextToken();
                    parameters.setLoopflowConstraintAdjustmentCoefficient(parser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + parser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "LinearProblemParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LinearProblemParameters> getExtensionClass() {
        return LinearProblemParameters.class;
    }

    private LinearProblemParameters.ObjectiveFunction stringToObjectiveFunction(String string) {
        try {
            return LinearProblemParameters.ObjectiveFunction.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown objective function value : %s", string));
        }
    }
}
