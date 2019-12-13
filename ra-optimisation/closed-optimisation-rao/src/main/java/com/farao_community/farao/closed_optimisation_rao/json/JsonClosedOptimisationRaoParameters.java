/*
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
        jsonGenerator.writeStringField("solver-type", closedOptimisationRaoParameters.getSolverType());
        jsonGenerator.writeNumberField("relative-mip-gap", closedOptimisationRaoParameters.getRelativeMipGap());
        jsonGenerator.writeNumberField("max-time-in-seconds", closedOptimisationRaoParameters.getMaxTimeInSeconds());
        jsonGenerator.writeNumberField("overload-penalty-cost", closedOptimisationRaoParameters.getOverloadPenaltyCost());
        jsonGenerator.writeNumberField("redispatching-sensitivity-threshold", closedOptimisationRaoParameters.getRdSensitivityThreshold());
        jsonGenerator.writeNumberField("pst-sensitivity-threshold", closedOptimisationRaoParameters.getPstSensitivityThreshold());
        jsonGenerator.writeNumberField("number-of-parallel-threads", closedOptimisationRaoParameters.getNumberOfParallelThreads());
        jsonGenerator.writeObjectField("problem-fillers", closedOptimisationRaoParameters.getFillersList());
        jsonGenerator.writeObjectField("pre-processors", closedOptimisationRaoParameters.getPreProcessorsList());
        jsonGenerator.writeObjectField("post-processors", closedOptimisationRaoParameters.getPostProcessorsList());
        jsonGenerator.writeEndObject();
    }

    @Override
    public ClosedOptimisationRaoParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ClosedOptimisationRaoParameters parameters = new ClosedOptimisationRaoParameters();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "solver-type":
                    parameters.setSolverType(jsonParser.nextTextValue());
                    break;
                case "relative-mip-gap":
                    jsonParser.nextToken();
                    parameters.setRelativeMipGap(jsonParser.getDoubleValue());
                    break;
                case "max-time-in-seconds":
                    jsonParser.nextToken();
                    parameters.setMaxTimeInSeconds(jsonParser.getDoubleValue());
                    break;
                case "overload-penalty-cost":
                    jsonParser.nextToken();
                    parameters.setOverloadPenaltyCost(jsonParser.getDoubleValue());
                    break;
                case "redispatching-sensitivity-threshold":
                    jsonParser.nextToken();
                    parameters.setRdSensitivityThreshold(jsonParser.getDoubleValue());
                    break;
                case "pst-sensitivity-threshold":
                    jsonParser.nextToken();
                    parameters.setPstSensitivityThreshold(jsonParser.getDoubleValue());
                    break;
                case "number-of-parallel-threads":
                    jsonParser.nextToken();
                    parameters.setNumberOfParallelThreads(jsonParser.getIntValue());
                    break;
                case "problem-fillers":
                    jsonParser.nextToken();
                    parameters.addAllFillers(jsonParser.readValueAs(new TypeReference<ArrayList<String>>() {
                    }));
                    break;
                case "pre-processors":
                    jsonParser.nextToken();
                    parameters.addAllPreProcessors(jsonParser.readValueAs(new TypeReference<ArrayList<String>>() {
                    }));
                    break;
                case "post-processors":
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
