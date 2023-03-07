/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonRangeActionsOptimizationParameters {

    private JsonRangeActionsOptimizationParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(RANGE_ACTIONS_OPTIMIZATION);
        jsonGenerator.writeNumberField(MAX_MIP_ITERATIONS, parameters.getRangeActionsOptimizationParameters().getMaxMipIterations());
        jsonGenerator.writeNumberField(PST_PENALTY_COST, parameters.getRangeActionsOptimizationParameters().getPstPenaltyCost());
        jsonGenerator.writeNumberField(PST_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getPstSensitivityThreshold());
        jsonGenerator.writeObjectField(PST_MODEL, parameters.getRangeActionsOptimizationParameters().getPstModel());
        jsonGenerator.writeNumberField(HVDC_PENALTY_COST, parameters.getRangeActionsOptimizationParameters().getHvdcPenaltyCost());
        jsonGenerator.writeNumberField(HVDC_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getHvdcSensitivityThreshold());
        jsonGenerator.writeNumberField(INJECTION_RA_PENALTY_COST, parameters.getRangeActionsOptimizationParameters().getInjectionRaPenaltyCost());
        jsonGenerator.writeNumberField(INJECTION_RA_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getInjectionRaSensitivityThreshold());
        jsonGenerator.writeObjectFieldStart(LINEAR_OPTIMIZATION_SOLVER);
        jsonGenerator.writeObjectField(SOLVER, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolver());
        jsonGenerator.writeNumberField(RELATIVE_MIP_GAP, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getRelativeMipGap());
        jsonGenerator.writeStringField(SOLVER_SPECIFIC_PARAMETERS, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolverSpecificParameters());
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case MAX_MIP_ITERATIONS:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setMaxMipIterations(jsonParser.getIntValue());
                    break;
                case PST_PENALTY_COST:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setPstPenaltyCost(jsonParser.getDoubleValue());
                    break;
                case PST_SENSITIVITY_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(jsonParser.getDoubleValue());
                    break;
                case PST_MODEL:
                    raoParameters.getRangeActionsOptimizationParameters().setPstModel(stringToPstModel(jsonParser.nextTextValue()));
                    break;
                case HVDC_PENALTY_COST:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setHvdcPenaltyCost(jsonParser.getDoubleValue());
                    break;
                case HVDC_SENSITIVITY_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(jsonParser.getDoubleValue());
                    break;
                case INJECTION_RA_PENALTY_COST:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setInjectionRaPenaltyCost(jsonParser.getDoubleValue());
                    break;
                case INJECTION_RA_SENSITIVITY_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(jsonParser.getDoubleValue());
                    break;
                case LINEAR_OPTIMIZATION_SOLVER:
                    jsonParser.nextToken();
                    deserializeLinearOptimizationSolver(jsonParser, raoParameters);
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize range action optimization parameters: unexpected field in %s (%s)", RANGE_ACTIONS_OPTIMIZATION, jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeLinearOptimizationSolver(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case SOLVER:
                    raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolver(stringToSolver(jsonParser.nextTextValue()));
                    break;
                case RELATIVE_MIP_GAP:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setRelativeMipGap(jsonParser.getDoubleValue());
                    break;
                case SOLVER_SPECIFIC_PARAMETERS:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolverSpecificParameters(jsonParser.getValueAsString());
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize linear optimization solver in range action optimization parameters: unexpected field in %s (%s)", LINEAR_OPTIMIZATION_SOLVER, jsonParser.getCurrentName()));
            }
        }
    }

    private static RangeActionsOptimizationParameters.PstModel stringToPstModel(String string) {
        try {
            return RangeActionsOptimizationParameters.PstModel.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown Pst model: %s", string));
        }
    }

    private static RangeActionsOptimizationParameters.Solver stringToSolver(String string) {
        try {
            return RangeActionsOptimizationParameters.Solver.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown solver: %s", string));
        }
    }
}
