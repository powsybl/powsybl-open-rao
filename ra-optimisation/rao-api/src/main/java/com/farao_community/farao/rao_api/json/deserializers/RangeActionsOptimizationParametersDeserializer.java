/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class RangeActionsOptimizationParametersDeserializer {

    private RangeActionsOptimizationParametersDeserializer() {
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
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize range action optimization parameters: unexpected field in %s (%s)", RANGE_ACTIONS_OPTIMIZATION, jsonParser.getCurrentName()));
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