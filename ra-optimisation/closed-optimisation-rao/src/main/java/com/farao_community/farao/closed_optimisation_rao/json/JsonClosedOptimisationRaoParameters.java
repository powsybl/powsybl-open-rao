/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.json;

import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoParameters;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(JsonRaoComputationParameters.ExtensionSerializer.class)
public class JsonClosedOptimisationRaoParameters implements JsonRaoComputationParameters.ExtensionSerializer<ClosedOptimisationRaoParameters> {
    @Override
    public void serialize(ClosedOptimisationRaoParameters closedOptimisationRaoParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("solverType", closedOptimisationRaoParameters.getSolverType());
        jsonGenerator.writeObjectField("problemFillers", closedOptimisationRaoParameters.getFillersList());
        jsonGenerator.writeObjectField("preProcessors", closedOptimisationRaoParameters.getPreProcessorsList());
        jsonGenerator.writeObjectField("postProcessors", closedOptimisationRaoParameters.getPostProcessorsList());
        jsonGenerator.writeEndObject();
    }

    @Override
    public ClosedOptimisationRaoParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ClosedOptimisationRaoParameters parameters = new ClosedOptimisationRaoParameters();

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {
                case "solverType":
                    parameters.setSolverType(jsonParser.nextTextValue());
                    break;
                case "problemFillers":
                    jsonParser.nextToken();
                    parameters.addAllFillers(jsonParser.readValueAs(new TypeReference<ArrayList<String>>() {
                    }));
                    break;
                case "preProcessors":
                    jsonParser.nextToken();
                    parameters.addAllPreProcessors(jsonParser.readValueAs(new TypeReference<ArrayList<String>>() {
                    }));
                    break;
                case "postProcessors":
                    jsonParser.nextToken();
                    parameters.addAllPostProcessors(jsonParser.readValueAs(new TypeReference<ArrayList<String>>() {
                    }));
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "ClosedOptimisationRaoParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-computation-parameters";
    }

    @Override
    public Class<? super ClosedOptimisationRaoParameters> getExtensionClass() {
        return ClosedOptimisationRaoParameters.class;
    }
}
