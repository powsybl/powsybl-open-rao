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
    public void serialize(LinearProblemParameters linearRaoParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("pst-penalty-cost", linearRaoParameters.getPstPenaltyCost());
        jsonGenerator.writeNumberField("pst-sensitivity-threshold", linearRaoParameters.getPstSensitivityThreshold());
        jsonGenerator.writeEndObject();
    }

    @Override
    public LinearProblemParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LinearProblemParameters parameters = new LinearProblemParameters();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "pst-penalty-cost":
                    jsonParser.nextToken();
                    parameters.setPstPenaltyCost(jsonParser.getDoubleValue());
                    break;
                case "pst-sensitivity-threshold":
                    jsonParser.nextToken();
                    parameters.setPstSensitivityThreshold(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
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
}
