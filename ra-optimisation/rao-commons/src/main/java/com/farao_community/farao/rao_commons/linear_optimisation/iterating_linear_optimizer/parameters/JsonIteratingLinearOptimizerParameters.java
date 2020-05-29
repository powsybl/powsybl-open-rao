/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.parameters;

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
    public void serialize(IteratingLinearOptimizerParameters linearRaoParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("max-number-of-iterations", linearRaoParameters.getMaxIterations());
        jsonGenerator.writeEndObject();
    }

    @Override
    public IteratingLinearOptimizerParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        IteratingLinearOptimizerParameters parameters = new IteratingLinearOptimizerParameters();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "max-number-of-iterations":
                    jsonParser.nextToken();
                    parameters.setMaxIterations(jsonParser.getIntValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
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
