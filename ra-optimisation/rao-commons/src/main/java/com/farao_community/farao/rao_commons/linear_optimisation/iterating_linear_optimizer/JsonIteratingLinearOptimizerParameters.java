/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

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
public class JsonIteratingLinearOptimizerParameters implements JsonRaoParameters.ExtensionSerializer<IteratingLinearOptimizerParameters> {

    @Override
    public void serialize(IteratingLinearOptimizerParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("max-number-of-iterations", parameters.getMaxIterations());
        jsonGenerator.writeBooleanField("loopflow-approximation", parameters.isLoopflowApproximation());
        jsonGenerator.writeEndObject();
    }

    @Override
    public IteratingLinearOptimizerParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        IteratingLinearOptimizerParameters parameters = new IteratingLinearOptimizerParameters();

        while (!parser.nextToken().isStructEnd()) {
            switch (parser.getCurrentName()) {
                case "max-number-of-iterations":
                    parser.nextToken();
                    parameters.setMaxIterations(parser.getIntValue());
                    break;
                case "loopflow-approximation":
                    parser.nextToken();
                    parameters.setLoopflowApproximation(parser.getBooleanValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + parser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "IteratingLinearOptimizerParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super IteratingLinearOptimizerParameters> getExtensionClass() {
        return IteratingLinearOptimizerParameters.class;
    }
}
