/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
final class JsonRangeActionsOptimizationParameters {

    private JsonRangeActionsOptimizationParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(RANGE_ACTIONS_OPTIMIZATION);
        jsonGenerator.writeNumberField(MAX_MIP_ITERATIONS, parameters.getRangeActionsOptimizationParameters().getMaxMipIterations());
        jsonGenerator.writeNumberField(PST_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getPstSensitivityThreshold());
        jsonGenerator.writeObjectField(PST_MODEL, parameters.getRangeActionsOptimizationParameters().getPstModel());
        jsonGenerator.writeNumberField(HVDC_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getHvdcSensitivityThreshold());
        jsonGenerator.writeNumberField(INJECTION_RA_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getInjectionRaSensitivityThreshold());
        jsonGenerator.writeObjectField(RA_RANGE_SHRINKING, parameters.getRangeActionsOptimizationParameters().getRaRangeShrinking());
        jsonGenerator.writeObjectFieldStart(LINEAR_OPTIMIZATION_SOLVER);
        jsonGenerator.writeObjectField(SOLVER, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolver());
        jsonGenerator.writeNumberField(RELATIVE_MIP_GAP, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getRelativeMipGap());
        jsonGenerator.writeStringField(SOLVER_SPECIFIC_PARAMETERS, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolverSpecificParameters());
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case MAX_MIP_ITERATIONS -> {
                    jsonParser.nextToken();
                    searchTreeParameters.getRangeActionsOptimizationParameters().setMaxMipIterations(jsonParser.getIntValue());
                }
                case PST_SENSITIVITY_THRESHOLD -> {
                    jsonParser.nextToken();
                    searchTreeParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(jsonParser.getDoubleValue());
                }
                case PST_MODEL -> searchTreeParameters.getRangeActionsOptimizationParameters().setPstModel(stringToPstModel(jsonParser.nextTextValue()));
                case HVDC_SENSITIVITY_THRESHOLD -> {
                    jsonParser.nextToken();
                    searchTreeParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(jsonParser.getDoubleValue());
                }
                case INJECTION_RA_SENSITIVITY_THRESHOLD -> {
                    jsonParser.nextToken();
                    searchTreeParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(jsonParser.getDoubleValue());
                }
                case LINEAR_OPTIMIZATION_SOLVER -> {
                    jsonParser.nextToken();
                    deserializeLinearOptimizationSolver(jsonParser, searchTreeParameters);
                }
                case RA_RANGE_SHRINKING -> searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(stringToRaRangeShrinking(jsonParser.nextTextValue()));
                default -> throw new OpenRaoException(String.format("Cannot deserialize range action optimization parameters: unexpected field in %s (%s)", RANGE_ACTIONS_OPTIMIZATION, jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeLinearOptimizationSolver(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case SOLVER -> searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolver(stringToSolver(jsonParser.nextTextValue()));
                case RELATIVE_MIP_GAP -> {
                    jsonParser.nextToken();
                    searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setRelativeMipGap(jsonParser.getDoubleValue());
                }
                case SOLVER_SPECIFIC_PARAMETERS -> {
                    jsonParser.nextToken();
                    searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolverSpecificParameters(jsonParser.getValueAsString());
                }
                default -> throw new OpenRaoException(String.format("Cannot deserialize linear optimization solver in range action optimization parameters: unexpected field in %s (%s)", LINEAR_OPTIMIZATION_SOLVER, jsonParser.getCurrentName()));
            }
        }
    }

    private static SearchTreeRaoRangeActionsOptimizationParameters.PstModel stringToPstModel(String string) {
        try {
            return SearchTreeRaoRangeActionsOptimizationParameters.PstModel.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Unknown Pst model: %s", string));
        }
    }

    private static SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking stringToRaRangeShrinking(String string) {
        try {
            return SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Unknown Pst variation range shrinking: %s", string));
        }
    }

    private static SearchTreeRaoRangeActionsOptimizationParameters.Solver stringToSolver(String string) {
        try {
            return SearchTreeRaoRangeActionsOptimizationParameters.Solver.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Unknown solver: %s", string));
        }
    }
}
