/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.json;

import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoResult;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(JsonRaoComputationResult.ExtensionSerializer.class)
public class JsonClosedOptimisationRaoResult implements JsonRaoComputationResult.ExtensionSerializer<ClosedOptimisationRaoResult> {
    @Override
    public void serialize(ClosedOptimisationRaoResult resultExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("solverInfo", resultExtension.getSolverInfo());
        jsonGenerator.writeObjectField("variableInfos", resultExtension.getVariableInfos());
        jsonGenerator.writeObjectField("constraintInfos", resultExtension.getConstraintInfos());
        jsonGenerator.writeObjectField("objectiveInfo", resultExtension.getObjectiveInfo());
        jsonGenerator.writeEndObject();
    }

    @Override
    public ClosedOptimisationRaoResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ClosedOptimisationRaoResult resultExtension = new ClosedOptimisationRaoResult();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "solverInfo":
                    jsonParser.nextToken();
                    deserializeSolverInfo(jsonParser, deserializationContext, resultExtension);
                    break;
                case "variableInfos":
                    while (!jsonParser.nextToken().isStructEnd()) {
                        // Skip variable name for the map key, and if map is empty, return
                        if (!jsonParser.nextValue().isStructEnd()) {
                            deserializeVariableInfo(jsonParser, deserializationContext, resultExtension);
                        } else {
                            break;
                        }
                    }
                    break;
                case "constraintInfos":
                    while (!jsonParser.nextToken().isStructEnd()) {
                        // Skip constraint name for the map key, and if map is empty, return
                        if (!jsonParser.nextValue().isStructEnd()) {
                            deserializeConstraintInfo(jsonParser, deserializationContext, resultExtension);
                        } else {
                            break;
                        }
                    }
                    break;
                case "objectiveInfo":
                    jsonParser.nextToken();
                    deserializeObjectiveInfo(jsonParser, deserializationContext, resultExtension);
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return resultExtension;
    }

    private void deserializeSolverInfo(JsonParser jsonParser, DeserializationContext deserializationContext, ClosedOptimisationRaoResult result) throws IOException {
        Optional<Integer> numVariables = Optional.empty();
        Optional<Integer> numConstraints = Optional.empty();
        Optional<Long> numIterations = Optional.empty();
        Optional<Long> wallTime = Optional.empty();
        Optional<String> status = Optional.empty();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "numVariables":
                    numVariables = Optional.of(jsonParser.getValueAsInt());
                    break;
                case "numConstraints":
                    numConstraints = Optional.of(jsonParser.getValueAsInt());
                    break;
                case "numIterations":
                    numIterations = Optional.of(jsonParser.getValueAsLong());
                    break;
                case "wallTime":
                    wallTime = Optional.of(jsonParser.getValueAsLong());
                    break;
                case "status":
                    status = Optional.of(jsonParser.getValueAsString());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        if (numVariables.isPresent() &&
                numConstraints.isPresent() &&
                numIterations.isPresent() &&
                wallTime.isPresent() &&
                status.isPresent()) {
            result.setSolverInfo(
                    numVariables.get(),
                    numConstraints.get(),
                    numIterations.get(),
                    wallTime.get(),
                    status.get()
            );
        } else {
            throw new FaraoException("Incomplete SolverInfo block");
        }
    }

    private void deserializeVariableInfo(JsonParser jsonParser, DeserializationContext deserializationContext, ClosedOptimisationRaoResult result) throws IOException {
        Optional<String> name = Optional.empty();
        Optional<Double> solutionValue = Optional.empty();
        Optional<Double> lb = Optional.empty();
        Optional<Double> ub = Optional.empty();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "name":
                    name = Optional.of(jsonParser.getValueAsString());
                    break;
                case "solutionValue":
                    solutionValue = Optional.of(jsonParser.getValueAsDouble());
                    break;
                case "lb":
                    lb = Optional.of(jsonParser.getValueAsDouble());
                    break;
                case "ub":
                    ub = Optional.of(jsonParser.getValueAsDouble());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        if (name.isPresent() &&
                solutionValue.isPresent() &&
                lb.isPresent() &&
                ub.isPresent()) {
            result.addVariableInfo(
                    name.get(),
                    solutionValue.get(),
                    lb.get(),
                    ub.get()
            );
        } else {
            throw new FaraoException("Incomplete VariableInfo block");
        }
    }

    private void deserializeConstraintInfo(JsonParser jsonParser, DeserializationContext deserializationContext, ClosedOptimisationRaoResult result) throws IOException {
        Optional<String> name = Optional.empty();
        Optional<Double> dualValue = Optional.empty();
        Optional<Boolean> isLazy = Optional.empty();
        Optional<Double> lb = Optional.empty();
        Optional<Double> ub = Optional.empty();
        Optional<String> basisStatus = Optional.empty();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "name":
                    name = Optional.of(jsonParser.getValueAsString());
                    break;
                case "dualValue":
                    dualValue = Optional.of(jsonParser.getValueAsDouble());
                    break;
                case "lazy":
                    isLazy = Optional.of(jsonParser.getValueAsBoolean());
                    break;
                case "lb":
                    lb = Optional.of(jsonParser.getValueAsDouble());
                    break;
                case "ub":
                    ub = Optional.of(jsonParser.getValueAsDouble());
                    break;
                case "basisStatus":
                    basisStatus = Optional.of(jsonParser.getValueAsString());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        if (name.isPresent() &&
                dualValue.isPresent() &&
                isLazy.isPresent() &&
                lb.isPresent() &&
                ub.isPresent() &&
                basisStatus.isPresent()) {
            result.addConstraintInfo(
                    name.get(),
                    dualValue.get(),
                    isLazy.get(),
                    lb.get(),
                    ub.get(),
                    basisStatus.get()
            );
        } else {
            throw new FaraoException("Incomplete ConstraintInfo block");
        }
    }

    private void deserializeObjectiveInfo(JsonParser jsonParser, DeserializationContext deserializationContext, ClosedOptimisationRaoResult result) throws IOException {
        Optional<Boolean> maximization = Optional.empty();
        Optional<Double> value = Optional.empty();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "maximization":
                    maximization = Optional.of(jsonParser.getValueAsBoolean());
                    break;
                case "value":
                    value = Optional.of(jsonParser.getValueAsDouble());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        if (maximization.isPresent() &&
                value.isPresent()) {
            result.setObjectiveInfo(
                    maximization.get(),
                    value.get()
            );
        } else {
            throw new FaraoException("Incomplete ObjectiveInfo block");
        }
    }

    @Override
    public String getExtensionName() {
        return "ClosedOptimisationRaoResult";
    }

    @Override
    public String getCategoryName() {
        return "rao-computation-result";
    }

    @Override
    public Class<? super ClosedOptimisationRaoResult> getExtensionClass() {
        return ClosedOptimisationRaoResult.class;
    }
}
